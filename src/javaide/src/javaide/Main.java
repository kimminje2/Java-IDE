package javaide;

import javax.swing.SwingUtilities;

public class Main {

	public static void main(String[] args) {

		// 모든 기능은 Swing GUI에서 제공한다.
		SwingUtilities.invokeLater(() -> new SwingUI(new IDEController()).setVisible(true));
	}
}
