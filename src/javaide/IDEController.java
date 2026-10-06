package javaide;

public class IDEController {

    private final IDEState state;
    private final JavaFileManager fileManager;
    private final CompilerManager compilerManager;
    private final ProgramRunner programRunner;
    private final LogManager logManager;

    public IDEController() {
        this.state = new IDEState();
        this.fileManager = new JavaFileManager();
        this.compilerManager = new CompilerManager(fileManager);
        this.programRunner = new ProgramRunner();
        this.logManager = new LogManager();
    }

    public String uploadFile(String fileName) {
        if (!fileManager.isJavaFile(fileName)) {
            logManager.writeLog("UPLOAD FAIL (not a java file) - " + fileName);
            return "Error: not a java file - " + fileName;
        }

        if (!fileManager.fileExists(fileName)) {
            logManager.writeLog("UPLOAD FAIL (file not found) - " + fileName);
            return "Error: file not found - " + fileName;
        }

        if (fileName.equals(state.getCurrentFileName())) {
            logManager.writeLog("UPLOAD FAIL (already uploaded) - " + fileName);
            return "Error: file already uploaded - " + fileName;
        }

        state.setCurrentFileName(fileName);
        state.setCompiled(false);
        state.setCompileErrorFileName(null);

        logManager.writeLog("UPLOAD " + fileName);

        return "uploaded successfully - " + fileName;
    }

    public String compileFile() {
        String fileName = state.getCurrentFileName();

        if (fileName == null) {
            logManager.writeLog("COMPILE REFUSED (no file uploaded)");
            return "Error: no file uploaded";
        }

        CompileResult result = compilerManager.compile(fileName);

        if (result.isSuccess()) {
            state.setCompiled(true);
            state.setCompileErrorFileName(null);

            logManager.writeLog("COMPILE SUCCESS - " + fileName);

            return "compiled successfully - " + fileName;
        }

        state.setCompiled(false);
        state.setCompileErrorFileName(result.getErrorFileName());

        logManager.writeLog(
                "COMPILE FAIL (" + result.getErrorCount() + " errors) - " + fileName
        );

        return result.getErrorCount()
                + " compile errors occurred (lines "
                + result.getErrorLineNumbers()
                + ") - "
                + result.getErrorFileName();
    }

    public RunResult runProgram(String arguments) {
        String fileName = state.getCurrentFileName();

        if (fileName == null) {
            logManager.writeLog("RUN REFUSED (no file uploaded)");
            return RunResult.notExecuted("Error: no file uploaded");
        }

        if (!state.isCompiled()) {
            logManager.writeLog("RUN REFUSED (not compiled)");
            return RunResult.notExecuted("Error: compile the file first");
        }

        RunResult result = programRunner.run(fileName, arguments);

        if (!result.isExecuted()) {
            logManager.writeLog("RUN FAIL - " + fileName);
            return result;
        }

        logManager.writeLog(
                "RUN args=\"" + arguments + "\" exit=" + result.getExitCode()
        );

        return result;
    }

    public String reset() {
        state.reset();
        logManager.writeLog("RESET");

        return "reset completed";
    }

    public String getCompileError() {
        String errorFileName = state.getCompileErrorFileName();

        if (errorFileName == null || !fileManager.fileExists(errorFileName)) {
            logManager.writeLog("COMPILE ERROR FILE REFUSED (no compile error file)");
            return "Error: no compile error file";
        }

        logManager.writeLog("COMPILE ERROR FILE - " + errorFileName);

        String content = fileManager.readFile(errorFileName);

        if (content == null) {
            return "Error: no compile error file";
        }

        return content;
    }

    public String getLog() {
        return logManager.readLog();
    }

    public String exit() {
        logManager.writeLog("EXIT");
        return "Program terminated.";
    }
}
