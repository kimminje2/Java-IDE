package javaide;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.InvalidPathException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.StandardOpenOption;

// 모든 기본 파일 작업 담당
public class JavaFileManager {
	// GUI에서는 IOException을 Controller/UI까지 전달하여 실패 원인을 표시한다.
	public Path javaPath(String fileName) throws IOException {
		if (fileName == null || fileName.trim().isEmpty()) throw new IOException("Enter a Java filename.");
		try {
			Path path = Path.of(fileName.trim()).toAbsolutePath().normalize();
			if (path.getFileName() == null || !path.getFileName().toString().endsWith(".java")) {
				throw new IOException("Not a .java file: " + fileName);
			}
			return path;
		} catch (InvalidPathException e) {
			throw new IOException("Invalid filename: " + fileName, e);
		}
	}

	public String readSource(String fileName) throws IOException {
		Path path = javaPath(fileName);
		if (!Files.isRegularFile(path)) throw new IOException("File not found: " + path);
		return Files.readString(path, StandardCharsets.UTF_8);
	}

	public void saveSource(String fileName, String content, boolean overwrite) throws IOException {
		Path path = javaPath(fileName);
		if (overwrite) {
			if (!Files.isRegularFile(path)) throw new IOException("File not found: " + path);
			// CREATE를 지정하지 않아 외부에서 삭제된 파일을 새로 만들지 않는다.
			Files.writeString(path, content, StandardCharsets.UTF_8,
					StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
		} else {
			// 존재 검사 후 쓰기 사이에 파일이 생겨도 덮어쓰지 않도록 CREATE_NEW 사용
			try {
				Files.writeString(path, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
			} catch (FileAlreadyExistsException e) {
				throw new IOException("File already exists: " + path, e);
			}
		}
	}

	public void saveErrors(String javaFileName, String errors) throws IOException {
		Path path = javaPath(javaFileName);
		Files.writeString(Path.of(getErrorFileName(path.toString())), errors, StandardCharsets.UTF_8);
	}

	public void deleteSource(String fileName) throws IOException {
		Path path = javaPath(fileName);
		if (!Files.isRegularFile(path)) throw new IOException("File not found: " + path);
		Files.delete(path);
	}

	public String readErrors(String javaFileName) throws IOException {
		Path source = javaPath(javaFileName);
		Path errors = Path.of(getErrorFileName(source.toString()));
		if (!Files.isRegularFile(errors)) throw new IOException("No saved compile error file. Use Save Errors first.");
		return Files.readString(errors, StandardCharsets.UTF_8);
	}

	public String getErrorFileName(String javaFileName) {
		return javaFileName + ".error";
	}
}
