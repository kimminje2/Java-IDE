package javaide;

// Run 결과 보관, final 필드는 생성 후 변경X
public final class RunResult {
	// 실행 결과 여부, true여도 Exit Code가 0이라는 뜻은 아님
	private final boolean executed;
	// 실행 제한 시간 초과 여부
	private final boolean timedOut;
	// 실행 거부 / 실패 / 시간 초과 안내 메시지
	private final String message;
	// 실행 프로그램의 표준 출력
	private final String standardOutput;
	// 실행 프로그램의 표준 에러
	private final String errorOutput;
	// 프로세스 종료 코드, 실행 전 또는 시간 초과 결과는 -1
	private final int exitCode;
	// 이전 자동 Compile+Run용 시간, 현재 Run에서는 사용X
	private final long compileDurationMillis;
	// 실행 소요 시간(ms), 실행하지 않은 결과는 -1
	private final long runDurationMillis;

	// 모든 결과 필드 초기화, 아래 정적 메서드로 결과 생성
	private RunResult(boolean executed, boolean timedOut, String message,
			String standardOutput, String errorOutput, int exitCode,
			long compileDurationMillis, long runDurationMillis)
	{
		this.executed = executed;
		this.timedOut = timedOut;
		this.message = message;
		this.standardOutput = standardOutput;
		this.errorOutput = errorOutput;
		this.exitCode = exitCode;
		this.compileDurationMillis = compileDurationMillis;
		this.runDurationMillis = runDurationMillis;
	}

	// 프로세스 종료 결과 생성 : 출력 + 실제 Exit Code + 소요 시간
	public static RunResult executed(String standardOutput, String errorOutput, int exitCode, long runDurationMillis) {
		return new RunResult(true, false, "", standardOutput, errorOutput, exitCode, -1, runDurationMillis);
	}

	// 시간 초과 결과 생성 : 실행 여부 true, 시간 초과 true
	public static RunResult timedOut(String standardOutput, String errorOutput, long runDurationMillis) {
		return new RunResult(true, true, "Error: execution time exceeded (5 seconds)", standardOutput, errorOutput, -1, -1, runDurationMillis);
	}

	// 실행 거부 / 실행 실패 결과 생성 : 안내 메시지 저장
	public static RunResult notExecuted(String message) {
		return new RunResult(false, false, message, "", "", -1, -1, -1);
	}

	// 기존 결과를 복사해 Compile 시간을 붙임, 현재 호출하는 곳은 없음
	public RunResult withCompileDuration(long durationMillis) {
		return new RunResult(executed, timedOut, message, standardOutput, errorOutput, exitCode, durationMillis, runDurationMillis);
	}

	// Getter들 : ConsoleUI와 Controller에서 실행 결과 확인
	public boolean isExecuted() { return executed; }
	public boolean isTimedOut() { return timedOut; }
	public String getMessage() { return message; }
	public String getStandardOutput() { return standardOutput; }
	public String getErrorOutput() { return errorOutput; }
	public int getExitCode() { return exitCode; }
	public long getCompileDurationMillis() { return compileDurationMillis; }
	public long getRunDurationMillis() { return runDurationMillis; }
}
