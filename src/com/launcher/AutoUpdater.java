package com.launcher;

import org.update4j.Configuration;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Logger;
import java.util.logging.Level;

public class AutoUpdater {
    private static final Logger LOGGER = Logger.getLogger(AutoUpdater.class.getName());
    // Абсолютный URL к файлу update4j-config.xml (на GitHub должен быть raw‑контент)
    private static final String CONFIG_URL = "https://raw.githubusercontent.com/qpov/McLauncher/main/update4j-config.xml";

    public static void checkAndUpdate() {
        try {
            LOGGER.info("Чтение конфигурации обновления из: " + CONFIG_URL);
            // Читаем конфигурацию через InputStreamReader, чтобы избежать проблем с типами
            Configuration config = Configuration.read(new InputStreamReader(new URL(CONFIG_URL).openStream()));
            
            if (config.requiresUpdate()) {
                LOGGER.info("Обновление найдено, начинаем обновление...");
                // Метод update() для версии 1.5.9 возвращает boolean: true, если требуется перезапуск
                boolean restartRequired = config.update();
                if (restartRequired) {
                    LOGGER.info("Обновление завершено, требуется перезапуск приложения.");
                    // Здесь можно реализовать логику перезапуска приложения
                    // Например, завершить работу текущего процесса (и внешний лаунчер может перезапустить его)
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
