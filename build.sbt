import scala.util.chaining.scalaUtilChainingOps

val V = new {
  val distage       = "1.2.16"
  val logstage      = distage
  val scalatest     = "3.2.19"
  val scalacheck    = "1.18.0"
  val http4s        = "0.23.29"
  val doobie        = "1.0.0-RC6"
  val catsCore      = "2.12.0"
  val zio           = "2.1.13"
  val zioCats       = "23.0.0.8"
  val kindProjector = "0.13.3"
  val circeGeneric  = "0.14.8"
  val graalMetadata = "0.10.3"
  val catsEffect    = "3.5.4"
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
  val http4sServer = "org.http4s" %% "http4s-ember-server" % V.http4s
  val http4sClient = "org.http4s" %% "http4s-ember-client" % V.http4s
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

  val CoreDeps = Seq(
    distageCore,
    distageRoles,
    distageConfig,
    logstageSlf4j,
    distageDocker,
    distageTestkit % Test,
    scalatest % Test,
    scalacheck % Test,
    http4sDsl,
    http4sServer,
    http4sClient % Test,
    http4sCirce,
    circeGeneric,
    doobie,
    doobiePostgres,
    doobieHikari,
    catsCore,
    graalMetadata,
  )
}

inThisBuild(
  Seq(
    crossScalaVersions := Seq("2.13.15", "3.3.4"),
//    crossScalaVersions := Seq("3.3.4", "2.13.15"), // uncomment to use Scala 3 in IDE
    scalaVersion := crossScalaVersions.value.head,
    version      := "1.0.0",
    organization := "io.7mind",
  )
)

lazy val `bifunctor-tagless` = project
  .in(file("bifunctor-tagless"))
  .pipe(sharedSettings(Seq(Deps.zio, Deps.zioCats)))

lazy val `monofunctor-tagless` = project
  .in(file("monofunctor-tagless"))
  .pipe(sharedSettings(Seq(Deps.zio, Deps.zioCats)))

lazy val `monomorphic-cats` = project
  .in(file("monomorphic-cats"))
  .pipe(sharedSettings(Seq()))

lazy val `graal-resources` = project
  .in(file("graal-resources"))
  .settings(Compile / resourceDirectory := baseDirectory.value)

lazy val `distage-example` = project
  .in(file("."))
  .aggregate(
    `bifunctor-tagless`,
    `monofunctor-tagless`,
    `monomorphic-cats`,
    `graal-resources`,
  )
  .enablePlugins(GraalVMNativeImagePlugin, UniversalPlugin)

def sharedSettings(additionalDeps: Seq[ModuleID])(project: Project): Project = {
  project
    .settings(
      libraryDependencies ++= Deps.CoreDeps ++ additionalDeps,
      libraryDependencies ++= {
        if (scalaVersion.value.startsWith("2")) {
          Seq(compilerPlugin(Deps.kindProjector))
        } else {
          Seq.empty
        }
      },
      scalacOptions --= Seq("-Xfatal-warnings", "-Ykind-projector", "-Wnonunit-statement"),
      scalacOptions ++= {
        if (scalaVersion.value.startsWith("2")) {
          Seq(
            "-Xsource:3",
            "-P:kind-projector:underscore-placeholders",
            "-Wmacros:after",
          )
        } else {
          Seq(
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
        "-J-Xmx8G",
      ),
      graalVMNativeImageGraalVersion := Some("ol9-java17-22.3.1"),
      run / fork                     := true,
    )
    .dependsOn(`graal-resources`)
    .enablePlugins(GraalVMNativeImagePlugin, UniversalPlugin)
}

// for quick experiments with distage snapshots
ThisBuild / resolvers ++= Resolver.sonatypeOssRepos("snapshots")
