package javaide;

public class RunResult {

    private final boolean executed;
    private final String message;
    private final String standardOutput;
    private final String errorOutput;
    private final int exitCode;

    private RunResult(
            boolean executed,
            String message,
            String standardOutput,
            String errorOutput,
            int exitCode) {

        this.executed = executed;
        this.message = message;
        this.standardOutput = standardOutput;
        this.errorOutput = errorOutput;
        this.exitCode = exitCode;
    }

    public static RunResult executed(
            String standardOutput,
            String errorOutput,
            int exitCode) {

        return new RunResult(
                true,
                "",
                standardOutput,
                errorOutput,
                exitCode
        );
    }

    public static RunResult notExecuted(String message) {
        return new RunResult(
                false,
                message,
                "",
                "",
                -1
        );
    }

    public boolean isExecuted() {
        return executed;
    }

    public String getMessage() {
        return message;
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
}
