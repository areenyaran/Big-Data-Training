import org.apache.log4j.BasicConfigurator
import org.apache.log4j.varia.NullAppender
import org.apache.spark.sql.streaming.Trigger
import org.apache.spark.sql.{DataFrame, SparkSession, functions}

object KafkaConsumer_tweets {
  def main(args: Array[String]): Unit = {

    val nullAppender = new NullAppender
    BasicConfigurator.configure(nullAppender)

    val spark = SparkSession.builder
      .appName("KafkaConsumerTweets")
      .config("spark.mongodb.input.uri", "mongodb://127.0.0.1:27017/tweets.data")
      .config("spark.mongodb.output.uri", "mongodb://127.0.0.1:27017/tweets.data")
      .config("spark.sql.legacy.timeParserPolicy", "LEGACY")
      .master("local[*]")
      .getOrCreate()

    // Read data from Kafka topic
    val df = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "tweets")
      .option("failOnDataLoss", "false")
      .load()

    import spark.implicits._

    // Process the DataFrame containing JSON-formatted tweets & replace the date column with the new one from date type
    val df1 = df.selectExpr("CAST(value AS STRING)")
      .select(functions.json_tuple($"value", "id", "date", "user", "text", "retweets"))
      .toDF("id", "date", "user", "text", "retweets")
      .withColumn("formatted_date", functions.to_timestamp($"date", "EEE MMM dd HH:mm:ss zzz yyyy"))
      .drop("date")
      .withColumnRenamed("formatted_date", "date")

    df1.printSchema()

    // Write the processed DataFrame to the console
    df1.writeStream
      .outputMode("append")
      .format("console")
      .trigger(Trigger.ProcessingTime("5 seconds"))
      .start()

    // Write the processed DataFrame to MongoDB in the streaming
    df1.writeStream
      .foreachBatch { (batchDF: DataFrame, _: Long) =>
        batchDF.write
          .format("mongodb")
          .option("spark.mongodb.write.database", "tweets")
          .option("spark.mongodb.write.collection", "data")
          .option("spark.mongodb.write.connection.uri", "mongodb://localhost:27017")
          .mode("append")
          .save()
      }
      .start()
      .awaitTermination()
  }
}
