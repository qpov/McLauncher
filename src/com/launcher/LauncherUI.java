package com.launcher;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class LauncherUI extends JFrame {

    private static final String SETTINGS_FILE = "settings.txt";
    private Properties settings = new Properties();

    private JTextField nicknameField;
    private JTextField ramField;
    private JComboBox<String> serverComboBox;
    private JButton launchButton;
    private JButton openFolderButton;
    private JButton reinstallGameButton;
    private JButton toggleThemeButton;
    private JButton toggleModsButton;
    private JCheckBox hideLauncherCheckBox;
    private JPanel modPanel;

    protected List<ServerConfig> serverConfigs;

    public LauncherUI() {
        setTitle("QmLauncher 1.6.6");
        setSize(960, 540);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        loadSettings();
        loadNickname();
        loadRam();
        initUI();
        updateLaunchButton();
        loadServerConfigsInBackground();
    }

    private void loadSettings() {
        File file = new File(SETTINGS_FILE);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                settings.load(fis);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!settings.containsKey("nickname")) {
            settings.setProperty("nickname", "Player");
        }
        if (!settings.containsKey("theme")) {
            settings.setProperty("theme", "dark");
        }
        if (!settings.containsKey("ram")) {
            settings.setProperty("ram", "2");
        }
        if (!settings.containsKey("hideLauncher")) {
            settings.setProperty("hideLauncher", "true");
        }
    }

    private void saveSettings() {
        try (FileOutputStream fos = new FileOutputStream(SETTINGS_FILE)) {
            settings.store(fos, "Launcher Settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Фоновая загрузка конфигурации серверов
    private void loadServerConfigsInBackground() {
        new SwingWorker<ServerList, Void>() {
            @Override
            protected ServerList doInBackground() throws Exception {
                String url = "https://raw.githubusercontent.com/qpov/QmLauncher/refs/heads/main/servers.json?t="
                        + System.currentTimeMillis();
                return ConfigLoader.loadServerConfigs(url);
            }
            @Override
            protected void done() {
                try {
                    ServerList config = get();
                    if (config != null && config.servers != null) {
                        serverConfigs = config.servers;
                    } else {
                        JOptionPane.showMessageDialog(LauncherUI.this,
                                "Ошибка загрузки серверов.", "Error", JOptionPane.ERROR_MESSAGE);
                        serverConfigs = new ArrayList<>();
                    }
                    serverComboBox.removeAllItems();
                    for (ServerConfig sc : serverConfigs) {
                        serverComboBox.addItem(sc.name);
                    }
                    updateLaunchButton();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private void loadNickname() {
        String nick = settings.getProperty("nickname");
        nicknameField = new JTextField(nick, 15);
    }

    private void loadRam() {
        String ram = settings.getProperty("ram");
        ramField = new JTextField(ram, 5);
        ((AbstractDocument) ramField.getDocument()).setDocumentFilter(new DigitFilter());
    }

    // Папка установки для client.jar (скачивается в version/[сервер])
    private File getInstallDirForServer(String serverName) {
        File dir = new File("version", serverName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private void initUI() {
        applyTheme(settings.getProperty("theme"));

        nicknameField = new JTextField(settings.getProperty("nickname"), 15);
        ramField = new JTextField(settings.getProperty("ram"), 5);
        ((AbstractDocument) ramField.getDocument()).setDocumentFilter(new DigitFilter());
        hideLauncherCheckBox = new JCheckBox("Скрывать лаунчер при запуске игры",
                Boolean.parseBoolean(settings.getProperty("hideLauncher")));

        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Верхняя панель
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.add(new JLabel("Ник:"));
        topPanel.add(nicknameField);
        topPanel.add(new JLabel("Версия:"));
        serverComboBox = new JComboBox<>();
        serverComboBox.addActionListener(e -> updateLaunchButton());
        topPanel.add(serverComboBox);
        topPanel.add(new JLabel("Макс. ОЗУ (ГБ):"));
        topPanel.add(ramField);
        topPanel.add(hideLauncherCheckBox);
        panel.add(topPanel, BorderLayout.NORTH);

        // Панель модов (оставляем без изменений)
        modPanel = new JPanel();
        modPanel.setBorder(BorderFactory.createTitledBorder("Моды"));
        modPanel.setLayout(new BoxLayout(modPanel, BoxLayout.Y_AXIS));
        JPanel modsContainer = new JPanel(new BorderLayout());
        modsContainer.add(new JScrollPane(modPanel), BorderLayout.CENTER);
        toggleModsButton = new JButton("Включить все моды");
        modsContainer.add(toggleModsButton, BorderLayout.SOUTH);
        panel.add(modsContainer, BorderLayout.CENTER);

        // Нижняя панель кнопок
        JPanel buttonsPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        launchButton = new JButton("Установить/Запустить");
        launchButton.addActionListener(e -> {
            String serverName = (String) serverComboBox.getSelectedItem();
            if (serverName == null) return;
            File installDir = getInstallDirForServer(serverName);
            if (new File(installDir, "client.jar").exists()) {
                runGame(installDir, getServerConfigByName(serverName), nicknameField.getText().trim());
            } else {
                launchButton.setEnabled(false);
                installGameWithProgress();
            }
        });
        buttonsPanel.add(launchButton);

        openFolderButton = new JButton("Открыть папку");
        openFolderButton.addActionListener(e -> {
            String serverName = (String) serverComboBox.getSelectedItem();
            if (serverName == null) return;
            File installDir = getInstallDirForServer(serverName);
            if (installDir.exists()) {
                try {
                    Desktop.getDesktop().open(installDir);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this,
                            "Невозможно открыть папку: " + ex.getMessage(),
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this,
                        "Игра не установлена.", "Информация", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        buttonsPanel.add(openFolderButton);

        reinstallGameButton = new JButton("Переустановить");
        reinstallGameButton.addActionListener(e -> {
            int resp = JOptionPane.showConfirmDialog(this,
                    "Вы уверены, что хотите переустановить?",
                    "Подтверждение", JOptionPane.YES_NO_OPTION);
            if (resp == JOptionPane.YES_OPTION) {
                launchButton.setEnabled(false);
                installGameWithProgress();
            }
        });
        buttonsPanel.add(reinstallGameButton);

        toggleThemeButton = new JButton("Сменить тему");
        toggleThemeButton.addActionListener(e -> {
            String cur = settings.getProperty("theme");
            String next = cur.equals("dark") ? "light" : "dark";
            applyTheme(next);
            settings.setProperty("theme", next);
            saveSettings();
        });
        buttonsPanel.add(toggleThemeButton);

        panel.add(buttonsPanel, BorderLayout.SOUTH);
        add(panel);
    }

    private void applyTheme(String theme) {
        try {
            if (theme.equalsIgnoreCase("dark")) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
            }
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void updateLaunchButton() {
        String serverName = (String) serverComboBox.getSelectedItem();
        if (serverName == null) {
            launchButton.setText("Установить игру");
            return;
        }
        File installDir = getInstallDirForServer(serverName);
        if (new File(installDir, "client.jar").exists()) {
            launchButton.setText("Запустить");
        } else {
            launchButton.setText("Установить игру");
        }
    }

    private ServerConfig getServerConfigByName(String name) {
        if (serverConfigs != null) {
            for (ServerConfig sc : serverConfigs) {
                if (sc.name.equals(name)) {
                    return sc;
                }
            }
        }
        return null;
    }

    // При нажатии на "Установить игру" запускается задача, которая:
    // 1. Скачивает архивные части и распаковывает их в корневую папку (с заменой), только если соответствующая группа ещё не распакована.
    // 2. Скачивает client.jar в папку version/[сервер].
    // Прогресс отображается в процентах.
    private void installGameWithProgress() {
        JDialog dlg = new JDialog(this, "Установка...", true);
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setStringPainted(true);
        dlg.add(bar);
        dlg.setSize(300, 100);
        dlg.setLocationRelativeTo(this);

        String serverName = (String) serverComboBox.getSelectedItem();
        ServerConfig sc = getServerConfigByName(serverName);
        DownloadAllTask task = new DownloadAllTask(sc, serverName) {
            @Override
            protected void process(List<Integer> chunks) {
                if (!chunks.isEmpty()) {
                    bar.setValue(chunks.get(chunks.size() - 1));
                }
            }
            @Override
            protected void done() {
                dlg.dispose();
                updateLaunchButton();
                launchButton.setEnabled(true);
                System.out.println("Задача установки завершена.");
            }
        };
        task.execute();
        dlg.setVisible(true);
    }

    // Задача скачивания архивов и client.jar
    private class DownloadAllTask extends SwingWorker<Void, Integer> {
        private final ServerConfig serverConfig;
        private final String serverName;

        public DownloadAllTask(ServerConfig serverConfig, String serverName) {
            this.serverConfig = serverConfig;
            this.serverName = serverName;
        }

        @Override
        protected Void doInBackground() throws Exception {
            String apiUrl = "https://api.github.com/repos/qpov/QmLauncher/contents/data?ref=refs/heads/main";
            List<GHFileInfo> files = fetchFiles(apiUrl);
            System.out.println("Всего файлов в data: " + files.size());

            // Группируем архивы по префиксу (например, assets, lib, native)
            Map<String, List<GHFileInfo>> groups = new LinkedHashMap<>();
            for (GHFileInfo fi : files) {
                String lower = fi.name.toLowerCase();
                if (!lower.contains(".zip")) continue;
                String prefix;
                if (lower.endsWith(".zip")) {
                    prefix = fi.name.substring(0, fi.name.length() - 4);
                } else {
                    int idx = fi.name.lastIndexOf('.');
                    prefix = fi.name.substring(0, idx);
                    if (prefix.endsWith(".zip")) {
                        prefix = prefix.substring(0, prefix.length() - 4);
                    }
                }
                groups.computeIfAbsent(prefix, k -> new ArrayList<>()).add(fi);
            }
            System.out.println("Групп архивов: " + groups.size());

            int totalParts = 0;
            for (List<GHFileInfo> list : groups.values()) {
                totalParts += list.size();
            }
            boolean needClient = serverConfig != null && serverConfig.download_link.toLowerCase().endsWith(".jar");
            int totalUnits = totalParts + (needClient ? 1 : 0);
            final AtomicInteger doneUnits = new AtomicInteger(0);

            // Обрабатываем каждую группу архивов
            for (Map.Entry<String, List<GHFileInfo>> entry : groups.entrySet()) {
                String prefix = entry.getKey();
                List<GHFileInfo> groupFiles = entry.getValue();
                groupFiles.sort(Comparator.comparing(x -> x.name));
                System.out.println("Обрабатывается группа: " + prefix + ", частей: " + groupFiles.size());

                // Целевая папка = корень; если она уже существует и не пуста, пропускаем скачивание группы
                File destDir = new File("."); 
                if (destDir.exists() && destDir.listFiles() != null && destDir.listFiles().length > 0) {
                    // Если файлы из этой группы уже присутствуют (проверяем по наличию хотя бы одного файла с данным префиксом)
                    boolean alreadyExtracted = false;
                    for (File f : destDir.listFiles()) {
                        if (f.getName().toLowerCase().startsWith(prefix.toLowerCase())) {
                            alreadyExtracted = true;
                            break;
                        }
                    }
                    if (alreadyExtracted) {
                        System.out.println("Группа " + prefix + " уже извлечена, пропускаем.");
                        doneUnits.addAndGet(groupFiles.size());
                        continue;
                    }
                }

                File combined = combineParts(prefix, groupFiles, doneUnits, totalUnits);
                System.out.println("Объединённый архив: " + combined.getAbsolutePath());
                unzipToRoot(combined);
                if (!combined.delete()) {
                    combined.deleteOnExit();
                }
            }

            // Скачиваем client.jar, если требуется
            if (needClient) {
                File installDir = getInstallDirForServer(serverName);
                if (!installDir.exists()) installDir.mkdirs();
                File clientJar = new File(installDir, "client.jar");
                if (clientJar.exists()) clientJar.delete();
                System.out.println("Скачиваем client.jar для сервера: " + serverName);
                downloadFile(serverConfig.download_link, clientJar);
                doneUnits.incrementAndGet();
                int pct = (doneUnits.get() * 100) / totalUnits;
                setProgress(pct);
                publish(pct);
            }
            return null;
        }

        private List<GHFileInfo> fetchFiles(String apiUrl) throws IOException {
            HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            if (conn.getResponseCode() == 200) {
                try (InputStream in = conn.getInputStream();
                     InputStreamReader isr = new InputStreamReader(in)) {
                    JsonArray arr = new Gson().fromJson(isr, JsonArray.class);
                    List<GHFileInfo> list = new ArrayList<>();
                    for (int i = 0; i < arr.size(); i++) {
                        JsonObject obj = arr.get(i).getAsJsonObject();
                        if (!"file".equals(obj.get("type").getAsString())) continue;
                        String name = obj.get("name").getAsString();
                        String downloadUrl = obj.get("download_url").getAsString();
                        list.add(new GHFileInfo(name, downloadUrl));
                    }
                    return list;
                }
            } else {
                System.out.println("GitHub API вернул код: " + conn.getResponseCode());
                return Collections.emptyList();
            }
        }

        private File combineParts(String prefix, List<GHFileInfo> parts, AtomicInteger doneUnits, int totalUnits)
                throws IOException, InterruptedException, ExecutionException {
            File combinedFile = new File(prefix + ".zip");
            if (combinedFile.exists()) combinedFile.delete();

            ExecutorService exec = Executors.newFixedThreadPool(parts.size());
            List<Future<File>> futures = new ArrayList<>();
            for (GHFileInfo fi : parts) {
                Future<File> fut = exec.submit(() -> {
                    File temp = File.createTempFile("archpart", ".part");
                    System.out.println("Скачиваем часть: " + fi.name);
                    downloadFile(fi.downloadUrl, temp);
                    int d = doneUnits.incrementAndGet();
                    int pct = (d * 100) / totalUnits;
                    setProgress(pct);
                    publish(pct);
                    return temp;
                });
                futures.add(fut);
            }
            exec.shutdown();
            try (FileOutputStream fos = new FileOutputStream(combinedFile)) {
                for (Future<File> fut : futures) {
                    File partFile = fut.get();
                    try (FileInputStream fis = new FileInputStream(partFile)) {
                        byte[] buf = new byte[4096];
                        int read;
                        while ((read = fis.read(buf)) != -1) {
                            fos.write(buf, 0, read);
                        }
                    }
                    partFile.delete();
                }
            }
            return combinedFile;
        }

        private void downloadFile(String url, File dest) throws IOException {
            System.out.println("downloadFile => " + url + " -> " + dest.getName());
            try (InputStream in = new URL(url).openStream();
                 FileOutputStream fos = new FileOutputStream(dest)) {
                byte[] buf = new byte[4096];
                int r;
                while ((r = in.read(buf)) != -1) {
                    fos.write(buf, 0, r);
                }
            }
        }

        // Распаковка ZIP-архива в корневую папку (с заменой файлов)
        private void unzipToRoot(File zipFile) throws IOException {
            System.out.println("unzip => " + zipFile.getName() + " -> корень");
            File destDir = new File(".");
            if (!destDir.exists()) destDir.mkdirs();
            byte[] buffer = new byte[4096];
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    File outFile = createNewFile(destDir, entry.getName());
                    if (entry.isDirectory()) {
                        outFile.mkdirs();
                    } else {
                        outFile.getParentFile().mkdirs();
                        try (FileOutputStream fos = new FileOutputStream(outFile, false)) {
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }
                    }
                    zis.closeEntry();
                }
            }
        }
    }

    // Метод для защиты от Zip Slip
    private File createNewFile(File destDir, String entryName) throws IOException {
        File f = new File(destDir, entryName);
        String destPath = destDir.getCanonicalPath();
        String filePath = f.getCanonicalPath();
        if (!filePath.startsWith(destPath + File.separator)) {
            throw new IOException("Zip Slip: " + entryName);
        }
        return f;
    }

    // Запуск игры (baseClasspath – заполните сами)
    private void runGame(File installDir, ServerConfig cfg, String nickname) {
        try {
            launchButton.setEnabled(false);
            File clientJar = new File(installDir, "client.jar");
            if (!clientJar.exists()) {
                JOptionPane.showMessageDialog(this, "Не найден client.jar!", "Ошибка", JOptionPane.ERROR_MESSAGE);
                launchButton.setEnabled(true);
                return;
            }
            String xmx = "-Xmx" + ramField.getText().trim() + "G";
            String baseClasspath = ""; // заполните по необходимости
            String mainClass;
            if (cfg.fabric_version != null && !cfg.fabric_version.trim().isEmpty()) {
                mainClass = "net.fabricmc.loader.impl.launch.knot.KnotClient";
            } else if (cfg.forge_version != null && !cfg.forge_version.trim().isEmpty()) {
                mainClass = "net.minecraft.client.main.Main";
            } else {
                throw new IllegalArgumentException("Неизвестный загрузчик для сервера.");
            }
            ProcessBuilder pb = new ProcessBuilder(
                    "java",
                    xmx,
                    "-Djava.library.path=native",
                    "-cp", baseClasspath,
                    mainClass,
                    "--accessToken", "dummy",
                    "--uuid", "dummy-uuid",
                    "--clientId", "dummy-clientid",
                    "--xuid", "dummy-xuid",
                    "--version", cfg.minecraft_version,
                    "--gameDir", installDir.getAbsolutePath(),
                    "--assetsDir", new File("assets").getAbsolutePath(),
                    "--assetIndex", "19",
                    "--username", nickname
            );
            pb.directory(new File("."));
            pb.inheritIO();
            Process proc = pb.start();
            JOptionPane.showMessageDialog(this, "Игра запускается...");
            if (Boolean.parseBoolean(settings.getProperty("hideLauncher"))) {
                setVisible(false);
            }
            new Thread(() -> {
                try {
                    proc.waitFor();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                SwingUtilities.invokeLater(() -> {
                    launchButton.setEnabled(true);
                    setVisible(true);
                });
            }).start();
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Ошибка запуска: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            launchButton.setEnabled(true);
        }
    }

    // Статический класс для хранения информации о файлах из GitHub
    private static class GHFileInfo {
        String name;
        String downloadUrl;
        GHFileInfo(String name, String downloadUrl) {
            this.name = name;
            this.downloadUrl = downloadUrl;
        }
    }

    // Фильтр для поля ввода RAM (только цифры)
    private class DigitFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (string.matches("\\d+")) {
                super.insertString(fb, offset, string, attr);
            }
        }
        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (text.matches("\\d+")) {
                super.replace(fb, offset, length, text, attrs);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LauncherUI launcher = new LauncherUI();
            launcher.setVisible(true);
        });
    }
}
