package com.launcher;

import org.update4j.Configuration;
import org.update4j.FileMetadata;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class AutoUpdater {

    private static final Logger LOGGER = Logger.getLogger(AutoUpdater.class.getName());
    private static final String CONFIG_URL = "https://raw.githubusercontent.com/qpov/McLauncher/main/update4j-config.xml";

    static {
        try {
            File logsDir = new File("logs");
            if (!logsDir.exists()) {
                logsDir.mkdirs();
            }
            FileHandler fileHandler = new FileHandler("logs/autoupdater.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(Level.ALL);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Не удалось настроить логирование", e);
        }
    }

    public static void checkAndUpdate() {
        try (InputStream remoteConfigStream = new URL(CONFIG_URL).openStream()) {
            String updatedConfigXml = updatePathsInConfig(remoteConfigStream);

            Configuration config = Configuration.read(new StringReader(updatedConfigXml));

            boolean requiresUpdate = false;
            List<FileMetadata> files = config.getFiles();
            for (FileMetadata file : files) {
                if (file.requiresUpdate()) {
                    requiresUpdate = true;
                    LOGGER.info("Файл требует обновления: " + file.getPath());
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
            LOGGER.log(Level.SEVERE, "Ошибка проверки обновлений", e);
        }
    }

    private static String updatePathsInConfig(InputStream configStream) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(configStream);

        String userDir = System.getProperty("user.dir").replace("\\", "/");

        NodeList fileNodes = doc.getElementsByTagName("file");
        for (int i = 0; i < fileNodes.getLength(); i++) {
            Element fileElem = (Element) fileNodes.item(i);
            String relPath = fileElem.getAttribute("path");
            if (relPath == null || relPath.trim().isEmpty()) continue;

            File absFile = new File(userDir, relPathUnix(relPath));
            String absolutePath = absFile.getAbsolutePath().replace("\\", "/");
            fileElem.setAttribute("path", absolutePath);
            // ВАЖНО: SHA-1 не трогаем!
        }

        Element root = doc.getDocumentElement();
        root.setAttribute("base", userDir);

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }

    private static String relPathUnix(String path) {
        return path.replace("\\", "/");
    }

    public static void main(String[] args) {
        checkAndUpdate();
    }
}