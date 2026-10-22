//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package Main;

import Debug.Debug;
import UI.AppPage;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.SwingUtilities;

public class Main {
    public Main() {
    }

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        SwingUtilities.invokeLater(AppPage::new);
        Debug debug = new Debug();
        debug.cleanLogs();
    }
}
