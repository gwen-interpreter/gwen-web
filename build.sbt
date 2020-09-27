
// Use file URI for gwen dep until sbt issue 1284 is fixed: https://github.com/sbt/sbt/issues/1284
lazy val gwen = ProjectRef(file("../gwen"), "root")
// lazy val gwen = ProjectRef(uri("git://github.com/gwen-interpreter/gwen.git"), "gwen")

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
  scalaVersion := "2.13.3",
  crossPaths := false,
  trapExit := false,
  scalacOptions ++= Seq(
    "-feature",
    "-language:postfixOps",
    "-deprecation",
    "-target:8",
    "-Xlint:_,-missing-interpolator"
  ),
  initialize := {
    val _ = initialize.value
    val javaVersion = sys.props("java.specification.version")
    if (javaVersion != "1.8")
      sys.error(s"JDK 8 is required to build this project. Found $javaVersion instead")
  }
)

lazy val mainDependencies = {
  val commonsIO = "2.8.0"
  val selenium = "3.141.59"
  val appliTools = "3.178.0"
  val driverMgr = "4.2.2"

  Seq(
    "commons-io" % "commons-io" % commonsIO,
    "org.seleniumhq.selenium" % "selenium-chrome-driver" % selenium,
    "org.seleniumhq.selenium" % "selenium-firefox-driver" % selenium,
    "org.seleniumhq.selenium" % "selenium-edge-driver" % selenium,
    "org.seleniumhq.selenium" % "selenium-ie-driver" % selenium,
    "org.seleniumhq.selenium" % "selenium-safari-driver" % selenium,
    "org.seleniumhq.selenium" % "selenium-support" % selenium excludeAll ExclusionRule(organization = "junit", name = "junit"),
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
  val scalaTest = "3.0.9"
  val mockitoAll = "1.10.19"

  Seq(
    "org.scalatest" %% "scalatest" % scalaTest,
    "org.mockito" % "mockito-all" % mockitoAll
  ).map(_ % Test)
}

val gwenWeb = (project in file(".")).settings(
  projectSettings,
  libraryDependencies ++= mainDependencies ++ testDependencies
) dependsOn gwen

mappings in(Compile, packageBin) ++= Seq(
  file("README.md") -> "README.txt",
  file("LICENSE") -> "LICENSE.txt",
  file("NOTICE") -> "NOTICE.txt",
  file("LICENSE-THIRDPARTY") -> "LICENSE-THIRDPARTY.txt",
  file("CHANGELOG") -> "CHANGELOG.txt"
)
