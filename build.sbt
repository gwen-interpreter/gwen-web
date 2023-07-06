enablePlugins(GitVersioning)

// gwen core & web versions
val gwenVersion = "3.44.3"
val gwenWebVersion = "3.51.2"

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
  description := "Web automation engine for Gwen",
  organization := "org.gweninterpreter",
  organizationHomepage := Some(url("https://github.com/gwen-interpreter")),
  startYear := Some(2014),
  licenses += "Apache License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"),
  homepage := Some(url("https://gweninterpreter.org")),
  versionScheme := Some("semver-spec"),
  scalaVersion := "3.3.0",
  crossPaths := false,
  trapExit := false,
  scalacOptions ++= Seq(
    "-feature",
    "-language:postfixOps",
    "-deprecation"
  ),
  initialize := {
    val _ = initialize.value
    val javaVersion = sys.props("java.specification.version")
    if (javaVersion != "11")
      sys.error(s"JDK 11 is required to build this project. Found $javaVersion instead")
  }
)

lazy val mainDependencies = {
  val selenium = "4.10.0"
  val driverMgr = "5.3.3"
  Seq(
    "org.seleniumhq.selenium" % "selenium-java" % selenium,
    "io.github.bonigarcia" % "webdrivermanager" % driverMgr excludeAll(
      ExclusionRule(organization = "org.slf4j", name = "jcl-over-slf4j"),
      ExclusionRule(organization = "org.slf4j", name = "slf4j-api")
    )
  ) ++ mainOverrides
}

lazy val mainOverrides = {
  val bc = "1.70"
  val nettyHandler = "4.1.89.Final"
  Seq(
    "org.bouncycastle" % "bcprov-jdk15on" % bc,
    "org.bouncycastle" % "bcpkix-jdk15on" % bc,
    "io.netty" % "netty-handler" % nettyHandler
  )
}

lazy val testDependencies = {
  val scalaTest = "3.2.16"
  val scalaTestPlusMockito = "3.2.11.0"
  val mockitoCore = "4.9.0"

  Seq(
    "org.scalatest" %% "scalatest" % scalaTest,
    "org.scalatestplus" %% "mockito-4-2" % scalaTestPlusMockito,
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

Test / parallelExecution := false
