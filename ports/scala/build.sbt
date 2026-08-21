ThisBuild / scalaVersion := "3.3.4"
ThisBuild / organization := "dev.wstein.flixlab"

lazy val root = (project in file("."))
  .settings(
    name := "allconstructs-scala",
    version := "0.1.0",
    Compile / mainClass := Some("dev.wstein.flixlab.allconstructs.Main"),
    libraryDependencies += "org.scalameta" %% "munit" % "1.0.2" % Test,
    testFrameworks += new TestFramework("munit.Framework")
  )
