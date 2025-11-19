package Converting;

import Config.ConfigurationManager;
import Debug.Debug;
import UI.AppPage;
import UI.PopupFrame;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

public class Converting {
    private final Object lock = new Object();
    String outputFormat;
    String inputFormat;
    File directory;
    String previousFilePath = "";
    private int numberOfFiles;
    private String timeLeft="";

    public Converting() {
        ConfigurationManager configurationManager = new ConfigurationManager("config");
        this.outputFormat = configurationManager.getConfigValue("outputFormat");
        this.inputFormat = configurationManager.getConfigValue("inputFormat");
    }

    public void prepareCommandsForCmd() {
        ConfigurationManager configurationManager = new ConfigurationManager("config");
        this.directory = new File(configurationManager.getConfigValue("input"));
        numberOfFiles = this.directory.listFiles((dir, name) -> name.toLowerCase().endsWith(inputFormat)).length;

        LocalDateTime startTime = LocalDateTime.now();
        int filesConverted = 0;
        while(true) {
            Debug.functionDebug("Starting converting cycle");
            File[] requiredExtensionFiles = this.directory.listFiles((dir, name) -> name.toLowerCase().endsWith(inputFormat));
            Debug.functionDebug(inputFormat);
            if (requiredExtensionFiles == null || requiredExtensionFiles.length == 0) {
                Debug.functionDebug("All files were processed.");
                System.out.println("Done.");
                PopupFrame.createPopup("Done.");
                AppPage.enableStartOption();
                timeLeft="";
                AppPage.clearProgress();
                break;
            }

            if(filesConverted>0){
                long secondsPassed = Duration.between(startTime, LocalDateTime.now()).getSeconds();
                BigDecimal secondsPerFile = new BigDecimal(secondsPassed)
                        .divide(new BigDecimal(filesConverted), 10, RoundingMode.HALF_UP);
                BigDecimal secondsLeft = secondsPerFile.multiply(new BigDecimal(requiredExtensionFiles.length));
                Duration d = Duration.ofSeconds(secondsLeft.longValue());

                long h = d.toHours();
                long m = d.toMinutesPart();
                long s = d.toSecondsPart();

                if(h==0 && m!=0){
                    timeLeft = String.format("%dm %ds", m, s);
                } else if (h == 0) {
                    timeLeft = String.format("%ds", s);
                }else {
                    timeLeft = String.format("%dh %dm %ds", h, m, s);
                }
            }

            AppPage.updateProgress(numberOfFiles-requiredExtensionFiles.length,numberOfFiles,timeLeft);

            File currentFile = requiredExtensionFiles[0];
            String currentFilePath = currentFile.getAbsolutePath();
            if (this.previousFilePath.equals(currentFilePath)) {
                Debug.functionDebug("PREVIOUS FILE IS MET");
                this.deleteFile(currentFile);
                System.out.println("ATTEMPTED TO DELETE");
            } else {
                if (!this.executeCmdCommand(this.createConvertingCommand(currentFile).toString())) {
                    Debug.functionDebug("callback is not good :<");
                    timeLeft="";
                    AppPage.clearProgress();
                    break;
                }

                this.deleteFile(currentFile);
                this.previousFilePath = currentFilePath;
            }
            filesConverted++;

            Debug.functionDebug("Cycle ended");
        }

    }

    private StringBuilder createConvertingCommand(File imageFile) {
        String result = imageFile.getName().toLowerCase().replace(inputFormat, this.outputFormat);
        System.out.println(result);
        System.out.println(this.outputFormat);
        StringBuilder command = new StringBuilder();
        command.append("magick ");
        command.append(imageFile.getAbsolutePath()).append(" ");
        command.append("-colorspace sRGB ");
        command.append("-auto-level ");
        command.append("-modulate 100,130 ");
        command.append("-auto-orient ");
        command.append(result);
        Debug.functionDebug("commandBuilder: \n" + command);
        return command;
    }

    private void deleteFile(File imageFile) {
        if (imageFile.exists()) {
            imageFile.delete();
        }

    }

    private boolean executeCmdCommand(String command) {
        synchronized(this.lock) {
            boolean var14;
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(new String[]{"cmd", "/c", command});
                processBuilder.directory(this.directory);
                Process process = processBuilder.start();
                Debug.functionDebug("executeCmdCommand: starting (cmd, /c " + command + ")");
                BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                try {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                    try {
                        StringBuilder outputBuilder = new StringBuilder();

                        String line;
                        while((line = inputReader.readLine()) != null) {
                            outputBuilder.append("Output: ").append(line).append("\n");
                        }

                        StringBuilder errorBuilder = new StringBuilder();

                        while((line = errorReader.readLine()) != null) {
                            errorBuilder.append("Error: ").append(line).append("\n");
                        }

                        int exitCode = process.waitFor();
                        String output = outputBuilder.toString();
                        String error = errorBuilder.toString();
                        System.out.println(output);
                        System.err.println(error);
                        boolean callback = exitCode == 0;
                        Debug.functionDebug("executeCmdCommand.callback: " + callback);
                        process.destroy();
                        var14 = callback;
                    } catch (Throwable var18) {
                        try {
                            errorReader.close();
                        } catch (Throwable var17) {
                            var18.addSuppressed(var17);
                        }

                        throw var18;
                    }

                    errorReader.close();
                } catch (Throwable var19) {
                    try {
                        inputReader.close();
                    } catch (Throwable var16) {
                        var19.addSuppressed(var16);
                    }

                    throw var19;
                }

                inputReader.close();
            } catch (InterruptedException | IOException var20) {
                var20.printStackTrace();
                Debug.functionDebug("executeCmdCommand: command.contains(\"There are no\")");
                return false;
            }

            return var14;
        }
    }
}
