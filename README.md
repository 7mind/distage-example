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
./launcher -u services:docker :leaderboard
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

### Videos:

* Functional Scala 2019 – [Hyperpragmatic Pure FP testing with distage-testkit](https://www.youtube.com/watch?v=CzpvjkUukAs)
* ScalaWAW Warsaw Meetup – [Livecoding this project](https://www.youtube.com/watch?v=C0srg5T0E4o&t=4971)
