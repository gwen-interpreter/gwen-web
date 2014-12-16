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