package javaide;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

// 화면 구성과 이벤트 처리만 담당한다. 파일/컴파일 기능은 Controller에 요청한다.
public class SwingUI extends JFrame implements ActionListener {
    private static final long serialVersionUID = 1L;
    private final IDEController controller;
    private final JTextField openFile = new JTextField(30);
    private final JTextField saveFile = new JTextField(30);
    private final JTextField arguments = new JTextField(30);
    private final JComboBox<String> recentFiles = new JComboBox<>();
    private final JTextArea editor = new JTextArea(20, 70);
    private final JTextArea result = new JTextArea(9, 70);
    private final JLabel status = new JLabel();
    private final JButton openButton = new JButton("Open");
    private final JButton saveButton = new JButton("Save");
    private final JButton compileButton = new JButton("Compile");
    private final JButton errorsButton = new JButton("Save Errors");
    private final JButton deleteButton = new JButton("Delete");
    private final JButton clearButton = new JButton("Clear");
    private final JButton runButton = new JButton("Run");
    private final JButton recentButton = new JButton("Recent Files");
    private final JButton openRecentButton = new JButton("Open Recent");
    private final JButton previewButton = new JButton("Source Preview");
    private final JButton errorFileButton = new JButton("Compile Error File");
    private final JButton logButton = new JButton("Show Log");
    private final JButton clearLogButton = new JButton("Clear Log");
    private final JButton exitButton = new JButton("Exit");
    private final JButton[] buttons = {openButton, saveButton, compileButton, errorsButton, deleteButton,
            clearButton, runButton, recentButton, openRecentButton, previewButton, errorFileButton,
            logButton, clearLogButton, exitButton};
    private boolean busy;

