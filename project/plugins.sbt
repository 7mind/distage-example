val izumi_version = "0.9.0-SNAPSHOT"

addSbtPlugin("io.7mind.izumi" % "sbt-izumi-deps" % izumi_version)

addSbtPlugin("io.7mind.izumi" % "sbt-izumi" % izumi_version)

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.7")

// bootstrap
lazy val selfPlugin = RootProject(file("../sbt/sbt-bom-distage-sample"))
lazy val root = project.in(file(".")).dependsOn(selfPlugin)
