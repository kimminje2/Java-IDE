package javaide;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 외부 java 프로세스 실행과 출력 수집 담당. 화면 컴포넌트는 참조하지 않는다.
public class ProgramRunner {
    private static final Pattern PACKAGE = Pattern.compile("(?m)^\\s*package\\s+([a-zA-Z_$][\\w$]*(?:\\.[a-zA-Z_$][\\w$]*)*)\\s*;");

    // 출력이 도착했을 때 호출할 메서드 하나를 약속한다.
    // GUI에서는 ProgramOutputListener 클래스가 이 인터페이스를 구현한다.
    public interface OutputListener {
        void onOutput(String text);
    }

    // 기존 호출은 표준 출력/에러를 구분하여 반환하고 5초 제한을 적용한다.
    public RunResult run(String fileName, String arguments) {
        return run(fileName, arguments, null, true);
    }

    public RunResult run(String fileName, String arguments, OutputListener listener, boolean timeLimit) {
        Path source = Path.of(fileName).toAbsolutePath().normalize();
        String className;
        try {
            className = findClassName(source);
        } catch (IOException e) {
            return RunResult.notExecuted("Error: failed to read source - " + e.getMessage());
        }

        List<String> command = createCommand(source, className, arguments);

        long started = System.nanoTime();
        Process process = null;
        StreamCollector output = null;
        StreamCollector error = null;
        Thread outputThread = null;
        Thread errorThread = null;
        try {
            // Run 직후 Stop을 눌러도 새 프로세스를 남기지 않는다.
            if (Thread.currentThread().isInterrupted()) {
                return RunResult.stopped("", "", -1, 0);
            }
            ProcessBuilder builder = new ProcessBuilder(command);
            // 공용 결과창에서는 두 스트림을 합쳐 도착 순서대로 읽는다.
            builder.redirectErrorStream(listener != null);
            process = builder.start();
            process.getOutputStream().close(); // 현재 버전은 대화형 stdin을 제공하지 않는다.
            if (listener != null) {
                listener.onOutput(">>> running " + className + " ...\n");
            }
            output = new StreamCollector(process.getInputStream(), listener);
            error = new StreamCollector(process.getErrorStream(), null);
            outputThread = new Thread(output, "javaide-stdout");
            errorThread = new Thread(error, "javaide-stderr");
            outputThread.start();
            errorThread.start();

            // 1. 프로그램 종료(또는 시간 제한)를 기다린다.
            boolean finished = waitForProgram(process, timeLimit);
            // 2. 종료 직전의 출력까지 모두 읽은 뒤 결과를 만든다.
            waitForOutput(outputThread, errorThread);
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            if (!finished) {
                return RunResult.timedOut(output.text(), error.text(), process.exitValue(), elapsed);
            }
            return RunResult.executed(output.text(), error.text(), process.exitValue(), elapsed);
        } catch (InterruptedException e) {
            // 13장 interrupt: 대기를 깨운 뒤 실제 자식 프로세스도 종료한다.
            int exitCode = stopInterruptedProgram(process, outputThread, errorThread);
            Thread.currentThread().interrupt();
            String standardOutput = "";
            if (output != null) {
                standardOutput = output.text();
            }
            String errorOutput = "";
            if (error != null) {
                errorOutput = error.text();
            }
            return RunResult.stopped(standardOutput, errorOutput,
                    exitCode, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
        } catch (IOException e) {
            if (process != null) {
                process.destroyForcibly();
            }
            return RunResult.notExecuted("Error: failed to execute program - " + e.getMessage());
        }
    }

    // package가 있으면 "패키지명.클래스명", 없으면 "클래스명"을 반환한다.
    private String findClassName(Path source) throws IOException {
        String className = source.getFileName().toString().replaceFirst("\\.java$", "");
        String text = Files.readString(source, StandardCharsets.UTF_8);
        Matcher matcher = PACKAGE.matcher(text);
        if (matcher.find()) {
            className = matcher.group(1) + "." + className;
        }
        return className;
    }

    private List<String> createCommand(Path source, String className, String arguments) {
        String executable = "java";
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            executable = "java.exe";
        }
        String java = Path.of(System.getProperty("java.home"), "bin", executable).toString();
        List<String> command = new ArrayList<>();
        command.add(java);
        command.add("-Dfile.encoding=UTF-8");
        command.add("-Dstdout.encoding=UTF-8");
        command.add("-Dstderr.encoding=UTF-8");
        command.add("-cp");
        command.add(source.getParent().toString());
        command.add(className);
        if (arguments != null && !arguments.trim().isEmpty()) {
            for (String argument : arguments.trim().split("\\s+")) {
                command.add(argument);
            }
        }

        return command;
    }

    // true: 종료를 확인함(성공 여부는 종료 코드로 판단), false: 5초 제한으로 강제 종료함.
    private boolean waitForProgram(Process process, boolean timeLimit) throws InterruptedException {
        if (!timeLimit) {
            process.waitFor();
            return true;
        }
        boolean finished = process.waitFor(5, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            process.waitFor();
        }
        return finished;
    }

    // join()은 해당 출력 수집 스레드의 run()이 끝날 때까지 기다린다.
    private void waitForOutput(Thread outputThread, Thread errorThread) throws InterruptedException {
        if (outputThread != null) {
            outputThread.join();
        }
        if (errorThread != null) {
            errorThread.join();
        }
    }

    // 스레드 interrupt만으로 자식 java 프로세스가 종료되지는 않는다.
    private int stopInterruptedProgram(Process process, Thread outputThread, Thread errorThread) {
        int exitCode = -1;
        if (process == null) {
            return exitCode;
        }
        process.destroyForcibly();
        try {
            process.waitFor();
            exitCode = process.exitValue();
            waitForOutput(outputThread, errorThread);
        } catch (InterruptedException again) {
            // 이미 프로세스에 강제 종료를 요청했다. 호출한 곳에서 중단 상태를 복원한다.
        }
        return exitCode;
    }

    private static class StreamCollector implements Runnable {
        private static final int LIMIT = 65536;
        private final InputStream stream;
        private final OutputListener listener;
        private final StringBuilder content = new StringBuilder();
        private boolean truncated;

        StreamCollector(InputStream stream, OutputListener listener) {
            this.stream = stream;
            this.listener = listener;
        }

        @Override
        public void run() {
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                char[] buffer = new char[4096];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    // 줄바꿈을 기다리지 않아 print() 출력도 실행 중 즉시 전달된다.
                    if (listener != null) {
                        listener.onOutput(new String(buffer, 0, read));
                    }
                    int keep = Math.min(read, Math.max(0, LIMIT - content.length()));
                    if (keep > 0) {
                        content.append(buffer, 0, keep);
                    }
                    if (keep < read) {
                        truncated = true;
                    }
                }
            } catch (IOException e) {
                // 강제 종료 후 스트림이 닫힌 경우에도 이미 읽은 출력은 보존한다.
            }
        }

        String text() {
            return content.toString() + (truncated ? "\n[output truncated]" : "");
        }
    }
}
