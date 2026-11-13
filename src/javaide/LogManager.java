package javaide;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// 로그 기록, 조회, 초기화 담당
public class LogManager {
	// 프로그램 작업 폴더에 저장할 로그 파일
	private static final Path LOG_FILE = Path.of("ide.log");
	// 기록 시간 형식 : 년-월-일 시:분:초
	private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	// 기존 로그 뒤에 새 작업 기록 추가
	public void writeLog(String message) {
		// 현재 시간 + 메시지 + 줄바꿈 구성
		String line = "[" + LocalDateTime.now().format(FORMAT) + "] " + message + System.lineSeparator();
		try {
			// CREATE : 파일이 없으면 생성 / APPEND : 기존 내용 뒤에 추가
			Files.writeString(LOG_FILE, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			// 로그 저장 실패 시 콘솔에 오류 표시
			System.err.println("Error: failed to write log - " + e.getMessage());
		}
	}

	// 로그 전체를 UTF-8로 읽어 리턴
	public String readLog() {
		try {
			return Files.readString(LOG_FILE, StandardCharsets.UTF_8);
		} catch (IOException e) {
			// 파일이 없거나 읽기에 실패하면 안내 메시지 리턴
			return "Error: no log file";
		}
	}

	// 로그 내용을 비우고 성공 여부 리턴
	public boolean clearLog() {
		try {
			// 빈 문자열 저장, TRUNCATE_EXISTING으로 기존 내용 제거
			// 파일이 없으면 CREATE로 빈 로그 파일 생성
			Files.writeString(LOG_FILE, "", StandardCharsets.UTF_8,
					StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			return true;
		} catch (IOException e) {
			// 초기화 실패 = false
			return false;
		}
	}
}
