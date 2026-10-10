package javaide;

public class CompileResult {
    private final boolean attempted;
    private final boolean success;
    private final String fileName;
    private final int errorCount;
    private final String errorLineNumbers;
    private final String errorText;
    private final String errorFileName;
    private final long compileTimeMs;
    private final String message;

    private CompileResult(boolean attempted,
                          boolean success,
                          String fileName,
                          int errorCount,
                          String errorLineNumbers,
                          String errorText,
                          String errorFileName,
                          long compileTimeMs,
                          String message) {
        this.attempted = attempted;
        this.success = success;
        this.fileName = fileName;
        this.errorCount = errorCount;
        this.errorLineNumbers = errorLineNumbers;
        this.errorText = errorText;
        this.errorFileName = errorFileName;
        this.compileTimeMs = compileTimeMs;
        this.message = message;
    }

    public static CompileResult notAttempted(String message) {
        return new CompileResult(false, false, "", 0, "", "", "", 0, message);
    }

    public static CompileResult success(String fileName, long compileTimeMs) {
        return new CompileResult(true, true, fileName, 0, "", "", "", compileTimeMs, "");
    }

    public static CompileResult failure(String fileName,
                                        int errorCount,
                                        String errorLineNumbers,
                                        String errorText,
                                        String errorFileName,
                                        long compileTimeMs) {
        return new CompileResult(true, false, fileName, errorCount, errorLineNumbers,
                errorText, errorFileName, compileTimeMs, "");
    }

    public boolean isAttempted() {
        return attempted;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getFileName() {
        return fileName;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public String getErrorLineNumbers() {
        return errorLineNumbers;
    }

    public String getErrorText() {
        return errorText;
    }

    public String getErrorFileName() {
        return errorFileName;
    }

    public long getCompileTimeMs() {
        return compileTimeMs;
    }

    public String getMessage() {
        return message;
    }
}
