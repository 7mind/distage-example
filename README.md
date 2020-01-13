# distage example

Example `distage` project presented at Functional Scala 2019

Features [distage](https://izumi.7mind.io/latest/release/doc/distage/),
[Bifunctor Tagless Final](https://github.com/7mind/izumi/blob/v0.10.0-M5/fundamentals/fundamentals-bio/src/main/scala/izumi/functional/bio/package.scala),
[ZIO Environment](https://zio.dev) for composing test fixtures and [distage-docker](https://github.com/7mind/distage-example/blob/leaderboard/src/test/scala/leaderboard/PostgresDockerModule.scala) for setting up test containers.

To launch tests that require postgres ensure you have a `docker` daemon running in the background.

### Videos:

* ScalaWAW Warsaw Meetup – [Livecoding this project](https://www.youtube.com/watch?v=C0srg5T0E4o&t=4971)

* Functional Scala 2019 – [Hyperpragmatic Pure FP testing with distage-testkit](https://www.youtube.com/watch?v=CzpvjkUukAs)
