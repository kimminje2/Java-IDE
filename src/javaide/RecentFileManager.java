package javaide;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// 최근 업로드 경로 저장 및 조회 담당
public class RecentFileManager {
	// 작업 폴더에 기록 저장 : 프로그램 재시작 후에도 유지
	private static final Path HISTORY_FILE = Path.of("recent-uploads.txt");
	// 최근 기록 최대 10개
	private static final int MAX_ENTRIES = 10;

	// 성공한 업로드를 최신 순서로 저장
	public void record(String fileName) {
		// 기존 기록 읽기, 기록이 없으면 빈 목록
		List<String> entries = readEntries();
		// 같은 경로 문자열이 있으면 기존 항목 제거
		entries.remove(fileName);
		// 이번 업로드를 목록 맨 앞에 추가
		entries.add(0, fileName);
		// 10개를 넘으면 오래된 항목 제외
		if (entries.size() > MAX_ENTRIES) entries = entries.subList(0, MAX_ENTRIES);
		try {
			// 각 경로를 한 줄씩 UTF-8로 저장, 기존 기록 덮어쓰기
			Files.write(HISTORY_FILE, entries, StandardCharsets.UTF_8);
		} catch (IOException e) {
			// 기록 저장 실패 시 오류 표시
			System.err.println("Error: failed to save recent uploads - " + e.getMessage());
		}
	}

	// 기록 파일의 각 줄을 목록으로 읽기
	public List<String> readEntries() {
		try {
			// 항목 추가 / 삭제가 가능한 ArrayList로 리턴
			return new ArrayList<>(Files.readAllLines(HISTORY_FILE, StandardCharsets.UTF_8));
		} catch (IOException e) {
			// 파일이 없거나 읽기 실패 시 빈 목록 리턴
			return new ArrayList<>();
		}
	}
}
