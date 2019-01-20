import com.github.pshirshov.izumi.sbt.deps.Izumi.Deps
import com.github.pshirshov.izumi.sbt.deps.IzumiDeps
import com.github.pshirshov.izumi.sbt.deps.IzumiDeps.{R, V}
import sbt.Keys.libraryDependencies

name := "distage-workshop"

version := "0.1"

scalaVersion := "2.12.8"

organization in ThisBuild := "com.github.ratoshniuk.izumi.distage"


enablePlugins(IzumiGitEnvironmentPlugin)

val GlobalSettings = new DefaultGlobalSettingsGroup {
  override val settings: Seq[sbt.Setting[_]] = Seq(
    crossScalaVersions := Seq(
      V.scala_212,
    ),
    addCompilerPlugin(R.kind_projector),
    libraryDependencies ++= Seq(
      Izumi.R.distage_plugins,
      IzumiDeps.T.scalatest,
      Izumi.R.fundamentals_bio,
      Izumi.R.logstage_di
    ) ++ IzumiDeps.R.cats_all,
  )
}

lazy val WithoutBadPlugins = new SettingsGroup {
  override val disabledPlugins: Set[sbt.AutoPlugin] = Set(AssemblyPlugin)
}

lazy val DomainSettings = new SettingsGroup {
  override def settings: Seq[sbt.Setting[_]] = Seq(
    libraryDependencies ++= Seq(
      Izumi.R.distage_roles
    )
  )
}

lazy val AppSettings = new SettingsGroup {
  override val plugins: Set[sbt.Plugins] = Set(AssemblyPlugin)
  override val settings: Seq[sbt.Setting[_]] = Seq(
    libraryDependencies ++= Seq(Izumi.R.distage_roles, Izumi.R.distage_app),
  )
}

lazy val RoleSettings = new SettingsGroup {
  override val settings: Seq[sbt.Setting[_]] = Seq(
    libraryDependencies ++= Seq(Izumi.R.distage_roles_api, Izumi.R.distage_config),
  )
}

val SbtSettings = new SettingsGroup {
  override val settings: Seq[sbt.Setting[_]] = Seq(
    Seq(
      target ~= { t => t.toPath.resolve("primary").toFile }
      , crossScalaVersions := Seq(
        V.scala_212
      )
      , libraryDependencies ++= Seq(
        "org.scala-sbt" % "sbt" % sbtVersion.value
      )
      , sbtPlugin := true
    )
  ).flatten
}

lazy val inRoot = In(".").settings(WithoutBadPlugins)

lazy val inDomain = In("domain").settings(GlobalSettings, WithoutBadPlugins, DomainSettings)

lazy val inLib = In("lib").settings(GlobalSettings, WithoutBadPlugins)

lazy val inRoles = In("role").settings(GlobalSettings, RoleSettings, WithoutBadPlugins)

lazy val inApp = In("app").settings(GlobalSettings, AppSettings)

lazy val inSbt = In("sbt").settings(SbtSettings, WithoutBadPlugins)

lazy val common = inLib.as.module
  .settings(
    libraryDependencies ++= Seq(
      IzumiDeps.R.zio_interop
      , IzumiDeps.R.zio_core
    )
  )

lazy val users = inDomain.as.module
  .depends(common)

lazy val usersRole = inRoles.as.module
  .depends(users)

lazy val launcher = inApp.as.module
  .depends(usersRole)

lazy val sbtBomDistageSample = inSbt.as
  .module
  .settings(withBuildInfo("com.github.ratoshniuk.izumi.distage.sample", "DistageSample"))

lazy val workshop = inRoot.as.root
  .transitiveAggregate(launcher, sbtBomDistageSample)


/*
At this point use thse commands to setup project layout from sbt shell:

newModule role/accounts-role
newModule role/users-role
newModule app/launcher
*/
