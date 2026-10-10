package javaide;

import java.io.File;

public class IDEController {
    private final IDEState state;
    private final JavaFileManager fileManager;
    private final CompilerManager compilerManager;
    private final ProgramRunner programRunner;
    private final LogManager logManager;

    private CompileResult lastCompileResult;

    public IDEController() {
        state = new IDEState();
        fileManager = new JavaFileManager();
        compilerManager = new CompilerManager(fileManager);
        programRunner = new ProgramRunner();
        logManager = new LogManager();
    }

    public String uploadFile(String fileName) {
        if (fileName == null) {
            fileName = "";
        }

        fileName = fileName.trim();

        if (!fileManager.isJavaFile(fileName)) {
            logManager.writeLog("UPLOAD REFUSED (not a java file) - " + fileName);
            return "Error: not a java file - " + fileName;
        }

        if (!fileManager.fileExists(fileName)) {
            logManager.writeLog("UPLOAD REFUSED (file not found) - " + fileName);
            return "Error: file not found - " + fileName;
        }

        if (isSameFile(state.getCurrentFileName(), fileName)) {
            String displayName = fileManager.getDisplayFileName(fileName);
            logManager.writeLog("UPLOAD REFUSED (already uploaded) - " + displayName);
            return "Error: file already uploaded - " + displayName;
        }

        state.setCurrentFileName(new File(fileName).getAbsolutePath());
        state.setCompiled(false);
        lastCompileResult = null;

        String displayName = fileManager.getDisplayFileName(fileName);
        logManager.writeLog("UPLOAD " + displayName);
        return "uploaded successfully - " + displayName;
    }

    public CompileResult compileFile() {
        if (state.getCurrentFileName() == null) {
            logManager.writeLog("COMPILE REFUSED (no file uploaded)");
            return CompileResult.notAttempted("Error: no file uploaded");
        }

        CompileResult result = compilerManager.compile(state.getCurrentFileName());
        lastCompileResult = result;
        state.setCompiled(result.isSuccess());

        if (result.isSuccess()) {
            logManager.writeLog("COMPILE SUCCESS - " + result.getFileName());
        } else {
            logManager.writeLog(
                    "COMPILE FAIL (" + result.getErrorCount() + " errors) - " + result.getFileName());
        }

        return result;
    }

    public RunResult runProgram(String arguments) {
        if (state.getCurrentFileName() == null) {
            logManager.writeLog("RUN REFUSED (no file uploaded)");
            return RunResult.notExecuted("Error: no file uploaded");
        }

        if (!state.isCompiled()) {
            logManager.writeLog("RUN REFUSED (not compiled)");
            return RunResult.notExecuted("Error: compile the file first");
        }

        RunResult result = programRunner.run(state.getCurrentFileName(), arguments);

        if (result.isExecuted()) {
            logManager.writeLog(
                    "RUN args=\"" + safeArguments(arguments) + "\" exit=" + result.getExitCode());
        } else {
            logManager.writeLog("RUN FAIL - " + result.getMessage());
        }

        return result;
    }

    public String reset() {
        String previousFile = state.getCurrentFileName();
        state.reset();
        lastCompileResult = null;

        if (previousFile == null) {
            logManager.writeLog("RESET (no file uploaded)");
        } else {
            logManager.writeLog("RESET - " + fileManager.getDisplayFileName(previousFile));
        }

        return "reset completed";
    }

    public String getCompileError() {
        if (state.getCurrentFileName() == null) {
            logManager.writeLog("ERROR FILE REFUSED (no compile error file)");
            return "Error: no compile error file";
        }

        String errorFileName = fileManager.getErrorFileName(state.getCurrentFileName());
        if (!fileManager.fileExists(errorFileName)) {
            logManager.writeLog("ERROR FILE REFUSED (no compile error file)");
            return "Error: no compile error file";
        }

        logManager.writeLog("SHOW ERROR FILE - " + new File(errorFileName).getName());
        return fileManager.readFile(errorFileName);
    }

    public String getLog() {
        logManager.writeLog("SHOW LOG");
        return logManager.readLog();
    }

    public String getSourcePreview() {
        if (state.getCurrentFileName() == null) {
            logManager.writeLog("SOURCE PREVIEW REFUSED (no file uploaded)");
            return "Error: no file uploaded";
        }

        logManager.writeLog(
                "SOURCE PREVIEW - " + fileManager.getDisplayFileName(state.getCurrentFileName()));
        return fileManager.readSourceWithLineNumbers(state.getCurrentFileName());
    }

    public String getErrorSourcePreview() {
        if (state.getCurrentFileName() == null
                || lastCompileResult == null
                || lastCompileResult.isSuccess()
                || lastCompileResult.getErrorLineNumbers().isBlank()) {
            logManager.writeLog("ERROR SOURCE PREVIEW REFUSED (no compile error file)");
            return "Error: no compile error file";
        }

        logManager.writeLog(
                "ERROR SOURCE PREVIEW - " + fileManager.getDisplayFileName(state.getCurrentFileName()));

        return fileManager.readSourceLines(
                state.getCurrentFileName(),
                lastCompileResult.getErrorLineNumbers());
    }

    public String exit() {
        logManager.writeLog("EXIT");
        return "Program terminated.";
    }

    public String getCurrentFileName() {
        return state.getCurrentFileName();
    }

    public boolean isCompiled() {
        return state.isCompiled();
    }

    private boolean isSameFile(String currentFileName, String newFileName) {
        if (currentFileName == null) {
            return false;
        }

        File currentFile = new File(currentFileName).getAbsoluteFile();
        File newFile = new File(newFileName).getAbsoluteFile();

        try {
            return currentFile.getCanonicalFile().equals(newFile.getCanonicalFile());
        } catch (java.io.IOException e) {
            return currentFile.equals(newFile);
        }
    }

    private String safeArguments(String arguments) {
        if (arguments == null) {
            return "";
        }
        return arguments.replace("\"", "\\\"");
    }
}
