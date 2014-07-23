import sbt._, Keys._
import xerial.sbt.Sonatype
import sbtrelease._
import ReleaseStateTransformations._
import com.typesafe.sbt.pgp.PgpKeys

object build extends Build {

  val play2scalazFile = "Play2Scalaz.scala"
  val play2scalaz70File = "Play2Scalaz70Base.scala"
  val play2scalaz71File = "Play2Scalaz71Base.scala"
  val play2scalacheckFile = "Play2Scalacheck.scala"
  val rootProjectId = "root"
  val root211ProjectId = "root211"
  val play22 = "2.2.3"
  val play23v = "2.3.2"
  val scalaz71v = "7.1.0-RC1"
  val scalaz70 = "org.scalaz" %% "scalaz-core" % "7.0.6"
  val scalaz71 = "org.scalaz" %% "scalaz-core" % scalaz71v
  val scalacheck110 = "org.scalacheck" %% "scalacheck" % "1.10.1"
  val scalacheck111 = "org.scalacheck" %% "scalacheck" % "1.11.3"
  val sonatypeURL = "https://oss.sonatype.org/service/local/repositories/"
  val scalazDescription = "play framework2 and scalaz typeclasses converter"
  val scalacheckDescription = "play framework2 scalacheck binding"
  val copySources = taskKey[Unit]("copy source files")
  val generatedSourceDir = "generated"
  val cleanSrc = taskKey[Unit]("clean generated sources")
  val specLiteURL = s"https://raw.githubusercontent.com/scalaz/scalaz/v${scalaz71v}/tests/src/test/scala/scalaz/SpecLite.scala"
  val specLite = SettingKey[List[String]]("specLite")
  val checkGenerate = taskKey[Unit]("check generate")

  def gitHash: String = scala.util.Try(
    sys.process.Process("git rev-parse HEAD").lines_!.head
  ).getOrElse("master")

  def specLiteFile(dir: File, contents: List[String]): File = {
    val file = dir / "SpecLite.scala"
    IO.writeLines(file, contents)
    file
  }

  def releaseStepAggregateCross[A](key: TaskKey[A]): ReleaseStep = ReleaseStep(
    action = { state =>
      val extracted = Project extract state
      extracted.runAggregated(key in Global in extracted.get(thisProjectRef), state)
    },
    enableCrossBuild = true
  )

  val updateReadme = { state: State =>
    val extracted = Project.extract(state)
    val scalaV = extracted get scalaBinaryVersion
    val v = extracted get version
    val org =  extracted get organization
    val snapshotOrRelease = if(extracted get isSnapshot) "snapshots" else "releases"
    val readme = "README.md"
    val readmeFile = file(readme)
    val modules = projects.map(p => extracted get (name in p)).filterNot(Set(rootProjectId, root211ProjectId))
    val newReadme = Predef.augmentString(IO.read(readmeFile)).lines.map{ line =>
      val matchReleaseOrSnapshot = line.contains("SNAPSHOT") == v.contains("SNAPSHOT")
      def n = modules.find(line.contains).get
      if(line.startsWith("libraryDependencies") && matchReleaseOrSnapshot){
        s"""libraryDependencies += "${org}" %% "${n}" % "$v""""
      }else if(line.contains(sonatypeURL) && matchReleaseOrSnapshot){
        s"- [API Documentation](${sonatypeURL}${snapshotOrRelease}/archive/${org.replace('.','/')}/${n}_${scalaV}/${v}/${n}_${scalaV}-${v}-javadoc.jar/!/index.html)"
      }else line
    }.mkString("", "\n", "\n")
    IO.write(readmeFile, newReadme)
    val git = new Git(extracted get baseDirectory)
    git.add(readme) ! state.log
    git.commit("update " + readme) ! state.log
    "git diff HEAD^" ! state.log
    state
  }

