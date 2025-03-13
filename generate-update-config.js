const fs = require("fs");
const path = require("path");
const crypto = require("crypto");

const rootDir = process.argv[2] || ".";
const baseUriRemote = process.argv[3] || "https://raw.githubusercontent.com/qpov/McLauncher/main/";
const outputFile = process.argv[4] || "update4j-config.xml";

const exclude = [".git", ".gitignore", outputFile, "generate-update-config.js"];

function walkDir(dir, fileList = []) {
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    for (const entry of entries) {
        if (exclude.includes(entry.name) || entry.name.startsWith(".")) continue;
        const fullPath = path.join(dir, entry.name);
        if (entry.isDirectory()) {
            walkDir(fullPath, fileList);
        } else {
            const relPath = path.relative(rootDir, fullPath).replace(/\\/g, "/");
            const fileBuffer = fs.readFileSync(fullPath);
            const sha1 = crypto.createHash("sha1").update(fileBuffer).digest("hex");
            const stats = fs.statSync(fullPath);
            fileList.push({ relPath, sha1, size: stats.size });
        }
    }
    return fileList;
}

function generateConfig() {
    const fileList = walkDir(rootDir);
    // Получаем абсолютный путь и преобразуем его в file URI (например, file:///A:/Projects/McLauncher)
    let baseDir = path.resolve(rootDir).replace(/\\/g, "/");
    if (!baseDir.startsWith("/")) {
       baseDir = "/" + baseDir;
    }
    const baseUriLocal = "file://" + baseDir;
    
    let xml = `<?xml version="1.0" encoding="UTF-8"?>\n`;
    xml += `<configuration base="${baseUriLocal}">\n  <files>\n`;
    fileList.forEach(file => {
        // Для каждого файла в path используем абсолютный URI, получая его как baseUriLocal + "/" + относительный путь
        xml += `    <file uri="${baseUriRemote}${file.relPath}" path="${baseUriLocal}/${file.relPath}" sha1="${file.sha1}" size="${file.size}" />\n`;
    });
    xml += `  </files>\n</configuration>\n`;
    fs.writeFileSync(outputFile, xml);
    console.log(`Файл ${outputFile} успешно создан.`);
}

generateConfig();
