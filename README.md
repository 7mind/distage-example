[![Build Status](https://github.com/7mind/distage-example/workflows/Build/badge.svg)](https://github.com/7mind/distage-example/actions/workflows/build.yml)
[![Live demo](https://img.shields.io/badge/live%20demo-7mind.github.io%2Fdistage--example-blue?logo=github)](https://7mind.github.io/distage-example/)
[![License](https://img.shields.io/github/license/7mind/distage-example.svg)](https://github.com/7mind/distage-example/blob/develop/LICENSE)

# distage example

Example `distage` project.

Features [distage](https://izumi.7mind.io/distage/) from [Izumi project](https://izumi.7mind.io/) for dependency injection,
[BIO](https://izumi.7mind.io/bio/) typeclasses for bifunctor tagless final,
[distage-testkit](https://izumi.7mind.io/distage/distage-testkit) for testing,
[ZIO Environment](https://zio.dev) for composing test fixtures,
and [distage-framework-docker](https://izumi.7mind.io/distage/distage-framework-docker) for setting up test containers.

There are three variants of the example project:
- [bifunctor-tagless](bifunctor-tagless/src) – Main example. It's written in bifunctor tagless final style with [BIO](https://izumi.7mind.io/bio/) typeclasses, uses [ZIO](https://zio.dev) as a runtime and ZIO Environment for composing test fixtures.
- [monofunctor-tagless](monofunctor-tagless/src) – Written in monofunctor tagless final style with [Cats Effect](https://typelevel.org/cats-effect/) typeclasses, and can run using both [Cats IO](https://typelevel.org/cats-effect/) and [ZIO](https://zio.dev) runtimes.
- [monomorphic-cats](monomorphic-cats/src) – A simpler example written without tagless final, uses [Cats IO]() directly everywhere.

To launch tests that require postgres ensure you have a `docker` daemon running in the background.

Use `sbt test` to launch the tests.

You can launch the application with the following command.

```
# With docker daemon running
./launcher -u scene:managed :leaderboard

# Alternatively, with in-memory storage
./launcher -u repo:dummy :leaderboard
```

Afterwards you can call the HTTP methods:

```bash
curl -X POST http://localhost:8080/ladder/50753a00-5e2e-4a2f-94b0-e6721b0a3cc4/100
curl -X POST http://localhost:8080/profile/50753a00-5e2e-4a2f-94b0-e6721b0a3cc4 -d '{"name": "Kai", "description": "S C A L A"}'
# check leaderboard
curl -X GET http://localhost:8080/ladder
# user profile now shows the rank in the ladder along with profile data
curl -X GET http://localhost:8080/profile/50753a00-5e2e-4a2f-94b0-e6721b0a3cc4
```

### Reproducible toolchain via Nix (optional)

A `flake.nix` is included. With Nix flakes enabled you can drop into a shell
that has the exact JDK, sbt, Scala 3 and Node versions used by this project:

```bash
nix develop                  # one-off shell
direnv allow                 # automatic — uses the bundled .envrc
```

### Perfect simulation in the browser

<p align="center">
  <a href="https://7mind.github.io/distage-example/">
    <img src="https://img.shields.io/badge/%E2%96%B6%20Live%20demo-7mind.github.io%2Fdistage--example-blue?style=for-the-badge&logo=github&logoColor=white" alt="Live demo: 7mind.github.io/distage-example" />
  </a>
</p>

`distage-example` cross-builds to Scala.js, so the same
`LadderApi`/`ProfileApi` http4s routes also run entirely in the browser via
an in-process `LocalDispatcher` configured with `Repo -> Dummy` — no network,
no postgres, no docker. The demo UI in
`bifunctor-tagless/jvm/src/main/resources/webapp/` toggles each call between
**production** (real HTTP) and **simulation** (the in-page Scala.js build),
auto-selecting simulation when no production server answers.

Try the live deployment via the badge above (published to GitHub Pages on
every push to `develop`), or run it locally:

```bash
./launch-sim
```

Then open <http://localhost:8080/>. To enable Pages on your fork:
**Settings → Pages → Build and deployment → Source: GitHub Actions**.

#### Running the server and webpage separately

`./launch-sim` is just a wrapper. To run the steps yourself:

```bash
sbt copySimJs                            # build + copy the Scala.js bundle
./launcher -u repo:dummy :leaderboard    # start the server
```

The webpage can also be opened directly from disk via `file://` — the UI
auto-detects the backend at `http://localhost:8080`, and with no backend
reachable, falls back to the in-page simulation so the static page works on
its own.

#### Note

If `./launcher` command fails for you with some cryptic stack trace, there's most likely an issue with your Docker. First of all, check that you have `docker` and `contrainerd` daemons running. If you're using something else than Ubuntu, please stick to the relevant [installation page](https://docs.docker.com/engine/install/):
```
sudo systemctl status docker
sudo systemctl status contrainerd
```
Both of them should have `Active: active (running)` status. If your problem isn't gone yet, most likely you don't have your user in `docker` group. [Here](https://docs.docker.com/engine/install/) you can find a tutorial on how to do so. Don't forget to log out of your session or restart your virtual machine before proceeding. If you still have problems, don't hesitate to open an issue.

### Videos:

* Functional Scala 2019 – [Hyperpragmatic Pure FP testing with distage-testkit](https://www.youtube.com/watch?v=CzpvjkUukAs)
* ScalaWAW Warsaw Meetup – [Livecoding this project](https://www.youtube.com/watch?v=C0srg5T0E4o&t=4971)
* Source Talks — [Pragmatic Pure FP approach to application design and testing with distage](https://youtu.be/W60JO3TuFhc?t=1869)

#### GraalVM Native Image

Use `sbt` to build a native Linux binary with GraalVM NativeImage under Docker:

```bash
sbt bifunctor-taglessJVM/GraalVMNativeImage/packageBin
```

If you want to build the app using local `native-image` executable (e.g. on a Mac), comment out the `graalVMNativeImageGraalVersion` key in `build.sbt` first.

To test the native app with dummy repositories run:

```bash
./bifunctor-tagless/jvm/target/graalvm-native-image/bifunctor-tagless -u scene:managed -u repo:dummy :leaderboard
```

To test the native app with production repositories in Docker run:

```bash
./bifunctor-tagless/jvm/target/graalvm-native-image/bifunctor-tagless -u scene:managed -u repo:prod :leaderboard
```

Notes:

- Currently, the application builds with GraalVM `22.3`. Check other GraalVM images [here](https://github.com/graalvm/container/pkgs/container/graalvm-ce)
- JNA libraries are just regular Java resources, currently the NI config is generated for x86-64 Linux,
  you'll have to re-generate or manually edit it to run on different operating systems or architectures.
- The following bugs may still manifest, but it seems like they aren't blockers anymore:
    1. https://github.com/oracle/graal/issues/4797
    2. https://github.com/oracle/graal/issues/4282
- `-Djna.debug_load=true` key added to the native app command line might help to debug JNA-related issues

##### Assisted NI configuration generator

See Native Image [docs](https://www.graalvm.org/22.1/reference-manual/native-image/Agent/#assisted-configuration-of-native-image-builds) for details.

Add the following to Java commandline to run the Assisted configuration agent:

```
-agentlib:native-image-agent=access-filter-file=./ni-filter.json,config-output-dir=./src/main/resources/META-INF/native-image/auto-wip
```

Notes:
- The codepaths in `docker-java` are different for the cold state (when no containers are running) and the hot state. 
It seems like we've managed to build an exhaustive ruleset for `docker-java` so it's excluded in `ni-filter.json`.
If something is wrong and you need to generate the rules for `docker-java`, run the agent twice in both hot and cold state.
- Only `PluginConfig.const` works reliably under Native Image. So, ClassGraph analysis is disabled in `ni-filter.json`.
You can't make dynamic plugin resolution working under Native Image.
