// Use file URI for gwen dep until sbt issue 1284 is fixed: https://github.com/sbt/sbt/issues/1284
lazy val gwen = ProjectRef(file("../gwen"), "gwen")
// lazy val gwen = ProjectRef(uri("git://github.com/gwen-interpreter/gwen.git"), "gwen")

val gwenWeb = project in file(".") dependsOn(gwen) 

name := "gwen-web"

description := "A Gwen automation engine for the web"

organization := "org.gweninterpreter"

organizationHomepage := Some(url("http://gweninterpreter.org"))

startYear := Some(2014)

scalaVersion := "2.11.7"

crossPaths := false

scalacOptions += "-feature"

scalacOptions += "-language:postfixOps"

scalacOptions += "-deprecation"

scalacOptions += "-target:jvm-1.7"

licenses += "Apache License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")

homepage := Some(url("https://github.com/gwen-interpreter/gwen-web"))

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

javaSource in Compile := baseDirectory.value / "src/main/scala"

javaSource in Test := baseDirectory.value / "src/test/scala"

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"

libraryDependencies += "org.mockito" % "mockito-all" % "1.10.19" % "test"

libraryDependencies += "com.google.code.findbugs" % "jsr305" % "2.0.1" % "compile" 

libraryDependencies += "org.seleniumhq.selenium" % "selenium-java" % "2.51.0" excludeAll(
  ExclusionRule(organization = "org.seleniumhq.selenium", name = "selenium-htmlunit-driver")
)

mappings in (Compile, packageBin) ++= Seq(
  file("LICENSE") -> "LICENSE",
  file("NOTICE") -> "NOTICE",
  file("LICENSE-THIRDPARTY") -> "LICENSE-THIRDPARTY",
  file("CHANGELOG") -> "CHANGELOG"
)

