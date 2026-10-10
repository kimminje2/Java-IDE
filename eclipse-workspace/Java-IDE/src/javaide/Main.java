package javaide;

public class Main {

    public static void main(String[] args) {

        // 전체 기능을 관리하는 Controller 생성
        IDEController controller = new IDEController();

        // 콘솔 UI 생성
        // Controller를 전달하여 각 기능을 호출할 수 있게 함
        ConsoleUI consoleUI = new ConsoleUI(controller);

        // 메뉴 프로그램 시작
        consoleUI.start();
    }
}
