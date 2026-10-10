package javaide;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProgramRunner {
    private static final int TIMEOUT_SECONDS = 5;
    private static final Pattern PACKAGE = Pattern.compile(
            "(?m)^\\s*package\\s+([a-zA-Z_$][\\w$]*(?:\\.[a-zA-Z_$][\\w$]*)*)\\s*;");

    public RunResult run(String fileName, String arguments) {
        Path source = Path.of(fileName).toAbsolutePath().normalize();
        Path parent = source.getParent();
        String className = source.getFileName().toString().replaceFirst("\\.java$", "");
        try {
            String content = java.nio.file.Files.readString(source, StandardCharsets.UTF_8);
            Matcher matcher = PACKAGE.matcher(content);
            if (matcher.find()) className = matcher.group(1) + "." + className;
        } catch (IOException e) {
            return RunResult.notExecuted("Error: failed to read source - " + e.getMessage());
        }

        String java = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").toLowerCase().contains("win")
                        ? "java.exe" : "java").toString();
        List<String> command = new ArrayList<>();
        command.add(java);
        command.add("-cp");
        command.add(parent.toString());
        command.add(className);
        if (arguments != null && !arguments.trim().isEmpty()) {
            for (String argument : arguments.trim().split("\\s+")) command.add(argument);
        }

        long started = System.nanoTime();
        Process process = null;
        try {
            process = new ProcessBuilder(command).start();
            StreamCollector output = new StreamCollector(process.getInputStream());
            StreamCollector error = new StreamCollector(process.getErrorStream());
            Thread outputThread = new Thread(output, "javaide-stdout");
            Thread errorThread = new Thread(error, "javaide-stderr");
            outputThread.start();
            errorThread.start();
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                process.waitFor();
            }
            outputThread.join();
            errorThread.join();
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            if (!finished) return RunResult.timedOut(output.text(), error.text(), elapsed);
            return RunResult.executed(output.text(), error.text(), process.exitValue(), elapsed);
        } catch (IOException e) {
            return RunResult.notExecuted("Error: failed to execute program - " + e.getMessage());
        } catch (InterruptedException e) {
            if (process != null) process.destroyForcibly();
            Thread.currentThread().interrupt();
            return RunResult.notExecuted("Error: program execution interrupted");
        }
    }

    private static final class StreamCollector implements Runnable {
        private static final int LIMIT = 65536;
        private final InputStream stream;
        private final StringBuilder content = new StringBuilder();
        private boolean truncated;

        StreamCollector(InputStream stream) { this.stream = stream; }

        @Override
        public void run() {
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                char[] buffer = new char[4096];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    int keep = Math.min(read, Math.max(0, LIMIT - content.length()));
                    if (keep > 0) content.append(buffer, 0, keep);
                    if (keep < read) truncated = true;
                }
            } catch (IOException e) {
                if (!truncated) content.append("Output read failed: ").append(e.getMessage());
            }
        }

        String text() {
            return content.toString() + (truncated ? System.lineSeparator() + "[output truncated]" : "");
        }
    }
}
