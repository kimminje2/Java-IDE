package javaide;

import java.util.Scanner;

public class ConsoleUI {
    private final Scanner scanner;
    private final IDEController controller;

    public ConsoleUI(IDEController controller) {
        this.controller = controller;
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        boolean running = true;

        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();
            printSeparator();

            switch (choice) {
                case "1":
                    handleUpload();
                    break;
                case "2":
                    handleCompile();
                    break;
                case "3":
                    handleRun();
                    break;
                case "4":
                    System.out.println(controller.reset());
                    break;
                case "5":
                    System.out.print(controller.getCompileError());
                    break;
                case "6":
                    System.out.print(controller.getLog());
                    break;
                case "7":
                    System.out.println(controller.exit());
                    running = false;
                    break;
                case "8":
                    System.out.print(controller.getSourcePreview());
                    break;
                case "9":
                    System.out.print(controller.getErrorSourcePreview());
                    break;
                case "10":
                    handleCompileAndRun();
                    break;
                default:
                    System.out.println("Error: invalid menu choice");
                    break;
            }
        }

        scanner.close();
    }

    private void handleUpload() {
        System.out.print("Type Java Filename: ");
        String fileName = scanner.nextLine();
        System.out.println(controller.uploadFile(fileName));
    }

    private void handleCompile() {
        CompileResult result = controller.compileFile();
        printCompileResult(result);
    }

    private void handleRun() {
        System.out.print("Type Program Arguments (Enter for none): ");
        String arguments = scanner.nextLine();

        RunResult result = controller.runProgram(arguments);
        printRunResult(result);
    }

    private void handleCompileAndRun() {
        CompileResult compileResult = controller.compileFile();
        printCompileResult(compileResult);

        if (!compileResult.isAttempted() || !compileResult.isSuccess()) {
            return;
        }

        System.out.print("Type Program Arguments (Enter for none): ");
        String arguments = scanner.nextLine();
        RunResult runResult = controller.runProgram(arguments);
        printRunResult(runResult);
    }

    private void printCompileResult(CompileResult result) {
        if (!result.isAttempted()) {
            System.out.println(result.getMessage());
            return;
        }

        if (result.isSuccess()) {
            System.out.println("compiled successfully - " + result.getFileName());
        } else {
            String lines = result.getErrorLineNumbers();
            if (lines.isBlank()) {
                lines = "unknown";
            }

            System.out.println(
                    result.getErrorCount()
                    + " compile errors occurred (lines "
                    + lines
                    + ") - "
                    + result.getErrorFileName());
        }

        System.out.println("Compile Time: " + result.getCompileTimeMs() + " ms");
    }

    private void printRunResult(RunResult result) {
        if (!result.isExecuted()) {
            System.out.println(result.getMessage());
            return;
        }

        if (!result.getStandardOutput().isBlank()) {
            System.out.print(result.getStandardOutput());
        }

        if (!result.getErrorOutput().isBlank()) {
            System.err.print(result.getErrorOutput());
        }

        System.out.println("Exit Code: " + result.getExitCode());
        System.out.println("Execution Time: " + result.getExecutionTimeMs() + " ms");
    }

    private void printMenu() {
        printSeparator();
        System.out.println("1. Java File Upload");
        System.out.println("2. Compile");
        System.out.println("3. Run");
        System.out.println("4. Reset");
        System.out.println("5. Compile Error File");
        System.out.println("6. Show Log");
        System.out.println("7. Exit");
        System.out.println("8. Source Code Preview        [Extra]");
        System.out.println("9. Error Source Preview       [Extra]");
        System.out.println("10. Compile & Run             [Extra]");
        System.out.print("Choice: ");
    }

    private void printSeparator() {
        System.out.println("##########################");
    }
}
