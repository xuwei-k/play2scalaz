name := "play2scalaz"

scalaVersion := "2.10.4-RC3"

organization := "com.github.xuwei-k"

description := "play framework2 and scalaz typeclasses converter"

resolvers += "typesafe" at "http://typesafe.artifactoryonline.com/typesafe/releases"

licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT"))

scmInfo := Some(ScmInfo(
  url("https://github.com/xuwei-k/play2scalaz"),
  "scm:git:git@github.com:xuwei-k/play2scalaz.git"
))

pomExtra := (
<url>https://github.com/xuwei-k/play2scalaz</url>
<developers>
  <developer>
    <id>xuwei-k</id>
    <name>Kenji Yoshida</name>
    <url>https://github.com/xuwei-k</url>
  </developer>
</developers>
)

def gitHash: Option[String] = scala.util.Try(
  sys.process.Process("git show -s --oneline").lines_!.head.split(" ").head
).toOption

scalacOptions in (Compile, doc) ++= {
  val tag = if(isSnapshot.value) gitHash.getOrElse("master") else { "v" + version.value }
  Seq(
    "-sourcepath", baseDirectory.value.getAbsolutePath,
    "-doc-source-url", s"https://github.com/xuwei-k/play2scalaz/tree/${tag}€{FILE_PATH}.scala"
  )
}

initialCommands in console := "import play2scalaz._"

scalacOptions ++= Seq("-language:_", "-deprecation", "-unchecked", "-Xlint")

parallelExecution in Test := false

testOptions in Test += Tests.Argument(TestFrameworks.ScalaCheck, "-workers", "1")

val scalazV = "7.1.0-M5"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.2.2",
  "org.scalaz" %% "scalaz-core" % scalazV,
  "org.scalaz" %% "scalaz-scalacheck-binding" % scalazV % "test"
)

val specLiteURL = s"https://raw.github.com/scalaz/scalaz/v${scalazV}/tests/src/test/scala/scalaz/SpecLite.scala"
val specLite = SettingKey[List[String]]("specLite")

specLite := {
  println(s"downloading from ${specLiteURL}")
  val lines = IO.readLinesURL(url(specLiteURL))
  println("download finished")
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
