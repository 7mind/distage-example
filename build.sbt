val V = new {
  val distage         = "0.9.5-M19"
  val scalatest       = "3.0.8"
  val scalacheck      = "1.14.0"
  val http4s          = "0.21.0-M4"
  val doobie          = "0.8.0-RC1"
  val zio             = "1.0.0-RC12-1"
  val zioCats         = "2.0.0.0-RC3"
  val kindProjector   = "0.10.3"
  val circeDerivation = "0.12.0-M7"
}

val Deps = new {
  val scalatest  = "org.scalatest" %% "scalatest" % V.scalatest
  val scalacheck = "org.scalacheck" %% "scalacheck" % V.scalacheck

  val distageCore    = "io.7mind.izumi" %% "distage-core" % V.distage
  val distageRoles   = "io.7mind.izumi" %% "distage-roles" % V.distage
  val distageStatic  = "io.7mind.izumi" %% "distage-static" % V.distage
  val distageConfig  = "io.7mind.izumi" %% "distage-config" % V.distage
  val distageTestkit = "io.7mind.izumi" %% "distage-testkit" % V.distage

  val http4sDsl    = "org.http4s" %% "http4s-dsl" % V.http4s
  val http4sServer = "org.http4s" %% "http4s-blaze-server" % V.http4s
  val http4sClient = "org.http4s" %% "http4s-blaze-client" % V.http4s
  val http4sCirce  = "org.http4s" %% "http4s-circe" % V.http4s

  val circeDerivation = "io.circe" %% "circe-derivation" % V.circeDerivation

  val doobie         = "org.tpolecat" %% "doobie-core" % V.doobie
  val doobiePostgres = "org.tpolecat" %% "doobie-postgres" % V.doobie
  val doobieHikari   = "org.tpolecat" %% "doobie-hikari" % V.doobie

  val kindProjector = "org.typelevel" %% "kind-projector" % V.kindProjector

  val zio     = "dev.zio" %% "zio" % V.zio
  val zioCats = "dev.zio" %% "zio-interop-cats" % V.zioCats
}

inThisBuild(
  Seq(
    scalaVersion := "2.13.0",
    version := "1.0.0-SNAPSHOT",
    organization := "io.7mind",
  )
)

lazy val livecode = project
  .in(file("."))
  .settings(
    name := "livecode",
    scalacOptions --= Seq("-Werror", "-Xfatal-warnings"),
    libraryDependencies ++= Seq(
      Deps.distageCore,
      Deps.distageRoles,
      Deps.distageStatic,
      Deps.distageConfig,
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
