val izumi_version = "0.6.24"

addSbtPlugin("com.github.pshirshov.izumi.r2" % "sbt-izumi-deps" % izumi_version)

addSbtPlugin("com.github.pshirshov.izumi.r2" % "sbt-izumi" % izumi_version)

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.7")

// bootstrap
lazy val selfPlugin = RootProject(file("../sbt/sbt-bom-distage-sample"))
lazy val root = project.in(file(".")).dependsOn(selfPlugin)
