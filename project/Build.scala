import sbt._, Keys._
import xerial.sbt.Sonatype
import sbtrelease._
import sbtrelease.ReleasePlugin.autoImport._
import ReleaseStateTransformations._
import com.typesafe.sbt.pgp.PgpKeys
import sbtbuildinfo.Plugin._
import scalaprops.ScalapropsPlugin.autoImport._

object build extends Build {

  private[this] val sonatypeURL = "https://oss.sonatype.org/service/local/repositories/"

  private[this] def gitHash: String = scala.util.Try(
    sys.process.Process("git rev-parse HEAD").lines_!.head
  ).getOrElse("master")

  private[this] def releaseStepAggregateCross[A](key: TaskKey[A]): ReleaseStep = ReleaseStep(
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
    val modules = projects.map(p => extracted get (name in p))
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

  private[this] val unusedWarnings = (
    "-Ywarn-unused" ::
    "-Ywarn-unused-import" ::
    Nil
  )

  private[this] val Scala211 = "2.11.7"

  val commonSettings = (
    Sonatype.sonatypeSettings ++
    buildInfoSettings ++
    scalapropsWithScalazlaws
  ) ++ Seq(
    scalaVersion := Scala211,
    crossScalaVersions := "2.10.5" :: Scala211 :: Nil,
    organization := "com.github.xuwei-k",
    licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
    commands += Command.command("updateReadme")(updateReadme),
    scalapropsVersion := "0.1.13",
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
    scalacOptions ++= (
      "-deprecation" ::
      "-unchecked" ::
      "-Xlint" ::
      "-language:existentials" ::
      "-language:higherKinds" ::
      "-language:implicitConversions" ::
      Nil
    ),
    scalacOptions ++= PartialFunction.condOpt(CrossVersion.partialVersion(scalaVersion.value)){
      case Some((2, v)) if v >= 11 =>
        "-Xsource:2.10" :: unusedWarnings
    }.toList.flatten,
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
      setNextVersion,
      commitNextVersion,
      (updateReadme: ReleaseStep),
      releaseStepAggregateCross(Sonatype.SonatypeKeys.sonatypeReleaseAll),
      pushChanges
    ),
    sourceGenerators in Compile <+= buildInfo,
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
    }.toList
  ) ++ Seq(Compile, Test).flatMap(c =>
    scalacOptions in (c, console) ~= {_.filterNot(unusedWarnings.toSet)}
  )

  lazy val play2scalaz = Project("play2scalaz", file(".")).settings(
    commonSettings
  ).settings(
    name := "play2scalaz",
    libraryDependencies += "com.typesafe.play" %% "play-json" % "2.4.0",
    libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.1.3",
    buildInfoPackage := "play2scalaz",
    buildInfoObject := "Play2ScalazBuildInfo",
    description := "play framework2 and scalaz typeclasses converters"
  )

}
