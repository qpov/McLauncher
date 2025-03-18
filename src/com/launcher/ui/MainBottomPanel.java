package com.launcher.ui;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainBottomPanel extends JPanel {

    private float panelAlpha = 0.5f; // прозрачность панели 50%

    public JPanel leftPanel;
    public JPanel rightPanel;

    public JButton playButton; // "Играть" / "Установить" / "Переустановить"
    public JButton toggleModeButton; // Кнопка переключения режима
    public JComboBox<String> serverComboBox;
    public JTextField nicknameField;
    public AtomicBoolean reinstallMode = new AtomicBoolean(false);

    public JButton openFolderButton; // "Папка"
    public JButton settingsButton; // "Настройки"

    private static final int COMPONENT_HEIGHT = 28;
    private static final int PANEL_HEIGHT = 60;

    public MainBottomPanel() {
        setPreferredSize(new Dimension(0, PANEL_HEIGHT));
        setLayout(new BorderLayout());
        setOpaque(false);
        // Задаём чёрный фон для нижней панели
        setBackground(Color.BLACK);

        // Создаем левую панель с отступом 10 пикселей по горизонтали и 16 по вертикали
        leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 16));
        leftPanel.setOpaque(false);

        Font biggerFont = loadSemiBoldFont(16f);

        // Кнопка "Играть"/"Установить"/"Переустановить"
        playButton = new JButton("Играть");
        styleTransparentButton(playButton, biggerFont);
        FontMetrics fm = leftPanel.getFontMetrics(biggerFont);
        String maxText = "Переустановить";
        int maxTextWidth = fm.stringWidth(maxText);
        playButton.setPreferredSize(new Dimension(maxTextWidth + 6, COMPONENT_HEIGHT));
        leftPanel.add(playButton);

        // Кнопка переключения режима
        toggleModeButton = new JButton(loadScaledIcon("resources/img/angles-up-down.png", 24, 24));
        styleTransparentButton(toggleModeButton, biggerFont);
        toggleModeButton.setPreferredSize(new Dimension(28, COMPONENT_HEIGHT));
        leftPanel.add(toggleModeButton);

        leftPanel.add(Box.createHorizontalStrut(3));

        // Метка "Версия:" с левым отступом 5 пикселей
        JLabel versionLabel = new JLabel("Версия:");
        versionLabel.setForeground(Color.WHITE);
        versionLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        leftPanel.add(versionLabel);

        // Выпадающий список (JComboBox) для выбора сервера – без отступов
        serverComboBox = new JComboBox<>();
        serverComboBox.setPreferredSize(new Dimension(200, COMPONENT_HEIGHT));
        serverComboBox.setOpaque(true);
        serverComboBox.setBackground(Color.BLACK);
        serverComboBox.setForeground(Color.WHITE);
        serverComboBox.setBorder(BorderFactory.createEmptyBorder());
        serverComboBox.putClientProperty("FlatLaf.style",
                "background: #000000;" +
                "foreground: #ffffff");
        serverComboBox.updateUI();
        serverComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected,
                        cellHasFocus);
                if (isSelected) {
                    label.setBackground(Color.WHITE);
                    label.setForeground(Color.BLACK);
                } else {
                    label.setBackground(Color.BLACK);
                    label.setForeground(Color.WHITE);
                }
                label.setOpaque(true);
                return label;
            }
        });
        leftPanel.add(serverComboBox);

        leftPanel.add(Box.createHorizontalStrut(3));

        // Метка "Ник:" с левым отступом 5 пикселей
        JLabel nicknameLabel = new JLabel("Ник:");
        nicknameLabel.setForeground(Color.WHITE);
        nicknameLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        leftPanel.add(nicknameLabel);

        // Поле ввода ника – без дополнительных отступов
        nicknameField = new JTextField("Player", 10);
        nicknameField.setPreferredSize(new Dimension(100, COMPONENT_HEIGHT));
        nicknameField.setOpaque(true);
        nicknameField.setBackground(Color.BLACK);
        nicknameField.setForeground(Color.WHITE);
        nicknameField.setBorder(BorderFactory.createEmptyBorder());
        nicknameField.setSelectionColor(Color.WHITE);
        nicknameField.setSelectedTextColor(Color.BLACK);
        leftPanel.add(nicknameField);

        add(leftPanel, BorderLayout.WEST);

        rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 16));
        rightPanel.setOpaque(false);

        openFolderButton = new JButton(loadScaledIcon("resources/img/folder.png", 24, 24));
        styleTransparentButton(openFolderButton, biggerFont);
        openFolderButton.setPreferredSize(new Dimension(28, COMPONENT_HEIGHT));
        rightPanel.add(openFolderButton);

        settingsButton = new JButton(loadScaledIcon("resources/img/settings.png", 24, 24));
        styleTransparentButton(settingsButton, biggerFont);
        settingsButton.setPreferredSize(new Dimension(28, COMPONENT_HEIGHT));
        rightPanel.add(settingsButton);

        add(rightPanel, BorderLayout.EAST);

        initButtonActions();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        Color bg = getBackground();
        if (bg == null) {
            bg = Color.BLACK;
        }
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, panelAlpha));
        g2d.setColor(bg);
        g2d.fillRect(0, 0, getWidth(), getHeight());
        g2d.dispose();
        super.paintComponent(g);
    }

    // Метод для задания прозрачности панели
    public void setPanelAlpha(float alpha) {
        this.panelAlpha = alpha;
        repaint();
    }

    private void styleTransparentButton(JButton btn, Font font) {
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setBorder(null);
        btn.setFont(font);
        btn.setForeground(Color.WHITE);
        btn.setRolloverEnabled(false);
    }

    private ImageIcon loadScaledIcon(String path, int width, int height) {
        ImageIcon icon = new ImageIcon(path);
        if (icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
            System.err.println("Не удалось загрузить иконку: " + path);
            return icon;
        }
        Image scaled = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private Font loadSemiBoldFont(float size) {
        try {
            return Font.createFont(Font.TRUETYPE_FONT, new File("resources/fonts/Aptos-SemiBold.ttf"))
                    .deriveFont(size);
        } catch (Exception ex) {
            ex.printStackTrace();
            return new Font("SansSerif", Font.BOLD, (int) size);
        }
    }

    private void initButtonActions() {
        playButton.addActionListener(e -> {
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof LauncherUI) {
                ((LauncherUI) window).onPlayClicked();
            }
        });

        toggleModeButton.addActionListener(e -> {
            boolean newVal = !reinstallMode.get();
            reinstallMode.set(newVal);
            playButton.setText(newVal ? "Переустановить" : "Играть");
        });

        openFolderButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(new File("version"));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        settingsButton.addActionListener(e -> {
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof LauncherUI) {
                LauncherUI launcher = (LauncherUI) window;
                launcher.switchToSettings();
            }
        });
    }
    
    // Метод для обновления текста кнопки "Играть"/"Установить" и видимости кнопки toggleModeButton
    // Если в папке version/<serverName>/client.jar есть файл – устанавливаем "Играть" и показываем toggleModeButton,
    // иначе "Установить" и скрываем toggleModeButton.
    public void updatePlayButtonText() {
        Object selected = serverComboBox.getSelectedItem();
        if (selected == null) {
            playButton.setText("Играть");
            toggleModeButton.setVisible(false);
            return;
        }
        String serverName = selected.toString();
        File installDir = new File("version", serverName);
        File clientJar = new File(installDir, "client.jar");
        if (clientJar.exists()) {
            playButton.setText("Играть");
            toggleModeButton.setVisible(true);
        } else {
            playButton.setText("Установить");
            toggleModeButton.setVisible(false);
        }
    }
    
    public JPanel getLeftPanel() {
        return leftPanel;
    }
    
    public JPanel getRightPanel() {
        return rightPanel;
    }
}
