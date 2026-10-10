package javaide;

public class RunResult {
    private final boolean executed;
    private final String standardOutput;
    private final String errorOutput;
    private final int exitCode;
    private final long executionTimeMs;
    private final String message;

    private RunResult(boolean executed,
                      String standardOutput,
                      String errorOutput,
                      int exitCode,
                      long executionTimeMs,
                      String message) {
        this.executed = executed;
        this.standardOutput = standardOutput;
        this.errorOutput = errorOutput;
        this.exitCode = exitCode;
        this.executionTimeMs = executionTimeMs;
        this.message = message;
    }

    public static RunResult notExecuted(String message) {
        return new RunResult(false, "", "", -1, 0, message);
    }

    public static RunResult executed(String standardOutput,
                                     String errorOutput,
                                     int exitCode,
                                     long executionTimeMs) {
        return new RunResult(true, standardOutput, errorOutput, exitCode, executionTimeMs, "");
    }

    public boolean isExecuted() {
        return executed;
    }

    public String getStandardOutput() {
        return standardOutput;
    }

    public String getErrorOutput() {
        return errorOutput;
    }

    public int getExitCode() {
        return exitCode;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public String getMessage() {
        return message;
    }
}
