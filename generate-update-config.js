// node generate-update-config.js . "https://raw.githubusercontent.com/qpov/McLauncher/main/" update4j-config.xml

const fs = require("fs");
const path = require("path");
const crypto = require("crypto");

// Корневая папка, которую будем сканировать (обычно корень проекта)
const rootDir = process.argv[2] || ".";
// Базовый URI, по которому будут доступны файлы (например, ваш GitHub URL)
const baseUri = process.argv[3] || "https://raw.githubusercontent.com/qpov/McLauncher/main/";
// Имя выходного XML-файла конфигурации
const outputFile = process.argv[4] || "update4j-config.xml";

// Список исключений (файлы/папки, которые не должны попадать в конфигурацию)
const exclude = [".git", ".gitignore", "update4j-config.xml", "launcher.exe"];

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
            fileList.push({ path: relPath, sha1: hex, size: stats.size });
        }
    }
    return fileList;
}

function generateConfig() {
    const fileList = walkDir(rootDir);
    let xml = `<?xml version="1.0" encoding="UTF-8"?>\n`;
    // Здесь задаем базовый каталог, относительно которого будут размещаться файлы.
    // update4j подставит рабочую директорию пользователя, когда запустится лаунчер.
    xml += `<configuration base="${process.cwd()}">\n  <files>\n`;
    fileList.forEach(file => {
        // Не выводим атрибут path – update4j возьмет путь из base и относительный путь файла из URI.
        xml += `    <file uri="${baseUri}${file.path}" sha1="${file.sha1}" size="${file.size}" />\n`;
    });
    xml += `  </files>\n</configuration>\n`;
    fs.writeFileSync(outputFile, xml);
    console.log(`Файл ${outputFile} сгенерирован успешно.`);
}

generateConfig();
