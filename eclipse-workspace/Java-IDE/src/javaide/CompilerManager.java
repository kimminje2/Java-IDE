package javaide;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompilerManager {
    private static final Pattern ERROR_LINE =
            Pattern.compile("\\.java:(\\d+): error:");
    private final JavaFileManager fileManager;

    public CompilerManager(JavaFileManager fileManager) {
        this.fileManager = fileManager;
    }

    public CompileResult compile(String fileName) {
        long started = System.nanoTime();
        String errorFileName = fileManager.getErrorFileName(fileName);
        String errorText;
        int exitCode = -1;
        try {
            Path source = Path.of(fileName).toAbsolutePath().normalize();
            Path parent = source.getParent();
            String javac = Path.of(System.getProperty("java.home"), "bin",
                    executable("javac")).toString();
            Process process = new ProcessBuilder(javac, "-encoding", "UTF-8",
                    "-proc:none", "-cp", parent.toString(), "-d",
                    parent.toString(), source.toString())
                    .redirectErrorStream(true).start();
            errorText = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            exitCode = process.waitFor();
        } catch (IOException e) {
            errorText = "javac 실행 실패: " + e.getMessage() + System.lineSeparator();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorText = "컴파일 대기 중 중단됨" + System.lineSeparator();
        }
        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        if (exitCode == 0) {
            fileManager.deleteFile(errorFileName);
            return CompileResult.success(elapsed);
        }

        fileManager.writeFile(errorFileName, errorText);
        Matcher matcher = ERROR_LINE.matcher(errorText);
        List<Integer> lines = new ArrayList<>();
        while (matcher.find()) lines.add(Integer.parseInt(matcher.group(1)));
        StringBuilder numbers = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) numbers.append(", ");
            numbers.append(lines.get(i));
        }
        String source = fileManager.readFile(fileName);
        String context = source == null ? "" : SourceCodeFormatter.errorLines(source, lines);
        return CompileResult.failure(lines.size(), numbers.toString(),
                errorFileName, context, elapsed);
    }

    private static String executable(String name) {
        return System.getProperty("os.name").toLowerCase().contains("win")
                ? name + ".exe" : name;
    }
}
