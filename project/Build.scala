import sbt._, Keys._
import sbtrelease._
import sbtrelease.ReleasePlugin.autoImport._
import ReleaseStateTransformations._
import com.typesafe.sbt.pgp.PgpKeys
import sbtbuildinfo.BuildInfoPlugin.autoImport._

object build {

  private[this] val sonatypeURL = "https://oss.sonatype.org/service/local/repositories/"

  val tagName = Def.setting{
    s"v${if (releaseUseGlobalVersion.value) (version in ThisBuild).value else version.value}"
  }
  val tagOrHash = Def.setting{
    if(isSnapshot.value) gitHash() else tagName.value
  }

  private[this] def gitHash(): String =
    sys.process.Process("git rev-parse HEAD").lineStream_!.head

  private[this] def releaseStepAggregateCross[A](key: TaskKey[A]): ReleaseStep = ReleaseStep(
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
    git.commit(message = "update " + readme, sign = false) ! state.log
    sys.process.Process("git diff HEAD^") ! state.log
    state
  }

  private[this] val unusedWarnings = (
    "-Ywarn-unused" ::
    "-Ywarn-unused-import" ::
    Nil
  )

  private[this] val Scala211 = "2.11.12"

  val commonSettings = Seq(
    publishTo := Some(
      if (isSnapshot.value)
        Opts.resolver.sonatypeSnapshots
      else
        Opts.resolver.sonatypeStaging
    ),
    fullResolvers ~= {_.filterNot(_.name == "jcenter")},
    scalaVersion := Scala211,
    crossScalaVersions := Scala211 :: "2.12.6" :: "2.13.0-M3" :: Nil,
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
      "-Xsource:2.10" ::
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
      setNextVersion,
      commitNextVersion,
      (updateReadme: ReleaseStep),
      releaseStepCommand("sonatypeReleaseAll"),
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
    }.toList
  ) ++ Seq(Compile, Test).flatMap(c =>
    scalacOptions in (c, console) ~= {_.filterNot(unusedWarnings.toSet)}
  )

}
