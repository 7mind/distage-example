# Clarifications: scala.js "perfect simulation" webapp

**Context:** Extending distage-example with a small webapp that calls the same
endpoints as the curl commands in the README, with a runtime toggle between
"production" (real HTTP server) and "simulation" (the same backend code compiled
to scala.js and running in the browser with the `Dummy` axis selected).

The current code is not yet cross-buildable: `LadderApi`/`ProfileApi` directly
return `HttpRoutes[IO]`, and the plugins pull in http4s-ember, doobie, and
distage-framework-docker — none of which compile to scala.js. So the work
involves: (a) extracting a JS-portable "core" of each API behind a
serialized-input/serialized-output interface, (b) cross-building those modules
for JS, (c) adding `@JSExportTopLevel` entrypoints that build a distage object
graph with the dummy axis, and (d) shipping a tiny frontend with a backend
switch.

**How to answer:** Write your response on the `Answer:` line under each
question. Leave blank to skip. Reference questions by ID (e.g. `Q3`) in chat if
convenient.

---

## Q1: Which of the three variants should get the scala.js treatment?

**Context:** `bifunctor-tagless`, `monofunctor-tagless`, and `monomorphic-cats`
all expose the same endpoints. Cross-building all three roughly triples the
build.sbt surface, the plugin/role duplication, and the JS bundle work, but
they would all need to stay in sync if the example is meant to demonstrate that
"any distage app can be simulated."

**Suggestions:**
- **`monomorphic-cats` only** (recommended) — simplest stack (no BIO, no ZIO,
  no tagless), smallest delta, easiest to read as a demo. Other two variants
  stay JVM-only.
- **`bifunctor-tagless` only** — it's the "main" example per README, so
  showcasing simulation there has the most marketing value, but it pulls in
  zio + zio-interop-cats on JS which is heavier and adds a tagless layer to
  the simulation story.
- **All three** — uniform across variants, but a lot of duplicated build wiring
  for a demo project.

Answer: bifunctor-tagless only. zio etc work fine on js

---

## Q2: How should the project be split for cross-compilation?

**Context:** Today each variant is a single sbt project mixing JVM-only pieces
(http4s server, doobie, docker plugin, role main) with logic that could be
JS-portable (model classes, dummy repo impls, API logic). To cross-build, we
need to separate them.

**Suggestions:**
- **`<variant>-core` (cross JVM/JS) + `<variant>` (JVM, depends on core) +
  `<variant>-sim` (JS only, depends on core)** (recommended) — clearest
  layering: `core` holds model + repo traits + dummy impls + a "local API"
  layer; `<variant>` adds http4s/doobie/docker/role; `<variant>-sim` adds the
  `@JSExportTopLevel` adapter and scalajs-bundler config.
- **Convert `<variant>` itself to a cross project** — fewer modules, but the
  JVM side has to be reorganized so that all http4s/doobie code lives under a
  `jvm/` source tree; bigger diff to the existing layout.
- **Keep `<variant>` JVM-only, add a sibling `<variant>-sim` that
  *source-depends* on a curated subset of files** — minimal disturbance to
  the existing tree, but fragile (file lists drift) and harder to explain.

Answer: cross-projects with subfolders

---

## Q3: What should the "local call" boundary look like?

**Context:** The endpoints currently take typed parameters from the URL
(`UUIDVar`, `LongVar`) and JSON bodies, and they return JSON. For the
simulation, JS calls into the same logic, but the natural calling convention
across the JS/JVM boundary is `String => Future[String]` (or similar) so the
front-end doesn't have to know Scala types. The chosen shape determines what
gets exported to JS and what the http4s layer becomes.

**Suggestions:**
- **Method-style local API** (recommended) — define a trait like
  `LadderLocalApi { def getScores: IO[String]; def submitScore(userIdJson:
  String, scoreJson: String): IO[String] }` where each method corresponds to
  one endpoint; circe encodes/decodes inside. The http4s layer becomes a thin
  router on top, and the JS adapter exports the same methods. Pros: typed
  arguments stay typed inside Scala, JSON only at the wire boundary. Cons: one
  method per endpoint to maintain on both sides.
- **Single dispatch entrypoint** — one `def call(method: String, path: String,
  body: String): IO[Response]` that mimics an HTTP request/response. Pros: one
  export to JS, frontend code is identical for prod vs sim (just swap the
  transport). Cons: stringly-typed routing inside Scala — defeats some of the
  type safety the example is showcasing.
- **HTTP-shaped over a transport interface** — abstract over `Transport` where
  the prod impl uses fetch and the sim impl calls the in-browser app's
  router. Most idiomatic for "perfect simulation" but the heaviest change.

Answer: single dispatch entrypoint is more aligned to the purpose of the demo - to showcase how to build simulations sharing nearly all the code/approaches

---

## Q4: Which scala.js promise type should the JS export use?

