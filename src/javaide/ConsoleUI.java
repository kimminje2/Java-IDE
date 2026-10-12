package javaide;

import java.util.List;
import java.util.Scanner;

// 메뉴 입력, 파일 선택, 기능 결과 출력 담당
public class ConsoleUI {
	// 콘솔 입력을 줄 단위로 읽음
	private final Scanner scanner = new Scanner(System.in);
	// 실제 기능 호출 Controller
	private final IDEController controller;
	// Main에서 생성한 Controller 전달받음
	public ConsoleUI(IDEController controller) {
		this.controller = controller;
	}

	// 메인 메뉴 반복 : 기능 처리는 Controller에 요청
	public void start() {
		// true일 동안 메뉴 반복
		boolean running = true;

		while (running) {
			// 메뉴 표시
			printMenu();

			// 입력 스트림 종료(EOF)이면 종료, 그 외에는 메뉴 번호 입력
			if (!scanner.hasNextLine()) break;
			// 입력 앞뒤 공백 제거
			String choice = scanner.nextLine().trim();

			// 메뉴 번호에 해당하는 기능 호출
			switch (choice) {
				case "1": //파일 업로드
					System.out.print("Type Java Filename: ");
					// 입력 스트림이 열려 있으면 파일 경로 전달
					if (scanner.hasNextLine())
						System.out.println(controller.uploadFile(scanner.nextLine().trim()));
					break;
				case "2": // Compile
					// 직접 Compile 후 결과 출력, 엔터 대기X
					System.out.println(controller.compileFile());
					break;
				case "3":
					// 명령행 인자 입력 후 Run 요청
					handleRun();
					break;
				case "4":
					// 현재 파일 및 컴파일 상태 초기화
					System.out.println(controller.reset());
					break;
				case "5":
					// .error 파일 내용 출력 후 엔터 대기
					showAndWait(controller.getCompileError());
					break;
				case "6":
					// 로그 조회 화면 : C로 초기화, 엔터로 복귀
					handleLog();
					break;
				case "7":
					// 종료 로그 기록 후 메뉴 반복 종료
					System.out.println(controller.exit());
					running = false;
					break;
				case "8":
					// 파일을 선택한 뒤 소스 미리보기
					handleSourcePreview();
					break;
				case "9":
					// 최근 업로드 목록 출력 후 엔터 대기
					showAndWait(controller.getRecentUploads());
					break;
				// 지원하지 않는 메뉴 번호는 오류 표시 후 다시 입력
				default:
					System.out.println("Error: invalid menu choice");
			}
			System.out.println();
		}
	}

	// 조회 내용 출력 후 메인 메뉴 복귀 입력 대기
	private void showAndWait(String content) {
		System.out.println(content);
		waitForEnter();
	}

	// 엔터만 입력하면 복귀, 다른 내용 입력 시 계속 대기
	private void waitForEnter() {
		while (true) {
			System.out.print("Press Enter to return to menu: ");
			// 입력 스트림 종료 또는 공백 제거 후 빈 입력이면 메서드 종료
			if (!scanner.hasNextLine() || scanner.nextLine().trim().isEmpty()) return;
		}
	}

	// 프리뷰 대상 선택 및 소스 출력
	private void handleSourcePreview() {
		// 최근 업로드 목록 + 현재 파일을 프리뷰 후보로 조회
		List<String> files = controller.getPreviewFiles();
		// 후보가 없으면 안내 출력 후 엔터로 복귀
		if (files.isEmpty()) {
			showAndWait("No recent uploads");
			return;
		}
		System.out.println("Select source file to preview:");
		// 사용자가 선택할 번호는 1부터 표시
		for (int i = 0; i < files.size(); i++) {
			System.out.println((i + 1) + ". " + files.get(i));
		}
		while (true) {
			System.out.print("Select file number (Enter to return): ");
			if (!scanner.hasNextLine()) return;
			String choice = scanner.nextLine().trim();
			if (choice.isEmpty()) return;
			try {
				// 선택 번호를 정수로 변환, 목록 인덱스는 0부터 시작
				int index = Integer.parseInt(choice) - 1;
				// 목록 범위 안의 번호인지 검사
				if (index >= 0 && index < files.size()) {
					// 선택한 파일만 미리보기, 현재 Compile / Run 대상 변경X
					showAndWait(controller.getSourcePreview(files.get(index)));
					return;
				}
			} catch (NumberFormatException e) {
				// 정수로 변환할 수 없으면 선택 화면 유지
			}
			// 숫자가 아니거나 범위를 벗어나면 오류 출력 후 재입력
			System.out.println("Error: invalid file choice");
		}
	}

	// 로그 조회 화면 유지 : 초기화 후에도 엔터 입력까지 대기
	private void handleLog() {
		printLog();
		while (true) {
			System.out.print("Enter: return to menu / C: clear log\n: ");
			if (!scanner.hasNextLine()) return;
			String choice = scanner.nextLine().trim();
			if (choice.isEmpty()) return;
			// 대소문자 구분 없이 C 입력 처리
			if (choice.equalsIgnoreCase("c")) {
				// 로그 파일 초기화 요청 후 처리 결과 출력
				System.out.println(controller.clearLog());
				printLog();
			}
			else {
				System.out.println("Type C to clear the log, or press Enter to return.");
			}
		}
	}

	// 로그 내용 출력, 빈 파일이면 안내 메시지 표시
	private void printLog() {
		String log = controller.getLog();
		System.out.println(log.isEmpty() ? "Log is empty" : log);
	}

	// 실행 인자 입력 후 결과 출력, 자동 Compile은 수행X
	private void handleRun() {
		System.out.print("Type Program Arguments (Enter for none): ");
		if (!scanner.hasNextLine()) return;
		// 입력한 인자를 그대로 전달, 엔터만 입력하면 빈 문자열
		RunResult result = controller.runProgram(scanner.nextLine());
		// 컴파일 전 / 실패 상태이거나 실행 시작 실패 시 안내 후 복귀
		if (!result.isExecuted()) {
			System.out.println(result.getMessage());
			return;
		}
		// 실행 프로그램의 표준 출력 표시
		if (!result.getStandardOutput().isEmpty()) {
			System.out.print(result.getStandardOutput());
			// 출력 끝에 줄바꿈이 없으면 추가
			if (!result.getStandardOutput().endsWith("\n")) System.out.println();
		}
		// 실행 프로그램의 표준 에러를 System.err로 표시
		if (!result.getErrorOutput().isEmpty()) {
			System.err.print(result.getErrorOutput());
			// 에러 출력 끝에 줄바꿈이 없으면 추가
			if (!result.getErrorOutput().endsWith("\n")) System.err.println();
		}
		// 시간 초과이면 강제 종료 안내, 그 외에는 Exit Code 표시
		if (result.isTimedOut()) {
			System.out.println(result.getMessage());
		} else {
			System.out.println("Exit Code: " + result.getExitCode());
		}
		// 실행 소요 시간 출력 후 바로 메인 메뉴로 복귀
		System.out.println("Run time: " + result.getRunDurationMillis() + " ms");
	}

	// 메뉴 표시 : 로그 초기화는 6번 화면 안에서 처리
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
