package javaide;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

// 모든 기본 파일 작업 담당
public class JavaFileManager {
	// 파일명 존재 && 실제 일반 파일 = true
	public boolean fileExists(String fileName) {
		return fileName != null && !fileName.isEmpty() && Files.isRegularFile(Path.of(fileName));
	}

	// 확장자 .java 확인
	public boolean isJavaFile(String fileName) {
		return fileName != null && fileName.toLowerCase().endsWith(".java");
	}

	// 파일 읽기
	public String readFile(String fileName) {
		try {
			// UTF-8 파일 전체 읽기
			return Files.readString(Path.of(fileName), StandardCharsets.UTF_8);
		} catch (IOException e) {
			return null;
		}
	}
	// 파일 쓰기
	public boolean writeFile(String fileName, String content) {
		try {
			// UTF-8 파일 저장
			Files.writeString(Path.of(fileName), content, StandardCharsets.UTF_8);
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	// 파일 제거
	public boolean deleteFile(String fileName) {
		if (fileName == null) return false;
		try {
			Files.deleteIfExists(Path.of(fileName));
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public String getErrorFileName(String javaFileName) {
		return javaFileName + ".error";
	}
}
