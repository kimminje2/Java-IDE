package javaide;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 컴파일된 클래스 실행, 출력 수집, 시간 제한 담당
public class ProgramRunner {
	// 실행 제한 5초 : 무한 루프 등 장시간 실행 종료
	private static final int TIMEOUT_SECONDS = 5;
	// 소스의 package 선언 검색, (?m)은 각 줄 시작 기준 적용
	// group(1)에서 패키지 이름 추출
	private static final Pattern PACKAGE = Pattern.compile("(?m)^\\s*package\\s+([a-zA-Z_$][\\w$]*(?:\\.[a-zA-Z_$][\\w$]*)*)\\s*;");

	// 파일명과 인자를 받아 java 프로세스 실행
	public RunResult run(String fileName, String arguments) {
		// 소스 절대 경로와 소스가 있는 폴더 확인
		Path source = Path.of(fileName).toAbsolutePath().normalize();
		Path parent = source.getParent();
		// .java 확장자를 제거해 실행 클래스 이름 구성
		String className = source.getFileName().toString().replaceFirst("\\.java$", "");

		try {
			// 패키지 선언 확인을 위해 소스 읽기
			String content = java.nio.file.Files.readString(source, StandardCharsets.UTF_8);
			Matcher matcher = PACKAGE.matcher(content);
			// package가 있으면 패키지명.클래스명으로 실행
			if (matcher.find()) className = matcher.group(1) + "." + className;
		} catch (IOException e) {
			// 소스를 읽지 못하면 실행 중단
			return RunResult.notExecuted("Error: failed to read source - " + e.getMessage());
		}

		// 현재 Java 환경의 실행 파일 선택 : Windows는 java.exe
		String java = Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java").toString();
		// 실행 명령 구성 : java -cp 클래스폴더 클래스명 [인자...]
		// 각 항목을 따로 전달해 경로 안의 공백 보존
		List<String> command = new ArrayList<>();
		command.add(java);
		command.add("-cp");
		command.add(parent.toString());
		command.add(className);

		// 인자가 있으면 공백 기준으로 나눠 명령에 추가
		// 빈 입력은 인자 없이 실행, 따옴표 묶음 해석은 지원X
		if (arguments != null && !arguments.trim().isEmpty()) {
			for (String argument : arguments.trim().split("\\s+")) command.add(argument);
		}

		// 프로세스 시작 전 시간 측정
		long started = System.nanoTime();
		// 예외 처리에서도 실행 프로세스를 종료할 수 있도록 참조 보관
		Process process = null;
		try {
			// 외부 java 프로세스 시작
			process = new ProcessBuilder(command).start();
			// 프로그램 표준 출력 수집기 생성
			StreamCollector output = new StreamCollector(process.getInputStream());
			// 프로그램 표준 에러 수집기 생성
			StreamCollector error = new StreamCollector(process.getErrorStream());
			// 두 스트림을 별도 스레드로 동시에 읽음
			// 출력 버퍼가 가득 차서 실행이 멈추는 상황 방지
			Thread outputThread = new Thread(output, "javaide-stdout");
			Thread errorThread = new Thread(error, "javaide-stderr");
			outputThread.start();
			errorThread.start();

			// 최대 5초 대기 : 시간 안에 종료되면 true
			boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
			// 시간 초과 시 강제 종료 후 실제 종료까지 대기
			if (!finished) {
				process.destroyForcibly();
				process.waitFor();
			}

			// 프로세스 종료 뒤 남아 있는 출력까지 수집 완료 대기
			outputThread.join();
			errorThread.join();
			// 출력 수집을 포함한 경과 시간을 ms로 변환
			long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
			// 시간 초과 결과에도 수집한 출력과 소요 시간 포함
			if (!finished) return RunResult.timedOut(output.text(), error.text(), elapsed);

			// 출력, 표준 에러, 실제 종료 코드, 소요 시간 리턴
			return RunResult.executed(output.text(), error.text(), process.exitValue(), elapsed);

		} catch (IOException e) {
			// 프로세스를 시작하지 못하면 실행 실패 결과 리턴
			return RunResult.notExecuted("Error: failed to execute program - " + e.getMessage());
		} catch (InterruptedException e) {
			// 대기 중 인터럽트 발생 시 남은 프로세스 종료
			if (process != null) process.destroyForcibly();
			// 현재 스레드의 중단 상태 복원
			Thread.currentThread().interrupt();
			return RunResult.notExecuted("Error: program execution interrupted");
		}
	}

	// 프로세스 출력 스트림 하나를 읽는 내부 클래스
	// Runnable 구현으로 별도 스레드에서 수집 가능
	private static final class StreamCollector implements Runnable {
		// 스트림당 최대 65,536개의 char만 보관
		private static final int LIMIT = 65536;
		// 수집할 프로세스 출력 스트림
		private final InputStream stream;
		// 수집한 내용을 이어 붙일 버퍼
		private final StringBuilder content = new StringBuilder();
		// 저장 한도를 넘겨 일부 출력을 생략했는지 표시
		private boolean truncated;

		// 표준 출력 또는 표준 에러 스트림 전달받음
		StreamCollector(InputStream stream) { this.stream = stream; }

		@Override
		// 스레드 시작 시 호출되는 출력 수집 작업
		public void run() {
			// UTF-8로 읽고, try 종료 시 스트림 닫기
			try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				// 한 번에 최대 4,096개 char씩 읽기
				char[] buffer = new char[4096];
				int read;
				// -1은 스트림 끝, 줄바꿈이 없는 출력도 수집
				while ((read = reader.read(buffer)) != -1) {
					// 이번에 읽은 양과 남은 저장 공간 중 작은 값만 보관
					int keep = Math.min(read, Math.max(0, LIMIT - content.length()));
					if (keep > 0) content.append(buffer, 0, keep);
					// 저장 한도를 넘어도 스트림은 끝까지 읽음
					// 초과 부분은 저장하지 않고 생략 여부만 기록
					if (keep < read) truncated = true;
				}
			} catch (IOException e) {
				// 읽기 실패 시 생략 상태가 아니면 오류 내용을 추가
				if (!truncated) content.append("Output read failed: ").append(e.getMessage());
			}
		}

		// 수집한 문자열 리턴, 생략된 출력이 있으면 안내 추가
		String text() {
			return content.toString() + (truncated ? System.lineSeparator() + "[output truncated]" : "");
		}
	}
}
