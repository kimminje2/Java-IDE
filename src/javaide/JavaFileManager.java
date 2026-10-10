package javaide;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class JavaFileManager {

    public boolean fileExists(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }

        File file = new File(fileName);
        return file.isFile();
    }

    public boolean isJavaFile(String fileName) {
        return fileName != null
                && fileName.toLowerCase().endsWith(".java");
    }

    public String readFile(String fileName) {
        StringBuilder content = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }

            return content.toString();

        } catch (IOException e) {
            return null;
        }
    }

    public boolean writeFile(String fileName, String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(content);
            return true;

        } catch (IOException e) {
            return false;
        }
    }

    public boolean deleteFile(String fileName) {
        if (fileName == null) {
            return false;
        }

        File file = new File(fileName);

        return !file.exists() || file.delete();
    }

    public String getErrorFileName(String javaFileName) {
        return javaFileName + ".error";
    }
}
