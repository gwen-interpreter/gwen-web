
name := "gwen-web"

description := "Gwen Web Engine"

version := "0.1.0-SNAPSHOT"

organization := "org.gweninterpreter"

organizationHomepage := Some(url("http://gweninterpreter.org"))

startYear := Some(2014)

scalaVersion := "2.11.1"

crossScalaVersions := Seq("2.11.1", "2.10.4")

scalacOptions += "-feature"

scalacOptions += "-language:postfixOps"

licenses += "Apache License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

javaSource in Compile := baseDirectory.value / "src/main/scala"

javaSource in Test := baseDirectory.value / "src/test/scala"

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies += "org.gweninterpreter" %% "gwen" % "0.1.0-SNAPSHOT" withSources()

libraryDependencies += "org.scalatest" %% "scalatest" % "2.1.7" % "test"

libraryDependencies += "org.mockito" % "mockito-all" % "1.9.5" % "test"

libraryDependencies += "org.seleniumhq.selenium" % "selenium-java" % "2.42.1" excludeAll(
  ExclusionRule(organization = "org.seleniumhq.selenium", name = "selenium-htmlunit-driver"),
  ExclusionRule(organization = "net.java.dev.jna", name = "jna"),
  ExclusionRule(organization = "net.java.dev.jna", name = "jna-platform")
)

libraryDependencies += "net.java.dev.jna" % "jna" % "4.1.0"

libraryDependencies += "net.java.dev.jna" % "jna-platform" % "4.1.0"

packageArchetype.java_application

mappings in (Compile, packageBin) ++= Seq(
  file("LICENSE") -> "LICENSE",
  file("NOTICE") -> "NOTICE"
)

mappings in Universal += file("LICENSE") -> "LICENSE" 

mappings in Universal += file("NOTICE") -> "NOTICE" 

mappings in Universal <++= (packageBin in Compile, target ) map { (_, target) =>
  val dir = file("src/test/resources/features")
  (dir.***) pair relativeTo(dir.getParentFile)
}
