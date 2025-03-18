package com.launcher.ui;

import com.launcher.ServerConfig;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Окно «Настройки» в виде JDialog: RAM, чекбокс "Скрывать лаунчер",
 * кнопки "Переустановить", "Сменить тему", панель модов.
 */
public class SettingsDialog extends JDialog {

    private Properties settings;
    private JTextField ramField;
    private JCheckBox hideLauncherCheckBox;
    private JButton reinstallGameButton;
    private JButton toggleThemeButton;
    private JPanel modPanel;
    private JButton toggleModsButton;

    public SettingsDialog(Frame owner, Properties settings) {
        super(owner, "Настройки", true);
        this.settings = settings;

        setSize(500, 400);
        setLocationRelativeTo(owner);

        initSettingsUI();
    }

    private void initSettingsUI() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        getContentPane().add(panel);

        // Верхняя часть
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));

        // RAM
        topPanel.add(new JLabel("Макс. ОЗУ (ГБ):"));
        ramField = new JTextField(settings.getProperty("ram", "2"), 5);
        ((AbstractDocument) ramField.getDocument()).setDocumentFilter(new DigitFilter());
        ramField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                settings.setProperty("ram", ramField.getText().trim());
                saveSettings();
            }
        });
        topPanel.add(ramField);

        // Скрывать лаунчер
        hideLauncherCheckBox = new JCheckBox("Скрывать лаунчер",
                Boolean.parseBoolean(settings.getProperty("hideLauncher", "true")));
        hideLauncherCheckBox.addActionListener(e -> {
            settings.setProperty("hideLauncher", Boolean.toString(hideLauncherCheckBox.isSelected()));
            saveSettings();
        });
        topPanel.add(hideLauncherCheckBox);

        panel.add(topPanel, BorderLayout.NORTH);

        // Центр - панель модов
        modPanel = new JPanel();
        modPanel.setBorder(BorderFactory.createTitledBorder("Моды"));
        modPanel.setLayout(new BoxLayout(modPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(modPanel);
        panel.add(scrollPane, BorderLayout.CENTER);

        // ---------- Добавляем «нижнюю чёрную панель» ----------
        JPanel blackBottomPanel = new JPanel(new BorderLayout());
        blackBottomPanel.setBackground(Color.BLACK);

        // Внутри неё — кнопки (FlowLayout)
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonsPanel.setBackground(Color.BLACK);

        reinstallGameButton = new JButton("Переустановить");
        reinstallGameButton.setBackground(Color.BLACK);
        reinstallGameButton.setForeground(Color.WHITE);
        reinstallGameButton.setFocusPainted(false);
        reinstallGameButton.addActionListener(e -> {
            int resp = JOptionPane.showConfirmDialog(this,
                    "Вы уверены, что хотите переустановить?",
                    "Подтверждение", JOptionPane.YES_NO_OPTION);
            if (resp == JOptionPane.YES_OPTION) {
                // Здесь вызываем логику переустановки
                JOptionPane.showMessageDialog(this, "Здесь вызываем логику переустановки.");
            }
        });
        buttonsPanel.add(reinstallGameButton);

        toggleThemeButton = new JButton("Сменить тему");
        toggleThemeButton.setBackground(Color.BLACK);
        toggleThemeButton.setForeground(Color.WHITE);
        toggleThemeButton.setFocusPainted(false);
        toggleThemeButton.addActionListener(e -> {
            String cur = settings.getProperty("theme", "dark");
            String next = cur.equals("dark") ? "light" : "dark";
            settings.setProperty("theme", next);
            saveSettings();
            // Меняем LAF на лету
            if ("dark".equalsIgnoreCase(next)) {
                try {
                    UIManager.setLookAndFeel(new FlatDarkLaf());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                try {
                    UIManager.setLookAndFeel(new FlatLightLaf());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            SwingUtilities.updateComponentTreeUI(getOwner());
            SwingUtilities.updateComponentTreeUI(this);
        });
        buttonsPanel.add(toggleThemeButton);

        toggleModsButton = new JButton("Включить все моды");
        toggleModsButton.setBackground(Color.BLACK);
        toggleModsButton.setForeground(Color.WHITE);
        toggleModsButton.setFocusPainted(false);
        toggleModsButton.addActionListener(e -> {
            boolean allSelected = true;
            for (Component comp : modPanel.getComponents()) {
                if (comp instanceof JCheckBox) {
                    if (!((JCheckBox) comp).isSelected()) {
                        allSelected = false;
                        break;
                    }
                }
            }
            boolean newState = !allSelected;
            for (Component comp : modPanel.getComponents()) {
                if (comp instanceof JCheckBox) {
                    ((JCheckBox) comp).setSelected(newState);
                }
            }
        });
        buttonsPanel.add(toggleModsButton);

        // Добавляем панель кнопок внутрь чёрной панели
        blackBottomPanel.add(buttonsPanel, BorderLayout.CENTER);

        // А чёрную панель добавляем вниз
        panel.add(blackBottomPanel, BorderLayout.SOUTH);
    }

    /**
     * Обновляем список модов для выбранного сервера.
     */
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
            File[] files = modsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
            if (files != null) {
                for (File f : files) {
                    enabledMods.add(f.getName());
                }
            }
            File disFolder = new File(modsFolder, "disabled");
            if (disFolder.exists()) {
                File[] disFiles = disFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
                if (disFiles != null) {
                    for (File f : disFiles) {
                        disabledMods.add(f.getName());
                    }
                }
            }
        }
        if (!enabledMods.isEmpty() || !disabledMods.isEmpty()) {
            for (String modName : enabledMods) {
                JCheckBox cb = new JCheckBox(modName, true);
                cb.setBackground(Color.WHITE); // или что-то другое
                modPanel.add(cb);
            }
            for (String modName : disabledMods) {
                JCheckBox cb = new JCheckBox(modName, false);
                cb.setBackground(Color.WHITE);
                modPanel.add(cb);
            }
        } else {
            modPanel.add(new JLabel("Нет установленных модов."));
        }
        modPanel.revalidate();
        modPanel.repaint();
    }

    /**
     * Применяем выбранные моды (перемещаем между папками mods и mods/disabled).
     */
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
                File disFile = new File(disabledFolder, modFileName);
                if (!cb.isSelected()) {
                    if (modFile.exists()) {
                        modFile.renameTo(disFile);
                    }
                } else {
                    if (disFile.exists()) {
                        disFile.renameTo(modFile);
                    }
                }
            }
        }
    }

    /**
     * Сохранение настроек (ram, theme, hideLauncher).
     */
    private void saveSettings() {
        File file = new File("settings.txt");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            settings.store(fos, "Launcher Settings");
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Фильтр для ввода только цифр.
     */
    private static class DigitFilter extends DocumentFilter {
        @Override
        public void insertString(DocumentFilter.FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
            if (string.matches("\\d+")) {
                super.insertString(fb, offset, string, attr);
            }
        }
        @Override
        public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            if (text.matches("\\d+")) {
                super.replace(fb, offset, length, text, attrs);
            }
        }
    }
}
