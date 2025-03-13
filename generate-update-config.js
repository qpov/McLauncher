// node generate-update-config.js . "https://raw.githubusercontent.com/qpov/McLauncher/main/" update4j-config.xml

const fs = require("fs");
const path = require("path");
const crypto = require("crypto");

// Корневая папка, которую нужно сканировать (обычно это папка с вашим jar файлом и всем остальным)
const rootDir = process.argv[2] || "."; 
// Базовый URI, по которому будут доступны файлы обновления (на GitHub, например)
const baseUri = process.argv[3] || "https://raw.githubusercontent.com/qpov/McLauncher/main/";
// Выходной файл update4j-config.xml
const outputFile = process.argv[4] || "update4j-config.xml";

// Функция для рекурсивного обхода папок
function walkDir(dir, fileList = []) {
    const files = fs.readdirSync(dir);
    files.forEach(file => {
        // Пропускаем скрытые файлы и папки (начинающиеся с точки)
        if (file.startsWith(".")) return;
        const fullPath = path.join(dir, file);
        const stats = fs.statSync(fullPath);
        if (stats.isDirectory()) {
            walkDir(fullPath, fileList);
        } else {
            // Вычисляем относительный путь с учётом формата URL (используем прямые слэши)
            const relPath = path.relative(rootDir, fullPath).split(path.sep).join("/");
            // Вычисляем SHA1
            const fileBuffer = fs.readFileSync(fullPath);
            const hashSum = crypto.createHash("sha1");
            hashSum.update(fileBuffer);
            const hex = hashSum.digest("hex");
            fileList.push({ path: relPath, sha1: hex, size: stats.size });
        }
    });
    return fileList;
}

// Генерация XML-конфига
function generateConfig() {
    const fileList = walkDir(rootDir);
    let xml = `<?xml version="1.0" encoding="UTF-8"?>\n`;
    xml += `<configuration baseUri="${baseUri}">\n  <files>\n`;
    fileList.forEach(file => {
        xml += `    <file path="${file.path}" sha1="${file.sha1}" size="${file.size}" />\n`;
    });
    xml += `  </files>\n</configuration>\n`;
    fs.writeFileSync(outputFile, xml);
    console.log(`Файл ${outputFile} сгенерирован успешно.`);
}

generateConfig();
