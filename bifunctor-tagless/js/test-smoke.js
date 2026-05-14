#!/usr/bin/env node
/**
 * Smoke-test the in-browser testkit bundle outside the browser.
 *
 * Loads the linked Scala.js bundle from the JVM project's
 * `resources/webapp/test-main.js` (produced by `sbt copySimJs`) and runs
 * the cross-compiled `LadderTestDummy`/`ProfilesTestDummy`/`RanksTestDummy`
 * suites via the `@JSExportTopLevel("LeaderboardTestRunner")` entrypoint.
 * Every event must be `kind === "passed"`; any other status or zero
 * events fails the run with a non-zero exit code.
 *
 * Run from the repo root:
 *
 *   sbt copySimJs
 *   node bifunctor-tagless/js/test-smoke.js
 *
 * The bundle requires three globals that a normal browser context
 * provides natively but a `vm.createContext` sandbox does not:
 *   - `scalajsCom`: the Scala.js test-framework bridge entrypoint;
 *     `Test/fullLinkJS` unconditionally pulls in `JSRPC` which calls
 *     `scalajsCom.init(...)` at module init. A no-op `init` is enough.
 *   - `process`: testkit startup paths read `process.env`. An empty
 *     `env` is enough.
 *   - `crypto`: `IzUUID`'s SecureRandom shim wants either
 *     `crypto.getRandomValues` (browsers) or Node's `crypto` module.
 *     We expose Node's `webcrypto` so `crypto.getRandomValues` resolves.
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
  'test-main.js',
);

if (!fs.existsSync(BUNDLE)) {
  console.error(`Bundle not found at ${BUNDLE}. Run \`sbt copySimJs\` first.`);
  process.exit(1);
}

const bundle = fs.readFileSync(BUNDLE, 'utf8');
const ctx = {
  setTimeout, setInterval, clearTimeout, clearInterval, console,
  queueMicrotask, Promise,
  scalajsCom: { init: () => {} },
  process: { env: {} },
  crypto: require('crypto').webcrypto,
};
vm.createContext(ctx);
vm.runInContext(bundle + '\nthis.LeaderboardTestRunner = LeaderboardTestRunner;', ctx);

const Runner = ctx.LeaderboardTestRunner;
if (!Runner) {
  console.error('FAIL: LeaderboardTestRunner binding not produced by test-main.js');
  process.exit(1);
}

Runner.run().then(events => {
  let passed = 0;
  let failed = 0;
  let other = 0;
  for (const ev of events) {
    const dur = (typeof ev.duration === 'number' && !isNaN(ev.duration))
      ? ` (${ev.duration}ms)`
      : '';
    const tag = ev.kind === 'passed' ? 'ok  '
              : ev.kind === 'failed' ? 'FAIL'
              :                         ev.kind.toUpperCase();
    console.log(`${tag} ${ev.suite} :: ${ev.test}${dur}${ev.message ? ' — ' + ev.message : ''}`);
    if (ev.kind === 'passed') passed++;
    else if (ev.kind === 'failed') failed++;
    else other++;
  }
  console.log(`\n${events.length} events: ${passed} passed, ${failed} failed, ${other} other`);
  if (passed === 0) {
    console.error('FAIL: no passed events recorded');
    process.exit(1);
  }
  process.exit(failed + other === 0 ? 0 : 1);
}).catch(e => {
  console.error('FAIL:', e);
  process.exit(1);
});
