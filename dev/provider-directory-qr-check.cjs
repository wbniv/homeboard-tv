/* Run inside the existing browser-test container against actual coordinator PNG evidence. */
const fs=require('fs'),assert=require('assert');
const {PNG}=require('/workspace/artifacts/phone-browser/node_modules/pngjs');
const jsQR=require('/workspace/artifacts/phone-browser/node_modules/jsqr');
const image=process.argv[2],expected='https://play.google.com/store/apps/details?id=com.danielkorgel.projectivy.plugin.cinemaglow';
const png=PNG.sync.read(fs.readFileSync(image)),result=jsQR(new Uint8ClampedArray(png.data),png.width,png.height);
assert(result,'Actual TV screenshot must contain a readable phone QR code');assert.equal(result.data,expected);
console.log(JSON.stringify({screenshot:image,decoded:result.data,status:'pass'}));