**Context:** `IO[A]` doesn't cross the JS boundary; we have to expose a JS
`Promise` or `Thenable`. cats-effect's `IO#unsafeToPromise` exists on JS.

**Suggestions:**
- **`scala.scalajs.js.Promise[String]`** (recommended) — what JS code expects;
  `await`-able from plain JS or TS. Simplest possible interop.
- **`js.Thenable[String]`** — more permissive but rarely needed.
- **Synchronous `String` return** — only viable if every dummy impl is
  fully synchronous, which would constrain the design.

Answer: use scala.js promise

---

## Q5: What should the frontend be written in?

**Context:** The "small webapp" needs an input form per endpoint (or a free-
form curl-like UI), a result panel, and a toggle for prod/sim backend.

**Suggestions:**
- **Plain HTML + a few lines of vanilla JS** (recommended) — zero extra
  toolchain, served as static files; demonstrates the simulation idea without
  introducing a UI framework as a second axis of complexity. ~100 lines.
- **Scala.js + Laminar (or scalajs-dom only)** — keeps everything in Scala,
  but adds Laminar as a dependency and makes the frontend a third scala.js
  module.
- **TypeScript + small bundler (vite/esbuild)** — most realistic for a
  real-world setup but adds an npm toolchain to the repo just for the demo.

Answer: plain html + vanilla js - we want a minimalistic example

---

## Q6: How should the JS artifact be produced and served?

**Context:** Need to decide between sbt-scalajs `fastLinkJS`/`fullLinkJS`
output vs `scalajs-bundler`, and how the frontend loads it.

**Suggestions:**
- **`fullLinkJS` + ES module output, loaded directly by `index.html` via
  `<script type="module">`** (recommended) — no bundler, no npm, no
  webpack. The dummy backend uses no JS libraries that need npm packaging, so
  there's nothing to bundle.
- **scalajs-bundler with webpack** — necessary only if we pull in npm
  dependencies; overkill here.
- **Pre-built JS committed to the repo + sbt task to refresh it** — easier for
  someone just reading the README, but stale-artifact risk.

Answer: go the recommended way

---

## Q7: Where do the frontend assets live and how are they served?

**Context:** The webapp itself (HTML + JS bundle) needs a home. Two natural
options:

**Suggestions:**
- **Serve the static files from the same http4s server** (recommended) —
  add a `/` route that returns `index.html` and `/app.js` returning the
  linked JS. Then `./launcher :leaderboard` exposes the UI at
  `http://localhost:8080/` alongside the API. One process, no extra
  instructions in the README.
- **Static files in `./webapp/` opened directly from disk via `file://`** —
  simplest, but `file://` + CORS gotchas when "production" mode tries to call
  `http://localhost:8080`. Workable but rougher demo.
- **Separate tiny static server module** — overkill.

Answer: I think we should combine two options - we may serve the app through http4s AND let the user to open directly from file://

---

## Q8: How should the prod/sim toggle behave on the frontend?

**Context:** The user wants a switch between "real HTTP server" and "scala.js
simulation". The two modes need different request paths in the frontend code.

**Suggestions:**
- **Radio button / toggle in the UI; both modes available simultaneously**
  (recommended) — in "prod" mode the JS does `fetch('/ladder', ...)`; in
  "sim" mode it calls `window.LeaderboardSim.submitScore(...)`. The simulated
  backend is initialised once on page load. User can flip between them and
  see, for instance, that sim state doesn't propagate to prod and vice versa
  — which is itself a nice demonstration of "perfect simulation."
- **Build-time flag** — two separate HTML files, one per mode. Simpler code
  but you can't compare them side by side.
- **Two browser tabs, two servers** — defeats the purpose.

Answer: toggle in the UI

---

## Q9: Should the simulated backend's state persist across page reloads?

**Context:** Dummy repos are in-memory `Ref`s. By default they vanish on
reload.

**Suggestions:**
- **Vanish on reload** (recommended) — matches the "perfect simulation" =
  "fresh process" mental model; simpler.
- **Persist to `localStorage`** — nicer UX, but pulls a persistence concern
  into the dummy impls and dilutes the example.

Answer: vanish on reload is fine

---

## Q10: Naming convention for the new modules and JS export object?

**Context:** Need a name for the JS module (the cross-compiled "core") and
the JS-only module (the JS adapter/entrypoint).

**Suggestions:**
- **`monomorphic-cats-core` (cross) + `monomorphic-cats-sim` (JS only) +
  `monomorphic-cats` (JVM, unchanged name)**, with the JS export bound to
  `window.LeaderboardSim` (recommended).
- Something else — propose a naming scheme.

Answer: your naming is acceptable

---

## Q11: Anything to leave alone?

**Context:** Sanity check before I start moving files.

**Suggestions:**
- The GraalVM Native Image build configuration (`graalVMNativeImageOptions`,
  `ni-filter.json`, `graal-resources`) should remain functional for the
  chosen variant after the refactor.
