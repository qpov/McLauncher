package com.launcher;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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

    private List<ServerConfig> serverConfigs;
    private List<ModConfig> defaultMods;

    public LauncherUI() {
        setTitle("QmLauncher 1.6.0");
        setSize(960, 540);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        loadSettings();
        loadConfigs();
        loadNickname();
        loadRam();
        initUI();
        updateLaunchButton();
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

    private void loadConfigs() {
        String url = "https://raw.githubusercontent.com/qpov/QmLauncher/refs/heads/main/servers.json?t="
                + System.currentTimeMillis();
        ServerList config = ConfigLoader.loadServerConfigs(url);
        if (config != null && config.servers != null) {
            serverConfigs = config.servers;
        } else {
            JOptionPane.showMessageDialog(this, "Ошибка загрузки конфигурации серверов.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            serverConfigs = new ArrayList<>();
        }
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

    private File getInstallDirForServer(String serverName) {
        File dir = new File("version", serverName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private void initUI() {
        String theme = settings.getProperty("theme");
        applyTheme(theme);

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
        if (serverConfigs != null) {
            for (ServerConfig sc : serverConfigs) {
                serverComboBox.addItem(sc.name);
            }
        }
        serverComboBox.addActionListener(e -> {
            updateLaunchButton();
            updateModPanel();
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
        updateModPanel();

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
            toggleModsButton.setText(newState ? "Выключить все моды" : "Включить все моды");
        });
        modsContainer.add(toggleModsButton, BorderLayout.SOUTH);
        panel.add(modsContainer, BorderLayout.CENTER);

        // Нижняя панель кнопок
        JPanel buttonsPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        launchButton = new JButton();
        launchButton.addActionListener(e -> {
            String selectedServerName = (String) serverComboBox.getSelectedItem();
            ServerConfig selectedServer = getServerConfigByName(selectedServerName);
            File installDir = getInstallDirForServer(selectedServerName);
            if (installDir.exists() && new File(installDir, "client.jar").exists()) {
                if (!selectedServer.allow_custom_mods && modsModified(selectedServer.mods)) {
                    JOptionPane.showMessageDialog(this,
                            "Данный сервер не разрешает устанавливать дополнительные моды.",
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String nick = nicknameField.getText().trim();
                if (nick.isEmpty()) {
                    nick = "Player";
                }
                settings.setProperty("nickname", nick);
                settings.setProperty("ram", ramField.getText().trim());
                settings.setProperty("hideLauncher", Boolean.toString(hideLauncherCheckBox.isSelected()));
                saveSettings();
                if (!updateModsForServer(selectedServer)) {
                    JOptionPane.showMessageDialog(this,
                            "Ошибка при обновлении модов для выбранного сервера.",
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                applyModSelection(selectedServer);
                runGame(getInstallDirForServer(selectedServerName), selectedServer, nick);
            } else {
                installGameWithProgress();
            }
        });
        buttonsPanel.add(launchButton);

        openFolderButton = new JButton("Открыть папку с игрой");
        openFolderButton.addActionListener(e -> {
            String selectedServerName = (String) serverComboBox.getSelectedItem();
            File installDir = getInstallDirForServer(selectedServerName);
            if (installDir.exists()) {
                try {
                    Desktop.getDesktop().open(installDir);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this,
                            "Невозможно открыть папку: " + ex.getMessage(),
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this,
                        "Игра не установлена.",
                        "Информация", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        buttonsPanel.add(openFolderButton);

        reinstallGameButton = new JButton("Переустановить игру");
        reinstallGameButton.addActionListener(e -> installGameWithProgress());
        buttonsPanel.add(reinstallGameButton);

        toggleThemeButton = new JButton("Сменить тему");
        toggleThemeButton.addActionListener(e -> {
            String current = loadTheme();
            String next = current.equals("dark") ? "light" : "dark";
            applyTheme(next);
            saveTheme(next);
        });
        buttonsPanel.add(toggleThemeButton);

        panel.add(buttonsPanel, BorderLayout.SOUTH);
        add(panel);
    }

    private String loadTheme() {
        return settings.getProperty("theme");
    }

    private void saveTheme(String theme) {
        settings.setProperty("theme", theme);
        saveSettings();
    }

    private void applyTheme(String theme) {
        try {
            if (theme.equals("dark")) {
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
        String selectedServerName = (String) serverComboBox.getSelectedItem();
        File installDir = getInstallDirForServer(selectedServerName);
        if (installDir.exists() && new File(installDir, "client.jar").exists()) {
            launchButton.setText("Запустить");
        } else {
            launchButton.setText("Установить игру");
        }
    }

    // Реальная реализация обновления панели модов из вашей рабочей версии
    private void updateModPanel() {
        modPanel.removeAll();
        String selectedName = (String) serverComboBox.getSelectedItem();
        ServerConfig selectedServer = getServerConfigByName(selectedName);
        if (selectedServer != null) {
            File installDir = getInstallDirForServer(selectedServer.name);
            File modsFolder = new File(installDir, "mods");
            List<String> enabledMods = new ArrayList<>();
            List<String> disabledMods = new ArrayList<>();
            if (modsFolder.exists()) {
                File[] files = modsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
                if (files != null) {
                    for (File file : files) {
                        enabledMods.add(file.getName());
                    }
                }
                File disableFolder = new File(modsFolder, "disable");
                if (disableFolder.exists()) {
                    File[] disFiles = disableFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
                    if (disFiles != null) {
                        for (File file : disFiles) {
                            disabledMods.add(file.getName());
                        }
                    }
                }
            }
            for (String modName : enabledMods) {
                JCheckBox cb = new JCheckBox(modName, true);
                if (!selectedServer.allow_custom_mods && selectedServer.mods != null) {
                    boolean isRequired = false;
                    for (ModConfig req : selectedServer.mods) {
                        if ((req.name + ".jar").equalsIgnoreCase(modName)) {
                            isRequired = true;
                            break;
                        }
                    }
                    if (!isRequired) {
                        cb.setForeground(Color.RED);
                        cb.setToolTipText("Сервер запрещает использовать другие моды.");
                    }
                }
                modPanel.add(cb);
            }
            for (String modName : disabledMods) {
                JCheckBox cb = new JCheckBox(modName, false);
                if (selectedServer.mods != null) {
                    for (ModConfig req : selectedServer.mods) {
                        if ((req.name + ".jar").equalsIgnoreCase(modName)) {
                            cb.setForeground(Color.ORANGE);
                            cb.setToolTipText("Отсутствуют требуемые моды.");
                            break;
                        }
                    }
                }
                modPanel.add(cb);
            }
            if (selectedServer.mods != null) {
                for (ModConfig req : selectedServer.mods) {
                    String reqFile = req.name + ".jar";
                    if (!enabledMods.contains(reqFile) && !disabledMods.contains(reqFile)) {
                        JCheckBox cb = new JCheckBox(reqFile, false);
                        cb.setForeground(Color.ORANGE);
                        cb.setToolTipText("Отсутствуют требуемые моды.");
                        modPanel.add(cb);
                    }
                }
            }
        }
        modPanel.revalidate();
        modPanel.repaint();
    }

    // Реальная проверка изменений модов
    private boolean modsModified(List<ModConfig> requiredMods) {
        String selectedName = (String) serverComboBox.getSelectedItem();
        File installDir = getInstallDirForServer(selectedName);
        File modsFolder = new File(installDir, "mods");
        List<String> installed = new ArrayList<>();
        if (modsFolder.exists()) {
            File[] files = modsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
            if (files != null) {
                for (File file : files) {
                    installed.add(file.getName());
                }
            }
        }
        if (requiredMods == null) {
            return false;
        }
        for (ModConfig req : requiredMods) {
            String reqFile = req.name + ".jar";
            if (!installed.contains(reqFile)) {
                return true;
            }
        }
        return false;
    }

    // Реальная реализация обновления модов, как в вашем старом коде
    private boolean updateModsForServer(ServerConfig selectedServer) {
        try {
            File installDir = getInstallDirForServer(selectedServer.name);
            File gameModsFolder = new File(installDir, "mods");
            if (!gameModsFolder.exists()) {
                gameModsFolder.mkdirs();
            }
            File serverModsFolder = new File("mods", selectedServer.name);
            if (!serverModsFolder.exists()) {
                serverModsFolder.mkdirs();
            }
            if (selectedServer.name.equalsIgnoreCase("Default")) {
                File[] files = serverModsFolder.listFiles();
                if (files != null) {
                    for (File modFile : files) {
                        File destFile = new File(gameModsFolder, modFile.getName());
                        if (!destFile.exists()) {
                            Files.copy(modFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
            } else {
                if (!selectedServer.allow_custom_mods) {
                    File[] existing = gameModsFolder.listFiles();
                    if (existing != null) {
                        for (File f : existing) {
                            f.delete();
                        }
                    }
                    if (selectedServer.mods != null) {
                        for (ModConfig mod : selectedServer.mods) {
                            File modFile = new File(serverModsFolder, mod.name + ".jar");
                            if (!modFile.exists()) {
                                downloadFile(mod.url, modFile);
                            }
                            Files.copy(modFile.toPath(),
                                    new File(gameModsFolder, modFile.getName()).toPath(),
                                    StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                } else {
                    File[] files = serverModsFolder.listFiles();
                    if (files != null) {
                        for (File modFile : files) {
                            File destFile = new File(gameModsFolder, modFile.getName());
                            if (!destFile.exists()) {
                                Files.copy(modFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                    }
                }
            }
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    // Реальная реализация применения выбранных модов
    private void applyModSelection(ServerConfig selectedServer) {
        File installDir = getInstallDirForServer(selectedServer.name);
        File gameModsFolder = new File(installDir, "mods");
        if (!gameModsFolder.exists()) {
            gameModsFolder.mkdirs();
        }
        File disableFolder = new File(gameModsFolder, "disable");
        if (!disableFolder.exists()) {
            disableFolder.mkdirs();
        }
        Component[] comps = modPanel.getComponents();
        for (Component comp : comps) {
            if (comp instanceof JCheckBox) {
                JCheckBox cb = (JCheckBox) comp;
                String modFileName = cb.getText();
                File modFile = new File(gameModsFolder, modFileName);
                File disabledFile = new File(disableFolder, modFileName);
                if (!cb.isSelected()) {
                    if (modFile.exists()) {
                        try {
                            Files.move(modFile.toPath(), disabledFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                } else {
                    if (disabledFile.exists()) {
                        try {
                            Files.move(disabledFile.toPath(), modFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private void downloadFile(String fileURL, File destinationFile) throws IOException {
        URL url = new URL(fileURL);
        try (InputStream inputStream = url.openStream();
                FileOutputStream outputStream = new FileOutputStream(destinationFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    private void installGameWithProgress() {
        String selectedServerName = (String) serverComboBox.getSelectedItem();
        ServerConfig selectedServer = getServerConfigByName(selectedServerName);
        if (selectedServer != null) {
            File installDir = getInstallDirForServer(selectedServerName);
            JDialog progressDialog = new JDialog(this, "Загрузка...", true);
            JProgressBar progressBar = new JProgressBar(0, 100);
            progressBar.setIndeterminate(true);
            JButton cancelButton = new JButton("Отмена");

            JPanel p = new JPanel(new BorderLayout(10, 10));
            p.add(new JLabel("Скачивание игры..."), BorderLayout.NORTH);
            p.add(progressBar, BorderLayout.CENTER);
            p.add(cancelButton, BorderLayout.SOUTH);
            progressDialog.getContentPane().add(p);
            progressDialog.setSize(300, 120);
            progressDialog.setLocationRelativeTo(this);

            launchButton.setEnabled(false);

            DownloadTask worker = new DownloadTask(selectedServer.download_link, new File(installDir, "client.jar")) {
                @Override
                protected void done() {
                    progressDialog.dispose();
                    launchButton.setEnabled(true);
                    if (!isCancelled()) {
                        JOptionPane.showMessageDialog(LauncherUI.this,
                                "Игра скачана успешно!\nПуть установки: " + installDir.getAbsolutePath());
                        updateLaunchButton();
                    }
                }
            };

            cancelButton.addActionListener(e -> worker.cancel(true));
            worker.execute();
            progressDialog.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "Конфигурация для выбранного сервера не найдена.");
        }
    }

    private class DownloadTask extends SwingWorker<Void, Integer> {
        private final String url;
        private final File destination;

        public DownloadTask(String url, File destination) {
            this.url = url;
            this.destination = destination;
        }

        @Override
        protected Void doInBackground() throws Exception {
            URL downloadUrl = new URL(url);
            try (InputStream in = downloadUrl.openStream();
                    FileOutputStream out = new FileOutputStream(destination)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1 && !isCancelled()) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            return null;
        }

        @Override
        protected void done() {
            // Диалог закрывается в installGameWithProgress()
        }
    }

    // Реальная реализация запуска игры с использованием общего списка библиотек
    private void runGame(File installDir, ServerConfig selectedServer, String nickname) {
        try {
            launchButton.setEnabled(false);
            String clientJarPath = new File(installDir, "client.jar").getAbsolutePath();
            String maxRam = ramField.getText().trim();
            String xmxParam = "-Xmx" + maxRam + "G";

            // Общий базовый classpath
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
                finalClasspath = baseClasspath
                        + ";lib/net/fabricmc/fabric-loader/" + selectedServer.fabric_version + "/fabric-loader-"
                        + selectedServer.fabric_version + ".jar"
                        + ";lib/net/fabricmc/intermediary/1.21.4/intermediary-1.21.4.jar"
                        + ";lib/net/fabricmc/sponge-mixin/0.15.4+mixin.0.8.7/sponge-mixin-0.15.4+mixin.0.8.7.jar";
                mainClass = "net.fabricmc.loader.impl.launch.knot.KnotClient";
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
                        "--userType", "mojang",
                        "--versionType", "release",
                        "--version", selectedServer.minecraft_version,
                        "--gameDir",
                        getInstallDirForServer((String) serverComboBox.getSelectedItem()).getAbsolutePath(),
                        "--assetsDir", new File("assets").getAbsolutePath(),
                        "--assetIndex", "19",
                        "--username", nicknameField.getText().trim());
            } else if (selectedServer.forge_version != null && !selectedServer.forge_version.trim().isEmpty()) {
                finalClasspath = baseClasspath
                        + ";lib/net/minecraftforge/forge/" + selectedServer.forge_version + "/forge-"
                        + selectedServer.forge_version + "-client.jar";
                mainClass = "net.minecraft.client.main.Main";
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
                        "--gameDir",
                        getInstallDirForServer((String) serverComboBox.getSelectedItem()).getAbsolutePath(),
                        "--assetsDir", new File("assets").getAbsolutePath(),
                        "--assetIndex", "19",
                        "--username", nicknameField.getText().trim());
            } else {
                throw new IllegalArgumentException("Не удалось определить тип загрузчика для выбранного сервера.");
            }

            pb.directory(new File("."));
            pb.inheritIO();
            Process process = pb.start();
            String message = (selectedServer.fabric_version != null && !selectedServer.fabric_version.trim().isEmpty())
                    ? "Игра запускается через Fabric Loader..."
                    : "Игра запускается через Forge...";
            JOptionPane.showMessageDialog(this, message);

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
            JOptionPane.showMessageDialog(this,
                    "Ошибка при запуске игры: " + ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
            launchButton.setEnabled(true);
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LauncherUI launcher = new LauncherUI();
            launcher.setVisible(true);
        });
    }

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
}
