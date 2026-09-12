import org.scalajs.linker.interface.{ModuleKind, OutputPatterns}
import sbtcrossproject.CrossPlugin.autoImport.{CrossType, crossProject}

import scala.util.chaining.scalaUtilChainingOps

val V = new {
  val distage       = "1.2.25"
  val logstage      = distage
  val scalatest     = "3.2.20"
  val scalacheck    = "1.20.0"
  val http4s        = "0.23.37"
  val doobie        = "1.0.0-RC13"
  val catsCore      = "2.13.0"
  val zio           = "2.1.26"
  val zioCats       = "23.1.0.13"
  val kindProjector = "0.13.4"
  val circeGeneric  = "0.14.16"
  val graalMetadata = "1.1.12"
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

  val doobie         = "org.typelevel" %% "doobie-core" % V.doobie
  val doobiePostgres = "org.typelevel" %% "doobie-postgres" % V.doobie
  val doobieHikari   = "org.typelevel" %% "doobie-hikari" % V.doobie

  val kindProjector = "org.typelevel" % "kind-projector" % V.kindProjector cross CrossVersion.full

  val zio     = "dev.zio" %% "zio" % V.zio
  val zioCats = "dev.zio" %% "zio-interop-cats" % V.zioCats

  val catsCore = "org.typelevel" %% "cats-core" % V.catsCore

  val graalMetadata = "org.graalvm.buildtools" % "graalvm-reachability-metadata" % V.graalMetadata

  // Standard set of deps for JVM-only sub-projects (monomorphic-cats,
  // monofunctor-tagless, and the JVM half of bifunctor-tagless).
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
    crossScalaVersions := Seq("3.9.0", "3.3.8"),
//    crossScalaVersions := Seq("3.3.8", "3.9.0"), // uncomment to use Scala 3 in IDE
    scalaVersion := crossScalaVersions.value.head,
    version      := "1.0.0",
    organization := "io.7mind",
  )
)

// -----------------------------------------------------------------------------
// bifunctor-tagless: a cross-built project (JVM + Scala.js)
//
// The shared half holds everything that compiles to both targets: the model,
// the API classes (which return http4s `HttpRoutes`), the repository traits
// and their in-memory dummy implementations, and the `LocalDispatcher` that
// invokes the assembled routes in-process. The JVM half adds the postgres
// repositories, the http4s ember server with CORS + static-file serving, and
// the role/launcher infrastructure. The JS half adds a `@JSExportTopLevel`
// entrypoint that boots the same distage graph with `Repo -> Dummy` and
// surfaces the dispatcher to the browser as `window.LeaderboardSim`.
// -----------------------------------------------------------------------------

lazy val `bifunctor-tagless` = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Full)
  .in(file("bifunctor-tagless"))
  .settings(
    sharedScalaSettings,
    libraryDependencies ++= Seq(
      "io.7mind.izumi" %%% "distage-core" % V.distage,
      "io.7mind.izumi" %%% "distage-extension-plugins" % V.distage,
      "io.7mind.izumi" %%% "distage-extension-logstage" % V.distage,
      "io.7mind.izumi" %%% "logstage-core" % V.logstage,
      "org.http4s" %%% "http4s-dsl" % V.http4s,
      "org.http4s" %%% "http4s-circe" % V.http4s,
      "io.circe" %%% "circe-generic" % V.circeGeneric,
      "org.typelevel" %%% "cats-core" % V.catsCore,
      "dev.zio" %%% "zio" % V.zio,
      // `zio-managed` is required transitively by `zio-interop-cats`'s
      // ZManaged bridge classes; under scala.js the linker validates all
      // referenced classes, so we must pull the artifact in explicitly.
      "dev.zio" %%% "zio-managed" % V.zio,
      "dev.zio" %%% "zio-interop-cats" % V.zioCats,
    ),
  )
  .jvmConfigure(_.pipe(jvmSharedSettings(Seq(Deps.zio, Deps.zioCats))))
  .jsSettings(
    scalaJSUseMainModuleInitializer := false,
    // NoModule output: the linker emits a plain script that publishes
    // `@JSExportTopLevel` bindings on the global scope. We deliberately do
    // NOT use ESModule here because browsers refuse to load ES modules from
    // `file://` (the simulation page is meant to be usable both via the
    // http4s server and by opening index.html directly from disk).
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.NoModule) },
  )

// sbt-scalajs-crossproject derives the per-platform project IDs from the
// cross-project's name: `bifunctor-taglessJVM` and `bifunctor-taglessJS`.
// These aliases give us Project handles to reference in tasks below.
lazy val `bifunctor-taglessJVM` = `bifunctor-tagless`.jvm
lazy val `bifunctor-taglessJS`  = `bifunctor-tagless`.js

// Task: link the Scala.js bundle in `bifunctor-tagless-js` and copy the
// resulting main.js into the JVM project's resources/webapp/ so the http4s
// server (and a browser opening index.html directly) can find it.
lazy val copySimJs = taskKey[Seq[File]]("Link the simulation JS and copy it into the JVM resources/webapp/ directory.")

copySimJs := {
  val _      = (`bifunctor-taglessJS` / Compile / fullLinkJS).value
  val srcDir = (`bifunctor-taglessJS` / Compile / fullLinkJS / scalaJSLinkerOutputDirectory).value
  val outDir = (`bifunctor-taglessJVM` / baseDirectory).value / "src" / "main" / "resources" / "webapp"
  IO.createDirectory(outDir)
  val srcs = (srcDir ** "*.js").get ++ (srcDir ** "*.js.map").get
  srcs.map {
    f =>
      val dst = outDir / f.getName
      IO.copyFile(f, dst, preserveLastModified = true)
      dst
  }
}

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
    `bifunctor-taglessJVM`,
    `bifunctor-taglessJS`,
    `monofunctor-tagless`,
    `monomorphic-cats`,
    `graal-resources`,
  )
  .enablePlugins(GraalVMNativeImagePlugin, UniversalPlugin)

// Scalac/source settings shared by every Scala project in this build, both
// JVM-only ones and the cross-built bifunctor-tagless halves.
def sharedScalaSettings: Seq[Setting[_]] = Seq(
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
)

// JVM-only project settings (the http4s ember server, doobie, GraalVM native
// image, etc). Applied to the JVM half of `bifunctor-tagless` and — via
// `sharedSettings` below — to the plain JVM-only projects.
//
// Intentionally does NOT include `sharedScalaSettings`: the cross-built
// bifunctor-tagless applies those once on the cross project so they reach
// both halves, and applying them a second time here would set scalacOptions
// like `-Yretain-trees` twice and trip sbt-tpolecat's "set repeatedly"
// guard.
def jvmSharedSettings(additionalDeps: Seq[ModuleID])(project: Project): Project = {
  project
    .settings(
      libraryDependencies ++= Deps.CoreDeps ++ additionalDeps,
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

// Plain (non-cross) Scala project — used by `monomorphic-cats` and
// `monofunctor-tagless`, which stay JVM-only. These need both
// `sharedScalaSettings` (scalac flags + macro settings) and the JVM-only
// extras above.
def sharedSettings(additionalDeps: Seq[ModuleID])(project: Project): Project =
  jvmSharedSettings(additionalDeps)(project).settings(sharedScalaSettings)

// for quick experiments with distage snapshots
ThisBuild / resolvers += Resolver.sonatypeCentralSnapshots
