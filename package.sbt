import NativePackagerHelper._

enablePlugins(JavaAppPackaging)
enablePlugins(ClasspathJarPlugin)

// add general files to universal zip
Universal / mappings ++= Seq(
  file("README.md") -> "README.txt",
  file("LICENSE") -> "LICENSE.txt",
  file("NOTICE") -> "NOTICE.txt",
  file("LICENSE-THIRDPARTY") -> "LICENSE-THIRDPARTY.txt",
  file("CHANGELOG") -> "CHANGELOG.txt"
)

// include GWEN_CLASSPATH variable in app classpath of universal script
val bashClasspathPattern = "declare -r app_classpath=\"(.*)\"\n".r
bashScriptDefines := bashScriptDefines.value.map {
  case bashClasspathPattern(classpath) => "declare -r app_classpath=\"$GWEN_CLASSPATH:$SELENIUM_HOME/*:$SELENIUM_HOME/libs/*:" + classpath + "\"\n"
  case _@entry => entry
}
batScriptExtraDefines += """set "APP_CLASSPATH=%GWEN_CLASSPATH%;%SELENIUM_HOME%\*;%SELENIUM_HOME%\libs\*;%APP_CLASSPATH%""""

// Pass through -v option
bashScriptExtraDefines += """[[ " $@ " =~ " -v " ]] && addApp "-v""""

// Pass through -d option
bashScriptExtraDefines += """[[ " $@ " =~ " -d " ]] && addApp "-d""""

// add universal zip to published artifacts
val packageZip = taskKey[File]("package-zip")
packageZip := (Compile / baseDirectory).value / "target" / "universal" / (name.value + "-" + version.value + ".zip")
Universal / packageZip / artifact ~= { (art:Artifact) => art.withType("zip").withExtension("zip") }
addArtifact(Universal / packageZip / artifact, Universal / packageZip)
publish := ((publish) dependsOn (Universal / packageBin)).value
publishM2 := ((publishM2) dependsOn (Universal / packageBin)).value
publishLocal := ((publishLocal) dependsOn (Universal / packageBin)).value
PgpKeys.publishSigned := ((PgpKeys.publishSigned) dependsOn (Universal / packageBin)).value

executableScriptName := "gwen"

maintainer := "gweninterpreter.gwenify@com"
