import sbtrelease._
import xerial.sbt.Sonatype.sonatypeCentralHost

// we hide the existing definition for setReleaseVersion to replace it with our own
import sbtrelease.ReleaseStateTransformations.{setReleaseVersion=>_,_}

def setVersionOnly(selectVersion: Versions => String): ReleaseStep =  { st: State =>
  val vs = st.get(ReleaseKeys.versions).getOrElse(sys.error("No versions are set! Was this release part executed before inquireVersions?"))
  val selected = selectVersion(vs)

  st.log.info("Setting version to '%s'." format selected)
  val useGlobal =Project.extract(st).get(releaseUseGlobalVersion)
  val versionStr = (if (useGlobal) globalVersionString else versionString) format selected

  reapply(Seq(
    if (useGlobal) ThisBuild / version := selected
    else version := selected
  ), st)
}

lazy val setReleaseVersion: ReleaseStep = setVersionOnly(_._1)

releaseVersion := { ver =>
  Version(ver)
    .map(_.withoutQualifier)
    .map(_.bump(releaseVersionBump.value).unapply).getOrElse(versionFormatError(ver))
}

releaseCrossBuild := false
releasePublishArtifactsAction := PgpKeys.publishSigned.value
ThisBuild / sonatypeCredentialHost := sonatypeCentralHost

ThisBuild / publishTo := {
  if (isSnapshot.value) Some("central-snapshots" at "https://central.sonatype.com/repository/maven-snapshots/")
  else sonatypePublishToBundle.value
}

releaseProcess := Seq(
  checkSnapshotDependencies,
  inquireVersions,
  setReleaseVersion,
  runClean,
  runTest,
  tagRelease,
  publishArtifacts,
  releaseStepCommandAndRemaining("publishSigned"),
  releaseStepCommand("sonatypeBundleRelease"),
  pushChanges
)
