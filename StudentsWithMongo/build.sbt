ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

libraryDependencies += "org.apache.spark" %% "spark-core" % "3.5.0"
libraryDependencies += "org.mongodb.scala" %% "mongo-scala-driver" % "4.11.0"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.5.0"
libraryDependencies += "com.github.tototoshi" %% "scala-csv" % "1.3.10"

lazy val root = (project in file("."))
  .settings(
    name := "StudentsWithMongo"
  )
