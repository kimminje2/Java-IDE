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
        File sourceFile = new File(fileName).getAbsoluteFile();
        File workingDirectory = sourceFile.getParentFile();
        String className = getClassName(sourceFile.getName());

        List<String> command = new ArrayList<>();
        command.add("java");
        command.add(className);
        command.addAll(parseArguments(arguments));

        long startTime = System.nanoTime();

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(workingDirectory);

            Process process = processBuilder.start();

            StringBuilder standardOutput = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();

            Thread outputThread = new Thread(
                    () -> readStream(process.getInputStream(), standardOutput));
            Thread errorThread = new Thread(
                    () -> readStream(process.getErrorStream(), errorOutput));

            outputThread.start();
            errorThread.start();

            int exitCode = process.waitFor();
            outputThread.join();
            errorThread.join();

            long executionTimeMs = (System.nanoTime() - startTime) / 1_000_000;

            return RunResult.executed(
                    standardOutput.toString(),
                    errorOutput.toString(),
                    exitCode,
                    executionTimeMs
            );

        } catch (IOException e) {
            return RunResult.notExecuted(
                    "Error: failed to run program - " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return RunResult.notExecuted("Error: program execution interrupted");
        }
    }

    private String getClassName(String fileName) {
        return fileName.substring(0, fileName.length() - ".java".length());
    }

    private void readStream(InputStream stream, StringBuilder output) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        } catch (IOException e) {
            output.append("Stream read error: ")
                  .append(e.getMessage())
                  .append(System.lineSeparator());
        }
    }

    private List<String> parseArguments(String arguments) {
        ArrayList<String> result = new ArrayList<>();

        if (arguments == null || arguments.isBlank()) {
            return result;
        }

        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteCharacter = 0;

        for (int i = 0; i < arguments.length(); i++) {
            char ch = arguments.charAt(i);

            if ((ch == '"' || ch == '\'') && !inQuotes) {
                inQuotes = true;
                quoteCharacter = ch;
            } else if (inQuotes && ch == quoteCharacter) {
                inQuotes = false;
            } else if (Character.isWhitespace(ch) && !inQuotes) {
                if (current.length() > 0) {
                    result.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(ch);
            }
        }

        if (current.length() > 0) {
            result.add(current.toString());
        }

        return result;
    }
}
