package javaide;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class SourceCodeFormatter {
    private SourceCodeFormatter() { }

    public static String numbered(String source) {
        String[] lines = source.split("\\R", -1);
        int count = lines.length;
        if (count > 1 && lines[count - 1].isEmpty()) count--;
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < count; i++) {
            output.append(i + 1).append(" | ").append(lines[i])
                    .append(System.lineSeparator());
        }
        return output.toString();
    }

    public static String errorLines(String source, Collection<Integer> lineNumbers) {
        String[] lines = source.split("\\R", -1);
        Set<Integer> wanted = new HashSet<>(lineNumbers);
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (wanted.contains(i + 1)) {
                output.append(i + 1).append(" | ").append(lines[i])
                        .append(System.lineSeparator());
                output.append("    ^ compile error").append(System.lineSeparator());
            }
        }
        return output.toString();
    }
}
