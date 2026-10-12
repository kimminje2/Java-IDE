package javaide;

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

	//파일 업로드
	public String uploadFile(String fileName) {
		// .java 확장자 검사
		if (!fileManager.isJavaFile(fileName)) {
			logManager.writeLog("UPLOAD FAIL (not a java file) - " + fileName);
			return "Error: not a java file - " + fileName;
		}
		// 실제 파일 존재 검사
		if (!fileManager.fileExists(fileName)) {
			logManager.writeLog("UPLOAD FAIL (file not found) - " + fileName);
			return "Error: file not found - " + fileName;
		}
		// 이미 업로드 된 파일인지 검사
		if (fileName.equals(state.getCurrentFileName())) {
			logManager.writeLog("UPLOAD FAIL (already uploaded) - " + fileName);
			return "Error: file already uploaded - " + fileName;
		}

		state.setCurrentFileName(fileName); // 현재 파일 등록
		state.setCompiled(false); // 새 파일 = Compile 상태 false
		state.setCompileErrorFileName(null); // 이전 컴파일 오류 정보 제거
		recentFileManager.record(fileName); // 최근 업로드 목록 기록
		logManager.writeLog("UPLOAD " + fileName); // 로그 기록

		// 성공 메시지 리턴
		return "uploaded successfully - " + fileName;
	}

	// Compile 동작
	public String compileFile() {
		// 현재 Upload된 파일 확인
		String fileName = state.getCurrentFileName();
		// 파일이 없는 경우 Error
		if (fileName == null) {
			logManager.writeLog("COMPILE REFUSED (no file uploaded)");
			return "Error: no file uploaded";
		}

		// Compile
		CompileResult result = compileCurrent(fileName);
		// 결과 + 소요 시간 리턴
		return compileMessage(fileName, result) + System.lineSeparator() + "Compile time: " + result.getDurationMillis() + " ms";
	}

	private CompileResult compileCurrent(String fileName) {
		// 실제 컴파일 요청 : CompilerManager
		CompileResult result = compilerManager.compile(fileName);
		// 성공 여부 IDE 상태에 저장
		state.setCompiled(result.isSuccess());
		// 실패 시 .error 파일명 저장
		state.setCompileErrorFileName(result.getErrorFileName());

		// 로그 작성
		if (result.isSuccess()) {
			logManager.writeLog("COMPILE SUCCESS - " + fileName);
		}
		else {
			logManager.writeLog("COMPILE FAIL (" + result.getErrorCount() + " errors) - " + fileName);
		}

		return result;
	}

	// 오류 발생 소스 표시
	private String compileMessage(String fileName, CompileResult result) {
		// 성공 메시지
		if (result.isSuccess()) return "compiled successfully - " + fileName;

		// 실패 line + 메시지
		String lines = result.getErrorLineNumbers().isEmpty() ? "unknown" : result.getErrorLineNumbers();
		String summary = result.getErrorCount() + " compile errors occurred (lines " + lines + ") - " + result.getErrorFileName();

		// 오류가 발생한 실제 소스 표시
		if (!result.getSourceContext().isEmpty()) {
			summary += System.lineSeparator() + "Error source:" + System.lineSeparator() + result.getSourceContext();
		}

		return summary;
	}

	// Run 동작 : 컴파일 성공 상태에서만 실행
	public RunResult runProgram(String arguments) {
		// 현재 Upload된 파일 확인
		String fileName = state.getCurrentFileName();
		// 파일이 없으면 실행 거부 로그 기록 후 결과 리턴
		if (fileName == null) {
			logManager.writeLog("RUN REFUSED (no file uploaded)");
			return RunResult.notExecuted("Error: no file uploaded");
		}
		// 컴파일 전 / 실패 상태이면 실행 중단
		if (!state.isCompiled()) {
			logManager.writeLog("RUN REFUSED (not compiled)");
			// 저장된 오류 파일명 유무로 컴파일 전 / 실패 메시지 구분
			String message = state.getCompileErrorFileName() == null
					? "Error: compile the file first"
					: "Error: compilation failed - compile the file successfully first";
			return RunResult.notExecuted(message);
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
	public String getSourcePreview(String fileName) {
		// .java 확장자 검사
		if (!fileManager.isJavaFile(fileName)) return "Error: not a java file - " + fileName;
		// 선택한 소스 파일 내용 읽기
		String content = fileManager.readFile(fileName);
		// 읽기 실패 시 오류 메시지 리턴
		if (content == null) return "Error: source file cannot be read - " + fileName;
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
		// 현재 Upload된 파일 확인
		String currentFile = state.getCurrentFileName();
		// 현재 파일이 목록에 없으면 맨 앞에 추가, 중복 추가X
		if (currentFile != null && !files.contains(currentFile)) files.add(0, currentFile);
		// ConsoleUI에서 선택 번호를 붙일 파일 목록 리턴
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
		// 현재 파일명, 컴파일 여부, 오류 파일명 초기화
		state.reset();
		// 초기화 로그 기록, 기존 로그 및 최근 업로드 기록 유지
		logManager.writeLog("RESET");
		return "reset completed";
	}

	// 현재 컴파일 오류 파일 내용 조회
	public String getCompileError() {
		// 상태에 저장된 .error 파일명 확인
		String errorFileName = state.getCompileErrorFileName();
		// 오류 파일명이 없거나 실제 파일이 없으면 조회 거부
		if (errorFileName == null || !fileManager.fileExists(errorFileName)) {
			logManager.writeLog("COMPILE ERROR FILE REFUSED (no compile error file)");
			return "Error: no compile error file";
		}
		// 오류 파일 조회 로그 기록
		logManager.writeLog("COMPILE ERROR FILE - " + errorFileName);
		// .error 파일 내용 읽기
		String content = fileManager.readFile(errorFileName);
		// 읽기 실패 시 오류 메시지, 성공 시 파일 내용 리턴
		return content == null ? "Error: no compile error file" : content;
	}

	// LogManager에서 로그 전체를 읽어 리턴
	public String getLog() { return logManager.readLog(); }

	// 종료 로그 기록 후 종료 안내 리턴, 메뉴 종료는 ConsoleUI에서 처리
	public String exit() {
		logManager.writeLog("EXIT");
		return "Program terminated.";
	}
}
