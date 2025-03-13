// Эта команда просканирует текущую папку (замените точку нужным путем, если необходимо) и выведет в консоль строки
// node generate-update4j-config.js .

const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

// Функция для вычисления SHA-1 файла
function sha1File(filePath) {
    const hash = crypto.createHash('sha1');
    const data = fs.readFileSync(filePath);
    hash.update(data);
    return hash.digest('hex');
}

// Рекурсивная функция для обхода папки и сбора информации о файлах
function scanDirectory(dir, baseDir) {
    let files = [];
    const items = fs.readdirSync(dir);
    for (const item of items) {
        const fullPath = path.join(dir, item);
        const stats = fs.statSync(fullPath);
        if (stats.isDirectory()) {
            files = files.concat(scanDirectory(fullPath, baseDir));
        } else if (stats.isFile()) {
            // Получаем относительный путь с учётом стандартного разделителя "/"
            const relativePath = path.relative(baseDir, fullPath).replace(/\\/g, '/');
            files.push({ path: relativePath, size: stats.size, sha1: sha1File(fullPath) });
        }
    }
    return files;
}

// Указываем базовую директорию для сканирования (по умолчанию текущая)
const baseDir = process.argv[2] || '.';
const files = scanDirectory(baseDir, baseDir);

// Выводим XML-строки для каждого файла
files.forEach(file => {
    console.log(`<file path="${file.path}" hash="${file.sha1}" size="${file.size}" />`);
});
