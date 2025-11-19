//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package Debug;

import Config.ConfigurationManager;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;

public class Debug {
    private static File file = new File("C:/Users/kotmi/Documents/GitHub/Converter2/src/main/resources/logs.txt");
    public static final String PROJECT_PATH = "C:/Users/kotmi/Documents/GitHub/Converter/src/main/";
    public static boolean isDevBuild = false;
    private ConfigurationManager configurationManager = new ConfigurationManager("config");

    public Debug() {}

    public static void functionDebug(String logs) {
        try {
            if (isDevBuild) {
                SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
                Date date = new Date(System.currentTimeMillis());

                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));

                    try {
                        writer.write(formatter.format(date) + ":    " + logs + "\r\n");
                    } catch (Throwable var7) {
                        try {
                            writer.close();
                        } catch (Throwable var6) {
                            var7.addSuppressed(var6);
                        }

                        throw var7;
                    }

                    writer.close();
                } catch (IOException var8) {
                    System.out.println("Warning: FileWriter does not start.");
                    throw new RuntimeException(var8);
                }
            }
        } catch (Exception var9) {
            var9.printStackTrace();
        }

    }

    public void cleanLogs() {
        if (isDevBuild) {
            try {
                FileWriter fileWriter = new FileWriter(file);

                try {
                    fileWriter.write("");
                    fileWriter.flush();
                } catch (Throwable var5) {
                    try {
                        fileWriter.close();
                    } catch (Throwable var4) {
                        var5.addSuppressed(var4);
                    }

                    throw var5;
                }

                fileWriter.close();
            } catch (IOException var6) {
                System.out.println("Warning: FileWriter does not start.");
                throw new RuntimeException(var6);
            }
        }

    }
}
