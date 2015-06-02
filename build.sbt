name := "team-two"

scalaVersion := "2.11.6"

val sprayV = "1.3.2"

libraryDependencies ++= Seq(
  "io.spray" %% "spray-can" % sprayV,
  "io.spray" %% "spray-routing" % sprayV,
  "io.spray" %% "spray-json" % "1.3.1",  // NB: Not at sprayV. 1.3.2 does not exist.
  "io.spray" %% "spray-client" % sprayV,
  "com.typesafe.akka" %% "akka-actor" % "2.3.4",
  "io.spray" %% "spray-testkit" % sprayV % Test,
  "org.scalatest" %% "scalatest" % "2.2.4" % Test,
  "org.json4s" %% "json4s-jackson" % "3.2.11",
  "com.typesafe" % "config" % "1.2.1",
  "com.typesafe.slick" %% "slick" % "2.1.0",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "mysql" % "mysql-connector-java" % "latest.release",
  "c3p0" % "c3p0" % "0.9.1.2",
  "mysql" % "mysql-connector-java" % "latest.release",
  "org.scalaj" %% "scalaj-http" % "1.1.4",
  "org.apache.commons" % "commons-math3" % "3.0"
)
