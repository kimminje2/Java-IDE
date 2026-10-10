package javaide;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class JavaFileManager {

    public boolean fileExists(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return false;
        }
        return new File(fileName).isFile();
    }

    public boolean isJavaFile(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".java");
    }

    public String readFile(String fileName) {
        StringBuilder result = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append(System.lineSeparator());
            }
        } catch (IOException e) {
            return "Error: failed to read file - " + fileName;
        }

        return result.toString();
    }

    public boolean deleteFile(String fileName) {
        File file = new File(fileName);
        return !file.exists() || file.delete();
    }

    public String getErrorFileName(String javaFileName) {
        return javaFileName + ".error";
    }

    public String getDisplayFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        return new File(fileName).getName();
    }

    public String readSourceWithLineNumbers(String fileName) {
        StringBuilder result = new StringBuilder();
        int lineNumber = 1;

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(String.format("%4d | %s%n", lineNumber, line));
                lineNumber++;
            }
        } catch (IOException e) {
            return "Error: failed to read file - " + fileName;
        }

        return result.toString();
    }

    public String readSourceLines(String fileName, String errorLineNumbers) {
        ArrayList<Integer> targetLines = parseLineNumbers(errorLineNumbers);

        if (targetLines.isEmpty()) {
            return "Error: no compile error file";
        }

        StringBuilder result = new StringBuilder();
        int lineNumber = 1;

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (targetLines.contains(lineNumber)) {
                    result.append("Line ")
                          .append(lineNumber)
                          .append(" : ")
                          .append(line)
                          .append(System.lineSeparator());
                }
                lineNumber++;
            }
        } catch (IOException e) {
            return "Error: failed to read file - " + fileName;
        }

        if (result.length() == 0) {
            return "Error: no compile error file";
        }

        return result.toString();
    }

    private ArrayList<Integer> parseLineNumbers(String errorLineNumbers) {
        ArrayList<Integer> result = new ArrayList<>();

        if (errorLineNumbers == null || errorLineNumbers.isBlank()) {
            return result;
        }

        String[] numbers = errorLineNumbers.split(",");
        for (String number : numbers) {
            try {
                result.add(Integer.parseInt(number.trim()));
            } catch (NumberFormatException ignored) {
                // 잘못된 값은 무시한다.
            }
        }

        return result;
    }
}
