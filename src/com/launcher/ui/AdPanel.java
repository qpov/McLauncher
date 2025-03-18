package com.launcher.ui;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.Desktop;
import java.net.URI;
import java.io.File;

public class AdPanel extends JPanel {

    public AdPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);

        // HTML-контент
        String html = "<html><body style='color:white;'>"
                      + "Minecraft сервер Qm | "
                      + "<a href='https://discord.gg/NSZH6uffFa' style='color:#00bfff;'>"
                      + "https://discord.gg/NSZH6uffFa</a><br>"
                      + "Версия 1.21.4 | IP 185.217.199.129:25665"
                      + "</body></html>";

        JEditorPane adPane = new JEditorPane("text/html", html);
        adPane.setEditable(false);
        adPane.setOpaque(false);

        // Чтобы JEditorPane учитывал наш setFont(...) при HTML-контенте
        adPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);

        // Загружаем и назначаем Aptos-SemiBold
        Font semiBoldFont = loadSemiBoldFont(14f);
        adPane.setFont(semiBoldFont);

        // Обработчик ссылок
        adPane.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
                    try {
                        Desktop.getDesktop().browse(new URI(e.getURL().toString()));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        add(adPane, BorderLayout.WEST);
    }

    private Font loadSemiBoldFont(float size) {
        try {
            return Font.createFont(Font.TRUETYPE_FONT, new File("resources/fonts/Aptos-SemiBold.ttf"))
                       .deriveFont(size);
        } catch (Exception ex) {
            ex.printStackTrace();
            return new Font("SansSerif", Font.PLAIN, (int) size);
        }
    }
}
