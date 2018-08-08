import sbt.Keys.{description, homepage}

// Use file URI for gwen dep until sbt issue 1284 is fixed: https://github.com/sbt/sbt/issues/1284
lazy val gwen = ProjectRef(file("../gwen"), "root")
// lazy val gwen = ProjectRef(uri("git://github.com/gwen-interpreter/gwen.git"), "gwen")

resolvers ++= Seq(
  "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
)

lazy val gwenWebSettings = Seq(
  name := "gwen-web",
  description := "A Gwen automation engine for the web",
  organization := "org.gweninterpreter",
  organizationHomepage := Some(url("http://gweninterpreter.org")),
  startYear := Some(2014),
  scalaVersion := "2.12.6",
  crossPaths := false,
  trapExit := false,
  scalacOptions ++= Seq(
    "-feature",
    "-language:postfixOps",
    "-deprecation",
    "-target:jvm-1.8"
  ),
  licenses += "Apache License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"),
  homepage := Some(url("https://github.com/gwen-interpreter/gwen-web"))
)

lazy val commonDependencies = {
  val commonsIO = "2.6"
  val selenium = "3.14.0"

  Seq(
    "commons-io" % "commons-io" % commonsIO,
    "org.seleniumhq.selenium" % "selenium-chrome-driver" % selenium,
    "org.seleniumhq.selenium" % "selenium-firefox-driver" % selenium,
    "org.seleniumhq.selenium" % "selenium-ie-driver" % selenium,
    "org.seleniumhq.selenium" % "selenium-safari-driver" % selenium,
    "org.seleniumhq.selenium" % "selenium-support" % selenium excludeAll ExclusionRule(organization = "junit", name = "junit")
  )
}

lazy val testDependencies = {
  val scalaTest = "3.0.5"
  val mockitoAll = "1.10.19"

  Seq(
    "org.scalatest" %% "scalatest" % scalaTest,
    "org.mockito" % "mockito-all" % mockitoAll
  ).map(_ % Test)
}

val gwenWeb = (project in file(".")).settings(
  gwenWebSettings,
  libraryDependencies ++= commonDependencies ++ testDependencies
) dependsOn gwen

mappings in(Compile, packageBin) ++= Seq(
  file("README.md") -> "README.txt",
  file("LICENSE") -> "LICENSE.txt",
  file("NOTICE") -> "NOTICE.txt",
  file("LICENSE-THIRDPARTY") -> "LICENSE-THIRDPARTY.txt",
  file("CHANGELOG") -> "CHANGELOG.txt"
)

