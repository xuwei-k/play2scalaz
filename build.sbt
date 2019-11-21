import sbtcrossproject.{CrossProject, CrossType}
import sbtrelease._
import ReleaseStateTransformations._
import com.typesafe.sbt.pgp.PgpKeys

val scalapropsVersion = "0.6.2"
val Scala212 = "2.12.10"

Global / onChangedBuildSource := ReloadOnSourceChanges

val sonatypeURL = "https://oss.sonatype.org/service/local/repositories/"

val tagName = Def.setting{
  s"v${if (releaseUseGlobalVersion.value) (version in ThisBuild).value else version.value}"
}
val tagOrHash = Def.setting{
  if(isSnapshot.value) gitHash() else tagName.value
}

def gitHash(): String =
  sys.process.Process("git rev-parse HEAD").lineStream_!.head

def releaseStepAggregateCross[A](key: TaskKey[A]): ReleaseStep = ReleaseStep(
  action = { state =>
    val extracted = Project extract state
    extracted.runAggregated(key in Global in extracted.get(thisProjectRef), state)
  },
  enableCrossBuild = true
)

val play2scalazName = "play2scalaz"
val modules = play2scalazName :: Nil

val updateReadme = { state: State =>
  val extracted = Project.extract(state)
  val scalaV = extracted get scalaBinaryVersion
  val v = extracted get version
  val org =  extracted get organization
  val snapshotOrRelease = if(extracted get isSnapshot) "snapshots" else "releases"
  val readme = "README.md"
  val readmeFile = file(readme)
  val newReadme = Predef.augmentString(IO.read(readmeFile)).lines.map{ line =>
    val matchReleaseOrSnapshot = line.contains("SNAPSHOT") == v.contains("SNAPSHOT")
    def n = modules.find(line.contains).get
    if(line.startsWith("libraryDependencies") && matchReleaseOrSnapshot && line.contains(" %% ")){
      s"""libraryDependencies += "${org}" %% "${n}" % "$v""""
    }else if(line.startsWith("libraryDependencies") && matchReleaseOrSnapshot && line.contains(" %%% ")){
      s"""libraryDependencies += "${org}" %%% "${n}" % "$v""""
    }else if(line.contains(sonatypeURL) && matchReleaseOrSnapshot){
      s"- [API Documentation](${sonatypeURL}${snapshotOrRelease}/archive/${org.replace('.','/')}/${n}_${scalaV}/${v}/${n}_${scalaV}-${v}-javadoc.jar/!/index.html)"
    }else line
  }.mkString("", "\n", "\n")
  IO.write(readmeFile, newReadme)
  val git = new Git(extracted get baseDirectory)
  git.add(readme) ! state.log
  git.commit(message = "update " + readme, sign = false, signOff = false) ! state.log
  sys.process.Process("git diff HEAD^") ! state.log
  state
}

val unusedWarnings = (
  "-Ywarn-unused" ::
  Nil
)

val commonSettings = Def.settings(
  publishTo := sonatypePublishToBundle.value,
  fullResolvers ~= {_.filterNot(_.name == "jcenter")},
  scalaVersion := Scala212,
  crossScalaVersions := Scala212 :: "2.13.1" :: Nil,
  organization := "com.github.xuwei-k",
  licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
  commands += Command.command("updateReadme")(updateReadme),
  pomExtra := (
  <url>https://github.com/xuwei-k/play2scalaz</url>
  <developers>
    <developer>
      <id>xuwei-k</id>
      <name>Kenji Yoshida</name>
      <url>https://github.com/xuwei-k</url>
    </developer>
  </developers>
  <scm>
    <url>git@github.com:xuwei-k/play2scalaz.git</url>
    <connection>scm:git:git@github.com:xuwei-k/play2scalaz.git</connection>
    <tag>{tagOrHash.value}</tag>
  </scm>
  ),
  scalacOptions in (Compile, doc) ++= {
    Seq(
      "-sourcepath", (baseDirectory in LocalRootProject).value.getAbsolutePath,
      "-doc-source-url", s"https://github.com/xuwei-k/play2scalaz/tree/${tagOrHash.value}€{FILE_PATH}.scala"
    )
  },
  scalacOptions ++= (
    "-deprecation" ::
    "-unchecked" ::
    "-Xlint" ::
    "-language:existentials" ::
    "-language:higherKinds" ::
    "-language:implicitConversions" ::
    Nil
  ) ::: unusedWarnings,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    (updateReadme: ReleaseStep),
    tagRelease,
    releaseStepAggregateCross(PgpKeys.publishSigned),
    releaseStepCommand("sonatypeBundleRelease"),
    setNextVersion,
    commitNextVersion,
    (updateReadme: ReleaseStep),
    pushChanges
  ),
  buildInfoKeys := Seq[BuildInfoKey](
    organization,
    name,
    version,
    scalaVersion,
    sbtVersion,
    scalacOptions,
    licenses
  ),
  pomPostProcess := { node =>
    import scala.xml._
    import scala.xml.transform._
    def stripIf(f: Node => Boolean) = new RewriteRule {
      override def transform(n: Node) =
        if (f(n)) NodeSeq.Empty else n
    }
    val stripTestScope = stripIf { n => n.label == "dependency" && (n \ "scope").text == "test" }
    new RuleTransformer(stripTestScope).transform(node)(0)
  },
  credentials ++= PartialFunction.condOpt(sys.env.get("SONATYPE_USER") -> sys.env.get("SONATYPE_PASSWORD")){
    case (Some(user), Some(pass)) =>
      Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", user, pass)
  }.toList,
  Seq(Compile, Test).flatMap(c =>
    scalacOptions in (c, console) ~= {_.filterNot(unusedWarnings.toSet)}
  )
)


lazy val playJsonVersion = settingKey[String]("")

lazy val play2scalaz = CrossProject("play2scalaz", file("."))(JVMPlatform, JSPlatform).crossType(CrossType.Pure).settings(
  commonSettings,
  name := play2scalazName,
  scalapropsCoreSettings,
  playJsonVersion := "2.8.0",
  libraryDependencies += "com.github.scalaprops" %%% "scalaprops" % scalapropsVersion % "test",
  libraryDependencies += "com.github.scalaprops" %%% "scalaprops-scalaz" % scalapropsVersion % "test",
  libraryDependencies += "com.typesafe.play" %%% "play-json" % playJsonVersion.value,
  libraryDependencies += "org.scalaz" %%% "scalaz-core" % "7.2.29",
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
