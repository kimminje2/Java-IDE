package javaide;

public class IDEState {

    // 현재 사용 중인 Java 파일
    private String currentFileName;

    // 현재 파일이 정상 컴파일됐는지
    private boolean isCompiled;

    // 현재 컴파일 오류 파일명
    private String compileErrorFileName;


    public IDEState() {

        // 시작 시 초기 상태 설정
        reset();
    }


    public String getCurrentFileName() {
        return currentFileName;
    }


    public void setCurrentFileName(
            String currentFileName) {

        this.currentFileName =
                currentFileName;
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


    public void setCompileErrorFileName(
            String compileErrorFileName) {

        this.compileErrorFileName =
                compileErrorFileName;
    }


    public void reset() {

        // 처음 실행한 상태로 초기화
        currentFileName = null;
        isCompiled = false;
        compileErrorFileName = null;
    }
}
