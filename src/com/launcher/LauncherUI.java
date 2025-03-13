package com.launcher;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LauncherUI extends JFrame {

    private static final String NICKNAME_FILE = "nickname.txt";
    private static final String THEME_FILE = "theme.txt";

    private JTextField nicknameField;
    private JComboBox<String> serverComboBox;
    private JButton launchButton; // "Установить игру" или "Запустить"
    private JButton openFolderButton;
    private JButton reinstallGameButton; // Переустановить игру (перескачка client.jar)
    private JButton toggleThemeButton; // Сменить тему
    private JPanel modPanel;

    // Список серверов из JSON
    private List<ServerConfig> serverConfigs;
    // Дефолтный список требуемых модов для текущего сервера (из JSON)
    private List<ModConfig> defaultMods;

    public LauncherUI() {
        setTitle("McLauncher");
        setSize(960, 540);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        loadConfigs();
        loadNickname();
        initUI();
        updateLaunchButton();
    }

    // Загрузка конфигурации серверов из servers.json
    private void loadConfigs() {
        ServerList config = ConfigLoader.loadServerConfigs("servers.json");
        if (config != null && config.servers != null) {
            serverConfigs = config.servers;
        } else {
            JOptionPane.showMessageDialog(this, "Ошибка загрузки конфигурации серверов.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            serverConfigs = new ArrayList<>();
        }
    }

    // Папка установки для сервера: всегда version/<serverName>
    private File getInstallDirForServer(String serverName) {
        File dir = new File("version", serverName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    // Загрузка ника из файла
    private void loadNickname() {
        File f = new File(NICKNAME_FILE);
        String nick = "Player";
        if (f.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
                String line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    nick = line.trim();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        nicknameField = new JTextField(nick, 15);
    }

    // Сохранение ника
    private void saveNickname(String nick) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(NICKNAME_FILE))) {
            writer.println(nick);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Загрузка темы
    private String loadTheme() {
        File f = new File(THEME_FILE);
        String theme = "dark";
        if (f.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
                String line = reader.readLine();
                if (line != null && (line.equalsIgnoreCase("dark") || line.equalsIgnoreCase("light"))) {
                    theme = line.trim().toLowerCase();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return theme;
    }

    // Сохранение темы
    private void saveTheme(String theme) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(THEME_FILE))) {
            writer.println(theme);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Применение темы
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

    // Инициализация интерфейса
    private void initUI() {
        String theme = loadTheme();
        applyTheme(theme);

        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Верхняя панель: Ник и выбор сервера
        JPanel topPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        JPanel nickPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        nickPanel.add(new JLabel("Ник:"));
        nickPanel.add(nicknameField);
        topPanel.add(nickPanel);

        JPanel serverPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        serverPanel.add(new JLabel("Выберите сервер:"));
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
        serverPanel.add(serverComboBox);
        topPanel.add(serverPanel);
        panel.add(topPanel, BorderLayout.NORTH);

        // Панель для модов
        modPanel = new JPanel();
        modPanel.setBorder(BorderFactory.createTitledBorder("Моды"));
        modPanel.setLayout(new BoxLayout(modPanel, BoxLayout.Y_AXIS));
        updateModPanel();
        panel.add(new JScrollPane(modPanel), BorderLayout.CENTER);

        // Нижняя панель кнопок (Порядок: launch, reinstall, open folder, toggle theme)
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
                saveNickname(nick);
                if (!updateModsForServer(selectedServer)) {
                    JOptionPane.showMessageDialog(this,
                            "Ошибка при обновлении модов для выбранного сервера.",
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // Применяем выбор модов (включая возврат из disable)
                applyModSelection(selectedServer);
                runGame(installDir, selectedServer, nick);
            } else {
                installGameWithProgress();
            }
        });
        buttonsPanel.add(launchButton);

        // Кнопка "Переустановить игру" – теперь всегда скачивает client.jar заново
        reinstallGameButton = new JButton("Переустановить игру");
        reinstallGameButton.addActionListener(e -> {
            // При переустановке игры всегда вызываем скачивание client.jar
            installGameWithProgress();
        });
        buttonsPanel.add(reinstallGameButton);

        // Кнопка "Открыть папку с игрой"
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

        // Кнопка "Сменить тему"
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

    // Обновление текста кнопки "Установить игру"/"Запустить"
    private void updateLaunchButton() {
        String selectedServerName = (String) serverComboBox.getSelectedItem();
        File installDir = getInstallDirForServer(selectedServerName);
        if (installDir.exists() && new File(installDir, "client.jar").exists()) {
            launchButton.setText("Запустить");
        } else {
            launchButton.setText("Установить игру");
        }
    }

    // Обновление списка модов.
    // Сканируются папки mods (в папке установки) и mods/disable.
    // Если сервер запрещает дополнительные моды, лишние моды выделяются красным.
    // Если требуемый мод отсутствует, добавляется с оранжевым текстом.
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
                // Читаем включённые моды
                File[] files = modsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
                if (files != null) {
                    for (File file : files) {
                        enabledMods.add(file.getName());
                    }
                }
                // Если есть папка disable, читаем отключённые моды
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
            // Показываем включённые моды (чекбоксы отмечены)
            for (String modName : enabledMods) {
                JCheckBox cb = new JCheckBox(modName, true);
                // Если сервер не разрешает дополнительные моды и этот мод не входит в требуемые
                // – выделяем красным
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
            // Показываем отключённые моды (чекбоксы не отмечены)
            for (String modName : disabledMods) {
                JCheckBox cb = new JCheckBox(modName, false);
                // Если требуемый мод отсутствует – выделяем оранжевым
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
            // Проверяем наличие требуемых модов, которых нет ни в включённых, ни в
            // отключённых
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

    // Проверка, изменилось ли множество модов (для серверов, где allow_custom_mods
    // == false)
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
                return true; // Требуемый мод отсутствует
            }
        }
        return false;
    }

    // Скачивание файла по URL
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

    // Скачивание игры с прогрессом в папку version/<serverName>
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

            DownloadTask worker = new DownloadTask(selectedServer.download_link, new File(installDir, "client.jar")) {
                @Override
                protected void done() {
                    progressDialog.dispose();
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

    // SwingWorker для скачивания файла с прогрессом
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

    // Обновление/установка модов для выбранного сервера
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
            // Если сервер "Default", копируем все моды из serverModsFolder
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
                    // Если сервер запрещает дополнительные моды – очищаем папку и устанавливаем
                    // только требуемые
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
                    // Если разрешено, копируем все файлы из serverModsFolder
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

    // Новый метод: применение выбора модов.
    // Если мод не выбран (чекбокс не отмечен), перемещаем его в папку disable
    // (создаётся внутри mods),
    // если выбран – перемещаем обратно в папку mods.
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

    // Запуск игры через Fabric Loader
    private void runGame(File installDir, ServerConfig selectedServer, String nickname) {
        try {
            String clientJarPath = new File(installDir, "client.jar").getAbsolutePath();
            String classpath = clientJarPath
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

            ProcessBuilder pb = new ProcessBuilder(
                    "java",
                    "-Djava.library.path=native",
                    "-cp", classpath,
                    "net.fabricmc.loader.impl.launch.knot.KnotClient",
                    "--accessToken", "dummy",
                    "--uuid", "dummy-uuid",
                    "--clientId", "dummy-clientid",
                    "--xuid", "dummy-xuid",
                    "--userType", "mojang",
                    "--versionType", "release",
                    "--version", getServerConfigByName((String) serverComboBox.getSelectedItem()).minecraft_version,
                    "--gameDir", getInstallDirForServer((String) serverComboBox.getSelectedItem()).getAbsolutePath(),
                    "--assetsDir", new File("assets").getAbsolutePath(),
                    "--assetIndex", "19",
                    "--username", nicknameField.getText().trim());
            pb.directory(new File("."));
            pb.inheritIO();
            pb.start();

            JOptionPane.showMessageDialog(this, "Игра запускается через Fabric Loader...");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Ошибка при запуске игры через Fabric Loader: " + ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    // Поиск конфигурации сервера по имени
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
}