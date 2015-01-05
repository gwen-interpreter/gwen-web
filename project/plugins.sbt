// sbt-pgp uses BouncyCastle 1.49, sbt-native-packager uses 1.5.1, force former in latter to avoid conflict in publishSigned task
dependencyOverrides += "org.bouncycastle" % "bcpg-jdk15on" % "1.49"

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.5.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "0.8.0-RC2")

resolvers += Resolver.url("bintray-sbt-plugins", url("https://dl.bintray.com/sbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)

resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.6.4")

addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.3")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "0.2.1")