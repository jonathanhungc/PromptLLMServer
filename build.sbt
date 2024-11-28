ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.15"

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) =>
    xs match {
      case "MANIFEST.FM" :: Nil => MergeStrategy.discard
      case "services" :: _      => MergeStrategy.concat
      case _                    => MergeStrategy.discard
    }
  case "reference.conf" => MergeStrategy.concat
  case x if x.endsWith(".proto") => MergeStrategy.rename
  case x if x.contains("hadoop") => MergeStrategy.first
  case _ => MergeStrategy.first
}

Compile / PB.targets := Seq(
  scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
)

lazy val root = (project in file("."))
  .settings(
    name := "homework3",
    assembly / mainClass := Some("SlidingWindowTraining"),
    assembly / assemblyJarName := "sliding-window-training-small-data.jar",

    libraryDependencies ++= Seq(
      "com.github.finagle" %% "finch-core" % "0.31.0",
      "com.github.finagle" %% "finch-circe" % "0.31.0",
      "io.circe" %% "circe-generic" % "0.9.0",

      "org.scalatest" %% "scalatest" % "3.2.19" % "test",

      "org.slf4j" % "slf4j-api" % "2.0.16",

      "ch.qos.logback" % "logback-classic" % "1.5.6",

      "com.typesafe" % "config" % "1.4.3",

      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",

      "io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion,

      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,

      "org.scalaj" %% "scalaj-http" % "2.4.2"
    )
  )