package javaide;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// javac 실행 + 컴파일 오류 분석 담당
public class CompilerManager {
	// Test.java:3: error: 형식에서 줄 번호 추출
	// (\d+)에 해당하는 숫자를 group(1)로 가져옴
	private static final Pattern ERROR_LINE = Pattern.compile("\\.java:(\\d+): error:");
	// .error 파일 저장 및 소스 읽기에 사용할 객체
	private final JavaFileManager fileManager;

	// Controller의 파일 처리 객체 전달받음
	public CompilerManager(JavaFileManager fileManager) {
		this.fileManager = fileManager;
	}

	// 소스 컴파일 후 성공 / 실패 결과 리턴
	public CompileResult compile(String fileName) {
		// 시작 시간 측정
		long started = System.nanoTime();

		//오류 파일 이름
		String errorFileName = fileManager.getErrorFileName(fileName);
		String errorText;
		// 프로세스 실행 실패 시 성공으로 처리되지 않도록 -1로 시작
		int exitCode = -1;
		try {
			// 입력 경로를 절대 경로로 변환, . / .. 경로 정리
			Path source = Path.of(fileName).toAbsolutePath().normalize();
			// 소스가 있는 폴더 : 클래스 참조 경로 및 결과 저장 위치
			Path parent = source.getParent();

			// 현재 Java 환경의 bin 폴더에서 javac 실행 파일 선택
			String javac = Path.of(System.getProperty("java.home"), "bin", executable("javac")).toString();

			// javac 실행
			// -encoding : UTF-8 / -proc:none : 애너테이션 처리 비활성화
			// -cp : 클래스 참조 폴더 / -d : .class 저장 폴더
			// redirectErrorStream(true) : 표준 출력과 오류 출력을 합쳐 읽기
			Process process = new ProcessBuilder(javac, "-encoding", "UTF-8",
					"-proc:none", "-cp", parent.toString(), "-d",
					parent.toString(), source.toString())
					.redirectErrorStream(true).start();

			// 컴파일러의 출력 전체를 UTF-8 문자열로 저장
			errorText = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
			// 컴파일 종료까지 대기, 종료 코드 저장
			exitCode = process.waitFor();

		} catch (IOException e) {
			// 실행 파일을 찾지 못하는 등의 입출력 오류 저장
			errorText = "javac 실행 실패: " + e.getMessage() + System.lineSeparator();
		} catch (InterruptedException e) {
			// 대기 중 인터럽트 발생 : 현재 스레드의 중단 상태 복원
			Thread.currentThread().interrupt();
			errorText = "컴파일 대기 중 중단됨" + System.lineSeparator();
		}

		// 시작 이후 경과 시간을 나노초에서 밀리초로 변환
		long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

		// 종료 코드 0 = 성공
		if (exitCode == 0) {
			// 이전에 생성된 오류 파일 제거
			fileManager.deleteFile(errorFileName);
			return CompileResult.success(elapsed);
		}

		// 실패 시 컴파일러 출력을 .error 파일에 저장
		fileManager.writeFile(errorFileName, errorText);
		// 오류 메시지에서 정규식에 맞는 부분 검색
		Matcher matcher = ERROR_LINE.matcher(errorText);
		List<Integer> lines = new ArrayList<>();
		// 일치하는 오류마다 줄 번호를 숫자로 변환해 추가
		while (matcher.find()) lines.add(Integer.parseInt(matcher.group(1)));

		// 줄 번호 목록을 쉼표로 연결 : 예) 3, 7, 10
		StringBuilder numbers = new StringBuilder();
		for (int i = 0; i < lines.size(); i++) {
			if (i > 0) numbers.append(", ");
			numbers.append(lines.get(i));
		}

		// 소스를 다시 읽어 오류 줄에 해당하는 코드 추출
		String source = fileManager.readFile(fileName);
		// 소스를 읽지 못하면 오류 소스 표시는 빈 문자열
		String context = source == null ? "" : SourceCodeFormatter.errorLines(source, lines);

		// 추출한 오류 개수와 상세 정보, 소요 시간 리턴
		return CompileResult.failure(lines.size(), numbers.toString(), errorFileName, context, elapsed);
	}

	// Windows는 .exe 추가, 그 외 OS는 실행 파일명 그대로 사용
	private static String executable(String name) {
		return System.getProperty("os.name").toLowerCase().contains("win") ? name + ".exe" : name;
	}
}
