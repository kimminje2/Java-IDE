package javaide;

public class IDEState {

	// 현재 Open 또는 Save된 파일
	private String currentFileName;

	// 정상 Compile 상태인가?
	private boolean isCompiled;

	private String savedSource;
	private CompileResult lastCompileResult;

	public String getSavedSource() { return savedSource; }
	public void setSavedSource(String source) { savedSource = source; }
	public CompileResult getLastCompileResult() { return lastCompileResult; }
	public void setLastCompileResult(CompileResult result) { lastCompileResult = result; }


	// 초기 상태 설정
	public IDEState() {
		reset();
	}

	// Getter / Setter 들
	public String getCurrentFileName() {
		return currentFileName;
	}


	public void setCurrentFileName(String currentFileName) {
		this.currentFileName = currentFileName;
	}


	public boolean isCompiled() {
		return isCompiled;
	}


	public void setCompiled(boolean compiled) {
		isCompiled = compiled;
	}


	public void reset() {
		// 초기 상태
		currentFileName = null;
		isCompiled = false;
		savedSource = null;
		lastCompileResult = null;
	}
}
