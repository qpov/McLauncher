package com.launcher.ui.settings;

import com.launcher.ServerConfig;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SettingsMods extends JPanel {

    private JPanel modPanel;
    private JButton toggleAllButton;

    public SettingsMods() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBackground(new Color(0, 0, 0, 0)); // прозрачный фон

        // Заголовочная панель с кнопкой слева и заголовком "Моды" по центру
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        // Создаём анонимный класс для кнопки, чтобы вручную рисовать полупрозрачный фон
        toggleAllButton = new JButton("Включить все моды") {
            @Override
            protected void paintComponent(Graphics g) {
                // Рисуем полупрозрачный чёрный фон
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setColor(new Color(0, 0, 0, 128)); // 50% прозрачность
                // Немного скруглим углы
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2d.dispose();

                // Стандартная отрисовка кнопки (текст и т.п.)
                super.paintComponent(g);
            }
        };
        toggleAllButton.setForeground(Color.WHITE);
        toggleAllButton.setFocusPainted(false);
        toggleAllButton.setRolloverEnabled(false);
        toggleAllButton.setContentAreaFilled(false);
        toggleAllButton.setOpaque(false);
        toggleAllButton.setBorder(null);

        // Обработчик нажатия
        toggleAllButton.addActionListener(e -> {
            boolean allSelected = true;
            for (Component comp : modPanel.getComponents()) {
                if (comp instanceof JCheckBox) {
                    if (!((JCheckBox) comp).isSelected()) {
                        allSelected = false;
                        break;
                    }
                }
            }
            // Если все включены, при нажатии выключаем их, иначе включаем
            boolean newState = !allSelected;
            for (Component comp : modPanel.getComponents()) {
                if (comp instanceof JCheckBox) {
                    ((JCheckBox) comp).setSelected(newState);
                }
            }
            // Обновляем текст кнопки
            toggleAllButton.setText(newState ? "Выключить все моды" : "Включить все моды");
        });

        headerPanel.add(toggleAllButton, BorderLayout.WEST);
        
        // Заголовок "Моды" по центру
        JLabel title = new JLabel("Моды");
        title.setForeground(Color.WHITE);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        headerPanel.add(title, BorderLayout.CENTER);
        
        add(headerPanel, BorderLayout.NORTH);

        // Панель для списка модов
        modPanel = new JPanel();
        modPanel.setLayout(new BoxLayout(modPanel, BoxLayout.Y_AXIS));
        modPanel.setOpaque(false);
        JScrollPane scrollPane = new JScrollPane(modPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null); // убираем обводку
        add(scrollPane, BorderLayout.CENTER);
    }

    // Обновление списка модов для выбранного сервера.
    public void updateModPanel(String serverName) {
        modPanel.removeAll();
        if (serverName == null) {
            modPanel.revalidate();
            modPanel.repaint();
            return;
        }
        File installDir = new File("version", serverName);
        File modsFolder = new File(installDir, "mods");
        List<String> enabledMods = new ArrayList<>();
        List<String> disabledMods = new ArrayList<>();
        if (modsFolder.exists()) {
            // Получаем включённые моды: файлы, лежащие непосредственно в папке mods
            File[] files = modsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
            if (files != null) {
                for (File f : files) {
                    enabledMods.add(f.getName());
                }
            }
            // Убедимся, что папка отключённых модов "disabled" существует
            File disabledFolder = new File(modsFolder, "disabled");
            if (!disabledFolder.exists()) {
                disabledFolder.mkdirs();
            }
            // Получаем отключённые моды из папки "disabled"
            File[] disFiles = disabledFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
            if (disFiles != null) {
                for (File f : disFiles) {
                    disabledMods.add(f.getName());
                }
            }
        }
        if (!enabledMods.isEmpty() || !disabledMods.isEmpty()) {
            // Сначала добавляем включённые моды
            for (String modName : enabledMods) {
                JCheckBox cb = new JCheckBox(modName, true);
                cb.setOpaque(false);
                cb.setForeground(Color.WHITE);
                modPanel.add(cb);
            }
            // Затем добавляем отключённые моды
            for (String modName : disabledMods) {
                JCheckBox cb = new JCheckBox(modName, false);
                cb.setOpaque(false);
                cb.setForeground(Color.WHITE);
                modPanel.add(cb);
            }
        } else {
            JLabel noMods = new JLabel("Нет установленных модов.");
            noMods.setForeground(Color.WHITE);
            noMods.setOpaque(false);
            modPanel.add(noMods);
        }
        modPanel.revalidate();
        modPanel.repaint();
        // Определяем, все ли моды включены
        boolean allSelected = true;
        for (Component comp : modPanel.getComponents()) {
            if (comp instanceof JCheckBox) {
                if (!((JCheckBox) comp).isSelected()) {
                    allSelected = false;
                    break;
                }
            }
        }
        toggleAllButton.setText(allSelected ? "Выключить все моды" : "Включить все моды");
    }

    // Применение выбранных настроек: если мод отключён (чекбокс не выбран), перемещаем файл в папку "disabled", иначе – обратно.
    public void applyModSelection(ServerConfig selectedServer) {
        if (selectedServer == null) return;
        File installDir = new File("version", selectedServer.name);
        File modsFolder = new File(installDir, "mods");
        if (!modsFolder.exists()) {
            modsFolder.mkdirs();
        }
        File disabledFolder = new File(modsFolder, "disabled");
        if (!disabledFolder.exists()) {
            disabledFolder.mkdirs();
        }
        for (Component comp : modPanel.getComponents()) {
            if (comp instanceof JCheckBox) {
                JCheckBox cb = (JCheckBox) comp;
                String modFileName = cb.getText();
                File modFile = new File(modsFolder, modFileName);
                File disabledFile = new File(disabledFolder, modFileName);
                if (!cb.isSelected()) {
                    if (modFile.exists()) {
                        modFile.renameTo(disabledFile);
                    }
                } else {
                    if (disabledFile.exists()) {
                        disabledFile.renameTo(modFile);
                    }
                }
            }
        }
    }
}
