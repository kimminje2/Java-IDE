package javaide;

public class Main {

	public static void main(String[] args) {

		// 전체 기능 관리 Controller 생성
		IDEController controller = new IDEController();

		// 상호작용 담당 Console UI 생성
		ConsoleUI consoleUI = new ConsoleUI(controller);

		// 메뉴 시작
		consoleUI.start();
	}
}
