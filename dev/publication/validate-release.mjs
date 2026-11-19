import {readFileSync, statSync} from 'node:fs';
import {createHash} from 'node:crypto';
import {resolve} from 'node:path';
export function validateRelease(release, root) {
  const fail = message => {throw new Error('Homeboard release: '+message);};
  if (release.channel !== 'early-release' || release.packageId !== 'net.biohack.homeboard') fail('invalid channel/package');
  if (!Number.isInteger(release.versionCode) || release.versionCode < 1 || !/^\d+\.\d+\.\d+$/.test(release.versionName) || Number(release.versionName.split('.').at(-1)) !== release.versionCode) fail('invalid version');
  for (const field of ['sha256','certificateSha256','sourceArchiveSha256']) if (!/^[a-f0-9]{64}$/.test(release[field])) fail('invalid '+field);
  if (!/^[a-f0-9]{40}$/.test(release.sourceCommit)) fail('invalid source commit');
  if (!/^\d{4}-\d{2}-\d{2}$/.test(release.publishedDate) || !Number.isFinite(Date.parse(release.publishedDate))) fail('invalid date');
  if (release.minSdk !== 26 || !Array.isArray(release.notes) || !release.notes.length || release.notes.some(n=>typeof n !== 'string'||!n.trim()||/placeholder|pending/i.test(n))) fail('missing release details');
  const prefix = `/homeboard/releases/${release.versionName}/`;
  for (const [field,hashField] of [['downloadUrl','sha256'],['sourceArchiveUrl','sourceArchiveSha256']]) {
    if (typeof release[field] !== 'string' || !release[field].startsWith(prefix) || !/^[\w./-]+$/.test(release[field]) || release[field].includes('..')) fail('invalid '+field);
    const file = resolve(root, 'public', release[field].slice(1));
    if (createHash('sha256').update(readFileSync(file)).digest('hex') !== release[hashField]) fail('hash mismatch: '+field);
    if (field === 'downloadUrl' && (statSync(file).size !== release.sizeBytes || release.sizeBytes > 25*1024*1024)) fail('size mismatch or Pages limit exceeded');
  }
  const sumFile = resolve(root, 'public', prefix.slice(1), 'SHA256SUMS');
  if (!readFileSync(sumFile,'utf8').includes(release.sha256+'  '+release.downloadUrl.split('/').at(-1))) fail('checksum file mismatch');
  return release;
}
