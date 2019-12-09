# distage Livecoding example

This is the final state of an example project livecoded at ScalaWAW Warsaw Meetup. Video: https://www.youtube.com/watch?v=C0srg5T0E4o&t=4971

Features [distage](https://izumi.7mind.io/latest/release/doc/distage/),
[Bifunctor Tagless Final](https://github.com/7mind/izumi/blob/v0.10.0-M5/fundamentals/fundamentals-bio/src/main/scala/izumi/functional/bio/package.scala),
[ZIO Environment](https://zio.dev) for composing test fixtures and [distage-docker](https://github.com/7mind/distage-livecode/blob/livecode/src/test/scala/livecode/PostgresDockerModule.scala) for setting up test containers.

To launch tests that require postgres you need to have `docker` daemon running in the background.
