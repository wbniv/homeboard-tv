const fs = require('node:fs');
const {chromium} = require('/workspace/artifacts/phone-browser/node_modules/playwright');
const QRCode = require('/site/node_modules/qrcode');
(async () => {
  const directory = '/workspace/docs/how-to/';
  fs.writeFileSync(directory + 'quick-start/download-qr.svg', await QRCode.toString('https://biohack.net/homeboard/', {
    type:'svg', errorCorrectionLevel:'M', margin:2, color:{dark:'#172d3c', light:'#ffffff'}
  }));
  const browser = await chromium.launch();
  try {
    const page = await browser.newPage();
    await page.goto('file://' + directory + 'quick-start-print.html');
    await page.evaluate(() => Promise.all([...document.images].map(img => img.decode())));
    await page.pdf({path:directory+'quick-start-print.pdf', format:'A4', printBackground:true, preferCSSPageSize:true});
    const pdf = fs.readFileSync(directory+'quick-start-print.pdf').toString('latin1');
    const count = (pdf.match(/\/Type\s*\/Page\b/g)||[]).length;
    if (count !== 2) throw new Error(`Expected two A4 pages; got ${count}`);
    console.log('Generated download QR and two-page A4 PDF.');
  } finally { await browser.close(); }
})().catch(e => {console.error(e); process.exit(1);});
