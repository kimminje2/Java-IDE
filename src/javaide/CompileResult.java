package javaide;

public class CompileResult {

    private final boolean success;
    private final int errorCount;
    private final String errorLineNumbers;
    private final String errorFileName;

    private CompileResult(
            boolean success,
            int errorCount,
            String errorLineNumbers,
            String errorFileName) {

        this.success = success;
        this.errorCount = errorCount;
        this.errorLineNumbers = errorLineNumbers;
        this.errorFileName = errorFileName;
    }

    public static CompileResult success() {
        return new CompileResult(true, 0, "", null);
    }

    public static CompileResult failure(
            int errorCount,
            String errorLineNumbers,
            String errorFileName) {

        return new CompileResult(
                false,
                errorCount,
                errorLineNumbers,
                errorFileName
        );
    }

    public boolean isSuccess() {
        return success;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public String getErrorLineNumbers() {
        return errorLineNumbers;
    }

    public String getErrorFileName() {
        return errorFileName;
    }
}
