[![Build Status](https://github.com/7mind/distage-example/workflows/Build/badge.svg)](https://github.com/7mind/distage-example/actions/workflows/build.yml)
[![License](https://img.shields.io/github/license/7mind/distage-example.svg)](https://github.com/7mind/distage-example/blob/develop/LICENSE)

# distage example

Example `distage` project presented at Functional Scala 2019

Features [distage](https://izumi.7mind.io/distage/),
and [distage-docker](https://izumi.7mind.io/distage/distage-framework-docker) for setting up test containers.

- [distage-example-bifunctor-tf](distage-example-bifunctor-tf). Written in bifunctorial way with [Bifunctor Tagless Final](https://izumi.7mind.io/bio/), using [ZIO 2](https://zio.dev) as a runtime and ZIO Environment with distage-testkit for composing test fixtures.
- [distage-example-monofunctor-tf](distage-example-monofunctor-tf). Written in monofunctorial way with [Cats Core](https://typelevel.org/cats/), using [ZIO 2](https://zio.dev) as a runtime and ZIO Environment with distage-testkit for composing test fixtures.
- [distage-example-monomorphic-cats](distage-example-monomorphic-cats). Written in monomorphic way with [Cats Effect 3](https://typelevel.org/cats-effect/) as a runtime with distage-testkit for composing test fixtures.

To launch tests that require postgres ensure you have a `docker` daemon running in the background.

Use `sbt test` to launch the tests.

You can launch the application with the following command.

```
./launcher -u scene:managed :leaderboard
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
sbt GraalVMNativeImage/packageBin
```

If you want to build the app using local `native-image` executable (e.g. on a Mac), comment out the `graalVMNativeImageGraalVersion` key in `build.sbt` first.

To test the native app with dummy repositories run:

```bash
./target/graalvm-native-image/leaderboard -u scene:managed -u repo:dummy :leaderboard
```

To test the native app with production repositories in Docker run:

```bash
./target/graalvm-native-image/leaderboard -u scene:managed -u repo:prod :leaderboard
```


Notes:

- Currently, the application builds with GraalVM `22.3`. Check other GraalVM images [here](https://github.com/graalvm/container/pkgs/container/graalvm-ce)
- JNA libraries are just regular Java resources, currently the NI config is generated for x86-64 Linux,
  you'll have to re-generate or manually edit it to run on different operating systems or architectures.
- These bugs still may manifest, but it seems like they aren't blockers anymore:
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
