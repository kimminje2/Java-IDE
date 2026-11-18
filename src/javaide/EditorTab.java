package javaide;

import java.awt.Font;
import java.nio.file.Path;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentListener;

// 탭 하나에 편집창과 기존 Controller를 한 개씩 두어 파일 상태를 분리한다.
class EditorTab extends JScrollPane {
    private static final long serialVersionUID = 1L;
    final JTextArea editor = new JTextArea(22, 80);
    final IDEController controller = new IDEController();
    private String untitledName;

    EditorTab(String name, DocumentListener listener) {
        untitledName = name;
        editor.setName("editor");
        editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        editor.setTabSize(4);
        setViewportView(editor);
        // 편집창의 내용 변경을 DocumentListener에 알린다.
        editor.getDocument().addDocumentListener(listener);
    }

    String fileName() {
        String path = controller.getCurrentFileName();
        return path == null ? untitledName : Path.of(path).getFileName().toString();
    }

    boolean isModified() {
        return controller.hasUnsavedChanges(editor.getText());
    }

    int lineCount() {
        return editor.getDocument().getLength() == 0 ? 0 : editor.getLineCount();
    }

    void setUntitledName(String name) {
        untitledName = name;
    }
}
