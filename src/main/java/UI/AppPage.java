package UI;

import Config.ConfigurationManager;
import Converting.Converting;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Objects;
import javax.swing.*;

public class AppPage extends JFrame {
    private JTextField inputPathField = new JTextField(30);
    private ConfigurationManager configurationManager = new ConfigurationManager("config");
    private static JButton startButton;
    private static JLabel labelForPictureIcon;
    private static JComboBox<String> dropdownInput;
    private static JComboBox<String> dropdownOutput;
    private static JLabel progressText;

    public AppPage() {
        super("Converter");

        this.inputPathField.setEditable(false);
        this.inputPathField.setFocusable(false);
        this.inputPathField.setText(this.configurationManager.getConfigValue("input"));

        JButton selectButton = new JButton("Change Input Folder");
        selectButton.addActionListener((e) -> {
            JOptionPane.showMessageDialog(this, "Select any File from your folder!");
            this.selectFolder(this.inputPathField, "input");
        });

        this.setSize(650, 600);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);

        gbc.gridy = 0;
        panel.add(this.createPanel(this.inputPathField, selectButton), gbc);

        gbc.gridy = 1;
        panel.add(new JLabel("Input format:"), gbc);

        String[] inputItems = new String[]{".cr2", ".jpg", ".png"};
        dropdownInput = new JComboBox<>(inputItems);
        gbc.gridy = 2;
        panel.add(dropdownInput, gbc);
        dropdownInput.setSelectedItem(this.configurationManager.getConfigValue("inputFormat"));
        dropdownInput.addActionListener((e) -> {
            this.configurationManager.setConfigValue("inputFormat", (String) dropdownInput.getSelectedItem());
            if(Objects.equals(dropdownInput.getSelectedItem(), dropdownOutput.getSelectedItem())){
                highlightConflictingFormats(true);
            }else {
                highlightConflictingFormats(false);
            }
        });

        gbc.gridy = 3;
        panel.add(new JLabel("Output format:"), gbc);

        String[] outputItems = new String[]{".jpg", ".png"};
        dropdownOutput = new JComboBox<>(outputItems);
        gbc.gridy = 4;
        panel.add(dropdownOutput, gbc);
        dropdownOutput.setSelectedItem(this.configurationManager.getConfigValue("outputFormat"));
        dropdownOutput.addActionListener((e) -> {
            this.configurationManager.setConfigValue("outputFormat", (String) dropdownOutput.getSelectedItem());
            if(Objects.equals(dropdownInput.getSelectedItem(), dropdownOutput.getSelectedItem())){
                highlightConflictingFormats(true);
            }else {
                highlightConflictingFormats(false);
            }
        });

        startButton = new JButton("Start");
        startButton.addActionListener((e) -> {
            startButton.setEnabled(false);
            startButton.setBackground(Color.RED);
            Thread thread = new Thread(() -> {
                Converting converting = new Converting();
                converting.prepareCommandsForCmd();
            });
            thread.start();
        });

        gbc.gridy = 5;
        panel.add(startButton, gbc);

        progressText = new JLabel();
        progressText.setVisible(false);
        progressText.setHorizontalAlignment(SwingConstants.CENTER);
        progressText.setFont(new Font("Arial", Font.BOLD, 20));
        gbc.gridy = 6;
        panel.add(progressText,gbc);

        this.add(panel);
        this.setVisible(true);
    }

    public static void updateProgress(int done, int allItems, String timeLeft) {
        try {
            String timeLeftModified = "";
            BigDecimal percentage = new BigDecimal(done)
                    .divide(new BigDecimal(allItems), 6, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100));

            DecimalFormat df = new DecimalFormat("0.0");

            if (done == allItems) {
                progressText.setVisible(false);
                progressText.setText("");
            } else {
                progressText.setVisible(true);
                if(!timeLeft.isEmpty()){
                    timeLeftModified = " " + timeLeft + " left.";
                }
                progressText.setText("Progress: " + df.format(percentage) + "%  (" + done + "/" + allItems + ")" + timeLeftModified);
            }
        }catch (Exception e){
            e.printStackTrace();
            progressText.setVisible(true);
            progressText.setText("Failed to update progress");
        }
    }

    public static void clearProgress() {
        try {
                progressText.setVisible(false);
                progressText.setText("");
        }catch (Exception e){
            e.printStackTrace();
            progressText.setVisible(true);
            progressText.setText("Failed to update progress");
        }
    }

    private JPanel createPanel(JTextField textField, JButton button) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(textField, "Center");
        panel.add(button, "East");
        panel.setPreferredSize(new Dimension((int)((double)this.getWidth() * 0.8), (int)((double)this.getHeight() * 0.1)));
        return panel;
    }

    private void selectFolder(JTextField textField, String configKey) {
        FileDialog fileDialog = new FileDialog(this, "Select a folder", 0);
        fileDialog.setVisible(true);
        fileDialog.setLocationRelativeTo((Component)null);
        String selectedFolderPath = fileDialog.getDirectory();

        if (selectedFolderPath != null && !selectedFolderPath.contains(" ")) {
            textField.setText(selectedFolderPath);
            this.configurationManager.setConfigValue(configKey, selectedFolderPath);
        }

        if(selectedFolderPath != null && selectedFolderPath.contains(" ")){
            JOptionPane.showMessageDialog(
                    null,
                    "Folder path should not contain spaces in names.",
                    "Error",
                    JOptionPane.WARNING_MESSAGE
            );
            selectFolder(textField,configKey);
        }
    }

    public static void setPictureIcon(String pictureFile) {
        labelForPictureIcon.setIcon(new ImageIcon(pictureFile));
        labelForPictureIcon.setVisible(true);
    }

    public static void enableStartOption() {
        startButton.setEnabled(true);
        startButton.setBackground(Color.LIGHT_GRAY);
    }

    public static void highlightConflictingFormats(boolean switcher){
        if(switcher) {
            startButton.setEnabled(false);
            dropdownInput.setBackground(Color.RED);
            dropdownOutput.setBackground(Color.RED);
        }else {
            startButton.setEnabled(true);
            dropdownInput.setBackground(Color.WHITE);
            dropdownOutput.setBackground(Color.WHITE);
        }
    }
}
