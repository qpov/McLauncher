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
    // URL к удалённой конфигурации update4j (raw‑контент)
    private static final String CONFIG_URL = "https://raw.githubusercontent.com/qpov/McLauncher/main/update4j-config.xml";

    static {
        try {
            // Создаем папку logs, если её нет
            File logsDir = new File("logs");
            if (!logsDir.exists()) {
                logsDir.mkdirs();
            }
            // Настраиваем логирование в файл
            FileHandler fileHandler = new FileHandler("logs/autoupdater.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(Level.ALL);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Не удалось настроить логирование", e);
        }
    }

    public static void checkAndUpdate() {
        try {
            LOGGER.info("Чтение конфигурации обновления из: " + CONFIG_URL);
            // 1) Скачиваем удалённую конфигурацию
            try (InputStream remoteConfigStream = new URL(CONFIG_URL).openStream()) {

                // 2) Преобразуем XML: заменяем относительные пути на абсолютные, используя user.dir
                String updatedConfigXml = updatePathsInConfig(remoteConfigStream);

                // 3) Читаем конфигурацию из обновлённого XML
                Configuration config = Configuration.read(
                        new InputStreamReader(
                                new ByteArrayInputStream(updatedConfigXml.getBytes(StandardCharsets.UTF_8)),
                                StandardCharsets.UTF_8));

                // 4) Проверяем, какие файлы нуждаются в обновлении, и логируем
                List<FileMetadata> allFiles = config.getFiles();
                boolean anyRequiresUpdate = false;
                for (FileMetadata fm : allFiles) {
                    if (fm.requiresUpdate()) {
                        anyRequiresUpdate = true;
                        LOGGER.info("Файл требует обновления: " + fm.getPath()
                                + " | Ожидаемый SHA-1: " + fm.getChecksum());
                    }
                }

                if (!anyRequiresUpdate) {
                    LOGGER.info("Обновление не требуется.");
                    return;
                }

                // 5) Вызываем update() в блоке try/catch, чтобы залогировать, если возникнут ошибки
                LOGGER.info("Обновление найдено, начинаем обновление...");
                boolean restartRequired;
                try {
                    restartRequired = config.update();
                } catch (Exception ex) {
                    // Если при обновлении какого-то файла возникла ошибка — логируем и выходим
                    LOGGER.log(Level.SEVERE, "Ошибка при обновлении файлов", ex);
                    return;
                }

                // 6) Если обновление завершилось без исключений, логируем результат
                if (restartRequired) {
                    LOGGER.info("Обновление завершено, требуется перезапуск приложения.");
                    System.exit(0);
                } else {
                    LOGGER.info("Обновление завершено, перезапуск не требуется.");
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Исключение при проверке/выполнении обновления", e);
        }
    }

    /**
     * Метод преобразует конфигурационный XML, заменяя значение атрибута "path"
     * для каждого файла на абсолютный путь, используя рабочую директорию пользователя.
     */
    private static String updatePathsInConfig(InputStream configStream) throws Exception {
        // Создаем парсер XML
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(configStream);

        // Получаем рабочую директорию пользователя
        String userDir = System.getProperty("user.dir").replace("\\", "/");

        // Обходим все элементы <file>
        NodeList fileNodes = doc.getElementsByTagName("file");
        for (int i = 0; i < fileNodes.getLength(); i++) {
            Element fileElem = (Element) fileNodes.item(i);
            String relPath = fileElem.getAttribute("path");
            if (relPath == null || relPath.trim().isEmpty()) {
                continue;
            }
            // Если путь уже абсолютный – пропускаем
            if (new File(relPath).isAbsolute()) {
                continue;
            }
            // Формируем абсолютный путь, объединяя userDir и относительный путь
            File absFile = new File(userDir, relPath);
            String absolutePath = absFile.getAbsolutePath().replace("\\", "/");
            // Обновляем атрибут path
            fileElem.setAttribute("path", absolutePath);
        }

        // Обновляем атрибут base, если он есть
        Element root = doc.getDocumentElement();
        if (root.hasAttribute("base")) {
            root.setAttribute("base", userDir);
        }

        // Преобразуем обновленный XML обратно в строку
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }

    public static void main(String[] args) {
        checkAndUpdate();
        // Остальной код (или вызов LauncherUI.main(...)) можно разместить здесь
    }
}
