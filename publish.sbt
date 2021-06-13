publishMavenStyle := true
pomIncludeRepository := { _ => false }
Test / publishArtifact := false

pomExtra := (
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
