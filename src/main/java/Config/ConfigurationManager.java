package Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ConfigurationManager {
    private final Properties properties = new Properties();
    private final Path configFile;

    public ConfigurationManager(String fileName) {
        String safeName = fileName == null || fileName.isBlank() ? "config" : fileName;
        if (safeName.contains("/") || safeName.contains("\\") || safeName.contains("..")) {
            throw new IllegalArgumentException("Invalid configuration file name.");
        }
        configFile = Path.of(System.getProperty("user.home"), ".converter2", safeName + ".properties");
        loadDefaults();
        loadConfig();
    }

    private void loadDefaults() {
        try (InputStream input = ConfigurationManager.class.getResourceAsStream("/config.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not load default configuration.", exception);
        }
        properties.putIfAbsent("input", "");
        properties.putIfAbsent("inputFormat", ".png");
        properties.putIfAbsent("outputFormat", ".jpg");
        properties.putIfAbsent("autoUpload", "false");
    }

    private void loadConfig() {
        if (!Files.isRegularFile(configFile)) {
            return;
        }
        try (InputStream input = Files.newInputStream(configFile)) {
            properties.load(input);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read configuration: " + configFile, exception);
        }
    }

    public void saveConfig() {
        Path temporaryFile = null;
        try {
            Files.createDirectories(configFile.getParent());
            temporaryFile = Files.createTempFile(configFile.getParent(), ".config-", ".tmp");
            try (var output = Files.newOutputStream(temporaryFile)) {
                properties.store(output, "Converter settings");
            }
            try {
                Files.move(temporaryFile, configFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, configFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not save configuration: " + configFile, exception);
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException ignored) {
                    // The original config is still intact; a stale temp file is safe to remove later.
                }
            }
        }
    }

    public String getConfigValue(String key) {
        return properties.getProperty(key);
    }

    public String getConfigKey(String value) {
        return properties.stringPropertyNames().stream()
                .filter(key -> java.util.Objects.equals(properties.getProperty(key), value))
                .findFirst().orElse(null);
    }

    public void setConfigValue(String key, String value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Configuration key and value are required.");
        }
        properties.setProperty(key, value);
        saveConfig();
    }

    public boolean isRecorded(String text) {
        return properties.containsKey(text) || properties.containsValue(text);
    }

    public List<String> getAllKeys() {
        return new ArrayList<>(properties.stringPropertyNames());
    }

    public List<String> getAllValues() {
        return properties.stringPropertyNames().stream()
                .map(properties::getProperty).toList();
    }

    public void cleanConfig() {
        properties.clear();
        saveConfig();
    }

    public String getLastKey() {
        List<String> keys = getAllKeys();
        return keys.isEmpty() ? null : keys.get(keys.size() - 1);
    }

    public void deleteKey(String key) {
        properties.remove(key);
        saveConfig();
    }

    public void deleteValue(String value) {
        String key = getConfigKey(value);
        if (key != null) {
            deleteKey(key);
        }
    }
}
