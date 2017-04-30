// Use file URI for gwen dep until sbt issue 1284 is fixed: https://github.com/sbt/sbt/issues/1284
lazy val gwen = ProjectRef(file("../gwen"), "gwen")
// lazy val gwen = ProjectRef(uri("git://github.com/gwen-interpreter/gwen.git"), "gwen")

val gwenWeb = project in file(".") dependsOn(gwen) 

name := "gwen-web"

description := "A Gwen automation engine for the web"

organization := "org.gweninterpreter"

organizationHomepage := Some(url("http://gweninterpreter.org"))

startYear := Some(2014)

scalaVersion := "2.12.1"

crossPaths := false

scalacOptions += "-feature"

scalacOptions += "-language:postfixOps"

scalacOptions += "-deprecation"

scalacOptions += "-target:jvm-1.8"

licenses += "Apache License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")

homepage := Some(url("https://github.com/gwen-interpreter/gwen-web"))

javaSource in Compile := baseDirectory.value / "src/main/scala"

javaSource in Test := baseDirectory.value / "src/test/scala"

resolvers += Resolver.mavenLocal

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies += "org.seleniumhq.selenium" % "selenium-chrome-driver" % "3.4.0"

libraryDependencies += "org.seleniumhq.selenium" % "selenium-firefox-driver" % "3.4.0"

libraryDependencies += "org.seleniumhq.selenium" % "selenium-ie-driver" % "3.4.0"

libraryDependencies += "org.seleniumhq.selenium" % "selenium-safari-driver" % "3.4.0"

libraryDependencies += "org.seleniumhq.selenium" % "selenium-support" % "3.4.0" excludeAll(
  ExclusionRule(organization = "junit", name="junit")
)

libraryDependencies += "commons-io" % "commons-io" % "2.5"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

libraryDependencies += "org.mockito" % "mockito-all" % "1.10.19" % "test"

mappings in (Compile, packageBin) ++= Seq(
  file("LICENSE") -> "LICENSE",
  file("NOTICE") -> "NOTICE",
  file("LICENSE-THIRDPARTY") -> "LICENSE-THIRDPARTY",
  file("CHANGELOG") -> "CHANGELOG"
)

