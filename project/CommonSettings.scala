import sbt.Keys._
import sbt._

object CommonSettings {

  val settings: Seq[Def.Setting[_]] =
      Seq(organization := "com.despegar.flowly",
          publishTo := Some("Nexus Despegar" at s"https://cnd-backend/nexus/repository/${if (isSnapshot.value) "maven-snapshots" else "maven-releases"}/"),
          resolvers += Opts.resolver.mavenLocalFile,
          resolvers += Resolver.mavenLocal,
          resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/",
          resolvers += "Nexus Public Repository" at "https://cnd-backend/nexus/repository/maven-all/",
          scalaVersion := "2.13.0",
          crossScalaVersions := Seq("2.12.8", "2.13.0"))
}