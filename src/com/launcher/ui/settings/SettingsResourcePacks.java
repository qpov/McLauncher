package com.launcher.ui.settings;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class SettingsResourcePacks extends JPanel {
    private JList<String> resourcePackList;
    private DefaultListModel<String> listModel;

    public SettingsResourcePacks() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBackground(new Color(0, 0, 0, 0));

        JLabel title = new JLabel("Ресурспаки");
        title.setForeground(Color.WHITE);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        add(title, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        resourcePackList = new JList<>(listModel);
        resourcePackList.setForeground(Color.WHITE);
        resourcePackList.setBackground(new Color(0, 0, 0, 0));
        resourcePackList.setOpaque(false);
        JScrollPane scrollPane = new JScrollPane(resourcePackList);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null); // убираем обводку
        add(scrollPane, BorderLayout.CENTER);
    }

    // Обновление списка ресурспаков из папки resourcepacks выбранного сервера.
    public void updateResourcePackList(String serverName) {
        listModel.clear();
        if (serverName == null) return;
        File installDir = new File("version", serverName);
        File resourceDir = new File(installDir, "resourcepacks");
        if (resourceDir.exists() && resourceDir.isDirectory()) {
            File[] files = resourceDir.listFiles();
            if (files != null && files.length > 0) {
                for (File f : files) {
                    listModel.addElement(f.getName());
                }
            } else {
                listModel.addElement("Нет установленных ресурспаков.");
            }
        } else {
            listModel.addElement("Нет установленных ресурспаков.");
        }
    }
}
