package javaide;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

// 소스에 줄 번호 및 오류 줄 표시를 붙이는 공통 기능
public final class SourceCodeFormatter {
	// 객체 생성 제한, static 메서드로 바로 사용
	private SourceCodeFormatter() { }

	// 전체 소스에 1부터 시작하는 줄 번호 추가
	public static String numbered(String source) {
		// \R은 여러 형태의 줄바꿈 처리, -1은 마지막 빈 항목도 보존
		String[] lines = source.split("\\R", -1);
		// 표시할 줄 개수 확인
		int count = lines.length;
		// 파일 끝의 줄바꿈 때문에 생긴 마지막 빈 항목은 제외
		if (count > 1 && lines[count - 1].isEmpty()) count--;

		// 번호와 소스 내용을 순서대로 연결
		StringBuilder output = new StringBuilder();
		for (int i = 0; i < count; i++) {
			// 출력 형식 : 줄 번호 | 소스 코드
			output.append(i + 1).append(" | ").append(lines[i]).append(System.lineSeparator());
		}
		return output.toString();
	}

	// 오류 줄 번호에 해당하는 소스만 추출
	public static String errorLines(String source, Collection<Integer> lineNumbers) {
		String[] lines = source.split("\\R", -1);
		// 중복 줄 번호 제거, 오류 줄 포함 여부 확인에 사용
		Set<Integer> wanted = new HashSet<>(lineNumbers);
		StringBuilder output = new StringBuilder();

		for (int i = 0; i < lines.length; i++) {
			// 소스 순서대로 순회하며 오류가 있는 줄만 출력
			if (wanted.contains(i + 1)) {
				output.append(i + 1).append(" | ").append(lines[i]).append(System.lineSeparator());
				// 오류 줄 안내 표시, 정확한 오류 문자 위치를 나타내는 표시는 아님
				output.append("    ^ compile error").append(System.lineSeparator());
			}
		}
		return output.toString();
	}
}
