package javaide;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CompilerManager {

    private final JavaFileManager fileManager;

    public CompilerManager(JavaFileManager fileManager) {
        this.fileManager = fileManager;
    }

    public CompileResult compile(String fileName) {
        String errorFileName = fileManager.getErrorFileName(fileName);

        try {
            ProcessBuilder processBuilder =
                    new ProcessBuilder("javac", fileName);

            Process process = processBuilder.start();

            StringBuilder errorTextBuilder = new StringBuilder();

            try (BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {

                String line;

                while ((line = errorReader.readLine()) != null) {
                    errorTextBuilder.append(line)
                            .append(System.lineSeparator());
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                fileManager.deleteFile(errorFileName);

                return CompileResult.success();
            }

            String errorText = errorTextBuilder.toString();
            int errorCount = parseErrorCount(errorText);
            String errorLineNumbers = parseErrorLineNumbers(errorText);

            fileManager.writeFile(errorFileName, errorText);

            return CompileResult.failure(
                    errorCount,
                    errorLineNumbers,
                    errorFileName
            );

        } catch (IOException e) {
            String errorText =
                    "Error: failed to execute javac."
                    + System.lineSeparator()
                    + e.getMessage();

            fileManager.writeFile(errorFileName, errorText);

            return CompileResult.failure(
                    1,
                    "",
                    errorFileName
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            String errorText = "Error: compile process interrupted.";
            fileManager.writeFile(errorFileName, errorText);

            return CompileResult.failure(
                    1,
                    "",
                    errorFileName
            );
        }
    }

    private int parseErrorCount(String errorText) {
        int errorCount = 0;

        String[] lines = errorText.split("\\R");

        for (String line : lines) {
            if (isCompileErrorLine(line)) {
                errorCount++;
            }
        }

        return errorCount;
    }

    private String parseErrorLineNumbers(String errorText) {
        StringBuilder lineNumbers = new StringBuilder();

        String[] lines = errorText.split("\\R");

        for (String line : lines) {
            if (!isCompileErrorLine(line)) {
                continue;
            }

            int javaIndex = line.indexOf(".java:");
            int numberStart = javaIndex + ".java:".length();
            int numberEnd = line.indexOf(':', numberStart);

            if (javaIndex == -1 || numberEnd == -1) {
                continue;
            }

            String lineNumber =
                    line.substring(numberStart, numberEnd).trim();

            if (!lineNumber.matches("\\d+")) {
                continue;
            }

            if (lineNumbers.length() > 0) {
                lineNumbers.append(", ");
            }

            lineNumbers.append(lineNumber);
        }

        return lineNumbers.toString();
    }

    private boolean isCompileErrorLine(String line) {
        return line.contains(".java:")
                && line.contains(": error:");
    }
}
