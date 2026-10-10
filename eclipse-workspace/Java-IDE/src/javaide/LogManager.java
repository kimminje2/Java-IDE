package javaide;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogManager {
    private static final Path LOG_FILE = Path.of("ide.log");
    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void writeLog(String message) {
        String line = "[" + LocalDateTime.now().format(FORMAT) + "] "
                + message + System.lineSeparator();
        try {
            Files.writeString(LOG_FILE, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Error: failed to write log - " + e.getMessage());
        }
    }

    public String readLog() {
        try {
            return Files.readString(LOG_FILE, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "Error: no log file";
        }
    }

    public boolean clearLog() {
        try {
            Files.writeString(LOG_FILE, "", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
