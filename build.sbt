name := "play2scalaz"

scalaVersion := "2.10.4-RC1"

organization := "com.github.xuwei-k"

resolvers += "typesafe" at "http://typesafe.artifactoryonline.com/typesafe/releases"

licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT"))

initialCommands in console := "import play2scalaz._"

scalacOptions ++= Seq("-language:_", "-deprecation", "-unchecked", "-Xlint")

parallelExecution in Test := false

testOptions in Test += Tests.Argument(TestFrameworks.ScalaCheck, "-workers", "1")

val scalazV = "7.1.0-M4"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.2.2-RC1",
  "org.scalaz" %% "scalaz-core" % scalazV,
  "org.scalaz" %% "scalaz-scalacheck-binding" % scalazV % "test"
)

val specLiteURL = "https://raw.github.com/scalaz/scalaz/v7.1.0-M4/tests/src/test/scala/scalaz/SpecLite.scala"
val specLite = SettingKey[List[String]]("specLite")

specLite := {
  sLog.value.info(s"downloading from ${specLiteURL}")
  val lines = IO.readLinesURL(url(specLiteURL))
  sLog.value.info("download finished")
  lines
}

def specLiteFile(dir: File, contents: List[String]): File = {
  val file = dir / "SpecLite.scala"
  IO.writeLines(file, contents)
  file
}

sourceGenerators in Test += task{
  Seq(specLiteFile((sourceManaged in Test).value, specLite.value))
}
