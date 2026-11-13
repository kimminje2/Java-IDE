package javaide;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IDEController {
	//각 클래스 연결
	private final IDEState state = new IDEState(); // 현재 상태
	private final JavaFileManager fileManager = new JavaFileManager(); // 파일 처리
	private final CompilerManager compilerManager = new CompilerManager(fileManager); // Compile
	private final ProgramRunner programRunner = new ProgramRunner(); // Run
	private final LogManager logManager = new LogManager(); // Log
	private final RecentFileManager recentFileManager = new RecentFileManager(); // 최근 파일

	// GUI 파일 작업: 실패 시 상태를 변경하지 않고 UI에서 오류를 표시한다.
	public String openSource(String fileName) throws IOException {
		String path = fileManager.javaPath(fileName).toString();
		String source = fileManager.readSource(path);
		state.reset();
		state.setCurrentFileName(path);
		state.setSavedSource(source);
		recentFileManager.record(path);
		logManager.writeLog("OPEN " + path);
		return source;
	}

	public String saveSource(String fileName, String source) throws IOException {
		boolean overwrite = fileName == null || fileName.trim().isEmpty();
		String path = overwrite ? requireCurrentFile() : fileManager.javaPath(fileName).toString();
		fileManager.saveSource(path, source, overwrite);
		// 다른 이름으로 저장하면 이전 파일의 컴파일 결과와 연결을 끊는다.
		if (!path.equals(state.getCurrentFileName())) state.reset();
		state.setCurrentFileName(path);
		state.setSavedSource(source);
		state.setCompiled(false);
		recentFileManager.record(path);
		logManager.writeLog("SAVE " + path);
		return "Saved successfully - " + path;
	}

	public String compileEditor(String source) throws IOException {
		String path = requireCurrentFile();
		if (!source.equals(state.getSavedSource())) throw new IOException("Save your changes before compiling.");
		if (!fileManager.readSource(path).equals(state.getSavedSource())) {
			throw new IOException("Source changed on disk. Open it again before compiling.");
		}
		// 디스크의 파일을 컴파일하며, GUI에서는 오류 파일을 자동 저장하지 않는다.
		CompileResult result = compilerManager.compile(path);
		state.setLastCompileResult(result);
		state.setCompiled(result.isSuccess());
		logManager.writeLog("GUI COMPILE " + (result.isSuccess() ? "SUCCESS - " : "FAIL - ") + path);
		String message = result.isSuccess() ? "Compiled successfully - " + path : result.getErrorText();
		if (!result.isSuccess() && !result.getSourceContext().isEmpty()) {
			message += System.lineSeparator() + "Error source (lines " + result.getErrorLineNumbers() + "):"
					+ System.lineSeparator() + result.getSourceContext();
		}
		return message + System.lineSeparator() + "Compile time: " + result.getDurationMillis() + " ms";
	}

	public String saveErrors() throws IOException {
		String path = requireCurrentFile();
		CompileResult result = state.getLastCompileResult();
		if (result == null || result.isSuccess() || result.getErrorText().isEmpty()) {
			throw new IOException("No compile errors to save.");
		}
		fileManager.saveErrors(path, result.getErrorText());
		String errorPath = fileManager.getErrorFileName(path);
		logManager.writeLog("SAVE ERRORS " + errorPath);
		return "Errors saved successfully - " + errorPath;
	}

	public String deleteCurrentFile() throws IOException {
		String path = requireCurrentFile();
		fileManager.deleteSource(path);
		state.reset();
		logManager.writeLog("DELETE " + path);
		return "Deleted successfully - " + path;
	}

	private String requireCurrentFile() throws IOException {
		String path = state.getCurrentFileName();
		if (path == null) throw new IOException("No file. Open or Save a Java file first.");
		return path;
	}

	public String getCurrentFileName() { return state.getCurrentFileName(); }

	public String getCompileStatus() {
		CompileResult result = state.getLastCompileResult();
		if (result == null) return "Compile: -";
		if (result.isSuccess()) return "Compile: Success";
		// javac 실행 실패 등 소스 줄 번호가 없는 오류는 0 errors로 표시하지 않는다.
		return result.getErrorCount() == 0 ? "Compile: Failed" : "Compile: " + result.getErrorCount() + " errors";
	}

	// Run 동작 : 컴파일 성공 상태에서만 실행
	public RunResult runProgram(String source, String arguments) {
		// 현재 Open/Save된 파일 확인
		String fileName = state.getCurrentFileName();
		// 파일이 없으면 실행 거부 로그 기록 후 결과 리턴
		if (fileName == null) {
			logManager.writeLog("RUN REFUSED (no file opened)");
			return RunResult.notExecuted("Error: no file opened");
		}
		// 컴파일 전 / 실패 상태이면 실행 중단
		if (!state.isCompiled()) {
			logManager.writeLog("RUN REFUSED (not compiled)");
			CompileResult last = state.getLastCompileResult();
			String message = last != null && !last.isSuccess()
					? "Error: compilation failed - compile the file successfully first"
					: "Error: compile the file first";
			return RunResult.notExecuted(message);
		}
		// 편집창 또는 디스크가 바뀌었다면 이전 .class를 실행하지 않는다.
		if (!source.equals(state.getSavedSource())) {
			logManager.writeLog("RUN REFUSED (unsaved changes)");
			return RunResult.notExecuted("Error: save and compile your changes before running.");
		}
		try {
			if (!fileManager.readSource(fileName).equals(state.getSavedSource())) {
				logManager.writeLog("RUN REFUSED (source changed on disk)");
				return RunResult.notExecuted("Error: source changed on disk. Open and compile it again.");
			}
		} catch (IOException e) {
			logManager.writeLog("RUN REFUSED (source unavailable)");
			return RunResult.notExecuted("Error: " + e.getMessage());
		}
		// 파일명과 명령행 인자를 ProgramRunner에 전달, 자동 Compile 수행X
		RunResult result = programRunner.run(fileName, arguments);
		// 시간 초과 시 별도 로그 기록
		if (result.isTimedOut()) {
			logManager.writeLog("RUN TIMEOUT - " + fileName);
		} else if (!result.isExecuted()) {
			// 실행 실패 로그 기록
			logManager.writeLog("RUN FAIL - " + fileName);
		} else {
			// 실행 인자와 종료 코드 기록, 오류 종료 코드도 기록
			logManager.writeLog("RUN args=\"" + arguments + "\" exit=" + result.getExitCode());
		}
		// 출력, 종료 코드, 소요 시간 등을 담은 결과 리턴
		return result;
	}

	// 선택한 파일의 소스 미리보기, 현재 Compile / Run 대상 변경X
	public String getSourcePreview(String fileName) throws IOException {
		String content = fileManager.readSource(fileName);
		// 미리보기 로그 기록
		logManager.writeLog("SOURCE PREVIEW - " + fileName);
		// 파일명 + 줄 번호를 붙인 전체 소스 리턴
		return "Source: " + fileName + System.lineSeparator()
				+ SourceCodeFormatter.numbered(content);
	}

	// 미리보기에서 선택할 파일 목록 구성
	public List<String> getPreviewFiles() {
		// 최근 업로드 목록을 복사해 후보 목록 생성
		List<String> files = new ArrayList<>(recentFileManager.readEntries());
		// 현재 Open/Save된 파일 확인
		String currentFile = state.getCurrentFileName();
		// 현재 파일이 목록에 없으면 맨 앞에 추가, 중복 추가X
		if (currentFile != null && !files.contains(currentFile)) files.add(0, currentFile);
		// GUI 최근 파일 선택 목록으로 사용
		return files;
	}

	// 최근 업로드 목록을 출력용 문자열로 구성
	public String getRecentUploads() {
		// 저장된 업로드 기록 읽기
		List<String> entries = recentFileManager.readEntries();
		// 기록이 없으면 안내 메시지 리턴
		if (entries.isEmpty()) return "No recent uploads";
		// 목록 제목 생성
		StringBuilder output = new StringBuilder("Recent uploads:");
		// 저장된 순서대로 표시, 각 파일 앞에 줄바꿈과 1부터 시작하는 번호 추가
		for (int i = 0; i < entries.size(); i++) {
			output.append(System.lineSeparator()).append(i + 1)
					.append(". ").append(entries.get(i));
		}
		// 완성된 목록 문자열 리턴
		return output.toString();
	}

	// LogManager에 로그 초기화 요청, 성공 여부에 맞는 메시지 리턴
	public String clearLog() {
		return logManager.clearLog() ? "Log cleared" : "Error: failed to clear log";
	}

	// 현재 작업 상태 초기화
	public String reset() {
		// 현재 파일, 저장 소스, 컴파일 상태 초기화
		state.reset();
		// 초기화 로그 기록, 기존 로그 및 최근 업로드 기록 유지
		logManager.writeLog("RESET");
		return "reset completed";
	}

	// Save Errors로 실제 저장한 파일을 조회한다. 컴파일 결과와 구별한다.
	public String getCompileError() throws IOException {
		String path = requireCurrentFile();
		String content = fileManager.readErrors(path);
		String errorPath = fileManager.getErrorFileName(path);
		logManager.writeLog("COMPILE ERROR FILE - " + errorPath);
		return "Saved error file: " + errorPath + System.lineSeparator() + content;
	}

	// LogManager에서 로그 전체를 읽어 리턴
	public String getLog() { return logManager.readLog(); }

	// 종료 로그 기록 후 안내 리턴, 창 종료는 SwingUI에서 처리
	public String exit() {
		logManager.writeLog("EXIT");
		return "Program terminated.";
	}
}
