package javaide;

public final class CompileResult {
    private final boolean success;
    private final int errorCount;
    private final String errorLineNumbers;
    private final String errorFileName;
    private final String sourceContext;
    private final long durationMillis;

    private CompileResult(boolean success, int errorCount, String errorLineNumbers,
            String errorFileName, String sourceContext, long durationMillis) {
        this.success = success;
        this.errorCount = errorCount;
        this.errorLineNumbers = errorLineNumbers;
        this.errorFileName = errorFileName;
        this.sourceContext = sourceContext;
        this.durationMillis = durationMillis;
    }

    public static CompileResult success(long durationMillis) {
        return new CompileResult(true, 0, "", null, "", durationMillis);
    }

    public static CompileResult failure(int errorCount, String errorLineNumbers,
            String errorFileName, String sourceContext, long durationMillis) {
        return new CompileResult(false, errorCount, errorLineNumbers,
                errorFileName, sourceContext, durationMillis);
    }

    public boolean isSuccess() { return success; }
    public int getErrorCount() { return errorCount; }
    public String getErrorLineNumbers() { return errorLineNumbers; }
    public String getErrorFileName() { return errorFileName; }
    public String getSourceContext() { return sourceContext; }
    public long getDurationMillis() { return durationMillis; }
}
