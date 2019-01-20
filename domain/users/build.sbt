libraryDependencies ++= Seq(
  "org.tpolecat" %% "doobie-core"      % "0.6.0",
  "org.tpolecat" %% "doobie-hikari"    % "0.6.0",
  "org.tpolecat" %% "doobie-postgres"  % "0.6.0",
  "org.tpolecat" %% "doobie-scalatest" % "0.6.0" % "test",
  "de.heikoseeberger" %% "akka-http-circe" % "1.24.3",
  "com.typesafe.akka" %% "akka-http" % "10.1.7",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.1.7" % Test
)
