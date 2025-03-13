package com.launcher;

import org.update4j.Configuration;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
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
            // Скачиваем удалённую конфигурацию
            InputStream remoteConfigStream = new URL(CONFIG_URL).openStream();
            // Преобразуем XML: заменяем относительные пути на абсолютные, используя рабочую директорию пользователя
            String updatedConfigXml = updatePathsInConfig(remoteConfigStream);
            // Читаем конфигурацию из обновлённого XML
            Configuration config = Configuration.read(
                    new InputStreamReader(
                            new ByteArrayInputStream(updatedConfigXml.getBytes("UTF-8")), "UTF-8"));

            if (config.requiresUpdate()) {
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
            LOGGER.log(Level.SEVERE, "Исключение при проверке обновления", e);
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
        // Здесь можно запускать остальной код приложения, например:
        // LauncherUI.launch();
    }
}
