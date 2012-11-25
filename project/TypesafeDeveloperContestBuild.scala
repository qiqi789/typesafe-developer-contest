import sbt._
import sbt.Keys._

object TypesafeDeveloperContestBuild extends Build {

  lazy val typesafeDeveloperContest = Project(
    id = "typesafe-developer-contest",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name := "Typesafe Developer Contest",
      organization := "org.contest",
      version := "1.0",
      scalaVersion := "2.9.2",
      // add other settings here
      resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases",
      libraryDependencies ++= Seq("com.typesafe.akka" % "akka-actor" % "2.0.3"
        )))
}
