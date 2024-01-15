ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

libraryDependencies += "org.apache.spark" %% "spark-core" % "3.3.2"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.3.2"
libraryDependencies += "org.apache.spark" %% "spark-mllib" % "3.3.2"
libraryDependencies += "org.apache.kafka" % "kafka-clients" % "3.6.1"
libraryDependencies += "org.apache.spark" %% "spark-streaming-kafka-0-10" % "3.3.2"
libraryDependencies += "org.apache.spark" %% "spark-sql-kafka-0-10" % "3.3.2"
// https://mvnrepository.com/artifact/org.mongodb.spark/mongo-spark-connector
libraryDependencies += "org.mongodb.spark" %% "mongo-spark-connector" % "10.1.1"

// and for python u need to run these commands : pip install kafka-python Flask pymongo plotly pandas
lazy val root = (project in file("."))
  .settings(
    name := "kafka"
  )
