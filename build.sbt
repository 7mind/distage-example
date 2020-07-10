val V = new {
  val distage         = "0.10.16"
  val logstage        = distage
  val scalatest       = "3.1.2"
  val scalacheck      = "1.14.3"
  val http4s          = "0.21.6"
  val doobie          = "0.9.0"
  val zio             = "1.0.0-RC21-2"
  val zioCats         = "2.1.3.0-RC16"
  val kindProjector   = "0.11.0"
  val circeDerivation = "0.13.0-M4"
}

val Deps = new {
  val scalatest  = "org.scalatest" %% "scalatest" % V.scalatest
  val scalacheck = "org.scalacheck" %% "scalacheck" % V.scalacheck

  val distageCore    = "io.7mind.izumi" %% "distage-core" % V.distage
  val distageConfig  = "io.7mind.izumi" %% "distage-extension-config" % V.distage
  val distageRoles   = "io.7mind.izumi" %% "distage-framework" % V.distage
  val distageDocker  = "io.7mind.izumi" %% "distage-framework-docker" % V.distage
  val distageTestkit = "io.7mind.izumi" %% "distage-testkit-scalatest" % V.distage
  val logstageSlf4j  = "io.7mind.izumi" %% "logstage-adapter-slf4j" % V.logstage

  val http4sDsl    = "org.http4s" %% "http4s-dsl" % V.http4s
  val http4sServer = "org.http4s" %% "http4s-blaze-server" % V.http4s
  val http4sClient = "org.http4s" %% "http4s-blaze-client" % V.http4s
  val http4sCirce  = "org.http4s" %% "http4s-circe" % V.http4s

  val circeDerivation = "io.circe" %% "circe-derivation" % V.circeDerivation

  val doobie         = "org.tpolecat" %% "doobie-core" % V.doobie
  val doobiePostgres = "org.tpolecat" %% "doobie-postgres" % V.doobie
  val doobieHikari   = "org.tpolecat" %% "doobie-hikari" % V.doobie

  val kindProjector = "org.typelevel" % "kind-projector" % V.kindProjector cross CrossVersion.full

  val zio     = "dev.zio" %% "zio" % V.zio
  val zioCats = "dev.zio" %% "zio-interop-cats" % V.zioCats
}

inThisBuild(
  Seq(
    scalaVersion := "2.13.3",
    version      := "1.0.0-SNAPSHOT",
    organization := "io.7mind",
  )
)

lazy val leaderboard = project
  .in(file("."))
  .settings(
    name := "leaderboard",
    scalacOptions --= Seq("-Werror", "-Xfatal-warnings"),
    libraryDependencies ++= Seq(
        Deps.distageCore,
        Deps.distageRoles,
        Deps.distageConfig,
        Deps.logstageSlf4j,
        Deps.distageDocker % Test,
        Deps.distageTestkit % Test,
        Deps.scalatest % Test,
        Deps.scalacheck % Test,
        Deps.http4sDsl,
        Deps.http4sServer,
        Deps.http4sClient % Test,
        Deps.http4sCirce,
        Deps.circeDerivation,
        Deps.doobie,
        Deps.doobiePostgres,
        Deps.doobieHikari,
        Deps.zio,
        Deps.zioCats,
      ),
    addCompilerPlugin(Deps.kindProjector),
  )
