package javaide;

public class Main {

    public static void main(String[] args) {
        IDEController controller = new IDEController();
        ConsoleUI consoleUI = new ConsoleUI(controller);

        consoleUI.start();
    }
}
