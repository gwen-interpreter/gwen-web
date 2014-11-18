import com.typesafe.sbt.packager.archetypes._

name := "gwen-web"

description := "A Gherkin evaluation engine for automating online activities and web application testing."

version := "0.1.0-SNAPSHOT"

organization := "org.gweninterpreter"

organizationHomepage := Some(url("http://gweninterpreter.org"))

startYear := Some(2014)

scalaVersion := "2.11.2"

crossScalaVersions := Seq("2.11.2", "2.10.4")

scalacOptions += "-feature"

scalacOptions += "-language:postfixOps"

scalacOptions += "-deprecation"

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomIncludeRepository := { _ => false }

publishArtifact in Test := false

licenses += "Apache License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")

homepage := Some(url("http://gwen-interpreter.github.io/gwen-web/"))

pomExtra := (
  <scm>
    <connection>scm:git:git@github.com:gwen-interpreter/gwen-web.git</connection>
    <developerConnection>scm:git:git@github.com:gwen-interpreter/gwen-web.git</developerConnection>
    <url>git@github.com:gwen-interpreter/gwen-web.git</url>
  </scm>
  <developers>
    <developer>
      <id>bjuric</id>
      <name>Branko Juric</name>
      <url>https://github.com/bjuric</url>
    </developer>
    <developer>
      <id>bradywood</id>
      <name>Brady Wood</name>
      <url>https://github.com/bradywood</url>
    </developer>
  </developers>)

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

javaSource in Compile := baseDirectory.value / "src/main/scala"

javaSource in Test := baseDirectory.value / "src/test/scala"

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies += "org.gweninterpreter" %% "gwen" % "0.1.0-SNAPSHOT" withSources()

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.1" % "test"

libraryDependencies += "org.mockito" % "mockito-all" % "1.9.5" % "test"

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

packageArchetype.java_application

val packageZip = taskKey[File]("package-zip")

packageZip := (baseDirectory in Compile).value / "target" / "universal" / (name.value + "-" + version.value + ".zip")

artifact in (Universal, packageZip) ~= { (art:Artifact) => art.copy(`type` = "zip", extension = "zip") }

addArtifact(artifact in (Universal, packageZip), packageZip in Universal)

publish <<= (publish) dependsOn (packageBin in Universal)

publishM2 <<= (publishM2) dependsOn (packageBin in Universal)

publishLocal <<= (publishLocal) dependsOn (packageBin in Universal)

PgpKeys.publishSigned <<= (PgpKeys.publishSigned) dependsOn (packageBin in Universal)

mappings in Universal += file("LICENSE") -> "LICENSE" 

mappings in Universal += file("NOTICE") -> "NOTICE" 

mappings in Universal <++= (packageBin in Compile, target ) map { (_, target) =>
  val dir = file("./features")
  (dir.***) pair relativeTo(dir.getParentFile)
}

mappings in Universal <++= (com.typesafe.sbt.packager.Keys.makeBashScript in Universal, normalizedName in Universal) map { (script, name) =>
  for {
    s <- script.toSeq
  } yield s -> ("bin/gwen") 
}

mappings in Universal <++= (com.typesafe.sbt.packager.Keys.makeBatScript in Universal, normalizedName in Universal) map { (script, name) =>
  for {
    s <- script.toSeq
  } yield s -> ("bin/gwen.bat") 
}
