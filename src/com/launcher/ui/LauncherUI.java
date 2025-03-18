package com.launcher.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.launcher.ConfigLoader;
import com.launcher.ServerConfig;
import com.launcher.ServerList;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

public class LauncherUI extends JFrame {

    private static final String SETTINGS_FILE_NAME = "settings.txt";
    private Properties settings = new Properties();

    // Панель контента с CardLayout для центральной части
    private JPanel contentPanel;
    private final String MAIN_PANEL = "main";
    private final String SETTINGS_PANEL = "settings";

    public TitleBarPanel titleBarPanel;
    public MainBottomPanel mainBottomPanel;
    private SettingsPanel settingsPanel;

    private List<ServerConfig> serverConfigs;

    public LauncherUI() {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
            UIManager.put("ComboBox.arrowButtonForeground", Color.WHITE);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        Font aptosSemiBold;
        try {
            aptosSemiBold = Font.createFont(Font.TRUETYPE_FONT, new File("resources/fonts/Aptos-SemiBold.ttf"))
                    .deriveFont(14f);
        } catch (Exception ex) {
            ex.printStackTrace();
            aptosSemiBold = new Font("SansSerif", Font.BOLD, 14);
        }
        setGlobalFont(new FontUIResource(aptosSemiBold));

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(screenSize.width / 2, screenSize.height / 2);
        setLocationRelativeTo(null);
        setUndecorated(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        loadSettings();

        setContentPane(new BackgroundPanel("resources/img/bg.png", new BorderLayout()));
        Container cp = getContentPane();
        cp.setLayout(new BorderLayout());

        titleBarPanel = new TitleBarPanel(this, "QmLauncher", "v1.7.0");
        titleBarPanel.updateBackButton(false, null);
        mainBottomPanel = new MainBottomPanel();

        titleBarPanel.setPanelAlpha(0.5f);
        mainBottomPanel.setPanelAlpha(0.5f);

        contentPanel = new JPanel(new CardLayout());
        contentPanel.setOpaque(false);

        AdPanel adPanel = new AdPanel();
        adPanel.setOpaque(false);
        contentPanel.add(adPanel, MAIN_PANEL);

        settingsPanel = new SettingsPanel(this, settings, () -> switchToMain());
        settingsPanel.setOpaque(false);
        contentPanel.add(settingsPanel, SETTINGS_PANEL);

        cp.add(titleBarPanel, BorderLayout.NORTH);
        cp.add(contentPanel, BorderLayout.CENTER);
        cp.add(mainBottomPanel, BorderLayout.SOUTH);

        loadServerConfigsInBackground();
    }

    public static void setGlobalFont(FontUIResource f) {
        Enumeration<?> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, f);
            }
        }
    }

    public void onPlayClicked() {
        String serverName = (String) mainBottomPanel.serverComboBox.getSelectedItem();
        if (serverName == null) {
            JOptionPane.showMessageDialog(this, "Сервер не выбран.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        File installDir = getInstallDirForServer(serverName);
        File clientJar = new File(installDir, "client.jar");
        if (!clientJar.exists()) {
            mainBottomPanel.playButton.setEnabled(false);
            installGameWithProgress();
        } else {
            ServerConfig cfg = getServerConfigByName(serverName);
            if (cfg != null) {
                settingsPanel.applyModSelection(cfg);
                runGame(installDir, cfg, mainBottomPanel.nicknameField.getText().trim());
            }
        }
    }

    // Метод скачивает client.jar по URL из конфигурации сервера
    public void installGameWithProgress() {
        String serverName = (String) mainBottomPanel.serverComboBox.getSelectedItem();
        if (serverName == null) {
            JOptionPane.showMessageDialog(this, "Сервер не выбран.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        ServerConfig sc = getServerConfigByName(serverName);
        if (sc == null) {
            JOptionPane.showMessageDialog(this, "Конфигурация сервера не найдена.", "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        File installDir = getInstallDirForServer(serverName);
        if (!installDir.exists()) {
            installDir.mkdirs();
        }
        File clientJar = new File(installDir, "client.jar");

        JDialog dlg = new JDialog(this, "Установка...", true);
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setStringPainted(true);
        dlg.add(bar);
        dlg.setSize(300, 100);
        dlg.setLocationRelativeTo(this);

        new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() throws Exception {
                URL url = new URL(sc.download_link);
                URLConnection connection = url.openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                int contentLength = connection.getContentLength();
                try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
                        FileOutputStream fos = new FileOutputStream(clientJar)) {
                    byte[] dataBuffer = new byte[1024];
                    int bytesRead;
                    long totalRead = 0;
                    while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                        fos.write(dataBuffer, 0, bytesRead);
                        totalRead += bytesRead;
                        if (contentLength > 0) {
                            int progress = (int) (totalRead * 100 / contentLength);
                            publish(progress);
                        }
                    }
                }
                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {
                int progress = chunks.get(chunks.size() - 1);
                bar.setValue(progress);
            }

            @Override
            protected void done() {
                dlg.dispose();
                mainBottomPanel.playButton.setEnabled(true);
                mainBottomPanel.updatePlayButtonText();
            }
        }.execute();
        dlg.setVisible(true);
    }

    private void runGame(File installDir, ServerConfig cfg, String nickname) {
        try {
            mainBottomPanel.playButton.setEnabled(false);
            File clientJar = new File(installDir, "client.jar");
            if (!clientJar.exists()) {
                JOptionPane.showMessageDialog(this, "Не найден client.jar!", "Ошибка", JOptionPane.ERROR_MESSAGE);
                mainBottomPanel.playButton.setEnabled(true);
                return;
            }
            String xmx = "-Xmx" + settings.getProperty("ram", "2") + "G";
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
            ;
            String mainClass;
            if (cfg.fabric_version != null && !cfg.fabric_version.trim().isEmpty()) {
                mainClass = "net.fabricmc.loader.impl.launch.knot.KnotClient";
            } else if (cfg.forge_version != null && !cfg.forge_version.trim().isEmpty()) {
                mainClass = "net.minecraft.client.main.Main";
            } else {
                throw new IllegalArgumentException("Неизвестный загрузчик для сервера.");
            }
            ProcessBuilder pb = new ProcessBuilder(
                    "java", xmx, "-Djava.library.path=native", "-cp", baseClasspath,
                    mainClass, "--accessToken", "dummy", "--uuid", "dummy-uuid",
                    "--clientId", "dummy-clientid", "--xuid", "dummy-xuid",
                    "--version", cfg.minecraft_version, "--gameDir", installDir.getAbsolutePath(),
                    "--assetsDir", new File("assets").getAbsolutePath(), "--assetIndex", "19",
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
                    mainBottomPanel.playButton.setEnabled(true);
                    setVisible(true);
                });
            }).start();
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Ошибка запуска: " + ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
            mainBottomPanel.playButton.setEnabled(true);
        }
    }

    private File getInstallDirForServer(String serverName) {
        File dir = new File("version", serverName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
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
                    }
                    for (ServerConfig sc : serverConfigs) {
                        mainBottomPanel.serverComboBox.addItem(sc.name);
                    }
                    mainBottomPanel.updatePlayButtonText();
                    mainBottomPanel.serverComboBox.addActionListener(e -> {
                        String serverName = (String) mainBottomPanel.serverComboBox.getSelectedItem();
                        if (serverName != null) {
                            settingsPanel.updateModPanel(serverName);
                            mainBottomPanel.updatePlayButtonText();
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private void loadSettings() {
        File file = new File(SETTINGS_FILE_NAME);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                settings.load(fis);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            settings.setProperty("nickname", "Player");
            settings.setProperty("ram", "2");
            settings.setProperty("hideLauncher", "true");
            settings.setProperty("lastVersion", "");
            saveSettings();
        }
    }

    private void saveSettings() {
        File file = new File(SETTINGS_FILE_NAME);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            settings.store(fos, "Launcher Settings");
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void switchToMain() {
        CardLayout cl = (CardLayout) contentPanel.getLayout();
        cl.show(contentPanel, MAIN_PANEL);
        titleBarPanel.updateBackButton(false, null);
        titleBarPanel.setPanelAlpha(0.5f);
        mainBottomPanel.setPanelAlpha(0.5f);
    }

    public void switchToSettings() {
        CardLayout cl = (CardLayout) contentPanel.getLayout();
        cl.show(contentPanel, SETTINGS_PANEL);
        titleBarPanel.updateBackButton(true, () -> switchToMain());
        titleBarPanel.setPanelAlpha(0.5f);
        mainBottomPanel.setPanelAlpha(0.5f);
    }

    public JPanel getCardPanel() {
        return contentPanel;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LauncherUI launcher = new LauncherUI();
            launcher.setVisible(true);
        });
    }
}
