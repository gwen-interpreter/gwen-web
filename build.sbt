enablePlugins(GitVersioning)

// gwen core & web versions
val gwenVersion = "3.66.0"
val gwenWebVersion = "3.77.0"

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
  scalaVersion := "3.5.0",
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
  Seq(
    "org.seleniumhq.selenium" % "selenium-java" % "4.25.0",
    "io.github.bonigarcia" % "webdrivermanager" % "5.9.2" excludeAll(
      ExclusionRule(organization = "org.slf4j")
    )
  ) ++ mainOverrides
}

lazy val mainOverrides = {
  Seq(
    "com.fasterxml.jackson.core" %  "jackson-databind" % "2.17.2",
    "com.google.guava" % "guava" % "33.3.0-jre",
    "org.reactivestreams" % "reactive-streams" % "1.0.4",
    "org.slf4j" % "slf4j-api" % "1.7.36"
  )
}

dependencyOverrides ++= Seq(
  "com.fasterxml.jackson.core" %  "jackson-databind" % "2.17.2",
  "com.google.guava" % "guava" % "33.3.0-jre",
  "org.reactivestreams" % "reactive-streams" % "1.0.4",
  "org.slf4j" % "slf4j-api" % "1.7.36"
)

lazy val testDependencies = {
  Seq(
    "org.scalatest" %% "scalatest" % "3.2.18",
    "org.scalatestplus" %% "mockito-4-2" % "3.2.11.0",
    "org.mockito" % "mockito-core" % "4.11.0"
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
