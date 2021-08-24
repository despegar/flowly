import sbt.Keys._
import sbt._

object CommonSettings {

  val settings: Seq[Def.Setting[_]] =
      Seq(organization := "com.despegar.flowly",
          publishTo := Some("Nexus Despegar" at "https://backoffice-secure.despegar.com/nexus/repository/maven-releases/"),
          resolvers += Opts.resolver.mavenLocalFile,
          resolvers += Resolver.mavenLocal,
          resolvers += "Despegar Nexus Repository" at "https://backoffice-secure.despegar.com/nexus/repository/maven-all/",
          scalaVersion := "2.13.0",
          crossScalaVersions := Seq("2.12.8", "2.13.0"))
}