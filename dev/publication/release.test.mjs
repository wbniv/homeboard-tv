import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {validateRelease} from '../scripts/homeboard/validate-release.mjs';
const release = JSON.parse(readFileSync(new URL('../src/data/homeboard-release.json', import.meta.url)));
test('public release metadata matches the actual downloadable bytes', () => {
  assert.equal(validateRelease(release, process.cwd()), release);
});
test('release gate rejects tampered bytes, debug identity and placeholder metadata', () => {
  for (const change of [{sha256:'0'.repeat(64)}, {sizeBytes:1}, {packageId:'net.biohack.homeboard.debug'},
    {certificateSha256:'pending'}, {notes:['placeholder']}, {versionCode:0}, {sourceArchiveUrl:'/../../secret'}]) {
    assert.throws(() => validateRelease({...release,...change}, process.cwd()));
  }
});
