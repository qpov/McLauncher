const fs = require("fs");
const path = require("path");
const crypto = require("crypto");

const rootDir = process.argv[2] || ".";
const baseUri = process.argv[3] || "https://raw.githubusercontent.com/qpov/McLauncher/main/";
const outputFile = process.argv[4] || "update4j-config.xml";

const exclude = [".git", ".gitignore", outputFile, "generate-update-config.js"];

function walkDir(dir, fileList = []) {
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    entries.forEach(entry => {
        if (exclude.includes(entry.name) || entry.name.startsWith(".")) return;
        const fullPath = path.join(dir, entry.name);
        if (entry.isDirectory()) {
            walkDir(fullPath, fileList);
        } else {
            const relPath = path.relative(rootDir, fullPath).replace(/\\/g, "/");
            const fileBuffer = fs.readFileSync(fullPath);
            const sha1 = crypto.createHash("sha1").update(fileBuffer).digest("hex");
            const stats = fs.statSync(fullPath);
            fileList.push({ relPath, sha1: hex, size: stats.size });
        }
    });
    return fileList;
}

function generateConfig() {
    const fileList = walkDir(rootDir);
    let xml = `<?xml version="1.0" encoding="UTF-8"?>\n`;
    xml += `<configuration base="${path.resolve(rootDir).replace(/\\/g, "/")}">\n  <files>\n`;
    fileList.forEach(file => {
        xml += `    <file uri="${baseUri}${file.relPath}" path="${file.relPath}" sha1="${file.sha1}" size="${file.size}" />\n`;
    });
    xml += `  </files>\n</configuration>\n`;
    fs.writeFileSync("update4j-config.xml", xml);
    console.log(`Файл update4j-config.xml успешно создан.`);
}

generateConfig();
