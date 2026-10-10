package javaide;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class JavaFileManager {
    public boolean fileExists(String fileName) {
        return fileName != null && !fileName.isEmpty()
                && Files.isRegularFile(Path.of(fileName));
    }

    public boolean isJavaFile(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".java");
    }

    public String readFile(String fileName) {
        try {
            return Files.readString(Path.of(fileName), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    public boolean writeFile(String fileName, String content) {
        try {
            Files.writeString(Path.of(fileName), content, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean deleteFile(String fileName) {
        if (fileName == null) return false;
        try {
            Files.deleteIfExists(Path.of(fileName));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public String getErrorFileName(String javaFileName) {
        return javaFileName + ".error";
    }
}
