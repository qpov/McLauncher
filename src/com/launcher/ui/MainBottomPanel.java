package com.launcher.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

        // Добавляем слушатель, который сохраняет ник при потере фокуса
        nicknameField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                Window window = SwingUtilities.getWindowAncestor(MainBottomPanel.this);
                if (window instanceof LauncherUI) {
                    String newNickname = nicknameField.getText().trim();
                    ((LauncherUI) window).updateNickname(newNickname);
                    System.out.println("Ник сохранён: " + newNickname);
                }
            }
        });

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
            // Если текст кнопки "Установить", запускаем скачивание и распаковку архивов,
            // иначе вызываем стандартное действие (например, запуск игры)
            if ("Установить".equals(playButton.getText())) {
                playButton.setEnabled(false);
                System.out.println("Нажата кнопка 'Установить'. Запуск процедуры установки.");
                downloadAndExtractArchives();
            } else {
                Window window = SwingUtilities.getWindowAncestor(this);
                if (window instanceof LauncherUI) {
                    ((LauncherUI) window).onPlayClicked();
                }
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
    
    // Метод для скачивания client.jar (если отсутствует) и затем скачивания и распаковки архивов
    private void downloadAndExtractArchives() {
        // Получаем текущий сервер
        Object selected = serverComboBox.getSelectedItem();
        if (selected == null) {
            System.out.println("Сервер не выбран.");
            return;
        }
        String serverName = selected.toString();
        File installDir = new File("version", serverName);
        if (!installDir.exists()) {
            installDir.mkdirs();
            System.out.println("Создана директория установки: " + installDir.getAbsolutePath());
        }
        File clientJar = new File(installDir, "client.jar");
        
        // Скачиваем client.jar, если он отсутствует
        if (!clientJar.exists()) {
            System.out.println("Скачивание client.jar...");
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof LauncherUI) {
                LauncherUI launcher = (LauncherUI) window;
                // Получаем конфигурацию сервера через публичный метод
                com.launcher.ServerConfig sc = launcher.getServerConfigByNamePublic(serverName);
                if (sc != null && sc.download_link != null && !sc.download_link.isEmpty()) {
                    try {
                        downloadFile(sc.download_link, clientJar);
                        System.out.println("client.jar скачан в: " + clientJar.getAbsolutePath());
                    } catch (IOException ex) {
                        System.out.println("Ошибка при скачивании client.jar: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                } else {
                    System.out.println("Не удалось найти URL для client.jar.");
                }
            }
        } else {
            System.out.println("client.jar уже существует: " + clientJar.getAbsolutePath());
        }
        
        // Далее – скачивание и распаковка архивов
        String[] archives = {
            "assets.zip.001",
            "assets.zip.002",
            "assets.zip.003",
            "assets.zip.004",
            "lib.zip.001",
            "lib.zip.002",
            "native.zip.001"
        };
        
        // Группируем по базовому имени (удаляем последний сегмент, например, ".001")
        Map<String, List<String>> groups = new HashMap<>();
        for (String archive : archives) {
            int lastDot = archive.lastIndexOf('.');
            if (lastDot > 0) {
                String baseName = archive.substring(0, lastDot);
                groups.computeIfAbsent(baseName, k -> new ArrayList<>()).add(archive);
            }
        }
        
        System.out.println("Найдено групп архивов: " + groups.keySet());
        
        int totalGroups = groups.size();
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Установка...", true);
        JProgressBar bar = new JProgressBar(0, totalGroups);
        bar.setStringPainted(true);
        dlg.add(bar);
        dlg.setSize(300, 100);
        dlg.setLocationRelativeTo(this);
        
        new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() throws Exception {
                File tempDir = new File("temp_downloads");
                if (!tempDir.exists()) {
                    tempDir.mkdirs();
                    System.out.println("Создана временная папка: " + tempDir.getAbsolutePath());
                }
                
                int groupCount = 0;
                for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
                    String baseName = entry.getKey();
                    List<String> parts = entry.getValue();
                    parts.sort(Comparator.comparingInt(s -> Integer.parseInt(s.substring(s.lastIndexOf('.') + 1))));
                    System.out.println("Группа " + baseName + " состоит из частей: " + parts);
                    
                    List<File> downloadedParts = new ArrayList<>();
                    for (String part : parts) {
                        String fileUrl = "https://raw.githubusercontent.com/qpov/QmLauncher/refs/heads/main/data/" + part;
                        System.out.println("Начало скачивания: " + fileUrl);
                        File partFile = new File(tempDir, part);
                        downloadFile(fileUrl, partFile);
                        System.out.println("Скачан файл: " + partFile.getAbsolutePath());
                        downloadedParts.add(partFile);
                    }
                    
                    File combinedZip = new File(tempDir, baseName + ".zip");
                    try (FileOutputStream fos = new FileOutputStream(combinedZip)) {
                        for (File partFile : downloadedParts) {
                            try (FileInputStream fis = new FileInputStream(partFile)) {
                                byte[] buffer = new byte[4096];
                                int len;
                                while ((len = fis.read(buffer)) > 0) {
                                    fos.write(buffer, 0, len);
                                }
                            }
                        }
                    }
                    System.out.println("Объединён файл: " + combinedZip.getAbsolutePath());
                    
                    System.out.println("Начало распаковки: " + combinedZip.getName());
                    extractZip(combinedZip, new File("."));
                    System.out.println("Распаковка завершена для: " + combinedZip.getName());
                    
                    for (File partFile : downloadedParts) {
                        if (partFile.delete()) {
                            System.out.println("Временный файл удалён: " + partFile.getName());
                        } else {
                            System.out.println("Не удалось удалить временный файл: " + partFile.getName());
                        }
                    }
                    if (combinedZip.delete()) {
                        System.out.println("Объединённый архив удалён: " + combinedZip.getName());
                    } else {
                        System.out.println("Не удалось удалить объединённый архив: " + combinedZip.getName());
                    }
                    
                    groupCount++;
                    publish(groupCount);
                }
                
                if (tempDir.delete()) {
                    System.out.println("Временная папка удалена.");
                } else {
                    System.out.println("Не удалось удалить временную папку.");
                }
                return null;
            }

            @Override
            protected void process(java.util.List<Integer> chunks) {
                int progress = chunks.get(chunks.size() - 1);
                bar.setValue(progress);
                System.out.println("Прогресс: " + progress + " из " + totalGroups);
            }

            @Override
            protected void done() {
                dlg.dispose();
                playButton.setEnabled(true);
                updatePlayButtonText();
                System.out.println("Установка завершена!");
                JOptionPane.showMessageDialog(null, "Установка завершена!");
            }
        }.execute();
        dlg.setVisible(true);
    }
    
    // Метод для скачивания файла по URL в указанный File с логированием
    private void downloadFile(String fileUrl, File destination) throws IOException {
        URL url = new URL(fileUrl);
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        try (InputStream in = connection.getInputStream();
             FileOutputStream fos = new FileOutputStream(destination)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
        System.out.println("Файл скачан: " + destination.getName());
    }
    
    // Метод для распаковки zip-архива в указанную директорию с логированием
    private void extractZip(File zipFile, File targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                System.out.println("Распаковка записи: " + entry.getName());
                File outFile = new File(targetDir, entry.getName());
                if (entry.isDirectory()) {
                    if (outFile.mkdirs()) {
                        System.out.println("Создана директория: " + outFile.getAbsolutePath());
                    }
                } else {
                    File parent = outFile.getParentFile();
                    if (!parent.exists()) {
                        parent.mkdirs();
                        System.out.println("Созданы родительские директории для: " + outFile.getAbsolutePath());
                    }
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                        System.out.println("Файл распакован: " + outFile.getAbsolutePath());
                    }
                }
                zis.closeEntry();
            }
        }
    }
}
