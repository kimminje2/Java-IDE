package javaide;

public class IDEState {

    private String currentFileName;
    private boolean isCompiled;
    private String compileErrorFileName;

    public IDEState() {
        reset();
    }

    public String getCurrentFileName() {
        return currentFileName;
    }

    public void setCurrentFileName(String currentFileName) {
        this.currentFileName = currentFileName;
    }

    public boolean isCompiled() {
        return isCompiled;
    }

    public void setCompiled(boolean compiled) {
        isCompiled = compiled;
    }

    public String getCompileErrorFileName() {
        return compileErrorFileName;
    }

    public void setCompileErrorFileName(String compileErrorFileName) {
        this.compileErrorFileName = compileErrorFileName;
    }

    public void reset() {
        currentFileName = null;
        isCompiled = false;
        compileErrorFileName = null;
    }
}
