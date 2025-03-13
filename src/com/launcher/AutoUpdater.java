package com.launcher;

import org.update4j.Configuration;
import java.io.FileReader;

public class AutoUpdater {

    public static void checkAndUpdate() {
        try {
            // Укажите правильный путь к вашему файлу конфигурации update4j
            Configuration config = Configuration.read(new FileReader("update4j-config.xml"));
            if (config.requiresUpdate()) {  // Используем needsUpdate() вместо hasUpdate()
                config.update();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
