#!/usr/bin/env node
/**
 * Smoke-test the in-browser simulation bundle outside the browser.
 *
 * Loads the linked Scala.js bundle from the JVM project's
 * `resources/webapp/main.js` (produced by `sbt copySimJs`) and exercises the
 * same four endpoints the README's `curl` snippets call: submit a score, get
 * the ladder, set a profile, get the profile. Each call should return HTTP
 * 200; mismatches fail the run with a non-zero exit code.
 *
 * Run from the repo root:
 *
 *   sbt copySimJs
 *   node bifunctor-tagless/js/sim-smoke.js
 *
 * scala.js NoModule mode emits `let LeaderboardSim` at script top level. This
 * is intentionally script-scoped (so a browser <script src=...> exposes it as
 * an identifier in the script's lexical scope, not on `globalThis`); to thread
 * it out for assertions in Node we eval the bundle in a `vm` context and
 * re-export the binding via the trailing line.
 */
const fs = require('fs');
const path = require('path');
const vm = require('vm');

const BUNDLE = path.resolve(
  __dirname,
  '..',
  'jvm',
  'src',
  'main',
  'resources',
  'webapp',
  'main.js',
);

if (!fs.existsSync(BUNDLE)) {
  console.error(`Bundle not found at ${BUNDLE}. Run \`sbt copySimJs\` first.`);
  process.exit(1);
}

const bundle = fs.readFileSync(BUNDLE, 'utf8');
// Deliberately omit `process` from the context so this smoke test fails the
// same way a browser would if the bundle accidentally probes Node.js APIs at
// init. The Scala.js bundle must run without a `process` global.
const ctx = {
  setTimeout, setInterval, clearTimeout, clearInterval, console,
  queueMicrotask, Promise,
};
vm.createContext(ctx);
vm.runInContext(bundle + '\nthis.LeaderboardSim = LeaderboardSim;', ctx);

const Sim = ctx.LeaderboardSim;
if (!Sim) {
  console.error('FAIL: LeaderboardSim binding not produced by main.js');
  process.exit(1);
}

const USER = '50753a00-5e2e-4a2f-94b0-e6721b0a3cc4';

function expect(label, response, expectedStatus, bodyPredicate) {
  if (response.status !== expectedStatus) {
    console.error(`FAIL: ${label} returned HTTP ${response.status}, expected ${expectedStatus}. body=${response.body}`);
    process.exit(1);
  }
  if (bodyPredicate && !bodyPredicate(response.body)) {
    console.error(`FAIL: ${label} body did not match expectation: ${response.body}`);
    process.exit(1);
  }
  console.log(`ok   ${label} -> ${response.status} ${response.body || ''}`);
}

async function main() {
  // Submit a score (empty body, score from URL).
  expect('POST /ladder/{user}/100', await Sim.call('POST', `/ladder/${USER}/100`, ''), 200);

  // Fetch the ladder — must contain our user with score 100.
  const ladder = await Sim.call('GET', '/ladder', '');
  expect(
    'GET  /ladder',
    ladder,
    200,
    body => {
      const rows = JSON.parse(body);
      return Array.isArray(rows) && rows.some(([u, s]) => u === USER && s === 100);
    },
  );

  // Set a profile.
  expect(
    'POST /profile/{user}',
    await Sim.call('POST', `/profile/${USER}`, JSON.stringify({ name: 'Kai', description: 'S C A L A' })),
    200,
  );

  // Fetch the profile — must include rank + score from the ladder.
  const profile = await Sim.call('GET', `/profile/${USER}`, '');
  expect(
    'GET  /profile/{user}',
    profile,
    200,
    body => {
      const p = JSON.parse(body);
      return p.name === 'Kai' && p.description === 'S C A L A' && p.rank === 1 && p.score === 100;
    },
  );

  console.log('\nsim-smoke: all assertions passed');
}

main().catch(e => {
  console.error('FAIL:', e);
  process.exit(1);
});
