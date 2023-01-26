# distage example

Example `distage` project presented at Functional Scala 2019

Features [distage](https://izumi.7mind.io/distage/),
[Bifunctor Tagless Final](https://izumi.7mind.io/bio/),
[ZIO Environment](https://zio.dev) for composing test fixtures,
and [distage-docker](https://izumi.7mind.io/distage/distage-framework-docker) for setting up test containers.

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

#### GraalVM Native Image

Use `sbt` to build a native Linux binary via GraalVM NativeImage:

```bash
sbt GraalVMNativeImage/packageBin
./target/graalvm-native-image/leaderboard -Djna.debug_load=true -u scene:managed -u repo:dummy :leaderboard
```

Despite some [bugs](https://github.com/oracle/graal/issues/4282) in NativeImage the applications seems to be completely functional 
when compiled by `ol8-java17-22.1.0`. Oddly the `ol8-java11-22.1.0` produces broken binaries.

Check other GraalVM images [here](https://github.com/graalvm/container/pkgs/container/graalvm-ce)

Currently, the application does not build with GraalVM `22.2` and `22.3`, this seems to be a [bug](https://github.com/oracle/graal/issues/4797).

JNA libraries are just regular Java resources, currently the NI config is generated for x86-64 Linux,
you'll have to re-generate or manually edit it to run on different operating systems or architectures.

##### Assisted NI configuration generator

See Native Image [docs](https://www.graalvm.org/22.1/reference-manual/native-image/Agent/#assisted-configuration-of-native-image-builds) for details.

```bash
-agentlib:native-image-agent=access-filter-file=./ni-filter.json,config-output-dir=./src/main/resources/META-INF/native-image/auto
```

#### Note

If `./launcher` command fails for you with some cryptic stack trace, there's most likely an issue with your Docker. First of all, check that you have `docker` and `contrainerd` daemons running. If you're using something else than Ubuntu, please stick to the relevant [installation page](https://docs.docker.com/engine/install/):
```
sudo systemctl status docker
sudo systemctl status contrainerd
```
Both of them should have `Active: active (running)` status. If your problem isn't gone yet, most likely you don't have your user in `docker` group. [Here](https://docs.docker.com/engine/install/) you can find a tutorial on how to do so. Don't forget to logout of your session or restart your virtual machine before proceeding. If you still have problems, don't hesitate to open an issue.

### Videos:

* Functional Scala 2019 – [Hyperpragmatic Pure FP testing with distage-testkit](https://www.youtube.com/watch?v=CzpvjkUukAs)
* ScalaWAW Warsaw Meetup – [Livecoding this project](https://www.youtube.com/watch?v=C0srg5T0E4o&t=4971)
