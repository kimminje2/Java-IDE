package javaide;

// Compile 결과 보관, final 필드는 생성 후 변경X
public final class CompileResult {
	// 컴파일 성공 여부
	private final boolean success;
	// 오류 메시지에서 찾은 오류 개수
	private final int errorCount;
	// 오류 줄 번호 문자열 : 예) 3, 7
	private final String errorLineNumbers;
	// 오류가 발생한 소스 줄 + 줄 번호
	private final String sourceContext;
	// 컴파일 소요 시간(ms)
	private final long durationMillis;
	// Result Window와 Save Errors에서 사용할 컴파일러 출력 전체
	private final String errorText;

	// 결과 필드 초기화
	// 생성자를 private으로 제한 : success() / failure()로 생성
	private CompileResult(boolean success, int errorCount, String errorLineNumbers,
			String sourceContext, long durationMillis, String errorText) {
		this.success = success;
		this.errorCount = errorCount;
		this.errorLineNumbers = errorLineNumbers;
		this.sourceContext = sourceContext;
		this.durationMillis = durationMillis;
		this.errorText = errorText;
	}

	// 성공 결과 생성 : 오류 정보 없이 소요 시간 저장
	public static CompileResult success(long durationMillis) {
		return new CompileResult(true, 0, "", "", durationMillis, "");
	}

	// 실패 결과는 메모리에 보관하고 오류 파일 저장은 Save Errors에서만 수행한다.
	public static CompileResult failure(int errorCount, String errorLineNumbers,
			String sourceContext, long durationMillis, String errorText) {
		return new CompileResult(false, errorCount, errorLineNumbers, sourceContext, durationMillis, errorText);
	}

	// Getter들 : Controller에서 결과 확인 및 출력 메시지 구성
	public boolean isSuccess() { return success; }
	public int getErrorCount() { return errorCount; }
	public String getErrorLineNumbers() { return errorLineNumbers; }
	public String getSourceContext() { return sourceContext; }
	public long getDurationMillis() { return durationMillis; }
	public String getErrorText() { return errorText; }
}
