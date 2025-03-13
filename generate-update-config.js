const fs = require("fs");
const path = require("path");
const crypto = require("crypto");

const rootDir = process.argv[2] || ".";
const baseUri = process.argv[3] || "https://raw.githubusercontent.com/qpov/McLauncher/main/";
const outputFile = process.argv[4] || "update4j-config.xml";

const exclude = [".git", ".gitignore", "update4j-config.xml", "generate-update-config.js"];

function walkDir(dir, fileList = []) {
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    for (const entry of entries) {
        if (exclude.includes(entry.name) || entry.name.startsWith(".")) continue;
        const fullPath = path.join(dir, entry.name);
        if (entry.isDirectory()) {
            walkDir(fullPath, fileList);
        } else {
            const relPath = path.relative(rootDir, fullPath).split(path.sep).join("/");
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
    const baseDir = path.resolve(rootDir).replace(/\\/g, "/");
    let xml = `<?xml version="1.0" encoding="UTF-8"?>\n`;
    xml += `<configuration base="${baseDir}">\n  <files>\n`;
    fileList.forEach(file => {
        const absPath = `${baseDir}/${file.relPath}`.replace(/\\/g, "/");
        xml += `    <file uri="${baseUri}${file.relPath}" path="${absPath}" sha1="${file.sha1}" size="${file.size}" />\n`;
    });
    xml += `  </files>\n</configuration>\n`;
    fs.writeFileSync(outputFile, xml);
    console.log(`Файл ${outputFile} успешно сгенерирован.`);
}

generateConfig();
