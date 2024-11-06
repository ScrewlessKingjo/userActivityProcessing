// build.sbt
ThisBuild / version := "0.1"
ThisBuild / scalaVersion := "2.12.18"

lazy val root = (project in file("."))
  .settings(
    name := "DataHandler",
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % "3.5.3",
      "org.apache.spark" %% "spark-sql" % "3.5.3",
      "org.apache.spark" %% "spark-hive" % "3.5.3",
      "com.typesafe" % "config" % "1.4.2",
      "org.scala-lang" % "scala-library" % "2.12.18"
),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard // META-INF 폴더는 무시
      case x => MergeStrategy.first // 나머지 파일은 첫 번째 것을 선택
    }
  )