  val commonSettings = ReleasePlugin.releaseSettings ++ Sonatype.sonatypeSettings ++ Seq(
    scalaVersion := "2.10.4",
    crossScalaVersions := scalaVersion.value :: Nil,
    organization := "com.github.xuwei-k",
    resolvers += "typesafe" at "http://typesafe.artifactoryonline.com/typesafe/releases",
    licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
    commands += Command.command("updateReadme")(updateReadme),
    watchSources ++= (((baseDirectory in LocalRootProject).value / "sources") ** ".scala").get,
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
      <tag>{if(isSnapshot.value) gitHash else { "v" + version.value }}</tag>
    </scm>
    ),
    scalacOptions in (Compile, doc) ++= {
      val tag = if(isSnapshot.value) gitHash else { "v" + version.value }
      Seq(
        "-sourcepath", (baseDirectory in LocalRootProject).value.getAbsolutePath,
        "-doc-source-url", s"https://github.com/xuwei-k/play2scalaz/tree/${tag}€{FILE_PATH}.scala"
      )
    },
    logBuffered in Test := false,
    scalacOptions ++= Seq("-language:_", "-deprecation", "-unchecked", "-Xlint"),
    scalacOptions ++= (
      if(scalaVersion.value.startsWith("2.11"))
        Seq("-Xsource:2.10", "-Ywarn-unused", "-Ywarn-unused-import")
      else
        Nil
    ),
    PgpKeys.publishSigned <<= PgpKeys.publishSigned dependsOn compile,
    ReleasePlugin.ReleaseKeys.releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      (updateReadme: ReleaseStep),
      tagRelease,
      releaseStepAggregateCross(PgpKeys.publishSigned),
      setNextVersion,
      commitNextVersion,
      (updateReadme: ReleaseStep),
      releaseStepAggregateCross(Sonatype.SonatypeKeys.sonatypeReleaseAll),
      pushChanges
    ),
    checkGenerate := {
      val _ = (compile in Test).value
      val diff = sys.process.Process("git diff").lines_!
      assert(diff.size == 0, diff.mkString("\n"))
    },
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
    credentials ++= PartialFunction.condOpt(sys.env.get("SONATYPE_USER") -> sys.env.get("SONATYPE_PASS")){
      case (Some(user), Some(pass)) =>
        Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", user, pass)
    }.toList
  )

  def module(projectName: String, srcFiles: List[String], playVersion: String) =
    Project(projectName, file(projectName)).settings(commonSettings: _*).settings(
      libraryDependencies += "com.typesafe.play" %% "play-json" % playVersion,
      copySources := {
        srcFiles.foreach{ srcFile =>
          IO.copyFile(
            (baseDirectory in LocalRootProject).value / "sources" / srcFile,
            (scalaSource in Compile).value / generatedSourceDir / srcFile
          )
        }
      },
      compile in Compile <<= (compile in Compile) dependsOn copySources,
      packageSrc in Compile <<= (packageSrc in Compile).dependsOn(compile in Compile),
      cleanSrc := IO.delete((scalaSource in Compile).value / generatedSourceDir),
      clean <<= clean dependsOn cleanSrc
    )

  lazy val root = Project(rootProjectId, file(".")).settings(
    commonSettings: _*
  ).settings(
    publishArtifact := false,
    publish := {},
    publishLocal := {}
  ).aggregate(
    play22scalaz70,
    play22scalaz71,
    play22scalacheck110,
    play22scalacheck111,
    play23scalaz70,
    play23scalaz71,
    play23scalacheck110,
    play23scalacheck111
  )

  lazy val root211 = Project(root211ProjectId, file("root211")).settings(
    commonSettings: _*
  ).settings(
    publishArtifact := false,
    publish := {},
    publishLocal := {},
    scalaVersion := "2.11.2"
  ).aggregate(
    play23scalaz70,
    play23scalaz71,
    play23scalacheck110,
    play23scalacheck111
  )

  lazy val play23scalaz70 = module(
    "play23scalaz70", play2scalazFile :: play2scalaz70File :: Nil, play23v
  ).settings(
    description := scalazDescription,
    libraryDependencies += scalaz70
  )

  lazy val play23scalacheck110 = module(
    "play23scalacheck110", play2scalacheckFile :: Nil, play23v
  ).settings(
    description := scalacheckDescription,
    libraryDependencies += scalacheck110
  )

  lazy val play23scalaz71 = module(
    "play23scalaz71", play2scalazFile :: play2scalaz71File :: Nil, play23v
  ).settings(
    description := scalazDescription,
    libraryDependencies += scalaz71
  )

  lazy val play23scalacheck111 = module(
    "play23scalacheck111", play2scalacheckFile :: Nil, play23v
  ).settings(
    description := scalacheckDescription,
    libraryDependencies ++= Seq(
      scalacheck111,
      "org.scalaz" %% "scalaz-scalacheck-binding" % scalaz71v % "test"
    ),
    specLite := {
      println(s"downloading from ${specLiteURL}")
      val lines = IO.readLinesURL(url(specLiteURL))
      println("download finished")
      lines
    },
    sourceGenerators in Test += task{
      Seq(specLiteFile((sourceManaged in Test).value, specLite.value))
    }
  ).dependsOn(play23scalaz71 % "test->test")

  lazy val play22scalaz70 = module(
    "play22scalaz70", play2scalazFile :: play2scalaz70File :: Nil, play22
  ).settings(
    description := scalazDescription,
    libraryDependencies += scalaz70
  )

  lazy val play22scalacheck110 = module(
    "play22scalacheck110", play2scalacheckFile :: Nil, play22
  ).settings(
    name := "play22scalacheck110",
    description := scalacheckDescription,
    libraryDependencies += scalacheck110
  )

  lazy val play22scalaz71 = module(
    "play22scalaz71", play2scalazFile :: play2scalaz71File :: Nil, play22
  ).settings(
    description := scalazDescription,
    libraryDependencies += scalaz71
  )

  lazy val play22scalacheck111 = module(
    "play22scalacheck111", play2scalacheckFile :: Nil, play22
  ).settings(
    description := scalacheckDescription,
    libraryDependencies ++= Seq(
      scalacheck111,
      "org.scalaz" %% "scalaz-scalacheck-binding" % scalaz71v % "test"
    )
  )

}
