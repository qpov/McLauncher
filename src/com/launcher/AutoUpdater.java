package com.launcher;

import org.update4j.Configuration;
import org.update4j.FileMetadata;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
        } catch (IOException e) {
            LOGGER.severe("Ошибка при настройке логирования: " + e);
        }
    }

    public static void checkAndUpdate() {
        try (InputStream remoteConfigStream = new URL(CONFIG_URL).openStream()) {
            Configuration config = Configuration.read(
                new InputStreamReader(remoteConfigStream, StandardCharsets.UTF_8));
    
            boolean requiresUpdate = false;
            for (FileMetadata fm : config.getFiles()) {
                if (fm.requiresUpdate()) {
                    requiresUpdate = true;
                    LOGGER.info("Файл требует обновления: " + fm.getPath() + 
                                " | Ожидаемый SHA-1: " + fm.getChecksum());
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
        } catch (Exception e) {
            LOGGER.severe("Ошибка проверки обновлений: " + e);
        }
    }    
}
