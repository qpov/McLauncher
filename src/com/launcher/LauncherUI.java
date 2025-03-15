package com.launcher;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;
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
        setTitle("QmLauncher 1.6.5");
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

    // Фоновая загрузка конфигурации серверов из servers.json
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
                                "Ошибка загрузки серверов.", "Error",
                                JOptionPane.ERROR_MESSAGE);
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

    // Папка установки для client.jar (скачивается в version/[название сервера])
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

        // Панель модов (если нужна)
        modPanel = new JPanel();
        modPanel.setBorder(BorderFactory.createTitledBorder("Моды"));
        modPanel.setLayout(new BoxLayout(modPanel, BoxLayout.Y_AXIS));
        JPanel modsContainer = new JPanel(new BorderLayout());
        modsContainer.add(new JScrollPane(modPanel), BorderLayout.CENTER);
        toggleModsButton = new JButton("Включить все моды");
        // Логику переключения модов можно добавить по желанию
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
                runGame(installDir, getServerConfigByName(serverName), nicknameField.getText().trim());
            } else {
                installGameWithProgress();
            }
        });
        buttonsPanel.add(launchButton);

        openFolderButton = new JButton("Открыть папку");
        buttonsPanel.add(openFolderButton);

        reinstallGameButton = new JButton("Переустановить");
        reinstallGameButton.addActionListener(e -> installGameWithProgress());
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
            if ("dark".equalsIgnoreCase(theme)) {
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

    /*
     * При нажатии на кнопку "Установить игру" запускаются две параллельные задачи:
     * 1. Скачивание client.jar в папку version/[сервер] (если ссылка заканчивается
     * на ".jar").
     * 2. Если каталоги assets, lib или native отсутствуют, автоматическая загрузка
     * всех архивов из папки data на GitHub,
     * объединение частей и их извлечение в папку лаунчера.
     */
    private void installGameWithProgress() {
        new DownloadAllArchivesTask().execute();
    }

    // Задача для автоматической загрузки архивов из папки data на GitHub
    private class DownloadAllArchivesTask extends SwingWorker<Void, Integer> {

        @Override
        protected Void doInBackground() throws Exception {
            // Если папки assets, lib и native уже существуют – пропускаем скачивание
            // архивов
            if (new File("assets").exists() && new File("lib").exists() && new File("native").exists()) {
                System.out.println("assets, lib и native уже существуют. Скачивание архивов пропущено.");
                return null;
            }
            // Получаем список файлов из GitHub API
            String apiUrl = "https://api.github.com/repos/qpov/QmLauncher/contents/data?ref=refs/heads/main";
            List<GHFileInfo> files = fetchGitHubFiles(apiUrl);
            System.out.println("Найдено файлов: " + files.size());
            if (files.isEmpty()) {
                System.out.println("Файлы в папке data не найдены.");
                return null;
            }
            // Группируем файлы по префиксу (например, assets.7z, lib.7z, native.7z или
            // .zip)
            Map<String, List<GHFileInfo>> groups = new HashMap<>();
            for (GHFileInfo fi : files) {
                String lower = fi.name.toLowerCase();
                if (!lower.contains(".7z.") && !lower.contains(".zip."))
                    continue;
                int lastDot = fi.name.lastIndexOf('.');
                if (lastDot < 0)
                    continue;
                String prefix = fi.name.substring(0, lastDot);
                groups.computeIfAbsent(prefix, k -> new ArrayList<>()).add(fi);
            }
            System.out.println("Найдено групп архивов: " + groups.size());
            // Для каждой группы объединяем части и извлекаем архив
            for (Map.Entry<String, List<GHFileInfo>> entry : groups.entrySet()) {
                String prefix = entry.getKey();
                List<GHFileInfo> groupFiles = entry.getValue();
                groupFiles.sort(Comparator.comparing(fi -> fi.name));
                System.out.println("Обработка группы: " + prefix + " (частей: " + groupFiles.size() + ")");
                File combinedArchive = combineParts(prefix, groupFiles);
                System.out.println("Объединённый архив: " + combinedArchive.getAbsolutePath());
                extractArchive(combinedArchive, new File("."));
                combinedArchive.delete();
            }
            return null;
        }

        private List<GHFileInfo> fetchGitHubFiles(String apiUrl) throws IOException {
            List<GHFileInfo> result = new ArrayList<>();
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            if (conn.getResponseCode() == 200) {
                try (InputStream in = conn.getInputStream();
                        InputStreamReader isr = new InputStreamReader(in)) {
                    JsonArray arr = new Gson().fromJson(isr, JsonArray.class);
                    for (int i = 0; i < arr.size(); i++) {
                        JsonObject obj = arr.get(i).getAsJsonObject();
                        if (!"file".equals(obj.get("type").getAsString()))
                            continue;
                        String name = obj.get("name").getAsString();
                        String downloadUrl = obj.get("download_url").getAsString();
                        result.add(new GHFileInfo(name, downloadUrl));
                    }
                }
            }
            return result;
        }

        private File combineParts(String prefix, List<GHFileInfo> parts) throws IOException {
            // Определяем, какой формат: если хотя бы в одном имени есть ".zip.", считаем zip, иначе .7z
            // (или используйте логику как у вас — например, если name содержит ".zip.")
            boolean isZip = false;
            for (GHFileInfo fi : parts) {
                if (fi.name.toLowerCase().contains(".zip.")) {
                    isZip = true;
                    break;
                }
            }
        
            // Изначально prefix, например, "native.7z" (без .001)
            // Если prefix уже заканчивается на ".7z" или ".zip", удаляем, чтобы не было двойного .7z.7z
            if (prefix.endsWith(".7z")) {
                prefix = prefix.substring(0, prefix.length() - 3); // убираем ".7z"
            } else if (prefix.endsWith(".zip")) {
                prefix = prefix.substring(0, prefix.length() - 4); // убираем ".zip"
            }
        
            // Добавляем нужное расширение
            String ext = isZip ? ".zip" : ".7z";
        
            File combinedFile = new File(prefix + ext); // напр. "native.7z" или "assets.7z"
            if (combinedFile.exists()) {
                combinedFile.delete();
            }
        
            try (FileOutputStream fos = new FileOutputStream(combinedFile)) {
                for (GHFileInfo fi : parts) {
                    System.out.println("Скачивание части: " + fi.name);
                    File tempPart = File.createTempFile("archpart", ".part");
                    downloadFile(fi.downloadUrl, tempPart);
        
                    try (FileInputStream fis = new FileInputStream(tempPart)) {
                        byte[] buf = new byte[4096];
                        int read;
                        while ((read = fis.read(buf)) != -1) {
                            fos.write(buf, 0, read);
                        }
                    }
                    tempPart.delete();
                }
            }
            return combinedFile;
        }        

        private void extractArchive(File archive, File destDir) throws IOException {
            String name = archive.getName().toLowerCase();
            if (name.endsWith(".zip")) {
                unzip(archive, destDir);
            } else if (name.endsWith(".7z")) {
                extract7z(archive, destDir);
            } else {
                System.out.println("Неизвестный формат архива: " + archive.getName());
            }
        }

        private void downloadFile(String url, File outFile) throws IOException {
            try (InputStream in = new URL(url).openStream();
                    FileOutputStream fos = new FileOutputStream(outFile)) {
                byte[] buf = new byte[4096];
                int read;
                while ((read = in.read(buf)) != -1) {
                    fos.write(buf, 0, read);
                }
            }
        }
    }

    // Извлечение zip-архива
    private void unzip(File zipFile, File destDir) throws IOException {
        byte[] buffer = new byte[4096];
        if (!destDir.exists())
            destDir.mkdirs();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = newFile(destDir, entry.getName());
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    newFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
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

    // Извлечение 7z-архива с использованием Apache Commons Compress
    private void extract7z(File sevenZFile, File destDir) throws IOException {
        if (!destDir.exists())
            destDir.mkdirs();
        try (SevenZFile sevenZ = new SevenZFile(sevenZFile)) {
            SevenZArchiveEntry entry;
            byte[] content = new byte[8192];
            while ((entry = sevenZ.getNextEntry()) != null) {
                File outFile = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    outFile.mkdirs();
                    continue;
                }
                outFile.getParentFile().mkdirs();
                try (FileOutputStream out = new FileOutputStream(outFile)) {
                    int bytesRead;
                    while ((bytesRead = sevenZ.read(content)) > 0) {
                        out.write(content, 0, bytesRead);
                    }
                }
            }
        }
    }

    // Защита от Zip Slip для zip-архивов
    private File newFile(File destinationDir, String entryName) throws IOException {
        File destFile = new File(destinationDir, entryName);
        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Zip Slip: " + entryName);
        }
        return destFile;
    }

    // Класс для прямого скачивания client.jar
    private class DirectDownloadTask extends SwingWorker<Void, Integer> {
        private String fileURL;
        private File destinationFile;

        public DirectDownloadTask(String fileURL, File destinationFile) {
            this.fileURL = fileURL;
            this.destinationFile = destinationFile;
        }

        @Override
        protected Void doInBackground() throws Exception {
            URL url = new URL(fileURL);
            try (InputStream in = url.openStream();
                    FileOutputStream out = new FileOutputStream(destinationFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    if (isCancelled())
                        break;
                    out.write(buffer, 0, bytesRead);
                }
            }
            return null;
        }
    }

    // Запуск игры (пустой classpath – заполните сами)
    private void runGame(File installDir, ServerConfig selectedServer, String nickname) {
        try {
            launchButton.setEnabled(false);
            String clientJarPath = new File(installDir, "client.jar").getAbsolutePath();
            String maxRam = ramField.getText().trim();
            String xmxParam = "-Xmx" + maxRam + "G";
            // Добавьте свои библиотеки сюда:
            String baseClasspath = clientJarPath
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

            String finalClasspath;
            String mainClass;
            ProcessBuilder pb;
            if (selectedServer.fabric_version != null && !selectedServer.fabric_version.trim().isEmpty()) {
                finalClasspath = baseClasspath;
                mainClass = "net.fabricmc.loader.impl.launch.knot.KnotClient";
            } else if (selectedServer.forge_version != null && !selectedServer.forge_version.trim().isEmpty()) {
                finalClasspath = baseClasspath;
                mainClass = "net.minecraft.client.main.Main";
            } else {
                throw new IllegalArgumentException("Не удалось определить тип загрузчика для выбранного сервера.");
            }

            pb = new ProcessBuilder(
                    "java",
                    xmxParam,
                    "-Djava.library.path=native",
                    "-cp", finalClasspath,
                    mainClass,
                    "--accessToken", "dummy",
                    "--uuid", "dummy-uuid",
                    "--clientId", "dummy-clientid",
                    "--xuid", "dummy-xuid",
                    "--version", selectedServer.minecraft_version,
                    "--gameDir", installDir.getAbsolutePath(),
                    "--assetsDir", new File("assets").getAbsolutePath(),
                    "--assetIndex", "19",
                    "--username", nickname);
            pb.directory(new File("."));
            pb.inheritIO();
            Process process = pb.start();
            JOptionPane.showMessageDialog(this,
                    "Игра запускается через " + (selectedServer.fabric_version != null ? "Fabric" : "Forge") + "...");
            if (Boolean.parseBoolean(settings.getProperty("hideLauncher"))) {
                setVisible(false);
            }
            new Thread(() -> {
                try {
                    process.waitFor();
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
            JOptionPane.showMessageDialog(this, "Ошибка запуска: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            launchButton.setEnabled(true);
        }
    }

    // Структура для хранения информации о файле из GitHub
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
