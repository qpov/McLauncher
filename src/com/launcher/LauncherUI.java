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

    private static final String SETTINGS_FILE_NAME = "settings.txt";
    private Properties settings = new Properties();

    // Флаг, указывающий, что происходит начальная инициализация comboBox
    private boolean initializing = false;

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
        File file = new File(SETTINGS_FILE_NAME);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                settings.load(fis);
                System.out.println("Настройки загружены из " + file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Если файла нет, задаем значения по умолчанию и сохраняем их
            settings.setProperty("nickname", "Player");
            settings.setProperty("theme", "dark");
            settings.setProperty("ram", "2");
            settings.setProperty("hideLauncher", "true");
            settings.setProperty("lastVersion", "");
            saveSettings();
            System.out.println("Настройки по умолчанию сохранены в " + file.getAbsolutePath());
        }
    }

    private void saveSettings() {
        File file = new File(SETTINGS_FILE_NAME);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            settings.store(fos, "Launcher Settings");
            fos.flush();
            System.out.println("Настройки сохранены в " + file.getAbsolutePath());
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
                    initializing = true; // начинаем инициализацию
                    serverComboBox.removeAllItems();
                    for (ServerConfig sc : serverConfigs) {
                        serverComboBox.addItem(sc.name);
                    }
                    // Если в настройках сохранена последняя выбранная версия, устанавливаем её
                    String lastVersion = settings.getProperty("lastVersion", "");
                    boolean found = false;
                    if (!lastVersion.isEmpty()) {
                        for (int i = 0; i < serverComboBox.getItemCount(); i++) {
                            if (serverComboBox.getItemAt(i).equals(lastVersion)) {
                                serverComboBox.setSelectedIndex(i);
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found && serverComboBox.getItemCount() > 0) {
                        serverComboBox.setSelectedIndex(0);
                    }
                    initializing = false; // окончание инициализации
                    updateLaunchButton();
                    updateModPanel();
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

        // Никнейм с сохранением изменений при потере фокуса
        nicknameField = new JTextField(settings.getProperty("nickname"), 15);
        nicknameField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                settings.setProperty("nickname", nicknameField.getText().trim());
                saveSettings();
            }
        });

        // RAM с сохранением изменений при потере фокуса
        ramField = new JTextField(settings.getProperty("ram"), 5);
        ((AbstractDocument) ramField.getDocument()).setDocumentFilter(new DigitFilter());
        ramField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                settings.setProperty("ram", ramField.getText().trim());
                saveSettings();
            }
        });

        // Чекбокс для скрытия лаунчера с сохранением изменений
        hideLauncherCheckBox = new JCheckBox("Скрывать лаунчер при запуске игры",
                Boolean.parseBoolean(settings.getProperty("hideLauncher")));
        hideLauncherCheckBox.addActionListener(e -> {
            settings.setProperty("hideLauncher", Boolean.toString(hideLauncherCheckBox.isSelected()));
            saveSettings();
        });

        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Верхняя панель
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.add(new JLabel("Ник:"));
        topPanel.add(nicknameField);
        topPanel.add(new JLabel("Версия:"));
        serverComboBox = new JComboBox<>();
        serverComboBox.addActionListener(e -> {
            updateLaunchButton();
            updateModPanel();
            // Если не инициализируем, сохраняем выбранную версию
            if (!initializing) {
                String selected = (String) serverComboBox.getSelectedItem();
                if (selected != null) {
                    settings.setProperty("lastVersion", selected);
                    saveSettings();
                }
            }
        });
        topPanel.add(serverComboBox);
        topPanel.add(new JLabel("Макс. ОЗУ (ГБ):"));
        topPanel.add(ramField);
        topPanel.add(hideLauncherCheckBox);
        panel.add(topPanel, BorderLayout.NORTH);

        // Панель модов
        modPanel = new JPanel();
        modPanel.setBorder(BorderFactory.createTitledBorder("Моды"));
        modPanel.setLayout(new BoxLayout(modPanel, BoxLayout.Y_AXIS));
        JPanel modsContainer = new JPanel(new BorderLayout());
        modsContainer.add(new JScrollPane(modPanel), BorderLayout.CENTER);
        toggleModsButton = new JButton("Включить все моды");
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
        modsContainer.add(toggleModsButton, BorderLayout.SOUTH);
        panel.add(modsContainer, BorderLayout.CENTER);

        // Нижняя панель кнопок
        JPanel buttonsPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        launchButton = new JButton("Установить/Запустить");
        launchButton.addActionListener(e -> {
            String serverName = (String) serverComboBox.getSelectedItem();
            if (serverName == null)
                return;
            File installDir = getInstallDirForServer(serverName);
            if (new File(installDir, "client.jar").exists()) {
                applyModSelection(getServerConfigByName(serverName));
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
            if (serverName == null)
                return;
            File installDir = getInstallDirForServer(serverName);
            if (installDir.exists()) {
                try {
                    Desktop.getDesktop().open(installDir);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Невозможно открыть папку: " + ex.getMessage(),
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Игра не установлена.",
                        "Информация", JOptionPane.INFORMATION_MESSAGE);
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

    // Обновление панели модов: отображаем установленные моды
    private void updateModPanel() {
        modPanel.removeAll();
        String serverName = (String) serverComboBox.getSelectedItem();
        if (serverName == null) {
            modPanel.revalidate();
            modPanel.repaint();
            return;
        }
        File installDir = getInstallDirForServer(serverName);
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
                modPanel.add(cb);
            }
            for (String modName : disabledMods) {
                JCheckBox cb = new JCheckBox(modName, false);
                modPanel.add(cb);
            }
        } else {
            modPanel.add(new JLabel("Нет установленных модов."));
        }
        modPanel.revalidate();
        modPanel.repaint();
    }

    // Применение выбранных модов: перемещаем моды в папку disabled, если они
    // выключены, и обратно, если включены
    private void applyModSelection(ServerConfig selectedServer) {
        File installDir = getInstallDirForServer(selectedServer.name);
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

    // Задача установки: скачивание архивов и client.jar с отображением прогресса
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
                updateModPanel();
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
                if (!lower.contains(".zip"))
                    continue;
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
                // Для группы lib всегда скачиваем архив, для остальных – если в корне уже есть
                // файлы с данным префиксом, пропускаем
                if (!prefix.equalsIgnoreCase("lib")) {
                    boolean exists = false;
                    File root = new File(".");
                    for (File f : root.listFiles()) {
                        if (f.getName().toLowerCase().startsWith(prefix.toLowerCase())) {
                            exists = true;
                            break;
                        }
                    }
                    if (exists) {
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
                if (!installDir.exists())
                    installDir.mkdirs();
                File clientJar = new File(installDir, "client.jar");
                if (clientJar.exists())
                    clientJar.delete();
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
                        if (!"file".equals(obj.get("type").getAsString()))
                            continue;
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
            if (combinedFile.exists())
                combinedFile.delete();

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

        // Распаковка ZIP-архива в корневую папку с заменой файлов
        private void unzipToRoot(File zipFile) throws IOException {
            System.out.println("unzip => " + zipFile.getName() + " -> корень");
            File destDir = new File(".");
            if (!destDir.exists())
                destDir.mkdirs();
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

    // Защита от Zip Slip
    private File createNewFile(File destDir, String entryName) throws IOException {
        File f = new File(destDir, entryName);
        String destPath = destDir.getCanonicalPath();
        String filePath = f.getCanonicalPath();
        if (!filePath.startsWith(destPath + File.separator)) {
            throw new IOException("Zip Slip: " + entryName);
        }
        return f;
    }

    // Запуск игры с пустым baseClasspath (дополните библиотеки по необходимости)
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
            String baseClasspath = clientJar.getAbsolutePath()
                    + ";lib/ll/night-config/toml/3.7.4/toml-3.7.4.jar"
                    + ";lib/com/fasterxml/jackson/core/jackson-annotations/2.13.4/jackson-annotations-2.13.4.jar"
                    + ";lib/com/fasterxml/jackson/core/jackson-core/2.13.4/jackson-core-2.13.4.jar"
                    + ";lib/com/fasterxml/jackson/core/jackson-databind/2.13.4.2/jackson-databind-2.13.4.2.jar"
                    + ";lib/com/github/oshi/oshi-core/6.6.5/oshi-core-6.6.5.jar"
                    + ";lib/com/github/stephenc/jcip/jcip-annotations/1.0-1/jcip-annotations-1.0-1.jar"
                    + ";lib/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar"
                    + ";lib/com/google/guava/failureaccess/1.0.1/failureaccess-1.0.1.jar"
                    + ";lib/com/google/guava/failureaccess/1.0.2/failureaccess-1.0.2.jar"
                    + ";lib/com/google/guava/guava/32.1.2-jre/guava-32.1.2-jre.jar"
                    + ";lib/com/google/guava/guava/33.3.1-jre/guava-33.3.1-jre.jar"
                    + ";lib/com/ibm/icu/icu4j/76.1/icu4j-76.1.jar"
                    + ";lib/com/microsoft/azure/msal4j/1.17.2/msal4j-1.17.2.jar"
                    + ";lib/com/mojang/authlib/6.0.57/authlib-6.0.57.jar"
                    + ";lib/com/mojang/blocklist/1.0.10/blocklist-1.0.10.jar"
                    + ";lib/com/mojang/brigadier/1.3.10/brigadier-1.3.10.jar"
                    + ";lib/com/mojang/datafixerupper/8.0.16/datafixerupper-8.0.16.jar"
                    + ";lib/com/mojang/jtracy/1.0.29/jtracy-1.0.29-natives-windows.jar"
                    + ";lib/com/mojang/jtracy/1.0.29/jtracy-1.0.29.jar"
                    + ";lib/com/mojang/logging/1.5.10/logging-1.5.10.jar"
                    + ";lib/com/mojang/patchy/2.2.10/patchy-2.2.10.jar"
                    + ";lib/com/mojang/text2speech/1.17.9/text2speech-1.17.9.jar"
                    + ";lib/com/nimbusds/content-type/2.3/content-type-2.3.jar"
                    + ";lib/com/nimbusds/lang-tag/1.7/lang-tag-1.7.jar"
                    + ";lib/com/nimbusds/nimbus-jose-jwt/9.40/nimbus-jose-jwt-9.40.jar"
                    + ";lib/com/nimbusds/oauth2-oidc-sdk/11.18/oauth2-oidc-sdk-11.18.jar"
                    + ";lib/commons-codec/commons-codec/1.17.1/commons-codec-1.17.1.jar"
                    + ";lib/commons-io/commons-io/2.17.0/commons-io-2.17.0.jar"
                    + ";lib/commons-logging/commons-logging/1.3.4/commons-logging-1.3.4.jar"
                    + ";lib/de/oceanlabs/mcp/mcp_config/1.21.4-20241203.143248/mcp_config-1.21.4-20241203.143248-srg2off.jar"
                    + ";lib/io/netty/netty-buffer/4.1.115.Final/netty-buffer-4.1.115.Final.jar"
                    + ";lib/io/netty/netty-codec/4.1.115.Final/netty-codec-4.1.115.Final.jar"
                    + ";lib/io/netty/netty-common/4.1.115.Final/netty-common-4.1.115.Final.jar"
                    + ";lib/io/netty/netty-handler/4.1.115.Final/netty-handler-4.1.115.Final.jar"
                    + ";lib/io/netty/netty-resolver/4.1.115.Final/netty-resolver-4.1.115.Final.jar"
                    + ";lib/io/netty/netty-transport/4.1.115.Final/netty-transport-4.1.115.Final.jar"
                    + ";lib/io/netty/netty-transport-classes-epoll/4.1.115.Final/netty-transport-classes-epoll-4.1.115.Final.jar"
                    + ";lib/io/netty/netty-transport-native-unix-common/4.1.115.Final/netty-transport-native-unix-common-4.1.115.Final.jar"
                    + ";lib/it/unimi/dsi/fastutil/8.5.15/fastutil-8.5.15.jar"
                    + ";lib/net/fabricmc/fabric-loader/0.16.10/fabric-loader-0.16.10.jar"
                    + ";lib/net/fabricmc/intermediary/1.21.4/intermediary-1.21.4.jar"
                    + ";lib/net/fabricmc/sponge-mixin/0.15.4+mixin.0.8.7/sponge-mixin-0.15.4+mixin.0.8.7.jar"
                    + ";lib/net/java/dev/jna/jna/5.15.0/jna-5.15.0.jar"
                    + ";lib/net/java/dev/jna/jna-platform/5.15.0/jna-platform-5.15.0.jar"
                    + ";lib/net/jodah/typetools/0.6.3/typetools-0.6.3.jar"
                    + ";lib/net/minecraft/client/1.21.4/client-1.21.4-official.jar"
                    + ";lib/net/minecraftforge/accesstransformers/8.2.0/accesstransformers-8.2.0.jar"
                    + ";lib/net/minecraftforge/accesstransformers/8.2.2/accesstransformers-8.2.2.jar"
                    + ";lib/net/minecraftforge/bootstrap/2.1.6/bootstrap-2.1.6.jar"
                    + ";lib/net/minecraftforge/bootstrap/2.1.8/bootstrap-2.1.8.jar"
                    + ";lib/net/minecraftforge/bootstrap-api/2.1.6/bootstrap-api-2.1.6.jar"
                    + ";lib/net/minecraftforge/bootstrap-api/2.1.8/bootstrap-api-2.1.8.jar"
                    + ";lib/net/minecraftforge/coremods/5.2.1/coremods-5.2.1.jar"
                    + ";lib/net/minecraftforge/coremods/5.2.6/coremods-5.2.6.jar"
                    + ";lib/net/minecraftforge/eventbus/6.2.27/eventbus-6.2.27.jar"
                    + ";lib/net/minecraftforge/eventbus/6.2.8/eventbus-6.2.8.jar"
                    + ";lib/net/minecraftforge/fmlcore/1.21.4-54.0.6/fmlcore-1.21.4-54.0.6.jar"
                    + ";lib/net/minecraftforge/fmlcore/1.21.4-54.1.0/fmlcore-1.21.4-54.1.0.jar"
                    + ";lib/net/minecraftforge/fmlearlydisplay/1.21.4-54.0.6/fmlearlydisplay-1.21.4-54.0.6.jar"
                    + ";lib/net/minecraftforge/fmlearlydisplay/1.21.4-54.1.0/fmlearlydisplay-1.21.4-54.1.0.jar"
                    + ";lib/net/minecraftforge/fmlloader/1.21.4-54.0.6/fmlloader-1.21.4-54.0.6.jar"
                    + ";lib/net/minecraftforge/fmlloader/1.21.4-54.1.0/fmlloader-1.21.4-54.1.0.jar"
                    + ";lib/net/minecraftforge/forge/1.21.4-54.0.6/forge-1.21.4-54.0.6-client.jar"
                    + ";lib/net/minecraftforge/forge/1.21.4-54.0.6/forge-1.21.4-54.0.6-shim.jar"
                    + ";lib/net/minecraftforge/forge/1.21.4-54.0.6/forge-1.21.4-54.0.6-universal.jar"
                    + ";lib/net/minecraftforge/forge/1.21.4-54.1.0/forge-1.21.4-54.1.0-client.jar"
                    + ";lib/net/minecraftforge/forge/1.21.4-54.1.0/forge-1.21.4-54.1.0-shim.jar"
                    + ";lib/net/minecraftforge/forge/1.21.4-54.1.0/forge-1.21.4-54.1.0-universal.jar"
                    + ";lib/net/minecraftforge/forgespi/7.1.5/forgespi-7.1.5.jar"
                    + ";lib/net/minecraftforge/JarJarFileSystems/0.3.26/JarJarFileSystems-0.3.26.jar"
                    + ";lib/net/minecraftforge/JarJarMetadata/0.3.26/JarJarMetadata-0.3.26.jar"
                    + ";lib/net/minecraftforge/JarJarSelector/0.3.26/JarJarSelector-0.3.26.jar"
                    + ";lib/net/minecraftforge/javafmllanguage/1.21.4-54.0.6/javafmllanguage-1.21.4-54.0.6.jar"
                    + ";lib/net/minecraftforge/javafmllanguage/1.21.4-54.1.0/javafmllanguage-1.21.4-54.1.0.jar"
                    + ";lib/net/minecraftforge/lowcodelanguage/1.21.4-54.0.6/lowcodelanguage-1.21.4-54.0.6.jar"
                    + ";lib/net/minecraftforge/lowcodelanguage/1.21.4-54.1.0/lowcodelanguage-1.21.4-54.1.0.jar"
                    + ";lib/net/minecraftforge/mclanguage/1.21.4-54.0.6/mclanguage-1.21.4-54.0.6.jar"
                    + ";lib/net/minecraftforge/mclanguage/1.21.4-54.1.0/mclanguage-1.21.4-54.1.0.jar"
                    + ";lib/net/minecraftforge/mergetool-api/1.0/mergetool-api-1.0.jar"
                    + ";lib/net/minecraftforge/modlauncher/10.2.2/modlauncher-10.2.2.jar"
                    + ";lib/net/minecraftforge/modlauncher/10.2.4/modlauncher-10.2.4.jar"
                    + ";lib/net/minecraftforge/securemodules/2.2.20/securemodules-2.2.20.jar"
                    + ";lib/net/minecraftforge/securemodules/2.2.21/securemodules-2.2.21.jar"
                    + ";lib/net/minecraftforge/unsafe/0.9.2/unsafe-0.9.2.jar"
                    + ";lib/net/minecrell/terminalconsoleappender/1.2.0/terminalconsoleappender-1.2.0.jar"
                    + ";lib/net/minidev/accessors-smart/2.5.1/accessors-smart-2.5.1.jar"
                    + ";lib/net/minidev/json-smart/2.5.1/json-smart-2.5.1.jar"
                    + ";lib/net/sf/jopt-simple/jopt-simple/5.0.4/jopt-simple-5.0.4.jar"
                    + ";lib/optifine/OptiFine/1.21.4_HD_U_J3_pre5/OptiFine-1.21.4_HD_U_J3_pre5.jar"
                    + ";lib/org/apache/commons/commons-compress/1.27.1/commons-compress-1.27.1.jar"
                    + ";lib/org/apache/commons/commons-lang3/3.17.0/commons-lang3-3.17.0.jar"
                    + ";lib/org/apache/httpcomponents/httpclient/4.5.14/httpclient-4.5.14.jar"
                    + ";lib/org/apache/httpcomponents/httpcore/4.4.16/httpcore-4.4.16.jar"
                    + ";lib/org/apache/logging/log4j/log4j-api/2.24.1/log4j-api-2.24.1.jar"
                    + ";lib/org/apache/logging/log4j/log4j-core/2.24.1/log4j-core-2.24.1.jar"
                    + ";lib/org/apache/logging/log4j/log4j-slf4j2-impl/2.24.1/log4j-slf4j2-impl-2.24.1.jar"
                    + ";lib/org/apache/maven/maven-artifact/3.8.5/maven-artifact-3.8.5.jar"
                    + ";lib/org/apache/maven/maven-artifact/3.8.8/maven-artifact-3.8.8.jar"
                    + ";lib/org/jcraft/jorbis/0.0.17/jorbis-0.0.17.jar"
                    + ";lib/org/jline/jline-reader/3.12.1/jline-reader-3.12.1.jar"
                    + ";lib/org/jline/jline-reader/3.25.1/jline-reader-3.25.1.jar"
                    + ";lib/org/jline/jline-terminal/3.12.1/jline-terminal-3.12.1.jar"
                    + ";lib/org/jline/jline-terminal/3.25.1/jline-terminal-3.25.1.jar"
                    + ";lib/org/jline/jline-terminal-jna/3.12.1/jline-terminal-jna-3.12.1.jar"
                    + ";lib/org/jline/jline-terminal-jna/3.25.1/jline-terminal-jna-3.25.1.jar"
                    + ";lib/org/joml/joml/1.10.8/joml-1.10.8.jar"
                    + ";lib/org/lwjgl/lwjgl/3.3.3/lwjgl-3.3.3.jar"
                    + ";lib/org/lwjgl/lwjgl/3.3.3/lwjgl-3.3.3-natives-windows.jar"
                    + ";lib/org/lwjgl/lwjgl/3.3.3/lwjgl-3.3.3-natives-windows-arm64.jar"
                    + ";lib/org/lwjgl/lwjgl/3.3.3/lwjgl-3.3.3-natives-windows-x86.jar"
                    + ";lib/org/lwjgl/lwjgl-freetype/3.3.3/lwjgl-freetype-3.3.3.jar"
                    + ";lib/org/lwjgl/lwjgl-freetype/3.3.3/lwjgl-freetype-3.3.3-natives-windows.jar"
                    + ";lib/org/lwjgl/lwjgl-freetype/3.3.3/lwjgl-freetype-3.3.3-natives-windows-arm64.jar"
                    + ";lib/org/lwjgl/lwjgl-freetype/3.3.3/lwjgl-freetype-3.3.3-natives-windows-x86.jar"
                    + ";lib/org/lwjgl/lwjgl-glfw/3.3.3/lwjgl-glfw-3.3.3.jar"
                    + ";lib/org/lwjgl/lwjgl-glfw/3.3.3/lwjgl-glfw-3.3.3-natives-windows.jar"
                    + ";lib/org/lwjgl/lwjgl-glfw/3.3.3/lwjgl-glfw-3.3.3-natives-windows-arm64.jar"
                    + ";lib/org/lwjgl/lwjgl-glfw/3.3.3/lwjgl-glfw-3.3.3-natives-windows-x86.jar"
                    + ";lib/org/lwjgl/lwjgl-jemalloc/3.3.3/lwjgl-jemalloc-3.3.3.jar"
                    + ";lib/org/lwjgl/lwjgl-jemalloc/3.3.3/lwjgl-jemalloc-3.3.3-natives-windows.jar"
                    + ";lib/org/lwjgl/lwjgl-jemalloc/3.3.3/lwjgl-jemalloc-3.3.3-natives-windows-arm64.jar"
                    + ";lib/org/lwjgl/lwjgl-jemalloc/3.3.3/lwjgl-jemalloc-3.3.3-natives-windows-x86.jar"
                    + ";lib/org/lwjgl/lwjgl-openal/3.3.3/lwjgl-openal-3.3.3.jar"
                    + ";lib/org/lwjgl/lwjgl-openal/3.3.3/lwjgl-openal-3.3.3-natives-windows.jar"
                    + ";lib/org/lwjgl/lwjgl-openal/3.3.3/lwjgl-openal-3.3.3-natives-windows-arm64.jar"
                    + ";lib/org/lwjgl/lwjgl-openal/3.3.3/lwjgl-openal-3.3.3-natives-windows-x86.jar"
                    + ";lib/org/lwjgl/lwjgl-opengl/3.3.3/lwjgl-opengl-3.3.3.jar"
                    + ";lib/org/lwjgl/lwjgl-opengl/3.3.3/lwjgl-opengl-3.3.3-natives-windows.jar"
                    + ";lib/org/lwjgl/lwjgl-opengl/3.3.3/lwjgl-opengl-3.3.3-natives-windows-arm64.jar"
                    + ";lib/org/lwjgl/lwjgl-opengl/3.3.3/lwjgl-opengl-3.3.3-natives-windows-x86.jar"
                    + ";lib/org/lwjgl/lwjgl-stb/3.3.3/lwjgl-stb-3.3.3.jar"
                    + ";lib/org/lwjgl/lwjgl-stb/3.3.3/lwjgl-stb-3.3.3-natives-windows.jar"
                    + ";lib/org/lwjgl/lwjgl-stb/3.3.3/lwjgl-stb-3.3.3-natives-windows-arm64.jar"
                    + ";lib/org/lwjgl/lwjgl-stb/3.3.3/lwjgl-stb-3.3.3-natives-windows-x86.jar"
                    + ";lib/org/lwjgl/lwjgl-tinyfd/3.3.3/lwjgl-tinyfd-3.3.3.jar"
                    + ";lib/org/lwjgl/lwjgl-tinyfd/3.3.3/lwjgl-tinyfd-3.3.3-natives-windows.jar"
                    + ";lib/org/lwjgl/lwjgl-tinyfd/3.3.3/lwjgl-tinyfd-3.3.3-natives-windows-arm64.jar"
                    + ";lib/org/lwjgl/lwjgl-tinyfd/3.3.3/lwjgl-tinyfd-3.3.3-natives-windows-x86.jar"
                    + ";lib/org/lz4/lz4-java/1.8.0/lz4-java-1.8.0.jar"
                    + ";lib/org/openjdk/nashorn/nashorn-core/15.4/nashorn-core-15.4.jar"
                    + ";lib/org/ow2/asm/asm/9.7.1/asm-9.7.1.jar"
                    + ";lib/org/ow2/asm/asm-analysis/9.7.1/asm-analysis-9.7.1.jar"
                    + ";lib/org/ow2/asm/asm-commons/9.7.1/asm-commons-9.7.1.jar"
                    + ";lib/org/ow2/asm/asm-tree/9.7.1/asm-tree-9.7.1.jar"
                    + ";lib/org/ow2/asm/asm-util/9.7.1/asm-util-9.7.1.jar"
                    + ";lib/org/slf4j/slf4j-api/2.0.16/slf4j-api-2.0.16.jar"
                    // + ";lib/org/spongepowered/mixin/0.8.7/mixin-0.8.7.jar"
                    + ";lib/v1/objects/a7e5a6024bfd3cd614625aa05629adf760020304/client.jar";
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
                    "--username", nickname);
            pb.directory(new File("."));
            pb.inheritIO();
            Process proc = pb.start();
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
            JOptionPane.showMessageDialog(this, "Ошибка запуска: " + ex.getMessage(), "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
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
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
            if (string.matches("\\d+")) {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
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
