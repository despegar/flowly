lazy val root = (project in file("."))
  .settings(publishArtifact := false)
  .settings(CommonSettings.settings: _*)
  .settings(
    name := "flowly",
  )
  .aggregate(`flowly-core`, `flowly-mongodb`, `flowly-demo`)

val jacksonVersion = "2.13.0"

lazy val `flowly-core` = project
  .settings(CommonSettings.settings: _*)
  .settings(
    name := "flowly-core",
    libraryDependencies ++= Seq(
      "org.specs2" %% "specs2-core" % "4.12.12" % "test",
      "org.specs2" %% "specs2-mock" % "4.12.12" % "test",
      "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jacksonVersion,
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion
    )
  )

lazy val `flowly-mongodb` = project
  .settings(CommonSettings.settings: _*)
  .settings(
    name := "flowly-mongodb",
    libraryDependencies ++= Seq(
      "org.mongojack" % "mongojack" % "4.3.0",
      "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jacksonVersion,
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion
    )
  )
  .dependsOn(`flowly-core`)

lazy val `flowly-demo` = project
    .settings(CommonSettings.settings: _*)
    .settings(
      name := "flowly-demo",
      packagedArtifacts := Map.empty
    )
    .dependsOn(`flowly-core`, `flowly-mongodb`)

scalacOptions in Test ++= Seq("-Yrangepos")