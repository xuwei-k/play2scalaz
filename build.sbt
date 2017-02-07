import build._

lazy val play2scalaz = Project("play2scalaz", file(".")).settings(
  commonSettings,
  name := play2scalazName,
  libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.0-M2",
  libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.2.8",
  buildInfoPackage := "play2scalaz",
  buildInfoObject := "Play2ScalazBuildInfo",
  description := "play framework2 and scalaz typeclasses converters"
).enablePlugins(BuildInfoPlugin)
