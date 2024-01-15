import org.mongodb.scala.{MongoClient, MongoDatabase}
import com.github.tototoshi.csv._
import com.github.tototoshi.csv.CSVReader
import org.apache.spark.{SparkConf, SparkContext}
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
object AveragePriceCalculator {
  def main(args: Array [String]): Unit = {

    val conf = new SparkConf().setAppName("someApp").setMaster("local[*]")
    val sc = new SparkContext(conf)
    val csvFile = sc.textFile("C:\\Users\\Think\\Downloads\\test.csv")
    val productData = csvFile
      .map(line => {
        val fields = line.split(",")
        val pi = fields(1).trim.toInt
        val up = fields(2).trim.toDouble
        (pi, (up, 1))
      })
    val ps = productData.reduceByKey((a, b) => (a._1 + b._1, a._2 + b._2))
    val pa = ps.mapValues { case (x, y) => x/y}
      pa.foreach(println)
    }
  }