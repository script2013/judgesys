name := """relevance-judgement-system"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "se.radley" %% "play-plugins-salat" % "1.4.0"
)
