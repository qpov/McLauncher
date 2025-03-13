// node generate-update-config.js

// generate-update-config.js
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

// Функция для рекурсивного обхода файлов в папке
function getFiles(dir, fileList = [], baseDir = dir) {
  const files = fs.readdirSync(dir);
  files.forEach(file => {
    const filePath = path.join(dir, file);
    const relativePath = path.relative(baseDir, filePath).replace(/\\/g, '/');
    const stat = fs.statSync(filePath);
    // Игнорируем папки, которые не нужны (например, .git, node_modules, update4j-config.xml)
    if (stat.isDirectory()) {
      if (file === '.git' || file === 'node_modules') return;
      getFiles(filePath, fileList, baseDir);
    } else {
      // Если нужно игнорировать файлы с расширением .log, раскомментируйте следующую строку:
      // if (file.endsWith('.log')) return;
      // Если вы не хотите включать сам update4j‑config.xml, то:
      if (file === 'update4j-config.xml') return;
      fileList.push({ relativePath, fullPath: filePath, size: stat.size });
    }
  });
  return fileList;
}

// Функция для вычисления SHA1-хэша файла
function sha1OfFile(filePath) {
  const hash = crypto.createHash('sha1');
  const fileBuffer = fs.readFileSync(filePath);
  hash.update(fileBuffer);
  return hash.digest('hex');
}

// Задайте корневую папку для сканирования (по умолчанию текущая папка)
const rootDir = process.argv[2] || '.';
const files = getFiles(rootDir);

// Генерируем XML
let xml = '<?xml version="1.0" encoding="UTF-8"?>\n';
xml += '<configuration>\n';
xml += '    <files>\n';
files.forEach(file => {
  const sha1 = sha1OfFile(file.fullPath);
  xml += `        <file path="${file.relativePath}" sha1="${sha1}" size="${file.size}" />\n`;
});
xml += '    </files>\n';
// Если нужно, можно добавить дополнительные настройки update4j, например mainClass или restartAfterUpdate
xml += '    <mainClass>com.launcher.LauncherUI</mainClass>\n';
xml += '    <restartAfterUpdate>true</restartAfterUpdate>\n';
xml += '</configuration>\n';

// Записываем в файл update4j-config.xml
fs.writeFileSync('update4j-config.xml', xml, 'utf-8');
console.log('Generated update4j-config.xml');
