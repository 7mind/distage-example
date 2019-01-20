resolvers += Opts.resolver.sonatypeReleases
resolvers += Opts.resolver.sonatypeSnapshots

sbtPlugin := true
// we need this to be copied here for build bootstrapping
libraryDependencies ++= Seq("org.scala-sbt" % "sbt" % sbtVersion.value)

