package javaide;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RecentFileManager {
    private static final Path HISTORY_FILE = Path.of("recent-uploads.txt");
    private static final int MAX_ENTRIES = 10;

    public void record(String fileName) {
        List<String> entries = readEntries();
        entries.remove(fileName);
        entries.add(0, fileName);
        if (entries.size() > MAX_ENTRIES) entries = entries.subList(0, MAX_ENTRIES);
        try {
            Files.write(HISTORY_FILE, entries, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Error: failed to save recent uploads - " + e.getMessage());
        }
    }

    public List<String> readEntries() {
        try {
            return new ArrayList<>(Files.readAllLines(HISTORY_FILE, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }
}
