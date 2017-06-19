import build._
import org.scalajs.sbtplugin.cross.CrossProject

val scalapropsVersion = "0.5.0"

lazy val play2scalaz = CrossProject("play2scalaz", file("."), CrossType.Pure).settings(
  commonSettings,
  name := play2scalazName,
  scalapropsCoreSettings,
  libraryDependencies += "com.github.scalaprops" %%% "scalaprops" % scalapropsVersion % "test",
  libraryDependencies += "com.github.scalaprops" %%% "scalaprops-scalazlaws" % scalapropsVersion % "test",
  libraryDependencies += "com.typesafe.play" %%% "play-json" % "2.6.0-RC2",
  libraryDependencies += "org.scalaz" %%% "scalaz-core" % "7.2.13",
  buildInfoPackage := "play2scalaz",
  buildInfoObject := "Play2ScalazBuildInfo",
  description := "play framework2 and scalaz typeclasses converters"
).enablePlugins(BuildInfoPlugin).jsSettings(
  scalacOptions += {
    val a = (baseDirectory in LocalRootProject).value.toURI.toString
    val g = "https://raw.githubusercontent.com/xuwei-k/play2scalaz/" + tagOrHash.value
    s"-P:scalajs:mapSourceURI:$a->$g/"
  }
)

lazy val play2scalazJVM = play2scalaz.jvm
lazy val play2scalazJS = play2scalaz.js

lazy val root = Project("root", file(".")).settings(
  commonSettings,
  PgpKeys.publishLocalSigned := {},
  PgpKeys.publishSigned := {},
  publishLocal := {},
  publish := {},
  publishArtifact in Compile := false,
  scalaSource in Compile := file("dummy"),
  scalaSource in Test := file("dummy")
).aggregate(
  play2scalazJVM, play2scalazJS
)
