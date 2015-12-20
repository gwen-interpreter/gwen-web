enablePlugins(JavaAppPackaging)

enablePlugins(JDKPackagerPlugin)

mainClass in Compile := Some("gwen.web.WebInterpreter")

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

lazy val iconGlob = sys.props("os.name").toLowerCase match {
  case os if os.contains("mac") ⇒ "*.icns"
  case os if os.contains("win") ⇒ "*.ico"
  case _ ⇒ "*.png"
}

maintainer := "Gwen Interpreter, Org"

packageSummary := "gwen-web installer"

packageDescription := "Package which installs gwen-web, ready to run Given When thEN on the web"

jdkAppIcon :=  (sourceDirectory.value ** iconGlob).getPaths.headOption.map(file)

jdkPackagerType := "installer"

jdkPackagerJVMArgs := Seq("-Xmx1g")

jdkPackagerProperties := Map("app.name" -> name.value, "app.version" -> version.value)

jdkPackagerAssociations := Seq(
  FileAssociation("feature", "application/gwen", "Gwen feature file", jdkAppIcon.value)
)

// this is to help ubuntu 15.10
//antPackagerTasks in JDKPackager := (antPackagerTasks in JDKPackager).value orElse {
//  for {
//    f <- Some(file("/usr/lib/jvm/java-8-oracle/lib/ant-javafx.jar")) if f.exists()
//  } yield f
//}

//fork := true

