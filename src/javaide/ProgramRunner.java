package javaide;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ProgramRunner {

    public RunResult run(String fileName, String arguments) {
        File javaFile = new File(fileName).getAbsoluteFile();

        String className = getClassName(javaFile.getName());

        File parentDirectory = javaFile.getParentFile();

        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-cp");
        command.add(parentDirectory.getAbsolutePath());
        command.add(className);

        command.addAll(parseArguments(arguments));

        try {
            ProcessBuilder processBuilder =
                    new ProcessBuilder(command);

            Process process = processBuilder.start();

            StreamCollector outputCollector =
                    new StreamCollector(process.getInputStream());

            StreamCollector errorCollector =
                    new StreamCollector(process.getErrorStream());

            Thread outputThread = new Thread(outputCollector);
            Thread errorThread = new Thread(errorCollector);

            outputThread.start();
            errorThread.start();

            int exitCode = process.waitFor();

            outputThread.join();
            errorThread.join();

            return RunResult.executed(
                    outputCollector.getText(),
                    errorCollector.getText(),
                    exitCode
            );

        } catch (IOException e) {
            return RunResult.notExecuted(
                    "Error: failed to execute program"
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            return RunResult.notExecuted(
                    "Error: program execution interrupted"
            );
        }
    }

    private String getClassName(String fileName) {
        return fileName.substring(
                0,
                fileName.length() - ".java".length()
        );
    }

    /*
     * 공백으로 인자를 구분한다.
     * 따옴표로 감싼 문자열은 하나의 인자로 처리한다.
     * 예) 10000 "Kim Minje"
     */
    private List<String> parseArguments(String arguments) {
        List<String> result = new ArrayList<>();

        if (arguments == null || arguments.trim().isEmpty()) {
            return result;
        }

        StringBuilder current = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < arguments.length(); i++) {
            char c = arguments.charAt(i);

            if (c == '"') {
                insideQuotes = !insideQuotes;
                continue;
            }

            if (Character.isWhitespace(c) && !insideQuotes) {
                if (current.length() > 0) {
                    result.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            result.add(current.toString());
        }

        return result;
    }

    private static class StreamCollector implements Runnable {

        private final InputStream inputStream;
        private final StringBuilder text;

        StreamCollector(InputStream inputStream) {
            this.inputStream = inputStream;
            this.text = new StringBuilder();
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream))) {

                String line;

                while ((line = reader.readLine()) != null) {
                    text.append(line).append(System.lineSeparator());
                }

            } catch (IOException e) {
                text.append("Error: failed to read process output")
                        .append(System.lineSeparator());
            }
        }

        String getText() {
            return text.toString();
        }
    }
}
