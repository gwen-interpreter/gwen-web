enablePlugins(JDKPackagerPlugin)

import com.typesafe.sbt.SbtGit._

import com.typesafe.sbt.packager.archetypes._

// Use file URI for gwen dep until sbt issue 1284 is fixed: https://github.com/sbt/sbt/issues/1284
lazy val gwen = ProjectRef(file("../gwen"), "gwen")
// lazy val gwen = ProjectRef(uri("git://github.com/gwen-interpreter/gwen.git"), "gwen")

val gwenWeb = project in file(".") dependsOn(gwen) 

name := "gwen-web"

version := "1.0.0"

description := "An acceptance driven web automation engine."

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

homepage := Some(url("http://gwen-interpreter.github.io/gwen-web/"))

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

javaSource in Compile := baseDirectory.value / "src/main/scala"

javaSource in Test := baseDirectory.value / "src/test/scala"

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"

libraryDependencies += "org.mockito" % "mockito-all" % "1.10.19" % "test"

libraryDependencies += "com.google.code.findbugs" % "jsr305" % "2.0.1" % "compile" 

libraryDependencies += "org.seleniumhq.selenium" % "selenium-java" % "2.48.2" excludeAll(
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

lazy val iconGlob = sys.props("os.name").toLowerCase match {
  case os if os.contains("mac") ⇒ "*.icns"
  case os if os.contains("win") ⇒ "*.ico"
  case _ ⇒ "*.png"
}

maintainer := "Branko Juric and Brady Wood"
packageSummary := "Gwen installer"
packageDescription := "Package which installs gwen, ready to run Given When thEN."

jdkAppIcon :=  (sourceDirectory.value ** iconGlob).getPaths.headOption.map(file)

jdkPackagerType := "installer"

jdkPackagerJVMArgs := Seq("-Xmx1g")

jdkPackagerProperties := Map("app.name" -> name.value, "app.version" -> version.value)

jdkPackagerAppArgs := Seq(maintainer.value, packageSummary.value, packageDescription.value)

jdkPackagerAssociations := Seq(
  FileAssociation("gwen", "application/gwen", "Gwen file type", jdkAppIcon.value)
)

// this is to help ubuntu 15.10
//antPackagerTasks in JDKPackager := (antPackagerTasks in JDKPackager).value orElse {
//  for {
//    f <- Some(file("/usr/lib/jvm/java-8-oracle/lib/ant-javafx.jar")) if f.exists()
//  } yield f
//}

//fork := true
