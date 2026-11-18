package javaide;

import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {

        // 모든 기능은 Swing GUI에서 제공한다.
        // 화면 생성도 Swing의 화면 처리 스레드(EDT)에 맡긴다.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SwingUI window = new SwingUI(new IDEController());
                window.setVisible(true);
            }
        });
    }
}
