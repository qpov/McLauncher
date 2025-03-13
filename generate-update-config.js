// node generate-update-config.js . "https://raw.githubusercontent.com/qpov/McLauncher/main/" update4j-config.xml

const fs = require("fs");
const path = require("path");
const crypto = require("crypto");

// Корневая папка для сканирования (обычно корень проекта)
const rootDir = process.argv[2] || ".";
// Базовый URI для файлов (например, URL на GitHub)
const baseUri = process.argv[3] || "https://raw.githubusercontent.com/qpov/McLauncher/main/";
// Имя выходного XML-файла конфигурации
const outputFile = process.argv[4] || "update4j-config.xml";

// Файлы/папки, которые не нужно включать
const exclude = [".git", ".gitignore", "update4j-config.xml", "launcher.exe", "19.json"];

function walkDir(dir, fileList = []) {
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    for (const entry of entries) {
        if (exclude.includes(entry.name) || entry.name.startsWith(".")) continue;
        const fullPath = path.join(dir, entry.name);
        if (entry.isDirectory()) {
            walkDir(fullPath, fileList);
        } else {
            // Получаем относительный путь с использованием прямых слэшей
            const relPath = path.relative(rootDir, fullPath).split(path.sep).join("/");
            // Вычисляем SHA-1 хэш файла
            const fileBuffer = fs.readFileSync(fullPath);
            const hashSum = crypto.createHash("sha1");
            hashSum.update(fileBuffer);
            const hex = hashSum.digest("hex");
            const stats = fs.statSync(fullPath);
            fileList.push({ relPath, sha1: hex, size: stats.size });
        }
    }
    return fileList;
}

function generateConfig() {
    const fileList = walkDir(rootDir);
    // Вычисляем абсолютный путь к rootDir и преобразуем обратные слэши в прямые
    const baseDir = path.resolve(rootDir).split(path.sep).join("/");
    let xml = `<?xml version="1.0" encoding="UTF-8"?>\n`;
    xml += `<configuration base="${baseDir}">\n  <files>\n`;
    fileList.forEach(file => {
        xml += `    <file uri="${baseUri}${file.relPath}" path="${file.relPath}" sha1="${file.sha1}" size="${file.size}" />\n`;
    });
    xml += `  </files>\n</configuration>\n`;
    fs.writeFileSync(outputFile, xml);
    console.log(`Файл ${outputFile} сгенерирован успешно.`);
}

generateConfig();
