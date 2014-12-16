import com.typesafe.sbt.SbtGit._

import com.typesafe.sbt.packager.archetypes._

import SonatypeKeys._

name := "gwen-web"

description := "An acceptance driven web automation engine."

organization := "org.gweninterpreter"

organizationHomepage := Some(url("http://gweninterpreter.org"))

startYear := Some(2014)

scalaVersion := "2.11.4"

crossScalaVersions := Seq("2.11.4", "2.10.4")

scalacOptions += "-feature"

scalacOptions += "-language:postfixOps"

scalacOptions += "-deprecation"

licenses += "Apache License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")

homepage := Some(url("http://gwen-interpreter.github.io/gwen-web/"))

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

javaSource in Compile := baseDirectory.value / "src/main/scala"

javaSource in Test := baseDirectory.value / "src/test/scala"

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies += "org.gweninterpreter" %% "gwen" % "latest.integration" withSources()

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.1" % "test"

libraryDependencies += "org.mockito" % "mockito-all" % "1.10.14" % "test"

libraryDependencies += "com.google.code.findbugs" % "jsr305" % "2.0.1" % "compile" 

libraryDependencies += "org.seleniumhq.selenium" % "selenium-java" % "2.44.0" excludeAll(
  ExclusionRule(organization = "org.seleniumhq.selenium", name = "selenium-htmlunit-driver"),
  ExclusionRule(organization = "net.java.dev.jna", name = "jna"),
  ExclusionRule(organization = "net.java.dev.jna", name = "jna-platform")
)

libraryDependencies += "net.java.dev.jna" % "jna" % "4.1.0"

libraryDependencies += "net.java.dev.jna" % "jna-platform" % "4.1.0"

mappings in (Compile, packageBin) ++= Seq(
  file("LICENSE") -> "LICENSE",
  file("NOTICE") -> "NOTICE"
)
