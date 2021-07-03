lazy val gwenSrc = ProjectRef(file("../gwen"), "gwen")
lazy val gwenLib = "org.gweninterpreter" % "gwen" % "2.32.0"

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
  organizationHomepage := Some(url("http://gweninterpreter.org")),
  startYear := Some(2014),
  licenses += "Apache License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"),
  homepage := Some(url("https://github.com/gwen-interpreter/gwen")),
  scalaVersion := "3.0.0",
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
  val selenium = "3.141.59"
  val seleniumEdge = "3.141.0"
  val appliTools = "3.204.1"
  val driverMgr = "4.4.3"

  Seq(
    "org.seleniumhq.selenium" % "selenium-chrome-driver" % selenium,
    "org.seleniumhq.selenium" % "selenium-firefox-driver" % selenium,
    "org.seleniumhq.selenium" % "selenium-edge-driver" % selenium,
    "org.seleniumhq.selenium" % "selenium-ie-driver" % selenium,
    "org.seleniumhq.selenium" % "selenium-safari-driver" % selenium,
    "org.seleniumhq.selenium" % "selenium-support" % selenium excludeAll ExclusionRule(organization = "junit", name = "junit"),
    "com.microsoft.edge" % "msedge-selenium-tools-java" % seleniumEdge,
    "io.github.bonigarcia" % "webdrivermanager" % driverMgr,
    "com.applitools" % "eyes-selenium-java3" % appliTools excludeAll(
      ExclusionRule(organization = "org.apache.ant", name = "ant"),
      ExclusionRule(organization = "org.aspectj", name = "aspectjweaver"),
      ExclusionRule(organization = "org.openpnp", name = "opencv"),
      ExclusionRule(organization = "org.seleniumhq.selenium", name = "selenium-java"),
      ExclusionRule(organization = "org.springframework", name = "spring-context")
     )
  )
}

lazy val testDependencies = {
  val scalaTest = "3.2.9"
  val scalaTestPlusMockito = "3.2.9.0"
  val mockitoCore = "3.11.1"

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
