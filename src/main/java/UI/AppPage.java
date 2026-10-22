package UI;

import Config.ConfigurationManager;
import Converting.Converting;
import Uploader.GoogleDriveUploader;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.Objects;

public class AppPage extends JFrame {
    private static final Color BACKGROUND = new Color(31, 34, 38);
    private static final Color SURFACE = new Color(43, 48, 54);
    private static final Color SURFACE_ALT = new Color(55, 62, 69);
    private static final Color BORDER = new Color(73, 82, 91);
    private static final Color TEXT = new Color(244, 246, 248);
    private static final Color MUTED = new Color(178, 185, 193);
    private static final Color ACCENT = new Color(190, 242, 105);
    private static final Color ACCENT_TEXT = new Color(23, 30, 18);
    private static final Color STOP_ACCENT = new Color(255, 132, 119);
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".cr2", ".cr3", ".nef", ".arw", ".dng",
            ".gif", ".bmp", ".tif", ".tiff", ".webp", ".heic", ".heif", ".avif"
    );

    private static AppPage activeInstance;

    private final ConfigurationManager configurationManager = new ConfigurationManager("config");
    private final JTextField inputPathField = new JTextField();
    private final JComboBox<String> dropdownInput = new JComboBox<>(new String[]{".cr2", ".jpg", ".png"});
    private final JComboBox<String> dropdownOutput = new JComboBox<>(new String[]{".jpg", ".png"});
    private final JButton selectButton = new JButton("Browse folders");
    private final JButton startButton = new JButton("▶   Start conversion");
    private final JButton stopButton = new JButton("■   Stop");
    private final JButton uploadButton = new JButton("↗   Upload ready results to Google Drive");
    private final JCheckBox autoUploadCheckBox = new JCheckBox("Auto-upload to Google Drive after conversion");
    private final JPanel autoUploadPanel = new JPanel(new BorderLayout());
    private final JLabel progressText = new JLabel("Converted originals are removed after each successful file.");
    private final JLabel outputPathHint = new JLabel("Choose a source folder to see the output location.");
    private final JLabel outputFolderLabel = new JLabel("NEXT RESULTS FOLDER · CLICK TO OPEN");
    private final JProgressBar progressBar = new JProgressBar(0, 100);
    private volatile Converting activeConverter;
    private Path displayedOutputDirectory;
    private Path preparedOutputDirectory;
    private String preparedForInput;
    private String preparedForFormat;
    private boolean conversionActive;
    private boolean operationRunning;
    private boolean formatsConflict;

    public AppPage() {
        super("Converter Studio");
        activeInstance = this;
        configureLookAndFeelDefaults();
        buildWindow();
        bindEvents();
        refreshState();
        setVisible(true);
    }

    private static void configureLookAndFeelDefaults() {
        UIManager.put("Component.arc", 12);
        UIManager.put("Button.arc", 12);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("ProgressBar.arc", 8);
        UIManager.put("ScrollBar.thumbArc", 999);
    }

    private void buildWindow() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(760, 620));
        setSize(900, 700);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BACKGROUND);
        root.setBorder(new EmptyBorder(24, 34, 26, 34));
        root.add(buildContent(), BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.add(buildConversionCard(), BorderLayout.CENTER);
        return content;
    }

    private JPanel buildConversionCard() {
        JPanel card = createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        card.add(sectionHeading("SOURCE FOLDER", "All matching images in nested folders are included."));
        card.add(Box.createVerticalStrut(12));
        card.add(buildFolderPicker());
        card.add(Box.createVerticalStrut(25));
        card.add(sectionHeading("CONVERSION FORMAT", "Choose which images to convert and their new format."));
        card.add(Box.createVerticalStrut(12));
        card.add(buildFormatPicker());
        card.add(Box.createVerticalStrut(18));

        autoUploadCheckBox.setOpaque(false);
        autoUploadCheckBox.setForeground(new Color(197, 205, 216));
        autoUploadCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        autoUploadCheckBox.setFocusPainted(false);
        autoUploadCheckBox.setSelected(Boolean.parseBoolean(configurationManager.getConfigValue("autoUpload")));
        autoUploadPanel.setBorder(new EmptyBorder(10, 12, 10, 12));
        autoUploadPanel.add(autoUploadCheckBox, BorderLayout.CENTER);
        updateAutoUploadStyle();
        card.add(Box.createVerticalStrut(10));
        card.add(autoUploadPanel);
        card.add(Box.createVerticalStrut(12));
        stylePrimaryButton(startButton);
        startButton.setAlignmentX(LEFT_ALIGNMENT);
        startButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        JPanel actions = new JPanel(new BorderLayout(10, 0));
        actions.setOpaque(false);
        styleStopButton();
        stopButton.setEnabled(false);
        stopButton.setPreferredSize(new Dimension(124, 50));
        actions.add(startButton, BorderLayout.CENTER);
        actions.add(stopButton, BorderLayout.EAST);
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        card.add(actions);
        card.add(Box.createVerticalStrut(12));
        uploadButton.setText("↗   Upload ready results to Google Drive");
        styleSecondaryButton(uploadButton);
        uploadButton.setHorizontalAlignment(SwingConstants.LEFT);
        card.add(uploadButton);
        card.add(Box.createVerticalStrut(13));

        JPanel progressPanel = new JPanel(new BorderLayout(0, 9));
        progressPanel.setOpaque(false);
        progressText.setForeground(MUTED);
        progressText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        progressPanel.add(progressText, BorderLayout.NORTH);
        progressBar.setStringPainted(false);
        progressBar.setBorderPainted(false);
        progressBar.setForeground(ACCENT);
        progressBar.setBackground(SURFACE_ALT);
        progressBar.setPreferredSize(new Dimension(10, 7));
        progressPanel.add(progressBar, BorderLayout.CENTER);
        progressPanel.setName("progressPanel");
        card.add(progressPanel);

        card.add(Box.createVerticalGlue());
        JPanel footer = new JPanel(new BorderLayout(0, 7));
        footer.setOpaque(false);
        footer.add(createSeparator(), BorderLayout.NORTH);
        outputFolderLabel.setForeground(MUTED);
        outputFolderLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        JPanel destinationInfo = new JPanel(new BorderLayout(0, 6));
        destinationInfo.setOpaque(false);
        destinationInfo.add(outputFolderLabel, BorderLayout.NORTH);
        outputPathHint.setForeground(new Color(207, 215, 226));
        outputPathHint.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        destinationInfo.add(outputPathHint, BorderLayout.CENTER);
        footer.add(destinationInfo, BorderLayout.CENTER);
        card.add(Box.createVerticalStrut(17));
        card.add(footer);
        return card;
    }

    private JPanel buildFolderPicker() {
        JPanel picker = new JPanel(new BorderLayout(10, 0));
        picker.setOpaque(false);
        inputPathField.setEditable(false);
        inputPathField.setForeground(new Color(218, 225, 234));
        inputPathField.setBackground(SURFACE_ALT);
        inputPathField.setCaretColor(ACCENT);
        inputPathField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        inputPathField.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(11, 13, 11, 13)));
        String savedPath = configurationManager.getConfigValue("input");
        if (savedPath != null && !savedPath.isBlank()) {
            inputPathField.setText(savedPath);
            inputPathField.setToolTipText(savedPath);
        }

        selectButton.setPreferredSize(new Dimension(138, 43));
        styleSecondaryButton(selectButton);
        picker.add(inputPathField, BorderLayout.CENTER);
        picker.add(selectButton, BorderLayout.EAST);
        picker.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        return picker;
    }

    private JPanel buildFormatPicker() {
        JPanel formats = new JPanel(new GridLayout(1, 3, 12, 0));
        formats.setOpaque(false);
        formats.add(buildFormatTile("FROM", dropdownInput));

        JLabel arrow = new JLabel("→", SwingConstants.CENTER);
        arrow.setForeground(ACCENT);
        arrow.setFont(new Font("Segoe UI", Font.PLAIN, 25));
        formats.add(arrow);
        formats.add(buildFormatTile("TO", dropdownOutput));
        formats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 104));
        return formats;
    }

    private JPanel buildFormatTile(String label, JComboBox<String> comboBox) {
        JPanel tile = new JPanel(new BorderLayout(0, 8));
        tile.setBackground(SURFACE_ALT);
        Color tileBorder = "TO".equals(label) ? ACCENT : BORDER;
        tile.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(tileBorder, "TO".equals(label) ? 2 : 1, true),
                new EmptyBorder(10, 12, 10, 12)));
        JLabel caption = smallLabel("TO".equals(label) ? "TO  ·  OUTPUT" : "FROM  ·  SOURCE");
        if ("TO".equals(label)) caption.setForeground(ACCENT);
        comboBox.setForeground(TEXT);
        comboBox.setBackground(SURFACE_ALT);
        comboBox.setFont(new Font("Segoe UI", Font.BOLD, 15));
        comboBox.setFocusable(false);
        tile.add(caption, BorderLayout.NORTH);
        tile.add(comboBox, BorderLayout.CENTER);
        return tile;
    }

    private JPanel createCard() {
        JPanel card = new JPanel();
        card.setBackground(SURFACE);
        card.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(24, 24, 20, 24)));
        return card;
    }

    private JPanel sectionHeading(String title, String detail) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(smallLabel(title));
        panel.add(Box.createVerticalStrut(5));
        JLabel hint = new JLabel(detail);
        hint.setForeground(MUTED);
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        panel.add(hint);
        return panel;
    }

    private static JLabel smallLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED);
        label.setFont(new Font("Segoe UI", Font.BOLD, 10));
        return label;
    }

    private JPanel createSeparator() {
        JPanel line = new JPanel();
        line.setBackground(BORDER);
        line.setPreferredSize(new Dimension(1, 1));
        return line;
    }

    private static void stylePrimaryButton(JButton button) {
        button.setBackground(ACCENT);
        button.setForeground(ACCENT_TEXT);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(13, 18, 13, 18));
        button.putClientProperty("JButton.buttonType", "roundRect");
    }

    private static void styleSecondaryButton(JButton button) {
        button.setBackground(SURFACE_ALT);
        button.setForeground(new Color(222, 228, 237));
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(10, 13, 10, 13)));
        button.putClientProperty("JButton.buttonType", "roundRect");
    }

    private void styleStopButton() {
        stopButton.setBackground(SURFACE_ALT);
        stopButton.setForeground(STOP_ACCENT);
        stopButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        stopButton.setFocusPainted(false);
        stopButton.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(108, 57, 57), 1, true),
                new EmptyBorder(12, 18, 12, 18)));
        stopButton.putClientProperty("JButton.buttonType", "roundRect");
    }

    private void updateAutoUploadStyle() {
        boolean enabled = autoUploadCheckBox.isSelected();
        autoUploadPanel.setOpaque(true);
        autoUploadPanel.setBackground(enabled ? ACCENT : SURFACE_ALT);
        autoUploadPanel.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(enabled ? ACCENT : BORDER, enabled ? 2 : 1, true),
                new EmptyBorder(9, 11, 9, 11)));
        autoUploadCheckBox.setForeground(enabled ? ACCENT_TEXT : new Color(197, 205, 216));
        autoUploadCheckBox.setFont(new Font("Segoe UI", enabled ? Font.BOLD : Font.PLAIN, enabled ? 13 : 12));
        autoUploadCheckBox.setText(enabled
                ? "✓  AUTO-UPLOAD ON  ·  Send to Google Drive when finished"
                : "Auto-upload to Google Drive after conversion");
        autoUploadPanel.revalidate();
        autoUploadPanel.repaint();
    }

    private void bindEvents() {
        selectButton.addActionListener(event -> selectFolder());
        dropdownInput.addActionListener(event -> {
            persistSetting("inputFormat", (String) dropdownInput.getSelectedItem());
            refreshState();
        });
        dropdownOutput.addActionListener(event -> {
            persistSetting("outputFormat", (String) dropdownOutput.getSelectedItem());
            updateOutputHint();
            refreshState();
        });
        autoUploadCheckBox.addActionListener(event -> {
            persistSetting("autoUpload", Boolean.toString(autoUploadCheckBox.isSelected()));
            updateAutoUploadStyle();
        });
        inputPathField.setText(configurationManager.getConfigValue("input"));
        inputPathField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent event) { updateOutputHint(); refreshState(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent event) { updateOutputHint(); refreshState(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent event) { updateOutputHint(); refreshState(); }
        });
        startButton.addActionListener(event -> startConversion());
        stopButton.addActionListener(event -> stopConversion());
        uploadButton.addActionListener(event -> uploadConvertedFiles());
        outputPathHint.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        outputPathHint.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                openOutputDirectory();
            }
        });
        updateOutputHint();
    }

    private void selectFolder() {
        String configuredPath = inputPathField.getText().trim();
        File initialDirectory = configuredPath.isBlank() ? null : new File(configuredPath);
        JFileChooser chooser = initialDirectory != null && initialDirectory.isDirectory()
                ? new JFileChooser(initialDirectory)
                : new JFileChooser();
        chooser.setDialogTitle("Choose an image folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setApproveButtonText("Choose Folder");

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String selectedPath = chooser.getSelectedFile().getAbsolutePath();
            inputPathField.setText(selectedPath);
            inputPathField.setToolTipText(selectedPath);
            persistSetting("input", selectedPath);
            refreshState();
        }
    }

    private void persistSetting(String key, String value) {
        if (value == null) return;
        try {
            configurationManager.setConfigValue(key, value);
        } catch (RuntimeException exception) {
            JOptionPane.showMessageDialog(this,
                    "The setting could not be saved. It will only apply until the app closes.",
                    "Could Not Save Settings", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void startConversion() {
        if (operationRunning) return;
        String selectedPath = inputPathField.getText().trim();
        String inputFormat = (String) dropdownInput.getSelectedItem();
        String outputFormat = (String) dropdownOutput.getSelectedItem();
        boolean uploadAfterConversion = autoUploadCheckBox.isSelected();
        Path reservation = matchesPreparedOutput(selectedPath, outputFormat) ? preparedOutputDirectory : null;
        if (selectedPath.isBlank()) {
            JOptionPane.showMessageDialog(this, "Choose a source folder first.", "No Folder Selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Converting converter = new Converting();
        activeConverter = converter;
        conversionActive = true;
        setBusy(true, "Preparing your conversion…");
        Instant conversionStarted = Instant.now();
        Thread worker = new Thread(() -> {
            try {
                Converting.ConversionResult result = converter.convertFolder(
                        java.nio.file.Path.of(selectedPath), inputFormat, outputFormat,
                        (completed, total, currentFile) -> updateProgress(
                                completed, total, estimatedFinish(conversionStarted, completed, total), currentFile),
                        reservation);

                if (Files.isDirectory(result.outputDirectory())) {
                    SwingUtilities.invokeLater(() -> {
                        preparedOutputDirectory = result.outputDirectory();
                        preparedForInput = selectedPath;
                        preparedForFormat = outputFormat;
                        updateOutputHint();
                    });
                }

                SwingUtilities.invokeLater(() -> {
                    conversionActive = false;
                    activeConverter = null;
                    stopButton.setEnabled(false);
                });
                if (result.cancelled()) {
                    showPopup("Stopped safely. " + result.converted() + " file(s) were converted. Originals for the active and remaining files were kept.");
                } else if (result.total() == 0) {
                    showPopup("No " + inputFormat + " files were found in the selected folder.");
                } else if (!result.failures().isEmpty()) {
                    showPopup(buildFailureSummary(result));
                } else if (uploadAfterConversion) {
                    SwingUtilities.invokeLater(() -> progressText.setText("Uploading converted images to Google Drive…"));
                    try {
                        new GoogleDriveUploader().uploadFolder(result.outputDirectory().toString());
                        showPopup("Conversion and upload completed.\nResults saved to:\n" + result.outputDirectory());
                    } catch (Exception exception) {
                        showPopup("Conversion completed, but upload failed: " + safeMessage(exception)
                                + "\n\nResults are still saved to:\n" + result.outputDirectory());
                    }
                } else {
                    showPopup("Conversion completed: " + result.converted() + " files.\nResults saved to:\n" + result.outputDirectory());
                }
            } catch (Exception exception) {
                showPopup(safeMessage(exception));
            } finally {
                finishOperation();
            }
        }, "image-conversion");
        worker.start();
    }

    private void uploadConvertedFiles() {
        if (operationRunning) return;
        String selectedPath = inputPathField.getText().trim();
        if (selectedPath.isBlank()) {
            JOptionPane.showMessageDialog(this, "Choose a source folder first.", "No Folder Selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        java.nio.file.Path outputDirectory = findReadyOutputDirectory(selectedPath, (String) dropdownOutput.getSelectedItem());
        if (outputDirectory == null) {
            JOptionPane.showMessageDialog(this, "Convert some images first. Ready results will appear here.", "No Results Ready", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String outputPath = outputDirectory.toString();

        setBusy(true, "Connecting to Google Drive…");
        Thread worker = new Thread(() -> {
            try {
                new GoogleDriveUploader().uploadFolder(outputPath);
            } catch (Exception exception) {
                showPopup(safeMessage(exception));
            } finally {
                finishOperation();
            }
        }, "drive-upload");
        worker.start();
    }

    private void setBusy(boolean busy, String message) {
        operationRunning = busy;
        selectButton.setEnabled(!busy);
        dropdownInput.setEnabled(!busy);
        dropdownOutput.setEnabled(!busy);
        autoUploadCheckBox.setEnabled(!busy);
        uploadButton.setEnabled(!busy);
        progressText.setText(message);
        progressBar.setValue(0);
        progressBar.setIndeterminate(busy);
        progressBar.setVisible(busy);
        progressText.setVisible(busy);
        stopButton.setEnabled(busy && conversionActive);
        if (!busy) {
            activeConverter = null;
            conversionActive = false;
            stopButton.setEnabled(false);
        }
        refreshState();
    }

    private void stopConversion() {
        Converting converter = activeConverter;
        if (!conversionActive || converter == null) return;
        stopButton.setEnabled(false);
        progressText.setText("Stopping safely… The active original will be kept.");
        converter.cancel();
    }

    private static String estimatedFinish(Instant started, int completed, int total) {
        if (completed <= 0 || total <= completed) return "";
        long elapsedMillis = Math.max(1, Duration.between(started, Instant.now()).toMillis());
        long remainingMillis = (long) Math.ceil((double) elapsedMillis * (total - completed) / completed);
        Instant finish = Instant.now().plusMillis(remainingMillis);
        return "Estimated finish " + DateTimeFormatter.ofPattern("HH:mm")
                .withZone(ZoneId.systemDefault()).format(finish);
    }

    private void finishOperation() {
        SwingUtilities.invokeLater(() -> {
            setBusy(false, "Converted originals are removed after each successful file.");
            refreshState();
        });
    }

    private void refreshState() {
        formatsConflict = Objects.equals(dropdownInput.getSelectedItem(), dropdownOutput.getSelectedItem());
        boolean hasFolder = !inputPathField.getText().trim().isBlank();
        java.nio.file.Path readyOutput = hasFolder
                ? findReadyOutputDirectory(inputPathField.getText().trim(), (String) dropdownOutput.getSelectedItem())
                : null;
        boolean resultsReady = readyOutput != null;
        startButton.setEnabled(!operationRunning && hasFolder && !formatsConflict);
        uploadButton.setEnabled(!operationRunning && resultsReady);
        uploadButton.setText(resultsReady
                ? "↗   Upload ready results to Google Drive"
                : hasFolder ? "↗   No converted photos ready to upload yet" : "↗   Choose a source folder first");
        if (resultsReady) {
            uploadButton.setForeground(ACCENT);
            uploadButton.setBorder(new CompoundBorder(
                    BorderFactory.createLineBorder(ACCENT, 1, true),
                    new EmptyBorder(10, 13, 10, 13)));
        } else {
            styleSecondaryButton(uploadButton);
            uploadButton.setEnabled(false);
        }
        if (formatsConflict) {
            progressText.setText("Choose different input and output formats.");
            progressText.setVisible(true);
        } else if (!operationRunning && !hasFolder) {
            progressText.setText("Choose a source folder to get started.");
            progressText.setVisible(true);
        } else if (!operationRunning) {
            progressText.setText("Converted originals are removed after each successful file.");
            progressText.setVisible(true);
        }
        progressText.setForeground(formatsConflict ? new Color(255, 151, 129) : MUTED);
        revalidate();
        repaint();
    }

    private static java.nio.file.Path findReadyOutputDirectory(String input, String outputFormat) {
        java.util.List<java.nio.file.Path> candidates = Converting.existingOutputDirectoriesFor(
                java.nio.file.Path.of(input), outputFormat);
        for (int index = candidates.size() - 1; index >= 0; index--) {
            if (hasReadyPhotos(candidates.get(index))) return candidates.get(index);
        }
        return null;
    }

    private static boolean hasReadyPhotos(java.nio.file.Path directory) {
        try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.walk(directory)) {
            return files.filter(java.nio.file.Files::isRegularFile)
                    .map(path -> path.getFileName().toString().toLowerCase(java.util.Locale.ROOT))
                    .anyMatch(name -> IMAGE_EXTENSIONS.stream().anyMatch(name::endsWith));
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean matchesPreparedOutput(String input, String format) {
        return preparedOutputDirectory != null
                && Objects.equals(preparedForInput, input)
                && Objects.equals(preparedForFormat, format);
    }

    private void openOutputDirectory() {
        if (displayedOutputDirectory == null) return;
        Path target = displayedOutputDirectory;
        try {
            if (!Files.exists(target)) {
                try {
                    Files.createDirectory(target);
                    preparedOutputDirectory = target;
                    preparedForInput = inputPathField.getText().trim();
                    preparedForFormat = (String) dropdownOutput.getSelectedItem();
                    updateOutputHint();
                } catch (java.nio.file.FileAlreadyExistsException ignored) {
                    if (!Files.isDirectory(target)) throw ignored;
                }
            }
            if (!Files.isDirectory(target)) {
                JOptionPane.showMessageDialog(this, "The results path is not a folder.", "Cannot Open Folder", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!Desktop.isDesktopSupported()) {
                JOptionPane.showMessageDialog(this, "File Explorer cannot be opened on this system.", "Cannot Open Folder", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Desktop.getDesktop().open(target.toFile());
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, "Could not open the results folder: " + safeMessage(exception),
                    "Cannot Open Folder", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void updateOutputHint() {
        String input = inputPathField.getText().trim();
        if (input.isBlank()) {
            outputPathHint.setText("Choose a source folder to see the output location.");
            outputFolderLabel.setText("NEXT RESULTS FOLDER");
            outputPathHint.setForeground(MUTED);
            displayedOutputDirectory = null;
        } else {
            String format = (String) dropdownOutput.getSelectedItem();
            if (preparedOutputDirectory != null && !matchesPreparedOutput(input, format)) {
                preparedOutputDirectory = null;
                preparedForInput = null;
                preparedForFormat = null;
            }
            Path latestOutput = matchesPreparedOutput(input, format)
                    ? preparedOutputDirectory
                    : findReadyOutputDirectory(input, format);
            displayedOutputDirectory = latestOutput != null
                    ? latestOutput
                    : Converting.outputDirectoryFor(Path.of(input), format);
            outputFolderLabel.setText(latestOutput == null
                    ? "NEXT RESULTS FOLDER · CLICK TO OPEN"
                    : "RESULTS FOLDER · CLICK TO OPEN");
            outputPathHint.setText("<html><body style='width: 195px'><u>"
                    + escapeHtml(displayedOutputDirectory.toString()) + "</u> &nbsp;↗</body></html>");
            outputPathHint.setForeground(ACCENT);
            outputPathHint.setToolTipText("Click to open this folder in File Explorer");
        }
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String buildFailureSummary(Converting.ConversionResult result) {
        StringBuilder summary = new StringBuilder("Processed ")
                .append(result.converted()).append(" of ").append(result.total())
                .append(" files. Errors: ").append(result.failures().size())
                .append(". Successful results were saved to:\n")
                .append(result.outputDirectory());
        int detailsToShow = Math.min(5, result.failures().size());
        for (int index = 0; index < detailsToShow; index++) {
            summary.append("\n\n• ").append(result.failures().get(index));
        }
        if (result.failures().size() > detailsToShow) {
            summary.append("\n\nAnd ").append(result.failures().size() - detailsToShow).append(" more errors.");
        }
        return summary.toString();
    }

    private static String safeMessage(Exception exception) {
        return exception.getMessage() == null ? "The operation failed." : exception.getMessage();
    }

    private static void showPopup(String message) {
        SwingUtilities.invokeLater(() -> PopupFrame.createPopup(message));
    }

    public static void updateProgress(int done, int allItems, String timeLeft, String title) {
        AppPage app = activeInstance;
        if (app == null) return;
        SwingUtilities.invokeLater(() -> {
            if (allItems <= 0) return;
            int percentage = BigDecimal.valueOf(done)
                    .divide(BigDecimal.valueOf(allItems), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).intValue();
            app.progressBar.setValue(Math.min(100, percentage));
            app.progressBar.setVisible(true);
            app.progressText.setVisible(true);
            app.progressBar.setIndeterminate(false);
            app.progressText.setText(title + "  ·  " + done + " / " + allItems
                    + (timeLeft == null || timeLeft.isBlank() ? "" : "  ·  " + timeLeft));
        });
    }

    public static void clearProgress() {
        AppPage app = activeInstance;
        if (app == null) return;
        SwingUtilities.invokeLater(() -> {
            app.progressBar.setValue(0);
            app.progressBar.setVisible(false);
            app.progressText.setVisible(false);
        });
    }

    public static void enableStartOption() {
        if (activeInstance != null) activeInstance.refreshState();
    }

    public static void highlightConflictingFormats(boolean switcher) {
        if (activeInstance == null) return;
        activeInstance.formatsConflict = switcher;
        activeInstance.startButton.setEnabled(!switcher && !activeInstance.operationRunning
                && !activeInstance.inputPathField.getText().trim().isBlank());
    }
}
