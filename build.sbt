import com.typesafe.sbt.packager.docker._

import java.io.ByteArrayInputStream

val V = new {
  val distage         = "1.1.0-SNAPSHOT"
  val logstage        = distage
  val scalatest       = "3.2.12"
  val scalacheck      = "1.16.0"
  val http4s          = "0.23.12"
  val doobie          = "1.0.0-RC2"
  val zio             = "1.0.14"
  val zioCats         = "3.2.9.1"
  val kindProjector   = "0.13.2"
  val circeDerivation = "0.13.0-M5"
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
    scalaVersion := "2.13.8",
    version      := "1.0.0",
    organization := "io.7mind",
  )
)

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
      Deps.circeDerivation,
      Deps.doobie,
      Deps.doobiePostgres,
      Deps.doobieHikari,
      Deps.zio,
      Deps.zioCats,
    ),
    addCompilerPlugin(Deps.kindProjector),
    scalacOptions -= "-Xfatal-warnings",
    scalacOptions += "-Xsource:3",
    scalacOptions += "-P:kind-projector:underscore-placeholders",
    scalacOptions += "-Wmacros:after",
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
    GraalVMNativeImage / mainClass := Some("leaderboard.MainProfileProdDocker"),
    graalVMNativeImageOptions ++= Seq(
      "--no-fallback",
      "-H:+ReportExceptionStackTraces",
      "--allow-incomplete-classpath",
      "--report-unsupported-elements-at-runtime",
      "--enable-https",
      "--enable-http",
      "-J-Xmx4G",
    ),
    graalVMNativeImageGraalVersion := Some("java11-22.0.0.2"),
    // see https://github.com/sbt/sbt-native-packager/issues/1492
    GraalVMNativeImage / UniversalPlugin.autoImport.containerBuildImage := Def.taskDyn {
      Def.task {
        val baseImage     = s"ghcr.io/graalvm/graalvm-ce:ol8-${graalVMNativeImageGraalVersion.value.get}"
        val dockerCommand = (GraalVMNativeImage / DockerPlugin.autoImport.dockerExecCommand).value
        val streams       = Keys.streams.value

        val (baseName, tag) = baseImage.split(":", 2) match {
          case Array(n, t) => (n, t)
          case Array(n)    => (n, "latest")
        }

        val imageName = s"${baseName.replace('/', '-')}-native-image:$tag"
        import sys.process._
        if ((dockerCommand ++ Seq("image", "ls", imageName, "--quiet")).!!.trim.isEmpty) {
          streams.log.info(s"Generating new GraalVM native-image image based on $baseImage: $imageName")

          val dockerContent = Dockerfile(
            Cmd("FROM", baseImage),
            Cmd("WORKDIR", "/opt/graalvm"),
            ExecCmd("RUN", "gu", "install", "native-image"),
            ExecCmd("ENTRYPOINT", "native-image"),
            ExecCmd("RUN", "ln", "-s", s"/opt/graalvm-ce-${graalVMNativeImageGraalVersion.value.get}/bin/native-image", "/usr/local/bin/native-image"),
          ).makeContent

          val command = dockerCommand ++ Seq("build", "-t", imageName, "-")

          val pb: ProcessBuilder = sys.process.Process(command) #< new ByteArrayInputStream(dockerContent.getBytes())
          val ret = pb ! (DockerPlugin: { def publishLocalLogger(log: sbt.Logger): scala.AnyRef with scala.sys.process.ProcessLogger }).publishLocalLogger(streams.log)

          if (ret != 0)
            throw new RuntimeException("Nonzero exit value when generating GraalVM container build image: " + ret)

        } else
          streams.log.info(s"Using existing GraalVM native-image image: $imageName")

        Some(imageName)
      }: Def.Initialize[Task[Option[String]]]
    }.value,
  )
  .enablePlugins(GraalVMNativeImagePlugin, UniversalPlugin)

ThisBuild / resolvers += Resolver.sonatypeRepo("snapshots")
