package com.launcher.ui.settings;

import javax.swing.*;
import java.awt.*;

public class SettingsSkin extends JPanel {
    public SettingsSkin() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBackground(Color.BLACK);

        JLabel label = new JLabel("Скин настройки");
        label.setForeground(Color.WHITE);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        add(label, BorderLayout.CENTER);
    }
}
