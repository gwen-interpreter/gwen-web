import NativePackagerHelper._
import java.nio.file.Files
import java.security.MessageDigest
import scala.util.chaining._

enablePlugins(JavaAppPackaging)
enablePlugins(ClasspathJarPlugin)

val thinPackage = sys.env.get("THIN_PACKAGE").map(_.toBoolean).getOrElse(true)

Universal / mappings := {
  (Universal / mappings).value.filter { case (file, name) => 
    if (thinPackage) !name.startsWith("lib") || name.contains("gwen-web") else true
  }
}

// add general files to universal zip
Universal / mappings ++= Seq(
  Some(file("README.md") -> "README.txt"),
  Some(file("LICENSE") -> "LICENSE.txt"),
  Some(file("NOTICE") -> "NOTICE.txt"),
  Some(((Compile / dumpLicenseReport).value / "gwen-web-licenses.md") -> "LICENSE-THIRDPARTY.txt"),
  Some(file("CHANGELOG") -> "CHANGELOG.txt"),
  if (thinPackage) { 
    val configs = (Compile / update).value.configuration(ConfigRef("compile"))
    val deps = configs.toSeq flatMap { config => 
      config.modules flatMap { moduleConfig => 
        moduleConfig.artifacts map { case (artifact, file) =>
          val m = moduleConfig.module
          val buff = new Array[Byte](8192)
          val md = MessageDigest.getInstance("SHA-1")
          val fis = Files.newInputStream(file.toPath)
          try {
            while((fis.read(buff) tap { b => if (b != -1) md.update(buff, 0, b) }) != -1) { }
          } finally {
            fis.close()
          }
          val hash = md.digest().map("%02x".format(_)).mkString
          s"""{ "groupId": "${m.organization}", "artifactId": "${m.name}", "version": "${m.revision}", "packaging": "jar", "sha1": "$hash" }"""
        }
      }
    }
    val depsFile = (target.value / "dependencies.json") tap { file =>
      IO.write(file, s"""|[
                         |  ${deps.sorted.mkString(",\n  ")}
                         |]""".stripMargin)
    }
    Some(depsFile -> "DEPENDENCIES.json")
  } else None
).flatten

Universal / javaOptions ++= Seq(
  "-J-XX:+IgnoreUnrecognizedVMOptions",
  "-J--enable-native-access=ALL-UNNAMED",
  "-J--sun-misc-unsafe-memory-access=allow"
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
