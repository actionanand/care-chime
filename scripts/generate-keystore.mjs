#!/usr/bin/env node
import { execFileSync } from 'node:child_process';
import { existsSync, mkdtempSync, readFileSync, rmSync, writeFileSync, chmodSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import readline from 'node:readline/promises';

const outputFile = 'release-keystore.jks';
const encodedFile = 'keystore.b64.txt';
const alias = 'carechime';
if (existsSync(outputFile) || existsSync(encodedFile)) {
  throw new Error('Signing material already exists; refusing to overwrite it.');
}
async function getPassword() {
  if (process.env.KEYSTORE_PASSWORD) return process.env.KEYSTORE_PASSWORD;
  if (!process.stdin.isTTY) throw new Error('Set KEYSTORE_PASSWORD in a private environment for noninteractive use.');
  const rl = readline.createInterface({ input: process.stdin, output: process.stdout });
  const pass = await rl.question('Keystore password (input may echo; prefer KEYSTORE_PASSWORD environment variable): ');
  rl.close();
  return pass;
}
let temp;
try {
  const password = await getPassword();
  if (password.length < 12) throw new Error('Choose a long keystore password (at least 12 characters).');
  temp = mkdtempSync(join(tmpdir(), 'carechime-keystore-'));
  const key = join(temp, 'key.pem');
  const cert = join(temp, 'cert.pem');
  execFileSync('openssl', ['req', '-x509', '-newkey', 'rsa:3072', '-nodes', '-keyout', key, '-out', cert,
    '-days', '36500', '-subj', '/CN=Care Chime/OU=Mobile/O=Care Chime/C=IN'], {stdio: 'pipe'});
  execFileSync('openssl', ['pkcs12', '-export', '-inkey', key, '-in', cert,
    '-out', outputFile, '-name', alias, '-passout', 'env:KEYSTORE_PASSWORD'],
    { stdio: 'pipe', env: {...process.env, KEYSTORE_PASSWORD: password} });
  chmodSync(outputFile, 0o600);
  writeFileSync(encodedFile, readFileSync(outputFile).toString('base64'), {mode: 0o600});
  console.log(`Created ${outputFile} and ${encodedFile}; alias=${alias}; type=PKCS12.`);
  console.log('Securely back up BOTH the keystore and password offline. Never commit either file.');
} catch (error) {
  rmSync(outputFile, {force: true});
  rmSync(encodedFile, {force: true});
  console.error(error instanceof Error ? error.message : String(error));
  process.exitCode = 1;
} finally {
  if (temp) rmSync(temp, {recursive: true, force: true});
}
