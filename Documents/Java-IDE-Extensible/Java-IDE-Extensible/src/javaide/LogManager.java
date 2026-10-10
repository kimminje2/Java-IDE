package javaide;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogManager {
    private static final String LOG_FILE_NAME = "ide.log";
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void writeLog(String message) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE_NAME, true))) {
            writer.write("[" + getTimestamp() + "] " + message);
            writer.newLine();
        } catch (IOException ignored) {
            // 로그 실패 때문에 IDE가 비정상 종료되지 않도록 한다.
        }
    }

    public String readLog() {
        File logFile = new File(LOG_FILE_NAME);
        if (!logFile.exists()) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append(System.lineSeparator());
            }
        } catch (IOException e) {
            return "Error: failed to read log";
        }

        return result.toString();
    }

    private String getTimestamp() {
        return LocalDateTime.now().format(TIME_FORMAT);
    }
}
