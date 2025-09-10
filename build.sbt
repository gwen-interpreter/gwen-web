enablePlugins(GitVersioning)

git.baseVersion := "4.14.2"
git.useGitDescribe := true

val gwenWeb = (project in file("."))
  .settings(
    projectSettings,
    libraryDependencies ++= mainDependencies ++ testDependencies
  )

resolvers ++= Seq(
  "Sonatype Snapshots" at "https://central.sonatype.com/repository/maven-snapshots/"
)

lazy val projectSettings = Seq(
  name := "gwen-web",
  description := "Web automation engine for Gwen",
  organization := "org.gweninterpreter",
  organizationHomepage := Some(url("https://github.com/gwen-interpreter")),
  startYear := Some(2014),
  licenses += "Apache License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"),
  homepage := Some(url("https://gweninterpreter.org")),
  versionScheme := Some("semver-spec"),
  scalaVersion := "3.7.1",
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
    if (javaVersion != "17")
      sys.error(s"Java 17 is required to build this project. Found $javaVersion instead")
  }
)

lazy val mainDependencies = {
  Seq(
    "org.gweninterpreter" % "gwen" % "4.10.2",
    "org.seleniumhq.selenium" % "selenium-java" % "4.35.0" excludeAll(
      ExclusionRule("org.seleniumhq.selenium", "selenium-ie-driver")
    )
  ) ++ mainOverrides
}

lazy val mainOverrides = {
  Seq(
  )
}

lazy val testDependencies = {
  Seq(
    "org.scalatest" %% "scalatest" % "3.2.19",
    "org.scalatestplus" %% "mockito-5-12" % "3.2.19.0",
    "org.mockito" % "mockito-core" % "5.18.0"
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

Test / testOptions += Tests.Argument("-oF")
