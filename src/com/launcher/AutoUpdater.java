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
import java.security.MessageDigest;
import java.util.Formatter;
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
            File logsDir = new File("logs");
            if (!logsDir.exists()) {
                logsDir.mkdirs();
            }
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
            try (InputStream remoteConfigStream = new URL(CONFIG_URL).openStream()) {
                // Преобразуем XML: заменяем относительные пути на абсолютные и пересчитываем SHA-1 для локальных файлов
                String updatedConfigXml = updatePathsInConfig(remoteConfigStream);
                // Читаем конфигурацию из обновлённого XML
                Configuration config = Configuration.read(
                        new InputStreamReader(
                                new ByteArrayInputStream(updatedConfigXml.getBytes(StandardCharsets.UTF_8)),
                                StandardCharsets.UTF_8));

                // Логируем список файлов, требующих обновления
                List<FileMetadata> allFiles = config.getFiles();
                boolean anyRequiresUpdate = false;
                for (FileMetadata fm : allFiles) {
                    if (fm.requiresUpdate()) {
                        anyRequiresUpdate = true;
                        LOGGER.info("Файл требует обновления: " + fm.getPath() +
                                " | Ожидаемый SHA-1: " + fm.getChecksum());
                    }
                }
                if (!anyRequiresUpdate) {
                    LOGGER.info("Обновление не требуется.");
                    return;
                }
                LOGGER.info("Обновление найдено, начинаем обновление...");
                boolean restartRequired;
                try {
                    restartRequired = config.update();
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Ошибка при обновлении файлов", ex);
                    return;
                }
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
     * Преобразует конфигурационный XML, заменяя значение атрибута "path" для каждого файла
     * на абсолютный путь (на основе рабочей директории пользователя), а также пересчитывает SHA-1
     * для локальных файлов, если они существуют.
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
            File fileCandidate = new File(relPath);
            if (!fileCandidate.isAbsolute()) {
                // Формируем абсолютный путь, объединяя userDir и относительный путь
                File absFile = new File(userDir, relPath);
                String absolutePath = absFile.getAbsolutePath().replace("\\", "/");
                fileElem.setAttribute("path", absolutePath);

                // Если файл существует, пересчитываем SHA-1 и обновляем атрибут
                if (absFile.exists() && absFile.isFile()) {
                    String sha1 = calculateSHA1(absFile);
                    fileElem.setAttribute("sha1", sha1);
                }
            }
        }
        // Обновляем (или удаляем) атрибут base в корневом элементе, чтобы update4j использовал рабочую директорию
        Element root = doc.getDocumentElement();
        root.setAttribute("base", userDir);

        // Преобразуем XML обратно в строку
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }

    /**
     * Вычисляет SHA-1 контрольную сумму для заданного файла.
     */
    private static String calculateSHA1(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        try (InputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
        }
        return byteArray2Hex(md.digest());
    }

    private static String byteArray2Hex(final byte[] hash) {
        try (Formatter formatter = new Formatter()) {
            for (byte b : hash) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        }
    }

    public static void main(String[] args) {
        checkAndUpdate();
        // Запуск основного кода приложения, например:
        // LauncherUI.main(args);
    }
}
