package javaide;

public class IDEState {
    private String currentFileName;
    private boolean isCompiled;

    public String getCurrentFileName() {
        return currentFileName;
    }

    public void setCurrentFileName(String fileName) {
        this.currentFileName = fileName;
    }

    public boolean isCompiled() {
        return isCompiled;
    }

    public void setCompiled(boolean compiled) {
        this.isCompiled = compiled;
    }

    public void reset() {
        currentFileName = null;
        isCompiled = false;
    }
}
