// Run in the repository's pinned browser container; captures are design mockups.
const fs = require('node:fs');
const path = require('node:path');
const {chromium} = require('/workspace/artifacts/phone-browser/node_modules/playwright');
const directory = '/workspace/docs/plans/2026-11-19-android-app-widgets';
const names = ['index', 'picker', 'expanded', 'recovery', 'phone'];
(async () => {
  const browser = await chromium.launch();
  try {
    for (const name of names) {
      const file = path.join(directory, name + '.html');
      const source = fs.readFileSync(file, 'utf8');
      if (/<(?:img|script|iframe|link)\b[^>]*\b(?:src|href)=/i.test(source) || /url\(/i.test(source))
        throw new Error(name + ': mockup must have inline resources');
      const page = await browser.newPage({viewport: {width:1440, height:900}, deviceScaleFactor:1});
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
          return r.width && r.height && (r.right > 1441 || r.bottom > 901 || r.left < 0 || r.top < 0 || element.scrollWidth > element.clientWidth + 2);
        }).map(element => element.tagName + ': ' + element.textContent.slice(0,70)));
      if (errors.length || overflow.length) throw new Error(JSON.stringify({name,errors,overflow}));
      await page.screenshot({path:path.join(directory, name + '.png')});
      if (name === 'expanded') {
        await page.locator('#interact').click();
        if (await page.evaluate(() => document.activeElement.id) !== 'provider-row') throw new Error('Widget entry focus failed');
        await page.keyboard.press('Escape');
        if (await page.evaluate(() => document.activeElement.id) !== 'interact') throw new Error('Host escape focus failed');
      }
      await page.close();
      console.log('PASS ' + name + ': 1440x900, inline assets, no overflow/errors, PNG captured');
    }
    let total = 0;
    for (const file of fs.readdirSync(directory)) {
      const bytes = fs.statSync(path.join(directory,file)).size;
      if (bytes > 512 * 1024) throw new Error(file + ': exceeds 512 KiB');
      total += bytes;
    }
    if (total > 8 * 1024 * 1024) throw new Error('Bundle exceeds 8 MiB');
    console.log('PASS bundle size: ' + total + ' bytes');
  } finally { await browser.close(); }
})().catch(error => {console.error(error); process.exit(1);});
