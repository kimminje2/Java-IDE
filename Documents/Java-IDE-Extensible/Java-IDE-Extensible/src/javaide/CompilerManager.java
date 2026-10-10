package javaide;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class CompilerManager {

    private final JavaFileManager fileManager;

    public CompilerManager(JavaFileManager fileManager) {
        this.fileManager = fileManager;
    }

    public CompileResult compile(String fileName) {
        File sourceFile = new File(fileName).getAbsoluteFile();
        File workingDirectory = sourceFile.getParentFile();
        String displayFileName = sourceFile.getName();
        String errorFileName = fileManager.getErrorFileName(sourceFile.getPath());

        long startTime = System.nanoTime();

        try {
            ProcessBuilder processBuilder = new ProcessBuilder("javac", sourceFile.getName());
            processBuilder.directory(workingDirectory);

            Process process = processBuilder.start();

            String errorText = readStream(process.getErrorStream());
            // javac은 일반적으로 stdout을 사용하지 않지만 스트림을 비워 둔다.
            readStream(process.getInputStream());

            int exitCode = process.waitFor();
            long compileTimeMs = (System.nanoTime() - startTime) / 1_000_000;

            if (exitCode == 0) {
                fileManager.deleteFile(errorFileName);
                return CompileResult.success(displayFileName, compileTimeMs);
            }

            ErrorInfo errorInfo = parseErrorInfo(errorText);
            saveErrorFile(errorFileName, errorText);

            return CompileResult.failure(
                    displayFileName,
                    errorInfo.errorCount,
                    errorInfo.errorLineNumbers,
                    errorText,
                    new File(errorFileName).getName(),
                    compileTimeMs
            );

        } catch (IOException e) {
            long compileTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            String errorText = "Failed to execute javac: " + e.getMessage();
            saveErrorFile(errorFileName, errorText);

            return CompileResult.failure(
                    displayFileName,
                    1,
                    "",
                    errorText,
                    new File(errorFileName).getName(),
                    compileTimeMs
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            long compileTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            String errorText = "Compilation interrupted.";
            saveErrorFile(errorFileName, errorText);

            return CompileResult.failure(
                    displayFileName,
                    1,
                    "",
                    errorText,
                    new File(errorFileName).getName(),
                    compileTimeMs
            );
        }
    }

    private String readStream(java.io.InputStream stream) throws IOException {
        StringBuilder result = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append(System.lineSeparator());
            }
        }

        return result.toString();
    }

    private void saveErrorFile(String errorFileName, String errorText) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(errorFileName))) {
            writer.write(errorText);
        } catch (IOException ignored) {
            // error 파일 저장 실패가 IDE 자체를 종료시키지 않도록 한다.
        }
    }

    private ErrorInfo parseErrorInfo(String errorText) {
        int errorCount = 0;
        ArrayList<Integer> uniqueLineNumbers = new ArrayList<>();

        String[] lines = errorText.split("\\R");

        for (String line : lines) {
            int javaIndex = line.indexOf(".java:");
            if (javaIndex == -1) {
                continue;
            }

            int numberStart = javaIndex + ".java:".length();
            int numberEnd = line.indexOf(':', numberStart);

            if (numberEnd == -1) {
                continue;
            }

            String numberText = line.substring(numberStart, numberEnd).trim();
            String rest = line.substring(numberEnd + 1).trim();

            // javac의 일반적인 오류 형식: File.java:3: error: ...
            if (!rest.startsWith("error:")) {
                continue;
            }

            try {
                int lineNumber = Integer.parseInt(numberText);
                errorCount++;
                if (!uniqueLineNumbers.contains(lineNumber)) {
                    uniqueLineNumbers.add(lineNumber);
                }
            } catch (NumberFormatException ignored) {
                // 파싱할 수 없는 라인은 건너뛴다.
            }
        }

        StringBuilder lineNumbers = new StringBuilder();
        for (int i = 0; i < uniqueLineNumbers.size(); i++) {
            if (i > 0) {
                lineNumbers.append(", ");
            }
            lineNumbers.append(uniqueLineNumbers.get(i));
        }

        // 드문 환경에서 오류 문구 형식이 달라도 실패 자체는 1건 이상으로 표시한다.
        if (errorCount == 0 && !errorText.isBlank()) {
            errorCount = 1;
        }

        return new ErrorInfo(errorCount, lineNumbers.toString());
    }

    private static class ErrorInfo {
        private final int errorCount;
        private final String errorLineNumbers;

        private ErrorInfo(int errorCount, String errorLineNumbers) {
            this.errorCount = errorCount;
            this.errorLineNumbers = errorLineNumbers;
        }
    }
}
