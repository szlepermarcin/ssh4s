import CommonSettings._
logLevel := Level.Debug

val fs2Version = "2.1.0"
val catsVersion = "2.0.0"
val catsEffectVersion = "2.0.0"

lazy val modules: List[ProjectReference] = List(
  core
)

lazy val catsDependencies = Seq(
  "org.typelevel" %% "cats-core" % catsVersion,
  "org.typelevel" %% "cats-effect" % catsEffectVersion
)

lazy val fs2Dependencies = Seq(
  "co.fs2" %% "fs2-core" % fs2Version,
  "co.fs2" %% "fs2-io" % fs2Version
)

lazy val scodecDependencies = Seq(
  "org.scodec" %% "scodec-core" % "1.11.4",
  "org.scodec" %% "scodec-stream" % "2.0.0"
)

lazy val commonDependencies = catsDependencies ++ fs2Dependencies ++ scodecDependencies

lazy val commonSettings = commonBuildSettings ++ Seq(
  organization := "org.ssh4s",
  scalaVersion := "2.12.10",
  libraryDependencies ++= commonDependencies
)

lazy val root = (project in file("."))
  .settings(commonSettings)
  .settings(
    name := "ssh4s"
  ).aggregate(modules: _*)

lazy val core = net4sProject("core")

def net4sProject(name: String) =
  Project(name, file(name))
    .settings(commonSettings)
    .settings(
      moduleName := s"ssh4s-$name",
    )
