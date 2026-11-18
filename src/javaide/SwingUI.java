package javaide;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

// 메뉴와 탭, 대화상자를 담당한다. 파일/컴파일/실행은 기존 Controller에 요청한다.
public class SwingUI extends JFrame implements ActionListener {
    private static final long serialVersionUID = 1L;
    private final IDEController services;
    private final JTabbedPane tabs = new JTabbedPane();
    private final JTextArea result = new JTextArea(10, 80);
    private final JLabel status = new JLabel();
    private final JTextField arguments = new JTextField(18);
    private final JCheckBox timeLimit = new JCheckBox("5-second timeout", true);
    private final JComboBox<String> recentFiles = new JComboBox<>();
    private final JFileChooser chooser = new JFileChooser();
    private final List<JMenuItem> menuItems = new ArrayList<>();
    private FindReplaceDialog findDialog;
    private EditorTab workingTab;
    private Thread worker;
    private boolean running;
    private boolean stopRequested;
    private boolean closing;
    private int untitledNumber = 1;

    public SwingUI(IDEController controller) {
        super("Java IDE - Term Project 3");
        services = controller;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                command("Quit");
            }
        });
        createMenu();

        chooser.setFileFilter(new FileNameExtensionFilter("Java source (*.java)", "java"));
        chooser.setAcceptAllFileFilterUsed(false);

        JPanel root = new JPanel(new BorderLayout(6, 6));
        root.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        setContentPane(root);

        JPanel options = new JPanel(new FlowLayout(FlowLayout.LEFT));
        options.add(new JLabel("Arguments"));
        options.add(arguments);
        options.add(timeLimit);
        options.add(new JLabel("Recent"));
        options.add(recentFiles);

        recentFiles.setPreferredSize(new Dimension(220, 26));
        recentFiles.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value != null) {
                    setText(Path.of(value.toString()).getFileName().toString());
                    setToolTipText(value.toString());
                }
                return this;
            }
        });

        arguments.setToolTipText("Command-line arguments separated by spaces (not interactive stdin)");
        timeLimit.setToolTipText("Uncheck for long-running programs; use Run > Stop to finish them");
        root.add(options, BorderLayout.NORTH);

        result.setEditable(false);
        result.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

        JPanel lower = new JPanel(new BorderLayout());
        lower.add(new JLabel("Result Window"), BorderLayout.NORTH);
        lower.add(new JScrollPane(result), BorderLayout.CENTER);

        JPanel middle = new JPanel(new BorderLayout(6, 6));
        middle.add(tabs, BorderLayout.CENTER);
        middle.add(lower, BorderLayout.SOUTH);
        root.add(middle, BorderLayout.CENTER);
        root.add(status, BorderLayout.SOUTH);

        tabs.setName("tabs");
        result.setName("result");
        status.setName("status");
        arguments.setName("arguments");
        timeLimit.setName("timeLimit");
        recentFiles.setName("recentFiles");
        tabs.addChangeListener(new TabChangeListener());
        refreshRecentFiles();
        updateDisplay();
        pack();
        setMinimumSize(new Dimension(850, 650));
        setLocationRelativeTo(null);
    }

    private void createMenu() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");

        for (String name : new String[]{"New", "Open", "Close", "Save", "Save As", "Show Log"}) {
            addItem(file, name);
        }
        file.addSeparator();
        addItem(file, "Compile");

        JMenu tools = new JMenu("Tools");
        JMenu recent = new JMenu("Recent");
        for (String name : new String[]{"Recent Files", "Open Recent", "Source Preview"}) {
            addItem(recent, name);
        }

        JMenu errors = new JMenu("Compile Errors");
        for (String name : new String[]{"Save Errors", "Compile Error File"}) {
            addItem(errors, name);
        }

        JMenu cleanup = new JMenu("Cleanup");
        for (String name : new String[]{"Delete", "Clear", "Clear Log"}) {
            addItem(cleanup, name);
        }

        tools.add(recent);
        tools.add(errors);
        tools.add(cleanup);
        file.addSeparator();
        addItem(file, "Quit");
        JMenu run = new JMenu("Run");
        addItem(run, "Compile").setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK));
        addItem(run, "Run").setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, InputEvent.CTRL_DOWN_MASK));
        addItem(run, "Stop");
        bar.add(file);
        bar.add(run);
        bar.add(tools);
        setJMenuBar(bar);
    }

    private JMenuItem addItem(JMenu menu, String name) {
        JMenuItem item = new JMenuItem(name);
        item.setName(name);
        item.addActionListener(this);
        menu.add(item);
        menuItems.add(item);
        return item;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        command(e.getActionCommand());
    }

    private void command(String name) {
        EditorTab tab = activeTab();
        String command = name.toUpperCase(Locale.ROOT);
        if (tab != null) {
            command += " - " + tab.fileName();
        }
        services.recordCommand(command);
        if (closing) {
            return;
        }
        try {
            switch (name) {
                case "New":
                    newDocument();
                    break;
                case "Open":
                    Path path = chooseFile(false, null);
                    if (path != null) {
                        openDocument(path);
                    }
                    break;
                case "Close":
                    closeDocument();
                    break;
                case "Save":
                    saveDocument(false);
                    break;
                case "Save As":
                    saveDocument(true);
                    break;
                case "Compile":
                    startOperation(false);
                    break;
                case "Run":
                    startOperation(true);
                    break;
                case "Stop":
                    stopProgram();
                    break;
                case "Show Log":
                    appendMessage(services.getLog());
                    break;
                case "Clear Log":
                    appendMessage(services.clearLog());
                    break;
                case "Recent Files":
                    refreshRecentFiles();
                    appendMessage(services.getRecentUploads());
                    break;
                case "Open Recent":
                    openDocument(Path.of(selectedRecent()));
                    break;
                case "Source Preview":
                    appendMessage(services.getSourcePreview(selectedRecent()));
                    break;
                case "Save Errors":
                    appendMessage(requireTab().controller.saveErrors());
                    break;
                case "Compile Error File":
                    appendMessage(requireTab().controller.getCompileError());
                    break;
                case "Delete":
                    tab = editableTab();
                    appendMessage(tab.controller.deleteCurrentFile());
                    tab.setUntitledName(nextUntitledName());
                    break;
                case "Clear":
                    if (worker != null) {
                        throw new IOException("Wait for the current operation before clearing results.");
                    }
                    tab = requireTab();
                    tab.controller.reset();
                    tab.editor.setText("");
                    tab.setUntitledName(nextUntitledName());
                    result.setText("");
                    arguments.setText("");
                    recentFiles.setSelectedIndex(-1);
                    break;
                case "Quit":
                    quit();
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            services.recordCommand(name.toUpperCase(Locale.ROOT) + " FAIL - " + e.getMessage());
            appendMessage("Error: " + e.getMessage());
        }
        updateDisplay();
    }

    EditorTab activeTab() {
        return (EditorTab) tabs.getSelectedComponent();
    }

    JTextArea activeEditor() {
        return activeTab() == null ? null : activeTab().editor;
    }

    private EditorTab requireTab() throws IOException {
        EditorTab tab = activeTab();
        if (tab == null) {
            throw new IOException("No File. Use File > New or Open first.");
        }
        return tab;
    }

    private EditorTab editableTab() throws IOException {
        EditorTab tab = requireTab();
        if (tab == workingTab) {
            throw new IOException("Wait for this tab's operation to finish (or use Stop).");
        }
        return tab;
    }

    private void newDocument() {
        EditorTab tab = makeTab(nextUntitledName());
        tabs.addTab(tab.fileName(), tab);
        tabs.setSelectedComponent(tab);
        tab.editor.requestFocusInWindow();
    }

    private EditorTab makeTab(String name) {
        EditorTab tab = new EditorTab(name, new EditorChangeListener());
        tab.editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "findReplace");
        tab.editor.getActionMap().put("findReplace", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showFindDialog();
            }
        });
        return tab;
    }

    // 탭 선택 변경과 문서 내용 변경을 이름 있는 리스너로 구분한다.
    private class TabChangeListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent event) {
            editorChanged();
        }
    }

    private class EditorChangeListener implements DocumentListener {
        @Override
        public void insertUpdate(DocumentEvent event) {
            editorChanged();
        }

        @Override
        public void removeUpdate(DocumentEvent event) {
            editorChanged();
        }

        @Override
        public void changedUpdate(DocumentEvent event) {
            editorChanged();
        }
    }

    private void editorChanged() {
        if (findDialog != null) {
            findDialog.clearMatch();
        }
        updateDisplay();
    }

    private String nextUntitledName() {
        while (true) {
            String name = "Untitled" + untitledNumber++ + ".java";
            boolean used = Files.exists(Path.of(name)) || Files.exists(chooser.getCurrentDirectory().toPath().resolve(name));
            for (int i = 0; i < tabs.getTabCount(); i++) {
                if (((EditorTab) tabs.getComponentAt(i)).fileName().equalsIgnoreCase(name)) {
                    used = true;
                }
            }
            if (!used) {
                return name;
            }
        }
    }

    private EditorTab findOpen(Path path) throws IOException {
        Path normalized = path.toAbsolutePath().normalize();
        for (int i = 0; i < tabs.getTabCount(); i++) {
            EditorTab tab = (EditorTab) tabs.getComponentAt(i);
            String current = tab.controller.getCurrentFileName();
            if (current == null) {
                continue;
            }
            Path existing = Path.of(current);
            if (existing.equals(normalized) || (Files.exists(existing) && Files.exists(normalized)
                    && Files.isSameFile(existing, normalized))) {
                return tab;
            }
        }
        return null;
    }

    private void openDocument(Path path) throws IOException {
        EditorTab existing = findOpen(path);
        if (existing != null) {
            tabs.setSelectedComponent(existing);
            if (existing != workingTab) {
                String reloaded = existing.controller.reloadIfChanged(existing.editor.getText());
                if (reloaded != null) {
                    existing.editor.setText(reloaded);
                    existing.editor.setCaretPosition(0);
                    appendMessage("Reloaded changed file - " + existing.fileName());
                } else if (existing.isModified()) {
                    appendMessage("Already open; unsaved changes kept. Close and Open to load the disk version.");
                }
            }
            return;
        }
        EditorTab tab = makeTab(path.getFileName().toString());
        String source = tab.controller.openSource(path.toString());
        tab.editor.setText(source);
        tab.editor.setCaretPosition(0);
        tabs.addTab(tab.fileName(), tab);
        tabs.setSelectedComponent(tab);
        appendMessage("Opened successfully - " + tab.controller.getCurrentFileName());
        refreshRecentFiles();
        tab.editor.requestFocusInWindow();
    }

    private void saveDocument(boolean saveAs) throws IOException {
        EditorTab tab = editableTab();
        if (!saveAs && confirmSave(tab.fileName()) != JOptionPane.OK_OPTION) {
            services.recordCommand("SAVE CANCELLED - " + tab.fileName());
            return;
        }
        if (saveAs || tab.controller.getCurrentFileName() == null) {
            Path path = chooseFile(true, tab.fileName());
            if (path == null) {
                services.recordCommand("SAVE CANCELLED - " + tab.fileName());
                return;
            }
            if (Files.exists(path) || findOpen(path) != null) {
                String message = "File already exists: " + path;
                warning(message);
                throw new IOException(message);
            }
            try {
                appendMessage(tab.controller.saveSource(path.toString(), tab.editor.getText()));
            } catch (IOException e) {
                if (Files.exists(path)) {
                    warning(e.getMessage());
                }
                throw e;
            }
        } else {
            appendMessage(tab.controller.saveSource("", tab.editor.getText()));
        }
        refreshRecentFiles();
    }

    // 대화상자 호출만 분리해 테스트에서도 같은 메뉴 이벤트/저장 경로를 검증한다.
    protected Path chooseFile(boolean save, String suggestion) {
        if (save) {
            chooser.setSelectedFile(new java.io.File(chooser.getCurrentDirectory(), suggestion));
        }
        int choice;
        if (save) {
            choice = chooser.showSaveDialog(this);
        } else {
            choice = chooser.showOpenDialog(this);
        }
        if (choice == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile().toPath();
        }
        return null;
    }

    protected int confirmSave(String name) {
        return JOptionPane.showConfirmDialog(this, "Save " + name + "?", "Save",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
    }

    protected void warning(String text) {
        JOptionPane.showMessageDialog(this, text, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    protected boolean confirmDiscard(String name) {
        return JOptionPane.showConfirmDialog(this, "Discard unsaved changes to " + name + "?", "Unsaved changes",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION;
    }

    private void closeDocument() throws IOException {
        EditorTab tab = editableTab();
        if (tab.isModified() && !confirmDiscard(tab.fileName())) {
            return;
        }
        if (findDialog != null) {
            findDialog.clearMatch();
        }
        tabs.remove(tab);
    }

    private void startOperation(boolean run) throws IOException {
        if (worker != null) {
            throw new IOException("Another Compile/Run operation is in progress.");
        }
        EditorTab target = requireTab();
        if (target.controller.getCurrentFileName() == null) {
            throw new IOException("Save " + target.fileName() + " before compiling or running.");
        }

        // 시작할 때 선택 탭과 입력을 복사한다. 실행 중 탭을 바꾸어도 대상은 같다.
        String source = target.editor.getText();
        String args = arguments.getText();
        boolean timeout = timeLimit.isSelected();
        workingTab = target;
        running = run;
        stopRequested = false;
        if (!run) {
            appendMessage(">>> compiling " + target.fileName() + " ...");
        }

        // 13장 예제와 같이 Thread를 상속한 객체를 만든 뒤 start()한다.
        worker = new OperationThread(run, target, source, args, timeout);
        updateDisplay();
        worker.start();
    }

    private class OperationThread extends Thread {
        private final boolean runProgram;
        private final EditorTab target;
        private final String source;
        private final String args;
        private final boolean timeout;

        OperationThread(boolean runProgram, EditorTab target, String source,
                String args, boolean timeout) {
            super(runProgram ? "javaide-run" : "javaide-compile");
            this.runProgram = runProgram;
            this.target = target;
            this.source = source;
            this.args = args;
            this.timeout = timeout;
        }

        @Override
        public void run() {
            try {
                if (runProgram) {
                    executeProgram();
                } else {
                    compileSource();
                }
            } catch (IOException | RuntimeException e) {
                String command = runProgram ? "RUN" : "COMPILE";
                services.recordCommand(command + " FAIL - " + e.getMessage());
                showMessageLater("Error: " + e.getMessage());
            } finally {
                finishOperationLater();
            }
        }

        private void compileSource() throws IOException {
            String message = target.controller.compileEditor(source);
            showMessageLater(message);
        }

        private void executeProgram() {
            ProgramOutputListener listener = new ProgramOutputListener();
            RunResult execution = target.controller.runProgram(source, args, listener, timeout);
            showRunResultLater(execution);
        }
    }

    // ActionListener처럼 출력 알림을 받는 리스너이다.
    // ProgramRunner는 화면을 모르고 onOutput(text)만 호출한다.
    private class ProgramOutputListener implements ProgramRunner.OutputListener {
        @Override
        public void onOutput(String text) {
            appendOutputLater(text);
        }
    }

    // 아래 메서드들은 화면 작업을 EDT에 등록한다.
    // invokeLater의 Runnable은 새 작업 스레드가 아니라 EDT에서 실행된다.
    private void appendOutputLater(final String text) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                appendRaw(text);
            }
        });
    }

    private void showMessageLater(final String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                appendMessage(message);
            }
        });
    }

    private void showRunResultLater(final RunResult execution) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                showRunResult(execution);
            }
        });
    }

    private void showRunResult(RunResult execution) {
        if (!execution.getMessage().isEmpty()) {
            appendMessage(execution.getMessage());
        }
        if (execution.isExecuted()) {
            appendMessage("Exit Code: " + execution.getExitCode());
            appendMessage("Run time: " + execution.getRunDurationMillis() + " ms");
        }
    }

    private void finishOperationLater() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                finishOperation();
            }
        });
    }

    private void finishOperation() {
        worker = null;
        workingTab = null;
        running = false;
        stopRequested = false;
        updateDisplay();
        if (closing) {
            finishQuit();
        }
    }

    private void stopProgram() {
        if (worker != null && running && !stopRequested) {
            stopRequested = true;
            worker.interrupt();
            appendMessage("Stop requested.");
        }
    }

    private void quit() {
        if (worker != null && !running) {
            appendMessage("Wait for compilation to finish before quitting.");
            return;
        }
        for (int i = 0; i < tabs.getTabCount(); i++) {
            EditorTab tab = (EditorTab) tabs.getComponentAt(i);
            if (tab.isModified() && !confirmDiscard(tab.fileName())) {
                return;
            }
        }
        closing = true;
        if (worker != null) {
            services.recordCommand("STOP - Quit");
            stopProgram();
        } else {
            finishQuit();
        }
    }

    private void finishQuit() {
        services.exit();
        dispose();
    }

    @Override
    public void dispose() {
        if (worker != null && running && !stopRequested) {
            stopRequested = true;
            worker.interrupt();
        }
        if (findDialog != null) {
            findDialog.dispose();
        }
        super.dispose();
    }

    private void showFindDialog() {
        if (activeTab() == null) {
            return;
        }
        if (findDialog == null) {
            findDialog = new FindReplaceDialog(this);
        }
        findDialog.setVisible(true);
    }

    private void refreshRecentFiles() {
        Object selected = recentFiles.getSelectedItem();
        recentFiles.removeAllItems();
        for (String path : services.getPreviewFiles()) {
            recentFiles.addItem(path);
        }
        if (selected != null) {
            recentFiles.setSelectedItem(selected);
        }
    }

    private String selectedRecent() throws IOException {
        Object selected = recentFiles.getSelectedItem();
        if (selected == null) {
            throw new IOException("Select a recent file first.");
        }
        return selected.toString();
    }

    private void appendRaw(String text) {
        result.append(text);
        result.setCaretPosition(result.getDocument().getLength());
    }

    private void appendMessage(String text) {
        if (result.getDocument().getLength() > 0 && !result.getText().endsWith("\n")) {
            appendRaw("\n");
        }
        appendRaw(text + (text.endsWith("\n") ? "" : "\n"));
    }

    private void updateDisplay() {
        for (int i = 0; i < tabs.getTabCount(); i++) {
            EditorTab tab = (EditorTab) tabs.getComponentAt(i);
            tabs.setTitleAt(i, tab.fileName() + (tab.isModified() ? " *" : ""));
            tabs.setToolTipTextAt(i, tab.controller.getCurrentFileName());
        }
        EditorTab tab = activeTab();
        if (tab == null) {
            status.setText("No File | Lines: 0 | Compile: -");
        } else {
            status.setText("File: " + tab.fileName()
                    + " | Lines: " + tab.lineCount()
                    + " | " + tab.controller.getCompileStatus());
        }
        for (JMenuItem item : menuItems) {
            String name = item.getText();
            boolean enabled = !closing;
            if (name.equals("Compile") || name.equals("Run")) {
                enabled &= worker == null;
            } else if (name.equals("Stop")) {
                enabled &= worker != null && running && !stopRequested;
            } else if (name.equals("Save") || name.equals("Save As") || name.equals("Close") || name.equals("Delete")) {
                enabled &= tab != null && tab != workingTab;
            } else if (name.equals("Clear")) {
                enabled &= tab != null && worker == null;
            } else if (name.equals("Save Errors") || name.equals("Compile Error File")) {
                enabled &= tab != null && tab != workingTab;
            }
            item.setEnabled(enabled);
        }
    }
}
