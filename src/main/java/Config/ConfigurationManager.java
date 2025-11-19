//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package Config;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class ConfigurationManager {
    private Properties properties;
    private String configFile;

    public ConfigurationManager(String filePath) {
        this.configFile = "C:/Users/kotmi/Documents/GitHub/Converter2/src/main/resources/" + filePath + ".properties";
        this.properties = new Properties();
        this.loadConfig();
    }

    private void loadConfig() {
        try {
            InputStream inputStream = new FileInputStream(this.configFile);

            try {
                this.properties.load(inputStream);
            } catch (Throwable var5) {
                try {
                    inputStream.close();
                } catch (Throwable var4) {
                    var5.addSuppressed(var4);
                }

                throw var5;
            }

            inputStream.close();
        } catch (IOException var6) {
            var6.printStackTrace();
        }

    }

    public void saveConfig() {
        try {
            OutputStream outputStream = new FileOutputStream(this.configFile);

            try {
                this.properties.store(outputStream, (String)null);
            } catch (Throwable var5) {
                try {
                    outputStream.close();
                } catch (Throwable var4) {
                    var5.addSuppressed(var4);
                }

                throw var5;
            }

            outputStream.close();
        } catch (IOException var6) {
            var6.printStackTrace();
        }

    }

    public String getConfigValue(String key) {
        return this.properties.getProperty(key);
    }

    public String getConfigKey(String value) {
        Iterator var2 = this.properties.stringPropertyNames().iterator();

        String key;
        do {
            if (!var2.hasNext()) {
                return null;
            }

            key = (String)var2.next();
        } while(!this.properties.getProperty(key).equals(value));

        return key;
    }

    public void setConfigValue(String key, String value) {
        this.properties.setProperty(key, value);
        this.saveConfig();
    }

    public boolean isRecorded(String text) {
        if (this.properties.containsKey(text)) {
            return true;
        } else {
            return this.properties.contains(text);
        }
    }

    public List<String> getAllKeys() {
        return new ArrayList(this.properties.stringPropertyNames());
    }

    public List<String> getAllValues() {
        List<String> values = new ArrayList();
        Iterator var2 = this.properties.stringPropertyNames().iterator();

        while(var2.hasNext()) {
            String key = (String)var2.next();
            values.add(this.properties.getProperty(key));
        }

        return values;
    }

    public void cleanConfig() {
        this.properties.clear();
        this.saveConfig();
    }

    public String getLastKey() {
        return (String)this.getAllKeys().get(this.getAllKeys().size() - 1);
    }

    public void deleteKey(String key) {
        this.properties.remove(key);
        this.saveConfig();
    }

    public void deleteValue(String value) {
        this.properties.remove(this.getConfigKey(value));
        this.saveConfig();
    }
}
