# distage example

Example project for Functional Scala 2019

Features [distage](https://izumi.7mind.io/latest/release/doc/distage/),
[Bifunctor Tagless Final](https://github.com/7mind/izumi/blob/v0.10.0-M5/fundamentals/fundamentals-bio/src/main/scala/izumi/functional/bio/package.scala),
[ZIO Environment](https://zio.dev) for composing test fixtures and [distage-docker](https://github.com/7mind/distage-example/blob/leaderboard/src/test/scala/leaderboard/PostgresDockerModule.scala) for setting up test containers.

To launch tests that require postgres you need to have `docker` daemon running in the background.
