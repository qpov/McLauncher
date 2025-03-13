package com.launcher;

import org.update4j.Configuration;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.File;
import java.net.URL;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

public class AutoUpdater {
    private static final Logger LOGGER = Logger.getLogger(AutoUpdater.class.getName());
    // Абсолютный URL к файлу update4j-config.xml (с raw‑контентом)
    private static final String CONFIG_URL = "https://raw.githubusercontent.com/qpov/McLauncher/main/update4j-config.xml";

    static {
        try {
            // Создаем папку logs, если она не существует
            File logsDir = new File("logs");
            if (!logsDir.exists()) {
                logsDir.mkdirs();
            }
            // Создаем FileHandler для записи логов в файл logs/autoupdater.log в режиме добавления
            FileHandler fileHandler = new FileHandler("logs/autoupdater.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(Level.ALL);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Не удалось настроить логирование в файл", e);
        }
    }

    public static void checkAndUpdate() {
        try {
            LOGGER.info("Чтение конфигурации обновления из: " + CONFIG_URL);
            // Читаем конфигурацию через InputStreamReader
            Configuration config = Configuration.read(new InputStreamReader(new URL(CONFIG_URL).openStream()));
            
            if (config.requiresUpdate()) {
                LOGGER.info("Обновление найдено, начинаем обновление...");
                // Метод update() для версии update4j-1.5.9 возвращает boolean, 
                // который равен true, если требуется перезапуск
                boolean restartRequired = config.update();
                if (restartRequired) {
                    LOGGER.info("Обновление завершено, требуется перезапуск приложения.");
                    // Здесь можно реализовать перезапуск, например, завершить работу процесса
                    System.exit(0);
                } else {
                    LOGGER.info("Обновление завершено, перезапуск не требуется.");
                }
            } else {
                LOGGER.info("Обновление не требуется.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Исключение при проверке обновления", e);
        }
    }
}
