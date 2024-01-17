import org.mongodb.scala.{MongoClient, MongoDatabase}
import com.github.tototoshi.csv._
import com.github.tototoshi.csv.CSVReader
import org.mongodb.scala.bson._
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.Indexes._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Projections._
import org.mongodb.scala.model.Updates._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

// please see the notes in the end.
object StudentData extends App {

  val mongoUrl = "mongodb://localhost:27017/?minPool=2&maxPool=10"
  val mongoClient = MongoClient(mongoUrl)
  val database: MongoDatabase = mongoClient.getDatabase("StudentMongo")
  val collection = database.getCollection("students")

  Await.result(collection.createIndex(ascending("ID"), IndexOptions().unique(true)).toFuture, 10 second)

  // Part 1: Importing Student Data
  val reader = CSVReader.open("students.csv")
  val records = reader.all().drop(1) // skip the header

  records.foreach { fields =>
    val studentID = fields(0).toInt
    val firstName = fields(1)
    val lastName = fields(2)
    val dateOfBirth = fields(3)
    val courseIDs = fields(4).stripPrefix("[").stripSuffix("]").split(",").map(_.trim).toList
    val gpa = fields(5).toDouble

    val studentDocument = org.mongodb.scala.bson.BsonDocument(
      "ID" -> studentID,
      "FirstName" -> firstName,
      "LastName" -> lastName,
      "DateOfBirth" -> dateOfBirth,
      "CourseIDs" -> courseIDs,
      "GPA" -> gpa
    )

    // Insert the document into MongoDB collection
    Await.result(collection.insertOne(studentDocument).toFuture, 1 second)
  }

  // Part 2: Retrieving Students by Course
  println("Enter a course ID to retrieve students enrolled in that course:")
  val courseID = scala.io.StdIn.readLine()
  val studentsInCourse = collection
    .find(equal("CourseIDs", courseID))
    .projection(fields(include("FirstName", "LastName"), excludeId()))
    .toFuture()

  println(s"Students enrolled in ${courseID} course are : ")

  Await.result(studentsInCourse, 10 seconds).foreach { student =>
    println(s"Student: ${student.getString("FirstName")} ${student.getString("LastName")}")
  }
  println("--------------------------------------------------------")

  // Part 3: Retrieving Students by GPA
  println("Enter a minimum GPA to retrieve students with a GPA greater than or equal to that value:")
  val minGPA = scala.io.StdIn.readDouble()
  val studentsWithHigherGPA = collection
    .find(gte("GPA", minGPA))
    .projection(fields(include("FirstName", "LastName", "GPA"), excludeId()))
    .toFuture()

  Await.result(studentsWithHigherGPA, 10 seconds).foreach { student =>
    println(s"Student: ${student.getString("FirstName")} ${student.getString("LastName")}, GPA: ${student.getDouble("GPA")}")
  }

  reader.close()
  mongoClient.close()
}

//libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.5.0"
//libraryDependencies += "org.apache.spark" %% "spark-core" % "3.5.0"
//libraryDependencies += "org.mongodb.scala" %% "mongo-scala-driver" % "4.11.0"
//libraryDependencies += "com.github.tototoshi" %% "scala-csv" % "1.3.10"

// references
// https://github.com/tototoshi/scala-csv
// https://youtu.be/spVP5KJSEXc

//PS : I had several problems in inserting the data after parsing it into Json format
// & i noticed this way while searching , its not as the requirements but its work..

// this code work , but save the data as a string not whit its schema
/*
val schema = new StructType()
  .add("ID", IntegerType,true)
  .add("FirstName", StringType, true)
  .add("LastName", StringType, true)
  .add("DateOfBirth", DateType, true)
  .add("CourseIDs", ArrayType(IntegerType), true)
  .add("GPA", DoubleType,true)

val dfFromCSV = spark.read.option("header", true)
.csv("C:\\Users\\Think\\OneDrive\\Documents\\BigData\\StudentsWithMongo\\students.csv")

val dfWithJSON = dfFromCSV.withColumn("JsonFormatData", to_json(struct(
   col("ID"),
   col("FirstName"),
   col("LastName"),
   col("DateOfBirth"),
   col("CourseIDs"),
   col("GPA"))))

val jsonData = dfWithJSON.select("JsonFormatData").collect()
val jsonStrings = jsonData.map(row => row.getString(0))
val bsonDocuments = jsonStrings.map(jsonString => BsonDocument(jsonString))

val insertResult = Await.result(collection.insertMany(bsonDocuments.map(Document(_))).toFuture(), 10 seconds)
if (insertResult.wasAcknowledged()) {
  println(s"Inserted ${jsonStrings.length} documents into the MongoDB collection.")
} else {
  println("Insertion into MongoDB collection failed.")
}

 */
