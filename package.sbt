enablePlugins(JavaAppPackaging)
enablePlugins(ClasspathJarPlugin)

// add general files to universal zip
mappings in Universal += file("README.md") -> "README.txt"
mappings in Universal += file("LICENSE") -> "LICENSE.txt"
mappings in Universal += file("NOTICE") -> "NOTICE.txt"
mappings in Universal += file("LICENSE-THIRDPARTY") -> "LICENSE-THIRDPARTY.txt"
mappings in Universal += file("CHANGELOG") -> "CHANGELOG.txt"

// include GWEN_CLASSPATH variable in app classpath of universal script
val bashClasspathPattern = "declare -r app_classpath=\"(.*)\"\n".r
bashScriptDefines := bashScriptDefines.value.map {
  case bashClasspathPattern(classpath) => "declare -r app_classpath=\"$GWEN_CLASSPATH:$SELENIUM_HOME/*:$SELENIUM_HOME/libs/*:" + classpath + "\"\n"
  case _@entry => entry
}
batScriptExtraDefines += """set "APP_CLASSPATH=%GWEN_CLASSPATH%;%SELENIUM_HOME%\*;%SELENIUM_HOME%\libs\*;%APP_CLASSPATH%""""

// Set common pool parallelism to zero in parallel mode (required for Selenium 4)
bashScriptExtraDefines += """[[ "$*" == *--parallel* ]] && addJava "-Djava.util.concurrent.ForkJoinPool.common.parallelism=0""""
batScriptExtraDefines += """
  |set args="%*"
  |set args=%args:"=%
  |set args=%args:&=%
  |set args=%args:<=%
  |set args=%args:>=%
  |set args=%args:|=%
  |if not "x%args:--parallel=%" == "x%args%" (call :add_java "-Djava.util.concurrent.ForkJoinPool.common.parallelism=0")
  |""".stripMargin

// add universal zip to published artifacts
val packageZip = taskKey[File]("package-zip")
packageZip := (baseDirectory in Compile).value / "target" / "universal" / (name.value + "-" + version.value + ".zip")
artifact in (Universal, packageZip) ~= { (art:Artifact) => art.withType("zip").withExtension("zip") }
addArtifact(artifact in (Universal, packageZip), packageZip in Universal)
publish := ((publish) dependsOn (packageBin in Universal)).value
publishM2 := ((publishM2) dependsOn (packageBin in Universal)).value
publishLocal := ((publishLocal) dependsOn (packageBin in Universal)).value
PgpKeys.publishSigned := ((PgpKeys.publishSigned) dependsOn (packageBin in Universal)).value
