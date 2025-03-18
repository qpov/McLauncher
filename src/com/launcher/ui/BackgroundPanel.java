package com.launcher.ui;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Панель для отображения фонового изображения.
 * Используется как contentPane основного окна.
 */
public class BackgroundPanel extends JPanel {
    private Image backgroundImage;

    public BackgroundPanel(String imagePath, LayoutManager layout) {
        super(layout);
        // Панель не заливается сплошным цветом, прозрачность отключена
        setOpaque(false);
        try {
            backgroundImage = ImageIO.read(new File(imagePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Рисуем фоновое изображение, растянутое на весь размер панели
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }
}
