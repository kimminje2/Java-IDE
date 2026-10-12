package javaide;

public class IDEState {

	// 현재 Upload된 파일
	private String currentFileName;

	// 정상 Compile 상태인가?
	private boolean isCompiled;

	// 실패 시 생성된 .error 파일
	private String compileErrorFileName;


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


	public String getCompileErrorFileName() {
		return compileErrorFileName;
	}


	public void setCompileErrorFileName(String compileErrorFileName) {
		this.compileErrorFileName = compileErrorFileName;
	}


	public void reset() {
		// 초기 상태
		currentFileName = null;
		isCompiled = false;
		compileErrorFileName = null;
	}
}
