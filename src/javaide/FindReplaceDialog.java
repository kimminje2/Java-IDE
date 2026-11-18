package javaide;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;

// 14장의 JDialog 예제처럼 모달리스 대화상자로 구성한다.
class FindReplaceDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = 1L;
    private final SwingUI owner;
    private final JTextField find = new JTextField(22);
    private final JTextField replacement = new JTextField(22);
    private final JLabel message = new JLabel(" ");
    private JTextArea highlightedEditor;
    private Object highlight;

    FindReplaceDialog(SwingUI owner) {
        super(owner, "Find/Replace", false);
        this.owner = owner;
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        JPanel fields = new JPanel(new GridLayout(2, 2, 6, 6));
        fields.add(new JLabel("Find"));
        fields.add(find);
        fields.add(new JLabel("Replace with"));
        fields.add(replacement);
        JPanel buttons = new JPanel(new FlowLayout());
        for (String name : new String[]{"Find Next", "Replace", "Replace All", "Close"}) {
            JButton button = new JButton(name);
            button.setName(name);
            button.addActionListener(this);
            buttons.add(button);
        }
        find.setName("findText");
        replacement.setName("replaceText");
        message.setName("findMessage");
        find.setActionCommand("Find Next");
        find.addActionListener(this);
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(fields, BorderLayout.NORTH);
        root.add(buttons, BorderLayout.CENTER);
        root.add(message, BorderLayout.SOUTH);
        setContentPane(root);
        pack();
        setLocationRelativeTo(owner);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (command.equals("Close")) {
            clearMatch();
            setVisible(false);
            return;
        }
        JTextArea editor = owner.activeEditor();
        if (editor == null) {
            message.setText("No File");
            return;
        }
        String needle = find.getText();
        if (needle.isEmpty()) {
            clearMatch();
            message.setText("Enter text to find.");
            return;
        }
        if (command.equals("Replace")) {
            if (needle.equals(editor.getSelectedText())) {
                clearMatch();
                editor.replaceSelection(replacement.getText());
            }
            findNext(editor, needle);
        } else if (command.equals("Replace All")) {
            String source = editor.getText();
            int count = 0;
            int start = 0;
            int position;
            StringBuilder replaced = new StringBuilder();
            while ((position = source.indexOf(needle, start)) >= 0) {
                replaced.append(source, start, position).append(replacement.getText());
                start = position + needle.length();
                count++;
            }
            clearMatch();
            if (count == 0) {
                message.setText("Not found: " + needle);
                return;
            }
            replaced.append(source.substring(start));
            editor.setText(replaced.toString());
            message.setText("Replaced " + count + " occurrence(s).");
        } else {
            findNext(editor, needle);
        }
    }

    private void findNext(JTextArea editor, String needle) {
        clearMatch();
        String source = editor.getText();
        int position = source.indexOf(needle, editor.getCaretPosition());
        if (position < 0) {
            position = source.indexOf(needle);
        }
        if (position < 0) {
            message.setText("Not found: " + needle);
            return;
        }
        try {
            editor.select(position, position + needle.length());
            highlight = editor.getHighlighter().addHighlight(position, position + needle.length(),
                    new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW));
            highlightedEditor = editor;
            message.setText("Found at " + (position + 1));
        } catch (BadLocationException e) {
            message.setText("Search position changed. Try again.");
        }
    }

    // 탭 전환이나 편집 후 예전 검색 위치를 남기지 않는다.
    void clearMatch() {
        if (highlightedEditor != null && highlight != null) {
            highlightedEditor.getHighlighter().removeHighlight(highlight);
        }
        highlight = null;
        highlightedEditor = null;
    }
}
