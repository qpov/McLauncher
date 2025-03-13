package com.launcher;

import org.update4j.Configuration;
import org.update4j.FileMetadata;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class AutoUpdater {

    private static final Logger LOGGER = Logger.getLogger(AutoUpdater.class.getName());
    private static final String CONFIG_URL = "https://raw.githubusercontent.com/qpov/McLauncher/main/update4j-config.xml";

    static {
        try {
            File logsDir = new File("logs");
            if (!logsDir.exists()) logsDir.mkdirs();
            FileHandler fh = new FileHandler("logs/autoupdater.log", true);
            fh.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fh);
            LOGGER.setLevel(Level.ALL);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Ошибка инициализации логирования", e);
        }
    }

    public static void checkAndUpdate() {
        try {
            // Устанавливаем системное свойство, чтобы пропустить проверку контрольных сумм
            System.setProperty("org.update4j.checksums.skip", "true");

            try (InputStream remoteConfigStream = new URL(CONFIG_URL).openStream()) {
                Configuration config = Configuration.read(new InputStreamReader(remoteConfigStream, StandardCharsets.UTF_8));

                boolean requiresUpdate = false;
                for (FileMetadata fm : config.getFiles()) {
                    if (fm.requiresUpdate()) {
                        requiresUpdate = true;
                        LOGGER.info("Файл требует обновления: " + fm.getPath() + " | SHA-1: " + fm.getChecksum());
                    }
                }

                if (requiresUpdate) {
                    LOGGER.info("Обновление найдено, начинаем обновление...");
                    boolean restartRequired = config.update();
                    if (restartRequired) {
                        LOGGER.info("Обновление завершено, требуется перезапуск приложения.");
                        System.exit(0);
                    } else {
                        LOGGER.info("Обновление завершено, перезапуск не требуется.");
                    }
                } else {
                    LOGGER.info("Обновление не требуется.");
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Ошибка проверки обновлений", e);
        }
    }

    public static void main(String[] args) {
        checkAndUpdate();
    }
}
