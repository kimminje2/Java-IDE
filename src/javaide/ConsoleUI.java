package javaide;

import java.util.List;
import java.util.Scanner;

public class ConsoleUI {
    private final Scanner scanner = new Scanner(System.in);
    private final IDEController controller;

    public ConsoleUI(IDEController controller) {
        this.controller = controller;
    }

    public void start() {
        boolean running = true;
        while (running) {
            printMenu();
            if (!scanner.hasNextLine()) break;
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    System.out.print("Type Java Filename: ");
                    if (scanner.hasNextLine()) System.out.println(controller.uploadFile(scanner.nextLine().trim()));
                    break;
                case "2":
                    System.out.println(controller.compileFile());
                    break;
                case "3":
                    handleRun();
                    break;
                case "4":
                    System.out.println(controller.reset());
                    break;
                case "5":
                    showAndWait(controller.getCompileError());
                    break;
                case "6":
                    handleLog();
                    break;
                case "7":
                    System.out.println(controller.exit());
                    running = false;
                    break;
                case "8":
                    handleSourcePreview();
                    break;
                case "9":
                    showAndWait(controller.getRecentUploads());
                    break;
                default:
                    System.out.println("Error: invalid menu choice");
            }
            System.out.println();
        }
    }

    private void showAndWait(String content) {
        System.out.println(content);
        waitForEnter();
    }

    private void waitForEnter() {
        while (true) {
            System.out.print("Press Enter to return to menu: ");
            if (!scanner.hasNextLine() || scanner.nextLine().trim().isEmpty()) return;
        }
    }

    private void handleSourcePreview() {
        List<String> files = controller.getPreviewFiles();
        if (files.isEmpty()) {
            showAndWait("No recent uploads");
            return;
        }
        System.out.println("Select source file to preview:");
        for (int i = 0; i < files.size(); i++) {
            System.out.println((i + 1) + ". " + files.get(i));
        }
        while (true) {
            System.out.print("Select file number (Enter to return): ");
            if (!scanner.hasNextLine()) return;
            String choice = scanner.nextLine().trim();
            if (choice.isEmpty()) return;
            try {
                int index = Integer.parseInt(choice) - 1;
                if (index >= 0 && index < files.size()) {
                    showAndWait(controller.getSourcePreview(files.get(index)));
                    return;
                }
            } catch (NumberFormatException e) {
                // Stay in the selection screen for invalid input.
            }
            System.out.println("Error: invalid file choice");
        }
    }

    private void handleLog() {
        printLog();
        while (true) {
            System.out.print("Enter: return to menu / C: clear log: ");
            if (!scanner.hasNextLine()) return;
            String choice = scanner.nextLine().trim();
            if (choice.isEmpty()) return;
            if (choice.equalsIgnoreCase("c")) {
                System.out.println(controller.clearLog());
                printLog();
            } else {
                System.out.println("Type C to clear the log, or press Enter to return.");
            }
        }
    }

    private void printLog() {
        String log = controller.getLog();
        System.out.println(log.isEmpty() ? "Log is empty" : log);
    }

    private void handleRun() {
        System.out.print("Type Program Arguments (Enter for none): ");
        if (!scanner.hasNextLine()) return;
        RunResult result = controller.runProgram(scanner.nextLine());
        if (!result.isExecuted()) {
            System.out.println(result.getMessage());
            return;
        }
        if (!result.getStandardOutput().isEmpty()) {
            System.out.print(result.getStandardOutput());
            if (!result.getStandardOutput().endsWith("\n")) System.out.println();
        }
        if (!result.getErrorOutput().isEmpty()) {
            System.err.print(result.getErrorOutput());
            if (!result.getErrorOutput().endsWith("\n")) System.err.println();
        }
        if (result.isTimedOut()) {
            System.out.println(result.getMessage());
        } else {
            System.out.println("Exit Code: " + result.getExitCode());
        }
        System.out.println("Run time: " + result.getRunDurationMillis() + " ms");
    }

    private void printMenu() {
        System.out.println("##########################");
        System.out.println("1. Java File Upload");
        System.out.println("2. Compile");
        System.out.println("3. Run");
        System.out.println("4. Reset");
        System.out.println("5. Compile Error File");
        System.out.println("6. Show Log");
        System.out.println("7. Exit");
        System.out.println("8. Source Code Preview");
        System.out.println("9. Recent Uploads");
        System.out.print("Choice: ");
    }
}
