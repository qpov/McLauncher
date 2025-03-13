// node generate-update-config.js .

"use strict";

const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

// Корневая папка для сканирования; можно передать в качестве параметра, иначе текущая
const baseDir = process.argv[2] || '.';

// Функция для вычисления SHA1 хэша файла
function hashFile(filePath) {
    return new Promise((resolve, reject) => {
        const hash = crypto.createHash('sha1');
        const stream = fs.createReadStream(filePath);
        stream.on('error', reject);
        stream.on('data', (chunk) => hash.update(chunk));
        stream.on('end', () => resolve(hash.digest('hex')));
    });
}

// Функция для получения размера файла
function getFileSize(filePath) {
    return fs.statSync(filePath).size;
}

// Рекурсивная функция для сканирования папки
async function processDirectory(dir) {
    let files = [];
    const items = fs.readdirSync(dir);
    for (const item of items) {
        const fullPath = path.join(dir, item);
        const stats = fs.statSync(fullPath);
        if (stats.isDirectory()) {
            // Пропускаем папку .git и любые другие нежелательные папки
            if (item === '.git') continue;
            const subFiles = await processDirectory(fullPath);
            files = files.concat(subFiles);
        } else if (stats.isFile()) {
            // Пропускаем файлы с расширением .log и сам update4j-config.xml
            if (item.endsWith('.log') || item === 'update4j-config.xml') continue;
            // Вычисляем абсолютный путь и формируем URI с префиксом file:///
            const absolutePath = path.resolve(fullPath).replace(/\\/g, '/');
            const uri = 'file:///' + absolutePath;
            const sha1 = await hashFile(fullPath);
            const size = getFileSize(fullPath);
            // Можно сохранить и относительный путь (для удобства) – он будет использован как "path"
            const relativePath = path.relative(baseDir, fullPath).replace(/\\/g, '/');
            files.push({ path: relativePath, uri: uri, sha1: sha1, size: size });
        }
    }
    return files;
}

// Генерация XML файла конфигурации для update4j
async function generateUpdate4jConfig() {
    const files = await processDirectory(baseDir);
    let xml = '<?xml version="1.0" encoding="UTF-8"?>\n';
    xml += '<configuration>\n  <files>\n';
    for (const file of files) {
        xml += `    <file path="${file.path}" uri="${file.uri}" sha1="${file.sha1}" size="${file.size}" />\n`;
    }
    xml += '  </files>\n</configuration>\n';
    fs.writeFileSync('update4j-config.xml', xml, 'utf8');
    console.log('update4j-config.xml успешно сгенерирован.');
}

generateUpdate4jConfig().catch(err => {
    console.error(err);
    process.exit(1);
});