- The other two variants not chosen in Q1 should be left entirely untouched.
- Existing tests in the chosen variant should keep passing without
  modification (only build-file changes acceptable).

Anything else? Constraints I should know about (e.g. scala 2 vs 3 only, no
new top-level dependencies, must keep working with sbt 1.x, etc.)?

Answer: nothing comes to my mind, you may limit this task to scala 3 only if it makes your job easier (most likely not)

---

## Q12: What is the exact shape of the dispatch result?

**Context:** You picked the single-dispatch entrypoint (Q3): a function
`call(method: String, path: String, body: String): IO[???]`. To make this work
uniformly for prod (http4s converts it to an `HttpResponse`) and sim (JS
adapter converts it to a `Promise[???]`), I need to fix the response type.
The current endpoints can also fail (malformed UUID, decoding error, not
found), so the result has to carry a status as well as a body.

**Suggestions:**
- **`LocalResponse(status: Int, body: String)`** (recommended) — minimal HTTP-
  ish ADT. http4s side maps status → `Status`, body is the JSON string (or
  empty for 204). JS side returns `js.Promise[js.Dynamic]` with
  `{status, body}` fields so vanilla JS can branch on status. Keeps the
  example small and uniformly HTTP-shaped.
- **`Either[DispatchError, String]`** — leaves status mapping to each adapter
  separately; less uniform but more "domain-typed."
- **Plain `String`, exceptions for errors** — simplest happy path but the
  error story leaks Scala stack traces across the JS boundary.

Answer: you may use Either. Actually, if I remember correctly, the routing part of http4s can be run on scala.js. If that's true - you could just reuse existing routing - less work to do, better for the reader!

---

## Q13: CORS policy for the http4s server (because of file:// support in Q7).

**Context:** You want the UI to also work when opened from `file://`. A page
loaded from `file://` has origin `null`, so the http4s server has to send
`Access-Control-Allow-Origin: null` (or `*`) for the `/ladder` and `/profile`
endpoints in "prod" mode. Without this, the prod toggle won't work for
file://-served pages.

**Suggestions:**
- **Permissive CORS on all API routes** (recommended for a demo) —
  `Access-Control-Allow-Origin: *` plus the necessary methods/headers, applied
  to API routes via http4s `CORS` middleware. Acceptable because this is an
  example app, not a production service. README will call this out.
- **Reflect the requesting origin** — slightly safer but adds complexity for
  no real gain in a demo.
- **No CORS, document that file:// only works with sim mode** — simpler code
  but breaks half the demo when served from disk.

Answer: you can add permissive cors headers

---

## Q14: How does the linked JS artifact get from `bifunctor-tagless-sim/target/...` to where the browser loads it?

**Context:** `fullLinkJS` produces `main.js` inside the sim project's target
dir. The http4s server needs to serve it as `/app.js`, and the file://
workflow needs it next to `index.html`. Three plumbing options:

**Suggestions:**
- **sbt task `copySimJsToResources` that runs after `fullLinkJS` and copies
  `main.js` (+ source map) into `bifunctor-tagless/src/main/resources/webapp/`,
  which is also the static-files dir the http4s server reads from**
  (recommended) — one location, one source of truth. README documents:
  `sbt bifunctor-tagless-sim/fullLinkJS bifunctor-tagless/run`. The copied
  artifact is gitignored.
- **http4s reads the JS directly out of the sim target dir at runtime** —
  works for dev but breaks the GraalVM native-image build and the file://
  workflow.
- **Commit the linked JS to the repo** — simplest for a casual reader, but
  drifts. (Rejected unless you prefer it.)

Answer: you may go the recommended way

---

## Q15: How "ceremonious" should the JS entrypoint be?

**Context:** On the JVM side, the app uses `RoleAppMain.LauncherCats[IO]` with
plugins, roles, activations — the full distage-framework stack. On JS, much
of that machinery isn't useful (no CLI, no config files, no role lifecycle).
We have two reasonable poles:

**Suggestions:**
- **Mini-entrypoint with plain `Injector`** (recommended) — the JS module
  builds an `Injector[IO]` with `Activation(Repo -> Dummy, Scene -> Provided)`
  from the shared "core" plugin (which excludes http4s/doobie/docker), grabs
  the dispatcher out of the object graph once, and exports its methods as
  `@JSExportTopLevel("LeaderboardSim")`. No roles, no config loader on JS.
  Smallest JS bundle, clearest demo of "any distage graph can run in a
  browser." Cost: a tiny bit of duplication between this and the JVM
  `MainBase`.
- **Full `RoleAppMain` on JS** — proves the same launcher code runs in the
  browser, but requires `distage-extension-config` and
  `distage-framework` to be JS-compatible. They might be (distage-core is),
  but config relies on typesafe-config which is JVM-only — so this likely
  forces ditching HOCON config on the JS side anyway. Bigger bundle, more
  refactoring.

Answer: recommended

---
