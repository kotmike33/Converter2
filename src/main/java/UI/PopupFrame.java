//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package UI;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import javax.swing.*;

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
    public static void createLinkPopup(String link) {
        JFrame popupFrame = new JFrame("Popup");

        JLabel label = new JLabel(
                "<html><a href='" + link + "'>Link to Google Drive</a></html>"
        );

        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );

        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(
                            URI.create(link)
                    );
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        popupFrame.add(label);
        popupFrame.setSize(300, 100);
        popupFrame.setLocationRelativeTo(null);
        popupFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        popupFrame.setVisible(true);
    }
}
