package com.launcher;

import org.update4j.Configuration;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AutoUpdater {

    /**
     * Проверяет наличие обновлений и, если требуется, обновляет приложение.
     */
    public static void checkAndUpdate() {
        try {
            // URL на raw-версию вашего update4j-config.xml в GitHub (убедитесь, что ссылка корректна)
            String configUrl = "https://raw.githubusercontent.com/qpov/McLauncher/main/update4j-config.xml";
            // Читаем конфигурацию обновления с использованием UTF-8
            Configuration config = Configuration.read(
                    new InputStreamReader(new URL(configUrl).openStream(), StandardCharsets.UTF_8)
            );
            // Если обновление требуется – выполняем обновление и перезапускаем приложение
            if (config.requiresUpdate()) {
                System.out.println("Обнаружено обновление, начинаю обновление...");
                config.update(); // этот метод помечен как deprecated, но для версии 1.5.9 его можно использовать
                config.launch(); // запускает обновлённое приложение
                System.exit(0); // завершаем текущий процесс
            } else {
                System.out.println("Обновление не требуется.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
