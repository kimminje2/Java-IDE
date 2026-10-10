package javaide;

public final class RunResult {
    private final boolean executed;
    private final boolean timedOut;
    private final String message;
    private final String standardOutput;
    private final String errorOutput;
    private final int exitCode;
    private final long compileDurationMillis;
    private final long runDurationMillis;

    private RunResult(boolean executed, boolean timedOut, String message,
            String standardOutput, String errorOutput, int exitCode,
            long compileDurationMillis, long runDurationMillis) {
        this.executed = executed;
        this.timedOut = timedOut;
        this.message = message;
        this.standardOutput = standardOutput;
        this.errorOutput = errorOutput;
        this.exitCode = exitCode;
        this.compileDurationMillis = compileDurationMillis;
        this.runDurationMillis = runDurationMillis;
    }

    public static RunResult executed(String standardOutput, String errorOutput,
            int exitCode, long runDurationMillis) {
        return new RunResult(true, false, "", standardOutput, errorOutput,
                exitCode, -1, runDurationMillis);
    }

    public static RunResult timedOut(String standardOutput, String errorOutput,
            long runDurationMillis) {
        return new RunResult(true, true, "Error: execution time exceeded (5 seconds)",
                standardOutput, errorOutput, -1, -1, runDurationMillis);
    }

    public static RunResult notExecuted(String message) {
        return new RunResult(false, false, message, "", "", -1, -1, -1);
    }

    public RunResult withCompileDuration(long durationMillis) {
        return new RunResult(executed, timedOut, message, standardOutput,
                errorOutput, exitCode, durationMillis, runDurationMillis);
    }

    public boolean isExecuted() { return executed; }
    public boolean isTimedOut() { return timedOut; }
    public String getMessage() { return message; }
    public String getStandardOutput() { return standardOutput; }
    public String getErrorOutput() { return errorOutput; }
    public int getExitCode() { return exitCode; }
    public long getCompileDurationMillis() { return compileDurationMillis; }
    public long getRunDurationMillis() { return runDurationMillis; }
}
