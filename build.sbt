lazy val root = (project in file("."))
  .settings(publishArtifact := false)
  .settings(CommonSettings.settings: _*)
  .settings(
    name := "flowly",
  )
  .aggregate(`flowly-core`, `flowly-mongodb`)

val jacksonVersion = "2.9.9"

lazy val `flowly-core` = project
  .settings(CommonSettings.settings: _*)
  .settings(
    name := "flowly-core",
    libraryDependencies ++= Seq(
      "org.specs2" %% "specs2-core" % "4.6.0" % "test",
      "org.specs2" %% "specs2-mock" % "4.6.0" % "test",
      "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion % "test",
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jacksonVersion % "test",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion % "test",
      "org.scalacheck" %% "scalacheck" % "1.14.0" % "test"
    )
  )

lazy val `flowly-mongodb` = project
  .settings(CommonSettings.settings: _*)
  .settings(
    name := "flowly-mongodb",
    libraryDependencies ++= Seq(
      "org.mongojack" % "mongojack" % "2.10.0",
      "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jacksonVersion,
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion
    )
  )
  .dependsOn(`flowly-core`)

scalacOptions in Test ++= Seq("-Yrangepos")