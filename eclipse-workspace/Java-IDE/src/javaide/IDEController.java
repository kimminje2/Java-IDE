package javaide;

import java.util.ArrayList;
import java.util.List;

public class IDEController {
    private final IDEState state = new IDEState();
    private final JavaFileManager fileManager = new JavaFileManager();
    private final CompilerManager compilerManager = new CompilerManager(fileManager);
    private final ProgramRunner programRunner = new ProgramRunner();
    private final LogManager logManager = new LogManager();
    private final RecentFileManager recentFileManager = new RecentFileManager();

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
        recentFileManager.record(fileName);
        logManager.writeLog("UPLOAD " + fileName);
        return "uploaded successfully - " + fileName;
    }

    public String compileFile() {
        String fileName = state.getCurrentFileName();
        if (fileName == null) {
            logManager.writeLog("COMPILE REFUSED (no file uploaded)");
            return "Error: no file uploaded";
        }
        CompileResult result = compileCurrent(fileName);
        return compileMessage(fileName, result) + System.lineSeparator()
                + "Compile time: " + result.getDurationMillis() + " ms";
    }

    private CompileResult compileCurrent(String fileName) {
        CompileResult result = compilerManager.compile(fileName);
        state.setCompiled(result.isSuccess());
        state.setCompileErrorFileName(result.getErrorFileName());
        if (result.isSuccess()) {
            logManager.writeLog("COMPILE SUCCESS - " + fileName);
        } else {
            logManager.writeLog("COMPILE FAIL (" + result.getErrorCount()
                    + " errors) - " + fileName);
        }
        return result;
    }

    private String compileMessage(String fileName, CompileResult result) {
        if (result.isSuccess()) return "compiled successfully - " + fileName;
        String lines = result.getErrorLineNumbers().isEmpty()
                ? "unknown" : result.getErrorLineNumbers();
        String summary = result.getErrorCount() + " compile errors occurred (lines "
                + lines + ") - " + result.getErrorFileName();
        if (!result.getSourceContext().isEmpty()) {
            summary += System.lineSeparator() + "Error source:" + System.lineSeparator()
                    + result.getSourceContext();
        }
        return summary;
    }

    public RunResult runProgram(String arguments) {
        String fileName = state.getCurrentFileName();
        if (fileName == null) {
            logManager.writeLog("RUN REFUSED (no file uploaded)");
            return RunResult.notExecuted("Error: no file uploaded");
        }
        if (!state.isCompiled()) {
            logManager.writeLog("RUN REFUSED (not compiled)");
            String message = state.getCompileErrorFileName() == null
                    ? "Error: compile the file first"
                    : "Error: compilation failed - compile the file successfully first";
            return RunResult.notExecuted(message);
        }
        RunResult result = programRunner.run(fileName, arguments);
        if (result.isTimedOut()) {
            logManager.writeLog("RUN TIMEOUT - " + fileName);
        } else if (!result.isExecuted()) {
            logManager.writeLog("RUN FAIL - " + fileName);
        } else {
            logManager.writeLog("RUN args=\"" + arguments + "\" exit=" + result.getExitCode());
        }
        return result;
    }

    public String getSourcePreview() {
        String fileName = state.getCurrentFileName();
        if (fileName == null) return "Error: no file uploaded";
        return getSourcePreview(fileName);
    }

    // Previewing another file does not change the compile/run target.
    public String getSourcePreview(String fileName) {
        if (!fileManager.isJavaFile(fileName)) return "Error: not a java file - " + fileName;
        String content = fileManager.readFile(fileName);
        if (content == null) return "Error: source file cannot be read - " + fileName;
        logManager.writeLog("SOURCE PREVIEW - " + fileName);
        return "Source: " + fileName + System.lineSeparator()
                + SourceCodeFormatter.numbered(content);
    }

    public List<String> getPreviewFiles() {
        List<String> files = new ArrayList<>(recentFileManager.readEntries());
        String currentFile = state.getCurrentFileName();
        if (currentFile != null && !files.contains(currentFile)) files.add(0, currentFile);
        return files;
    }

    public String getRecentUploads() {
        List<String> entries = recentFileManager.readEntries();
        if (entries.isEmpty()) return "No recent uploads";
        StringBuilder output = new StringBuilder("Recent uploads:");
        for (int i = 0; i < entries.size(); i++) {
            output.append(System.lineSeparator()).append(i + 1)
                    .append(". ").append(entries.get(i));
        }
        return output.toString();
    }

    public String clearLog() {
        return logManager.clearLog() ? "Log cleared" : "Error: failed to clear log";
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
        return content == null ? "Error: no compile error file" : content;
    }

    public String getLog() { return logManager.readLog(); }

    public String exit() {
        logManager.writeLog("EXIT");
        return "Program terminated.";
    }
}
