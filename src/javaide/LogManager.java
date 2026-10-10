package javaide;

import java.io.BufferedReader;
import java.io.BufferedWriter;
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
        try (BufferedWriter writer =
                     new BufferedWriter(new FileWriter(LOG_FILE_NAME, true))) {

            writer.write(
                    "[" + getTimestamp() + "] " + message
            );

            writer.newLine();

        } catch (IOException e) {
            System.err.println("Error: failed to write log");
        }
    }

    public String readLog() {
        StringBuilder log = new StringBuilder();

        try (BufferedReader reader =
                     new BufferedReader(new FileReader(LOG_FILE_NAME))) {

            String line;

            while ((line = reader.readLine()) != null) {
                log.append(line).append(System.lineSeparator());
            }

            return log.toString();

        } catch (IOException e) {
            return "";
        }
    }

    private String getTimestamp() {
        return LocalDateTime.now().format(TIME_FORMAT);
    }
}
