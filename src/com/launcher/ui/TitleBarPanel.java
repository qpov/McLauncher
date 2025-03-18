package com.launcher.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class TitleBarPanel extends JPanel {

    private float panelAlpha = 0.5f; // прозрачность панели 50%
    
    private JPanel leftPanel;
    private JLabel launcherLabel;
    private JLabel versionLabel;
    private JButton minimizeButton;
    private JButton closeButton;

    private int posX, posY;

    public TitleBarPanel(JFrame parent, String launcherText, String versionText) {
        setOpaque(false);
        setDoubleBuffered(false);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(parent.getWidth(), 30));
        setBackground(Color.BLACK); // задаём чёрный фон

        Font semiBoldFont = loadSemiBoldFont(14f);

        // Левая панель
        leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        leftPanel.setOpaque(false);

        launcherLabel = new JLabel(launcherText);
        launcherLabel.setOpaque(false);
        launcherLabel.setForeground(Color.WHITE);
        launcherLabel.setFont(semiBoldFont.deriveFont(16f));
        leftPanel.add(launcherLabel);

        add(leftPanel, BorderLayout.WEST);

        // Центр – версия
        versionLabel = new JLabel(versionText, SwingConstants.CENTER);
        versionLabel.setOpaque(false);
        versionLabel.setForeground(Color.WHITE);
        versionLabel.setFont(semiBoldFont.deriveFont(14f));
        add(versionLabel, BorderLayout.CENTER);

        // Правая панель – кнопки
        JPanel windowButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        windowButtonsPanel.setOpaque(false);

        minimizeButton = createTransparentIconButton("resources/img/minimize.png", 20, 20);
        minimizeButton.addActionListener(e -> parent.setExtendedState(JFrame.ICONIFIED));
        windowButtonsPanel.add(minimizeButton);

        closeButton = createTransparentIconButton("resources/img/close.png", 20, 20);
        closeButton.addActionListener(e -> System.exit(0));
        windowButtonsPanel.add(closeButton);

        add(windowButtonsPanel, BorderLayout.EAST);

        // Перетаскивание окна мышью
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                posX = e.getX();
                posY = e.getY();
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                parent.setLocation(e.getXOnScreen() - posX, e.getYOnScreen() - posY);
            }
        });
    }

    // Метод для обновления кнопки "Назад"
    public void updateBackButton(boolean show, Runnable backAction) {
        leftPanel.removeAll();
        if (show) {
            // Устанавливаем текст как "QmLauncher | Настройки"
            launcherLabel.setText("QmLauncher | Настройки");
            leftPanel.add(launcherLabel);
            // Добавляем кнопку "Назад" с иконкой
            ImageIcon backIcon = loadScaledIcon("resources/img/back.png", 16, 16);
            JButton backButton = new JButton("Назад", backIcon);
            backButton.setRolloverEnabled(false);
            makeFullyTransparentButton(backButton, loadSemiBoldFont(14f));
            backButton.addActionListener(e -> backAction.run());
            leftPanel.add(backButton);
        } else {
            // На главном экране – только "QmLauncher"
            launcherLabel.setText("QmLauncher");
            leftPanel.add(launcherLabel);
        }
        leftPanel.revalidate();
        leftPanel.repaint();
    }

    // Метод для установки прозрачности панели (значение от 0.0 до 1.0)
    public void setPanelAlpha(float alpha) {
        this.panelAlpha = alpha;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        // Используем заданный фон (черный) с alpha = panelAlpha (0.5)
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, panelAlpha));
        g2.setColor(getBackground());
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }

    private JButton createTransparentIconButton(String path, int w, int h) {
        ImageIcon icon = loadScaledIcon(path, w, h);
        JButton btn = new JButton(icon);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setRolloverEnabled(false);
        btn.setPreferredSize(new Dimension(40, 30));
        return btn;
    }

    private void makeFullyTransparentButton(JButton btn, Font font) {
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setBorder(null);
        btn.setFont(font);
        btn.setForeground(Color.WHITE);
    }

    private ImageIcon loadScaledIcon(String path, int width, int height) {
        ImageIcon icon = new ImageIcon(path);
        if(icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0){
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
}
