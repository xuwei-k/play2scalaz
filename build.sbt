name := "play2scalaz"

scalaVersion := "2.10.4-RC1"

organization := "com.github.xuwei-k"

resolvers += "typesafe" at "http://typesafe.artifactoryonline.com/typesafe/releases"

licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT"))

initialCommands in console := "import play2scalaz._"

scalacOptions ++= Seq("-language:_", "-deprecation", "-unchecked", "-Xlint")

val scalazV = "7.0.5"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.2.2-RC1",
  "org.scalaz" %% "scalaz-core" % scalazV,
  "org.scalaz" %% "scalaz-scalacheck-binding" % scalazV % "test",
  "org.typelevel" %% "scalaz-specs2" % "0.1.5" % "test"
)

