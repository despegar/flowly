import sbt.Keys._
import sbt._

object CommonSettings {

  val settings: Seq[Def.Setting[_]] =
      Seq(organization := "com.despegar.flowly",
          publishTo := Some("Nexus Despegar" at s"http://nexus:8080/nexus/content/repositories/${if (isSnapshot.value) "snapshots" else "releases"}"),
          resolvers += Opts.resolver.mavenLocalFile,
          resolvers += Resolver.mavenLocal,
          resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/",
          resolvers += "Nexus Public Repository" at "http://nexus:8080/nexus/content/groups/public",
          resolvers += "Nexus Snapshots Repository" at "http://nexus:8080/nexus/content/repositories/snapshots",
          resolvers += "Nexus Proxies Repository" at "http://nexus:8080/nexus/content/groups/proxies",
          scalaVersion := "2.13.0",
          crossScalaVersions := Seq("2.12.8", "2.13.0"))
}