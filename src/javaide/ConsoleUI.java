package javaide;

import java.util.Scanner;

public class ConsoleUI {

    private final Scanner scanner;
    private final IDEController controller;

    public ConsoleUI(IDEController controller) {
        this.scanner = new Scanner(System.in);
        this.controller = controller;
    }

    public void start() {
        boolean running = true;

        while (running) {
            printMenu();

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    handleUpload();
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
                    System.out.println(controller.getCompileError());
                    break;

                case "6":
                    System.out.println(controller.getLog());
                    break;

                case "7":
                    System.out.println(controller.exit());
                    running = false;
                    break;

                default:
                    System.out.println("Error: invalid menu choice");
                    break;
            }

            System.out.println();
        }

        scanner.close();
    }

    private void handleUpload() {
        System.out.print("Type Java Filename: ");
        String fileName = scanner.nextLine().trim();

        System.out.println(controller.uploadFile(fileName));
    }

    private void handleRun() {
        System.out.print("Type Program Arguments (Enter for none): ");
        String arguments = scanner.nextLine();

        RunResult result = controller.runProgram(arguments);

        if (!result.isExecuted()) {
            System.out.println(result.getMessage());
            return;
        }

        if (!result.getStandardOutput().isEmpty()) {
            System.out.print(result.getStandardOutput());
            if (!result.getStandardOutput().endsWith("\n")) {
                System.out.println();
            }
        }

        if (!result.getErrorOutput().isEmpty()) {
            System.err.print(result.getErrorOutput());
            if (!result.getErrorOutput().endsWith("\n")) {
                System.err.println();
            }
        }

        System.out.println("Exit Code: " + result.getExitCode());
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
        System.out.print("Choice: ");
    }
}
