package com.launcher;

import org.update4j.Configuration;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.*;

public class AutoUpdater {

    private static final Logger logger = Logger.getLogger(AutoUpdater.class.getName());

    static {
        try {
            // Создаём FileHandler, который будет дописывать логи в файл update.log
            FileHandler fileHandler = new FileHandler("update.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.ALL);
        } catch (Exception e) {
            System.err.println("Не удалось инициализировать логгер: " + e.getMessage());
        }
    }

    /**
     * Проверяет наличие обновлений и, если требуется, обновляет приложение.
     */
    public static void checkAndUpdate() {
        try {
            // URL на raw-версию update4j-config.xml из вашего GitHub-репозитория
            String configUrl = "https://raw.githubusercontent.com/qpov/McLauncher/main/update4j-config.xml";
            logger.info("Чтение конфигурации обновления из: " + configUrl);
            // Читаем конфигурацию обновления с использованием UTF-8
            Configuration config = Configuration.read(
                    new InputStreamReader(new URL(configUrl).openStream(), StandardCharsets.UTF_8)
            );
            if (config.requiresUpdate()) {
                logger.info("Обновление требуется. Начинаем обновление...");
                config.update(); // Этот метод устарел, но для update4j 1.5.9 он работает
                logger.info("Обновление завершено. Запуск обновлённого приложения...");
                config.launch(); // Запускает обновлённое приложение
                logger.info("Приложение запущено. Завершаем текущий процесс.");
                System.exit(0);
            } else {
                logger.info("Обновление не требуется.");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Исключение при проверке обновления", e);
        }
    }
}
