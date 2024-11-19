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

      "org.deeplearning4j" % "deeplearning4j-core" % "1.0.0-M2.1",
      "org.deeplearning4j" % "deeplearning4j-ui-model" % "1.0.0-M2.1",
      "org.deeplearning4j" % "deeplearning4j-nlp" % "1.0.0-M2.1",

      "org.nd4j" % "nd4j-native-platform" % "1.0.0-M2.1",

      "org.slf4j" % "slf4j-api" % "2.0.16",

      "ch.qos.logback" % "logback-classic" % "1.5.6",

      "com.typesafe" % "config" % "1.4.3",
    )
  )