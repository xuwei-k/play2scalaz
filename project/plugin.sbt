scalacOptions ++= Seq("-deprecation", "-unchecked", "-language:_")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.23")

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.4.0")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.1")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.8")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.3")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")

addSbtPlugin("com.github.scalaprops" % "sbt-scalaprops" % "0.2.5")

fullResolvers ~= {_.filterNot(_.name == "jcenter")}
