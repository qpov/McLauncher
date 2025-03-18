package com.launcher.ui.settings;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class SettingsShaders extends JPanel {
    private JList<String> shaderList;
    private DefaultListModel<String> listModel;

    public SettingsShaders() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBackground(new Color(0, 0, 0, 0));

        JLabel title = new JLabel("Шейдеры");
        title.setForeground(Color.WHITE);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        add(title, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        shaderList = new JList<>(listModel);
        shaderList.setForeground(Color.WHITE);
        shaderList.setBackground(new Color(0, 0, 0, 0)); // прозрачный фон
        shaderList.setOpaque(false);
        JScrollPane scrollPane = new JScrollPane(shaderList);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null); // убираем обводку
        add(scrollPane, BorderLayout.CENTER);
    }

    // Обновление списка шейдеров из папки shaderpacks выбранного сервера.
    public void updateShaderList(String serverName) {
        listModel.clear();
        if (serverName == null) return;
        File installDir = new File("version", serverName);
        File shaderDir = new File(installDir, "shaderpacks");
        if (shaderDir.exists() && shaderDir.isDirectory()) {
            File[] files = shaderDir.listFiles();
            if (files != null && files.length > 0) {
                for (File f : files) {
                    listModel.addElement(f.getName());
                }
            } else {
                listModel.addElement("Нет установленных шейдеров.");
            }
        } else {
            listModel.addElement("Нет установленных шейдеров.");
        }
    }
}