    public SwingUI(IDEController controller) {
        super("Java IDE - Term Project 2");
        this.controller = controller;
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) { closeWindow(); }
        });

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setContentPane(root);
        JPanel files = new JPanel(new GridLayout(4, 1, 0, 6));
        files.add(fileRow("Open File", openFile, openButton));
        files.add(fileRow("Save File", saveFile, saveButton));
        files.add(fileRow("Arguments", arguments, runButton));
        files.add(recentRow());
        root.add(files, BorderLayout.NORTH);

        Font codeFont = new Font(Font.MONOSPACED, Font.PLAIN, 14);
        editor.setFont(codeFont);
        editor.setTabSize(4);
        result.setFont(codeFont);
        result.setEditable(false);
        JPanel middle = new JPanel(new BorderLayout(6, 6));
        middle.add(textPanel("Editing Window", editor), BorderLayout.CENTER);
        JPanel lower = new JPanel(new BorderLayout(6, 6));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 2));
        actions.add(compileButton);
        actions.add(errorsButton);
        actions.add(deleteButton);
        actions.add(clearButton);
        JPanel actionRows = new JPanel(new GridLayout(2, 1, 0, 4));
        actionRows.add(actions);
        JPanel tools = new JPanel(new GridLayout(1, 4, 8, 0));
        tools.add(errorFileButton);
        tools.add(logButton);
        tools.add(clearLogButton);
        tools.add(exitButton);
        actionRows.add(tools);
        lower.add(actionRows, BorderLayout.NORTH);
        lower.add(textPanel("Result Window", result), BorderLayout.CENTER);
        middle.add(lower, BorderLayout.SOUTH);
        root.add(middle, BorderLayout.CENTER);
        status.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
        root.add(status, BorderLayout.SOUTH);

        openFile.setName("openFile");
        saveFile.setName("saveFile");
        arguments.setName("arguments");
        recentFiles.setName("recentFiles");
        editor.setName("editor");
        result.setName("result");
        status.setName("status");
        saveFile.setToolTipText("New filename, or leave blank to overwrite the current file");
        compileButton.setToolTipText("Compile the saved Java file. Save changes first.");
        arguments.setToolTipText("Command-line arguments separated by spaces (not interactive console input)");
        previewButton.setToolTipText("Preview the selected recent file without changing the editor");
        errorFileButton.setToolTipText("Read the .error file previously saved with Save Errors");
        for (JButton button : buttons) {
            button.setName(button.getText());
            button.addActionListener(this);
        }
        // Enter도 각각 Open/Save 버튼과 같은 동작을 수행한다.
        openFile.setActionCommand("Open");
        saveFile.setActionCommand("Save");
        openFile.addActionListener(this);
        saveFile.addActionListener(this);
        arguments.setActionCommand("Run");
        arguments.addActionListener(this);
        editor.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateStatus(); }
            public void removeUpdate(DocumentEvent e) { updateStatus(); }
            public void changedUpdate(DocumentEvent e) { updateStatus(); }
        });
        refreshRecentFiles();
        updateStatus();
        pack();
        setMinimumSize(new Dimension(800, 680));
        setLocationRelativeTo(null);
    }

    private JPanel recentRow() {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        JLabel label = new JLabel("Recent");
        label.setPreferredSize(new Dimension(70, 24));
        label.setLabelFor(recentFiles);
        recentFiles.setPreferredSize(new Dimension(300, 26));
        // 선택값은 전체 경로로 유지하고 화면에는 파일명을 먼저 보여준다.
        recentFiles.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value != null) {
                    String path = value.toString();
                    int separator = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
                    setText(path.substring(separator + 1));
                    setToolTipText(path);
                }
                return this;
            }
        });
        recentFiles.addActionListener(event -> {
            Object selected = recentFiles.getSelectedItem();
            recentFiles.setToolTipText(selected == null ? null : selected.toString());
        });
        JPanel controls = new JPanel(new GridLayout(1, 3, 4, 0));
        controls.add(recentButton);
        controls.add(openRecentButton);
        controls.add(previewButton);
        row.add(label, BorderLayout.WEST);
        row.add(recentFiles, BorderLayout.CENTER);
        row.add(controls, BorderLayout.EAST);
        return row;
    }

    private void refreshRecentFiles() {
        Object selected = recentFiles.getSelectedItem();
        recentFiles.removeAllItems();
        for (String path : controller.getPreviewFiles()) recentFiles.addItem(path);
        if (selected != null) recentFiles.setSelectedItem(selected);
    }

    private String selectedRecentFile() throws IOException {
        Object selected = recentFiles.getSelectedItem();
        if (selected == null) throw new IOException("Select a recent file first.");
        return selected.toString();
    }

    private void openSource(String fileName) throws IOException {
        String source = controller.openSource(fileName);
        editor.setText(source);
        editor.setCaretPosition(0);
        openFile.setText(controller.getCurrentFileName());
        saveFile.setText("");
        refreshRecentFiles();
        recentFiles.setSelectedItem(controller.getCurrentFileName());
        showResult("Opened successfully - " + controller.getCurrentFileName());
    }

    private JPanel fileRow(String title, JTextField field, JButton button) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        JLabel label = new JLabel(title);
        label.setPreferredSize(new Dimension(70, 24));
        label.setLabelFor(field);
        row.add(label, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        row.add(button, BorderLayout.EAST);
        return row;
    }

    private JPanel textPanel(String title, JTextArea area) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.add(new JLabel(title), BorderLayout.NORTH);
        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        return panel;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (busy) return;
        try {
            switch (event.getActionCommand()) {
                case "Open":
                    openSource(openFile.getText());
                    break;
                case "Save":
                    showResult(controller.saveSource(saveFile.getText(), editor.getText()));
                    openFile.setText(controller.getCurrentFileName());
                    saveFile.setText("");
                    refreshRecentFiles();
                    recentFiles.setSelectedItem(controller.getCurrentFileName());
                    break;
                case "Compile":
                    startOperation(false);
                    break;
                case "Run":
                    startOperation(true);
                    break;
                case "Recent Files":
                    refreshRecentFiles();
                    showResult(controller.getRecentUploads());
                    break;
                case "Open Recent":
                    openSource(selectedRecentFile());
                    break;
                case "Source Preview":
                    showResult(controller.getSourcePreview(selectedRecentFile()));
                    break;
                case "Compile Error File":
                    showResult(controller.getCompileError());
                    break;
                case "Show Log":
                    String log = controller.getLog();
                    showResult(log.isEmpty() ? "Log is empty" : log);
                    break;
                case "Clear Log":
                    showResult(controller.clearLog());
                    break;
                case "Exit":
                    closeWindow();
                    break;
                case "Save Errors":
                    showResult(controller.saveErrors());
                    break;
                case "Delete":
                    showResult(controller.deleteCurrentFile());
                    openFile.setText("");
                    saveFile.setText("");
                    arguments.setText("");
                    recentFiles.setSelectedIndex(-1);
                    break;
                case "Clear":
                    controller.reset();
                    editor.setText("");
                    result.setText("");
                    openFile.setText("");
                    saveFile.setText("");
                    arguments.setText("");
                    recentFiles.setSelectedIndex(-1);
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            showResult("Error: " + e.getMessage());
        }
        updateStatus();
    }

    private void startOperation(final boolean run) {
        final String source = editor.getText();
        final String programArguments = arguments.getText();
        setBusy(true);
        showResult(run ? "Running... (maximum 5 seconds)" : "Compiling...");
        // Compile/Run 대기는 작업 스레드, 화면 갱신은 done()의 EDT에서 수행한다.
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws IOException {
                return run ? runMessage(controller.runProgram(source, programArguments)) : controller.compileEditor(source);
            }

            @Override
            protected void done() {
                try {
                    showResult(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    showResult("Error: operation interrupted.");
                } catch (ExecutionException e) {
                    showResult("Error: " + e.getCause().getMessage());
                } finally {
                    setBusy(false);
                    updateStatus();
                }
            }
        }.execute();
    }

    private String runMessage(RunResult run) {
        if (!run.isExecuted()) return run.getMessage();
        StringBuilder message = new StringBuilder();
        if (!run.getStandardOutput().isEmpty()) {
            message.append("Standard output:\n").append(run.getStandardOutput()).append('\n');
        }
        if (!run.getErrorOutput().isEmpty()) {
            message.append("Standard error:\n").append(run.getErrorOutput()).append('\n');
        }
        if (run.isTimedOut()) message.append(run.getMessage());
        else message.append("Exit Code: ").append(run.getExitCode());
        return message.append("\nRun time: ").append(run.getRunDurationMillis()).append(" ms").toString();
    }

    private void closeWindow() {
        if (busy) {
            showResult("Please wait for the current Compile/Run operation to finish before exiting.");
            return;
        }
        if (isDisplayable()) {
            controller.exit();
            dispose();
        }
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        for (JButton button : buttons) button.setEnabled(!busy);
        openFile.setEnabled(!busy);
        saveFile.setEnabled(!busy);
        arguments.setEnabled(!busy);
        recentFiles.setEnabled(!busy);
        editor.setEditable(!busy);
    }

    private void showResult(String message) {
        result.setText(message);
        result.setCaretPosition(0);
    }

    private void updateStatus() {
        String path = controller.getCurrentFileName();
        String file = path == null ? "No File" : "File: " + Path.of(path).getFileName();
        int lines = editor.getDocument().getLength() == 0 ? 0 : editor.getLineCount();
        status.setText(file + " | Lines: " + lines + " | " + controller.getCompileStatus());
        status.setToolTipText(path);
    }
}
