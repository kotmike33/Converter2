//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package UI;

import java.awt.Component;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class PopupFrame {
    public PopupFrame() {
    }

    public static void createPopup(String labelText) {
        JFrame popupFrame = new JFrame("Popup");
        JLabel label = new JLabel(labelText);
        label.setHorizontalAlignment(0);
        popupFrame.add(label);
        popupFrame.setSize(300, 100);
        popupFrame.setLocationRelativeTo((Component)null);
        popupFrame.setDefaultCloseOperation(2);
        popupFrame.setVisible(true);
    }
}
