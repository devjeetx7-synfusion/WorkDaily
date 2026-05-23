const fs = require('fs');
const path = require('path');

const srcPath = path.join(__dirname, 'src/contexts/LanguageContext.tsx');
let content = fs.readFileSync(srcPath, 'utf8');

// Find the commonKeys object
const startMatch = content.match(/const commonKeys = \{/);
if (!startMatch) {
  console.error("Could not find commonKeys object");
  process.exit(1);
}

const startIndex = startMatch.index;
const endIndex = content.indexOf('};\n\nconst langCodes', startIndex) + 1;

let commonKeysStr = content.substring(startIndex, endIndex).replace('const commonKeys = ', '');

// Execute it to get the object
let commonKeys;
try {
  commonKeys = eval('(' + commonKeysStr + ')');
} catch(e) {
  console.error("Error evaluating commonKeys", e);
  process.exit(1);
}

const langCodes = ["en", "hi", "bn", "pa", "mr", "ta", "te", "gu", "kn", "ml"];

const localesDir = path.join(__dirname, 'src/locales');
if (!fs.existsSync(localesDir)) {
  fs.mkdirSync(localesDir);
}

const locales = {};
langCodes.forEach(code => {
  locales[code] = {};
});

Object.keys(commonKeys).forEach(key => {
  const translations = commonKeys[key];
  langCodes.forEach((code, index) => {
    // Fallback to english if translation is missing
    locales[code][key] = translations[index] || translations[0];
  });
});

langCodes.forEach(code => {
  const outPath = path.join(localesDir, `${code}.json`);
  fs.writeFileSync(outPath, JSON.stringify(locales[code], null, 2));
  console.log(`Wrote ${outPath}`);
});

console.log("Done extracting locales.");
