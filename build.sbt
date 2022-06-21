enablePlugins(GitVersioning)

// gwen core & web versions
val gwenVersion = "3.16.2"
val gwenWebVersion = "3.17.3"

git.baseVersion := gwenWebVersion
git.useGitDescribe := true

lazy val gwenSrc = ProjectRef(file("../gwen"), "gwen")
lazy val gwenLib = "org.gweninterpreter" % "gwen" % gwenVersion

val gwenWeb = (project in file("."))
  .sourceDependency(gwenSrc, gwenLib)
  .settings(
    projectSettings,
    libraryDependencies ++= mainDependencies ++ testDependencies
  )

resolvers ++= Seq(
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
)

lazy val projectSettings = Seq(
  name := "gwen-web",
  description := "Web Automation and Robotics for Gherkin",
  organization := "org.gweninterpreter",
  organizationHomepage := Some(url("https://github.com/gwen-interpreter")),
  startYear := Some(2014),
  licenses += "Apache License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"),
  homepage := Some(url("https://gweninterpreter.org")),
  versionScheme := Some("semver-spec"),
  scalaVersion := "3.1.2",
  crossPaths := false,
  trapExit := false,
  scalacOptions ++= Seq(
    "-feature",
    "-language:postfixOps",
    "-deprecation",
    "-Xtarget:8"
  ),
  initialize := {
    val _ = initialize.value
    val javaVersion = sys.props("java.specification.version")
    if (javaVersion != "1.8")
      sys.error(s"JDK 8 is required to build this project. Found $javaVersion instead")
  }
)

lazy val mainDependencies = {
  val selenium = "4.2.2"
  val driverMgr = "5.2.0"

  Seq(
    "org.seleniumhq.selenium" % "selenium-java" % selenium,
    "io.github.bonigarcia" % "webdrivermanager" % driverMgr
  )
}

lazy val testDependencies = {
  val scalaTest = "3.2.11"
  val scalaTestPlusMockito = "3.2.10.0"
  val mockitoCore = "3.12.4"

  Seq(
    "org.scalatest" %% "scalatest" % scalaTest,
    "org.scalatestplus" %% "mockito-3-4" % scalaTestPlusMockito,
    "org.mockito" % "mockito-core" % mockitoCore
  ).map(_ % Test)
}

Compile / packageBin / mappings ++= Seq(
  file("README.md") -> "README.txt",
  file("LICENSE") -> "LICENSE.txt",
  file("NOTICE") -> "NOTICE.txt",
  file("LICENSE-THIRDPARTY") -> "LICENSE-THIRDPARTY.txt",
  file("CHANGELOG") -> "CHANGELOG.txt"
)

Test/ parallelExecution := false
