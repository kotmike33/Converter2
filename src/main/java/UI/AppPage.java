package UI;

import Config.ConfigurationManager;
import Converting.Converting;
import Uploader.GoogleDriveUploader;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Objects;
import javax.swing.*;

public class AppPage extends JFrame {

    private JTextField inputPathField =
            new JTextField(30);

    private ConfigurationManager configurationManager =
            new ConfigurationManager("config");

    private static JButton startButton;
    private static JButton selectButton;
    private static JButton uploadSelectedFolderButton;

    private static JLabel labelForPictureIcon;

    private static JComboBox<String> dropdownInput;
    private static JComboBox<String> dropdownOutput;

    private static JLabel progressText;

    private static JCheckBox autoUploadCheckBox;

    private static boolean operationRunning = false;
    private static boolean formatsConflict = false;

    public AppPage() {

        super("Converter");

        this.inputPathField.setEditable(false);
        this.inputPathField.setFocusable(false);

        this.inputPathField.setText(
                this.configurationManager
                        .getConfigValue("input")
        );

        // =========================
        // SELECT FOLDER BUTTON
        // =========================

        selectButton =
                new JButton("Change Input Folder");

        selectButton.addActionListener((e) -> {

            JOptionPane.showMessageDialog(
                    this,
                    "Select any File from your folder!"
            );

            this.selectFolder(
                    this.inputPathField,
                    "input"
            );
        });

        // =========================
        // WINDOW
        // =========================

        this.setSize(650, 600);

        this.setDefaultCloseOperation(
                EXIT_ON_CLOSE
        );

        this.setLocationRelativeTo(null);
        this.setResizable(false);

        // =========================
        // MAIN PANEL
        // =========================

        JPanel panel =
                new JPanel(new GridBagLayout());

        GridBagConstraints gbc =
                new GridBagConstraints();

        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets =
                new Insets(10, 10, 10, 10);

        // =========================
        // INPUT FOLDER
        // =========================

        gbc.gridy = 0;

        panel.add(
                this.createPanel(
                        this.inputPathField,
                        selectButton
                ),
                gbc
        );

        // =========================
        // INPUT FORMAT
        // =========================

        gbc.gridy = 1;

        panel.add(
                new JLabel("Input format:"),
                gbc
        );

        String[] inputItems = {
                ".cr2",
                ".jpg",
                ".png"
        };

        dropdownInput =
                new JComboBox<>(inputItems);

        dropdownInput.setSelectedItem(
                this.configurationManager
                        .getConfigValue("inputFormat")
        );

        dropdownInput.addActionListener((e) -> {

            this.configurationManager.setConfigValue(
                    "inputFormat",
                    (String) dropdownInput.getSelectedItem()
            );

            checkFormatConflict();
        });

        gbc.gridy = 2;

        panel.add(
                dropdownInput,
                gbc
        );

        // =========================
        // OUTPUT FORMAT
        // =========================

        gbc.gridy = 3;

        panel.add(
                new JLabel("Output format:"),
                gbc
        );

        String[] outputItems = {
                ".jpg",
                ".png"
        };

        dropdownOutput =
                new JComboBox<>(outputItems);

        dropdownOutput.setSelectedItem(
                this.configurationManager
                        .getConfigValue("outputFormat")
        );

        dropdownOutput.addActionListener((e) -> {

            this.configurationManager.setConfigValue(
                    "outputFormat",
                    (String) dropdownOutput.getSelectedItem()
            );

            checkFormatConflict();
        });

        gbc.gridy = 4;

        panel.add(
                dropdownOutput,
                gbc
        );

        // =========================
        // START BUTTON
        // =========================

        startButton =
                new JButton("Start");

        startButton.addActionListener((e) -> {

            if (operationRunning) {
                return;
            }

            disableControls();

            Thread thread =
                    new Thread(() -> {

                        try {

                            // =========================
                            // CONVERT
                            // =========================

                            Converting converting =
                                    new Converting();

                            converting.prepareCommandsForCmd();

                            // =========================
                            // AUTO UPLOAD
                            // =========================

                            if (autoUploadCheckBox.isSelected()) {

                                System.out.println(
                                        "Starting automatic Google Drive upload..."
                                );

                                GoogleDriveUploader uploader =
                                        new GoogleDriveUploader();

                                uploader.uploadFolder(
                                        configurationManager
                                                .getConfigValue("input")
                                );
                            }

                        } catch (Exception exception) {

                            PopupFrame.createPopup(
                                    exception.getMessage()
                            );

                            exception.printStackTrace();

                        } finally {

                            enableControls();
                        }
                    });

            thread.start();
        });

        gbc.gridy = 5;

        panel.add(
                startButton,
                gbc
        );

        // =========================
// AUTO UPLOAD TO GOOGLE DRIVE
// =========================

        autoUploadCheckBox =
                new JCheckBox(
                        "Auto upload to Google Drive after conversion"
                );

        autoUploadCheckBox.setSelected(
                Boolean.parseBoolean(
                        this.configurationManager
                                .getConfigValue("autoUpload")
                )
        );

        autoUploadCheckBox.addActionListener((e) -> {

            this.configurationManager.setConfigValue(
                    "autoUpload",
                    String.valueOf(
                            autoUploadCheckBox.isSelected()
                    )
            );
        });

        gbc.gridy = 6;

        panel.add(
                autoUploadCheckBox,
                gbc
        );

        // =========================
        // PROGRESS
        // =========================

        progressText =
                new JLabel();

        progressText.setVisible(false);

        progressText.setHorizontalAlignment(
                SwingConstants.CENTER
        );

        progressText.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        20
                )
        );

        gbc.gridy = 7;

        panel.add(
                progressText,
                gbc
        );

        // =========================
        // UPLOAD BUTTON
        // =========================

        gbc.gridy = 8;

        uploadSelectedFolderButton =
                new JButton(
                        "Upload all files to Google Drive"
                );

        panel.add(
                uploadSelectedFolderButton,
                gbc
        );

        uploadSelectedFolderButton.addActionListener((e) -> {

            if (operationRunning) {
                return;
            }

            disableControls();

            Thread thread =
                    new Thread(() -> {

                        try {

                            GoogleDriveUploader uploader =
                                    new GoogleDriveUploader();

                            uploader.uploadFolder(
                                    configurationManager
                                            .getConfigValue("input")
                            );

                        } catch (Exception exception) {

                            PopupFrame.createPopup(
                                    exception.getMessage()
                            );

                            exception.printStackTrace();

                        } finally {

                            enableControls();
                        }
                    });

            thread.start();
        });

        // =========================
        // ADD PANEL
        // =========================

        this.add(panel);

        this.setVisible(true);

        // Проверяем начальное состояние
        checkFormatConflict();
    }

    // =========================================================
    // DISABLE ALL CONTROLS
    // =========================================================

    private static void disableControls() {

        SwingUtilities.invokeLater(() -> {

            operationRunning = true;

            selectButton.setEnabled(false);

            dropdownInput.setEnabled(false);

            dropdownOutput.setEnabled(false);

            startButton.setEnabled(false);

            uploadSelectedFolderButton.setEnabled(false);
        });
    }

    // =========================================================
    // ENABLE ALL CONTROLS
    // =========================================================

    private static void enableControls() {

        SwingUtilities.invokeLater(() -> {

            operationRunning = false;

            selectButton.setEnabled(true);

            dropdownInput.setEnabled(true);

            dropdownOutput.setEnabled(true);

            uploadSelectedFolderButton.setEnabled(true);

            /*
             * Start зависит от конфликта форматов.
             *
             * Если форматы одинаковые -
             * Start остаётся выключенным.
             *
             * Если разные -
             * Start включается.
             */
            startButton.setEnabled(
                    !formatsConflict
            );

            updateFormatColors();
        });
    }

    // =========================================================
    // CHECK FORMAT CONFLICT
    // =========================================================

    private static void checkFormatConflict() {

        if (dropdownInput == null ||
                dropdownOutput == null) {
            return;
        }

        formatsConflict =
                Objects.equals(
                        dropdownInput.getSelectedItem(),
                        dropdownOutput.getSelectedItem()
                );

        updateFormatColors();

        /*
         * Если сейчас выполняется операция,
         * не трогаем состояние кнопок.
         */
        if (operationRunning) {
            return;
        }

        startButton.setEnabled(
                !formatsConflict
        );
    }

    // =========================================================
    // UPDATE FORMAT COLORS
    // =========================================================

    private static void updateFormatColors() {

        if (formatsConflict) {

            dropdownInput.setBackground(
                    Color.RED
            );

            dropdownOutput.setBackground(
                    Color.RED
            );

        } else {

            dropdownInput.setBackground(
                    Color.WHITE
            );

            dropdownOutput.setBackground(
                    Color.WHITE
            );
        }
    }

    // =========================================================
    // UPDATE PROGRESS
    // =========================================================

    public static void updateProgress(
            int done,
            int allItems,
            String timeLeft,
            String title
    ) {

        SwingUtilities.invokeLater(() -> {

            try {

                if (allItems <= 0) {
                    return;
                }

                String timeLeftModified = "";

                BigDecimal percentage =
                        new BigDecimal(done)
                                .divide(
                                        new BigDecimal(allItems),
                                        6,
                                        RoundingMode.HALF_UP
                                )
                                .multiply(
                                        new BigDecimal(100)
                                );

                DecimalFormat df =
                        new DecimalFormat("0.0");

                if (done >= allItems) {

                    progressText.setVisible(false);

                    progressText.setText("");

                } else {

                    progressText.setVisible(true);

                    if (!timeLeft.isEmpty()) {

                        timeLeftModified =
                                " " +
                                        timeLeft +
                                        " left.";
                    }

                    progressText.setText(
                            title +
                                    " Progress: " +
                                    df.format(percentage) +
                                    "%  (" +
                                    done +
                                    "/" +
                                    allItems +
                                    ")" +
                                    timeLeftModified
                    );
                }

            } catch (Exception e) {

                e.printStackTrace();

                progressText.setVisible(true);

                progressText.setText(
                        "Failed to update progress"
                );
            }
        });
    }

    // =========================================================
    // CLEAR PROGRESS
    // =========================================================

    public static void clearProgress() {

        SwingUtilities.invokeLater(() -> {

            try {

                progressText.setVisible(false);

                progressText.setText("");

            } catch (Exception e) {

                e.printStackTrace();

                progressText.setVisible(true);

                progressText.setText(
                        "Failed to update progress"
                );
            }
        });
    }

    // =========================================================
    // CREATE INPUT PANEL
    // =========================================================

    private JPanel createPanel(
            JTextField textField,
            JButton button
    ) {

        JPanel panel =
                new JPanel(
                        new BorderLayout()
                );

        panel.add(
                textField,
                "Center"
        );

        panel.add(
                button,
                "East"
        );

        panel.setPreferredSize(
                new Dimension(
                        (int)
                                ((double)
                                        this.getWidth()
                                        * 0.8),
                        (int)
                                ((double)
                                        this.getHeight()
                                        * 0.1)
                )
        );

        return panel;
    }

    // =========================================================
    // SELECT FOLDER
    // =========================================================

    private void selectFolder(
            JTextField textField,
            String configKey
    ) {

        FileDialog fileDialog =
                new FileDialog(
                        this,
                        "Select a folder",
                        FileDialog.LOAD
                );

        fileDialog.setVisible(true);

        fileDialog.setLocationRelativeTo(
                (Component) null
        );

        String selectedFolderPath =
                fileDialog.getDirectory();

        if (selectedFolderPath != null &&
                !selectedFolderPath.contains(" ")) {

            textField.setText(
                    selectedFolderPath
            );

            this.configurationManager.setConfigValue(
                    configKey,
                    selectedFolderPath
            );
        }

        if (selectedFolderPath != null &&
                selectedFolderPath.contains(" ")) {

            JOptionPane.showMessageDialog(
                    null,
                    "Folder path should not contain spaces in names.",
                    "Error",
                    JOptionPane.WARNING_MESSAGE
            );

            selectFolder(
                    textField,
                    configKey
            );
        }
    }

    // =========================================================
    // PICTURE ICON
    // =========================================================

    public static void setPictureIcon(
            String pictureFile
    ) {

        labelForPictureIcon.setIcon(
                new ImageIcon(pictureFile)
        );

        labelForPictureIcon.setVisible(true);
    }

    // =========================================================
    // LEGACY METHOD
    // =========================================================

    public static void enableStartOption() {

        if (operationRunning) {
            return;
        }

        startButton.setEnabled(
                !formatsConflict
        );
    }

    // =========================================================
    // LEGACY METHOD
    // =========================================================

    public static void highlightConflictingFormats(
            boolean switcher
    ) {

        formatsConflict = switcher;

        updateFormatColors();

        if (!operationRunning) {

            startButton.setEnabled(
                    !formatsConflict
            );
        }
    }
}