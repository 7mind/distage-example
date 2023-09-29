val V = new {
  val distage       = "1.1.0-M20"
  val logstage      = distage
  val scalatest     = "3.2.17"
  val scalacheck    = "1.17.0"
  val http4s        = "0.23.15"
  val doobie        = "1.0.0-RC4"
  val catsCore      = "2.9.0"
  val zio           = "1.0.18"
  val zioCats       = "13.0.0.2"
  val kindProjector = "0.13.2"
  val circeGeneric  = "0.14.6"
  val graalMetadata = "0.9.27"
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

  val circeGeneric = "io.circe" %% "circe-generic" % V.circeGeneric

  val doobie         = "org.tpolecat" %% "doobie-core" % V.doobie
  val doobiePostgres = "org.tpolecat" %% "doobie-postgres" % V.doobie
  val doobieHikari   = "org.tpolecat" %% "doobie-hikari" % V.doobie

  val kindProjector = "org.typelevel" % "kind-projector" % V.kindProjector cross CrossVersion.full

  val zio     = "dev.zio" %% "zio" % V.zio
  val zioCats = "dev.zio" %% "zio-interop-cats" % V.zioCats

  val catsCore = "org.typelevel" %% "cats-core" % V.catsCore

  val graalMetadata = "org.graalvm.buildtools" % "graalvm-reachability-metadata" % V.graalMetadata
}

inThisBuild(
  Seq(
    scalaVersion := "2.13.12",
//    scalaVersion       := "3.3.1", // uncomment to use Scala 3 with IDE
    crossScalaVersions := Seq("3.3.1", "2.13.12"),
    version            := "1.0.0",
    organization       := "io.7mind",
  )
)

// that's just for quick experiments with distage snapshots
ThisBuild / resolvers ++= Resolver.sonatypeOssRepos("snapshots")

lazy val leaderboard = project
  .in(file("."))
  .settings(
    name := "leaderboard",
    libraryDependencies ++= Seq(
      Deps.distageCore,
      Deps.distageRoles,
      Deps.distageConfig,
      Deps.logstageSlf4j,
      Deps.distageDocker,
      Deps.distageTestkit % Test,
      Deps.scalatest % Test,
      Deps.scalacheck % Test,
      Deps.http4sDsl,
      Deps.http4sServer,
      Deps.http4sClient % Test,
      Deps.http4sCirce,
      Deps.circeGeneric,
      Deps.doobie,
      Deps.doobiePostgres,
      Deps.doobieHikari,
      Deps.zio,
      Deps.zioCats,
      Deps.catsCore,
      Deps.graalMetadata,
    ),
    libraryDependencies ++= {
      if (scalaVersion.value.startsWith("2")) {
        Seq(compilerPlugin(Deps.kindProjector))
      } else {
        Seq.empty
      }
    },
    scalacOptions -= "-Xfatal-warnings",
    scalacOptions -= "-Ykind-projector",
    scalacOptions ++= {
      if (scalaVersion.value.startsWith("2")) {
        Seq(
          "-Xsource:3",
          "-P:kind-projector:underscore-placeholders",
          "-Wmacros:after",
        )
      } else {
        Seq(
          "-source:3.2",
          "-Ykind-projector:underscores",
          "-Yretain-trees",
        )
      }
    },
    scalacOptions ++= Seq(
      s"-Xmacro-settings:product-name=${name.value}",
      s"-Xmacro-settings:product-version=${version.value}",
      s"-Xmacro-settings:product-group=${organization.value}",
      s"-Xmacro-settings:scala-version=${scalaVersion.value}",
      s"-Xmacro-settings:scala-versions=${crossScalaVersions.value.mkString(":")}",
      s"-Xmacro-settings:sbt-version=${sbtVersion.value}",
      s"-Xmacro-settings:git-repo-clean=${git.gitUncommittedChanges.value}",
      s"-Xmacro-settings:git-branch=${git.gitCurrentBranch.value}",
      s"-Xmacro-settings:git-described-version=${git.gitDescribedVersion.value.getOrElse("")}",
      s"-Xmacro-settings:git-head-commit=${git.gitHeadCommit.value.getOrElse("")}",
    ),
    GraalVMNativeImage / mainClass := Some("leaderboard.GenericLauncher"),
    graalVMNativeImageOptions ++= Seq(
      "--no-fallback",
      "-H:+ReportExceptionStackTraces",
      "--report-unsupported-elements-at-runtime",
      "--enable-https",
      "--enable-http",
      "-J-Xmx4G",
    ),
    graalVMNativeImageGraalVersion := Some("ol9-java17-22.3.1"),
    run / fork                     := true,
  )
  .enablePlugins(GraalVMNativeImagePlugin, UniversalPlugin)
