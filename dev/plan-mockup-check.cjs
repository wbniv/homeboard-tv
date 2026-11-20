// Validate and capture a plan's mockup bundle. Run in the repository's pinned browser container:
//   node dev/plan-mockup-check.cjs docs/plans/<plan-bundle> <name> [<name> ...]
// Each <name>.html must be self-contained, fit 1440x900 without overflow and load without errors;
// a same-basename PNG is written beside it. Captures are design mockups, not product evidence.
const fs = require('node:fs');
const path = require('node:path');
const {chromium} = require('/workspace/artifacts/phone-browser/node_modules/playwright');

const [bundle, ...names] = process.argv.slice(2);
if (!bundle || bundle === '-h' || bundle === '--help' || !names.length) {
  console.log('Usage: node dev/plan-mockup-check.cjs <bundle-directory> <name> [<name> ...]');
  process.exit(bundle === '-h' || bundle === '--help' ? 0 : 2);
}
const directory = path.resolve('/workspace', bundle);

(async () => {
  const browser = await chromium.launch();
  try {
    for (const name of names) {
      const file = path.join(directory, name + '.html');
      const source = fs.readFileSync(file, 'utf8');
      if (/<(?:img|script|iframe|link)\b[^>]*\b(?:src|href)=/i.test(source) || /url\(/i.test(source))
        throw new Error(name + ': mockup must have inline resources');
      const page = await browser.newPage({viewport: {width: 1440, height: 900}, deviceScaleFactor: 1});
      const errors = [];
      page.on('pageerror', error => errors.push(error.message));
      page.on('request', request => {
        if (!request.url().startsWith('file:') && !request.url().startsWith('data:'))
          errors.push('External resource: ' + request.url());
      });
      await page.goto('file://' + file);
      await page.evaluate(() => document.fonts.ready);
      const overflow = await page.evaluate(() => [...document.querySelectorAll('main *')]
        .filter(element => {
          const r = element.getBoundingClientRect();
          return r.width && r.height && (r.right > 1441 || r.bottom > 901 || r.left < 0 || r.top < 0
            || element.scrollWidth > element.clientWidth + 2 || element.scrollHeight > element.clientHeight + 2
               && getComputedStyle(element).overflowY === 'hidden');
        }).map(element => element.tagName + '.' + element.className + ': ' + element.textContent.slice(0, 70)));
      if (errors.length || overflow.length) throw new Error(JSON.stringify({name, errors, overflow}, null, 1));
      // Every control must explain itself: clicking each button changes the feedback line or the page.
      const silent = await page.evaluate(() => [...document.querySelectorAll('button:not([disabled])')]
        .filter(button => !button.onclick).map(button => button.textContent.trim()));
      // Stepper buttons (−5% / +5%) are illustrative values, not navigation.
      const unexplained = silent.filter(label => !/^[−+]\d+%$/.test(label));
      if (unexplained.length) throw new Error(name + ': buttons without an explanation: ' + unexplained.join(', '));
      await page.screenshot({path: path.join(directory, name + '.png')});
      await page.close();
      console.log('PASS ' + name + ': 1440x900, inline assets, no overflow/errors, PNG captured');
    }
    let total = 0;
    for (const file of fs.readdirSync(directory)) {
      const bytes = fs.statSync(path.join(directory, file)).size;
      if (bytes > (file.endsWith('.png') ? 2 * 1024 * 1024 : 512 * 1024)) throw new Error(file + ': exceeds its size budget');
      total += bytes;
    }
    if (total > 8 * 1024 * 1024) throw new Error('Bundle exceeds 8 MiB');
    console.log('PASS bundle size: ' + total + ' bytes');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exit(1); });
