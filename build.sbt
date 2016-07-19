name := """akka-persistence-event-sourcing"""

version := "1.0.0"

organization := "org.kepteasy"

scalaVersion := "2.11.6"

resolvers ++= Seq(
  "snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
  "releases" at "http://oss.sonatype.org/content/repositories/releases",
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "spray repo" at "http://repo.spray.io"
)

Seq(Revolver.settings: _*)

scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8")

javaOptions := Seq("-Xdebug", "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000")

parallelExecution in Test := false

libraryDependencies ++= {
  val sprayVersion = "1.3.3"
  val akkaVersion = "2.4.0"
  Seq(
    "org.slf4j" % "slf4j-api" % "1.7.7",
    "ch.qos.logback" % "logback-core" % "1.1.2",
    "ch.qos.logback" % "logback-classic" % "1.1.2",
    "io.spray" %% "spray-can" % sprayVersion,
    "io.spray" %% "spray-routing" % sprayVersion,
    "io.spray" %% "spray-testkit" % sprayVersion,
    "io.spray" %% "spray-httpx" % sprayVersion,
    "io.spray" %% "spray-client" % sprayVersion,
    "org.json4s" %% "json4s-native" % "3.2.10",
    "joda-time" % "joda-time" % "2.4",
    "org.joda" % "joda-convert" % "1.7",
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
    "org.scalatest" %% "scalatest" % "2.2.1" % "test",
    "io.spray" %% "spray-testkit" % "1.3.1" % "test",
    "com.github.t3hnar" %% "scala-bcrypt" % "2.4",
    "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.10",
    // Titan
    "com.thinkaurelius.titan" % "titan-core" % "1.0.0",
    "com.thinkaurelius.titan" % "titan-cassandra" % "1.0.0",
    "com.thinkaurelius.titan" % "titan-es" % "1.0.0",
    "com.tinkerpop.blueprints" % "blueprints-core" % "2.6.0",
    "com.tinkerpop" % "frames" % "2.6.0",
    "com.tinkerpop" % "pipes" % "2.6.0"
  )
}


fork in run := true