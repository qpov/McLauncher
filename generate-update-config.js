// node generate-update-config.js

// generate-update-config.js
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

// Базовый URL – если вы хотите использовать GitHub для загрузки обновлений,
// имейте в виду, что для raw-версии файлов лучше использовать:
// "https://raw.githubusercontent.com/qpov/McLauncher/main"
// Здесь используем то, что вы указали:
const BASE_URL = 'https://raw.githubusercontent.com/qpov/McLauncher/main';

function computeSHA1(filePath) {
  const data = fs.readFileSync(filePath);
  return crypto.createHash('sha1').update(data).digest('hex');
}

function scanDirectory(dir, baseDir) {
  let fileList = [];
  const items = fs.readdirSync(dir);
  for (const item of items) {
    // Можно добавить фильтрацию – например, пропускать папку .git и сам файл update4j-config.xml
    if (item === '.git' || item === 'update4j-config.xml') continue;
    const fullPath = path.join(dir, item);
    const stat = fs.statSync(fullPath);
    if (stat.isDirectory()) {
      fileList = fileList.concat(scanDirectory(fullPath, baseDir));
    } else {
      // Получаем относительный путь с заменой обратных слэшей на прямые
      const relPath = path.relative(baseDir, fullPath).replace(/\\/g, '/');
      fileList.push({
        filePath: fullPath,
        relPath: relPath,
        size: stat.size,
        sha1: computeSHA1(fullPath)
      });
    }
  }
  return fileList;
}

function generateUpdateConfig() {
  // Корневая папка – предполагаем, что скрипт запускается из корня проекта
  const baseDir = process.cwd();
  const files = scanDirectory(baseDir, baseDir);
  
  // Начало XML-файла
  let xml = `<?xml version="1.0" encoding="UTF-8"?>\n<configuration>\n    <files>\n`;
  
  // Для каждого файла добавляем элемент <file>
  for (const file of files) {
    xml += `        <file path="${file.relPath}" uri="${BASE_URL}/${file.relPath}" sha1="${file.sha1}" size="${file.size}" />\n`;
  }
  
  // Закрываем теги
  xml += `    </files>\n</configuration>\n`;
  
  // Сохраняем в update4j-config.xml
  fs.writeFileSync('update4j-config.xml', xml, 'utf8');
  console.log('update4j-config.xml успешно создан.');
}

generateUpdateConfig();
