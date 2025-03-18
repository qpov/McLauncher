package com.launcher.ui;

import com.launcher.ServerConfig;
import com.launcher.ui.settings.SettingsGeneral;
import com.launcher.ui.settings.SettingsMods;
import com.launcher.ui.settings.SettingsShaders;
import com.launcher.ui.settings.SettingsResourcePacks;
import com.launcher.ui.settings.SettingsSkin;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class SettingsPanel extends JPanel {

    private Properties settings;
    private CardLayout cardLayout;
    private JPanel cardPanel;

    // Страницы настроек
    private SettingsMods settingsMods;
    private SettingsShaders settingsShaders;
    private SettingsResourcePacks settingsResourcePacks;
    private SettingsSkin settingsSkin;
    
    // Ссылка на родительское окно для получения выбранного сервера
    private JFrame owner;

    public SettingsPanel(JFrame owner, Properties settings, Runnable backAction) {
        this.settings = settings;
        this.owner = owner;
        setLayout(new BorderLayout());

        // Навигационная панель с кнопками
        JPanel navBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        navBar.setOpaque(false);
        String[] tabs = { "Настройки", "Моды", "Шейдеры", "Ресурспаки", "Скин" };
        for (String tab : tabs) {
            JButton btn = new JButton(tab) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    if (getModel().isPressed()) {
                        g2d.setColor(new Color(0, 0, 0, 150));
                    } else {
                        g2d.setColor(new Color(0, 0, 0, 127));
                    }
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                    g2d.dispose();
                    super.paintComponent(g);
                }
            };
            btn.setFocusPainted(false);
            btn.setRolloverEnabled(false);
            btn.setContentAreaFilled(false);
            btn.setOpaque(false);
            btn.setForeground(Color.WHITE);
            btn.setFont(loadSemiBoldFont(14f));
            btn.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
            // При нажатии на кнопку переключения вкладки вызываем обновление соответствующей страницы
            btn.addActionListener(e -> {
                cardLayout.show(cardPanel, tab);
                // Получаем текущий выбранный сервер из LauncherUI -> MainBottomPanel
                String serverName = getCurrentServerName();
                if (serverName == null) return;
                if (tab.equals("Моды")) {
                    settingsMods.updateModPanel(serverName);
                } else if (tab.equals("Шейдеры")) {
                    settingsShaders.updateShaderList(serverName);
                } else if (tab.equals("Ресурспаки")) {
                    settingsResourcePacks.updateResourcePackList(serverName);
                }
            });
            navBar.add(btn);
        }
        add(navBar, BorderLayout.NORTH);

        // Панель с контентом настроек (CardLayout)
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setOpaque(false);
        add(cardPanel, BorderLayout.CENTER);

        // Создаём страницы настроек
        SettingsGeneral generalPage = new SettingsGeneral(settings);
        settingsMods = new SettingsMods();
        settingsShaders = new SettingsShaders();
        settingsResourcePacks = new SettingsResourcePacks();
        settingsSkin = new SettingsSkin();

        cardPanel.add(generalPage, "Настройки");
        cardPanel.add(settingsMods, "Моды");
        cardPanel.add(settingsShaders, "Шейдеры");
        cardPanel.add(settingsResourcePacks, "Ресурспаки");
        cardPanel.add(settingsSkin, "Скин");

        cardLayout.show(cardPanel, "Настройки");
    }
    
    // Метод для получения текущего выбранного сервера из LauncherUI
    private String getCurrentServerName() {
        if (owner instanceof LauncherUI) {
            MainBottomPanel mbp = ((LauncherUI) owner).mainBottomPanel;
            Object sel = mbp.serverComboBox.getSelectedItem();
            return sel != null ? sel.toString() : null;
        }
        return null;
    }

    // Применяем выбранные моды
    public void applyModSelection(ServerConfig selectedServer) {
        if (settingsMods != null) {
            settingsMods.applyModSelection(selectedServer);
        }
    }
    
    // Обновляем панели настроек для выбранного сервера:
    // моды, шейдеры и ресурспаки.
    public void updateModPanel(String serverName) {
        if (settingsMods != null) {
            settingsMods.updateModPanel(serverName);
        }
        if (settingsShaders != null) {
            settingsShaders.updateShaderList(serverName);
        }
        if (settingsResourcePacks != null) {
            settingsResourcePacks.updateResourcePackList(serverName);
        }
    }

    private Font loadSemiBoldFont(float size) {
        try {
            return Font.createFont(Font.TRUETYPE_FONT, new File("resources/fonts/Aptos-SemiBold.ttf")).deriveFont(size);
        } catch (Exception ex) {
            ex.printStackTrace();
            return new Font("SansSerif", Font.BOLD, (int) size);
        }
    }
}
