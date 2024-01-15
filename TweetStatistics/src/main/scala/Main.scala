import org.apache.spark.{SparkContext, SparkConf}
import org.apache.log4j.BasicConfigurator
import org.apache.log4j.varia.NullAppender

object ElonMuskTweetStatistics {

  // Function to calculate the average and standard deviation of tweet lengths
  def calculateAvgAndStdDev(tweets: org.apache.spark.rdd.RDD[((String, String), Int)], keywords: Array[String]): (Double, Double) = {
    // Filter tweets that contain at least one of the input keywords
    val tweetsWithKeywords = tweets
      .filter { case ((text, _), _) =>
        val lowerText = text.toLowerCase
        // Check if any of the keywords are present in the tweet text
        keywords.exists { keyword =>
          text.toLowerCase.split("\\s+").contains(keyword.toLowerCase)
        }
      }

    val tweetLengths = tweetsWithKeywords
      .map { case ((text, _), _) =>
        // Split the tweet text into words and calculate the length (number of words)
        text.split("\\s+").length.toDouble
      }

    val count = tweetLengths.count()
    val totalLength = tweetLengths.sum()
    val avg = totalLength / count
    val stddev = math.sqrt(tweetLengths.map(len => math.pow(len - avg, 2)).sum / count)

    (avg, stddev)
  }

  def main(args: Array[String]): Unit = {

    // Configure log4j to prevent log messages
    val nullAppender = new NullAppender
    BasicConfigurator.configure(nullAppender)

    val conf = new SparkConf()
      .setMaster("local[4]")
      .setAppName("ElonMuskTweetStatistics")

    val sc = new SparkContext(conf)

    val tweetsRDD = sc.textFile("elonmusk_tweets.csv")

    println("Enter keywords separated by commas:")
    val userInput = scala.io.StdIn.readLine()
    val keywords = userInput.split(",").map(_.trim)

    // Transform the raw tweet data into a structured format by Split each line into
    // an array of fields using a comma as the delimiter & create a structure for each tweet
    // to be a tuple with the following elements:
    //    - The third field (x(2)), representing the tweet text.
    //    - The first part of the second field (x(1).split(" ")(0)), representing the date "created at"
    //    - A constant value of 1, as a repetition counter
    val tweets = tweetsRDD.map(line => line.split(","))
      .map(x => ((x(2), x(1).split(" ")(0)), 1))


    // Calculate keyword counts over time
    val keywordCounts = keywords.flatMap { keyword =>
      val keywordTweets = tweets.filter { case ((text, _), _) =>
        text.toLowerCase.split("\\s+").contains(keyword.toLowerCase)
      }

      val keywordDistribution = keywordTweets.reduceByKey(_ + _)

      val dateCounts = keywordDistribution.map { case (((text, date), count)) =>
        ((keyword, date), count)
      }
      dateCounts.collect()
    }

    keywords.foreach { keyword =>
      // Filter the keywordCounts RDD to only include counts related to the current keyword
      val kwdCounts = keywordCounts.filter { case ((k, _), _) => k == keyword }
      println(s"Keyword: $keyword")

      // Sort in ascending order
      val sortedDateCounts = kwdCounts
        .sortBy { case ((_, date), _) => date }

      sortedDateCounts.foreach { case ((_, date), count) =>
        println(s"($keyword, $date, $count)")
      }

      // Calculate and print the total occurrences of the current keyword over time
      println(s"Total Occurrences of '$keyword' Over Time: " +
        sortedDateCounts.map { case (_, count) => count }.sum)
      println("---------------------------------------------")
    }

    val totalTweets = tweets.count()

    val tweetsWithKeywords = tweets
      .filter { case ((text, _), _) =>
        val lowerText = text.toLowerCase
        // Check if any of the keywords are present in the tweet text
        keywords.exists { keyword =>
          text.toLowerCase.split("\\s+").contains(keyword.toLowerCase)
        }
      }.count()

    val twoKeywordTweets = tweets
      .filter { case ((text, _), _) =>
        val keywordSet = keywords.map(_.toLowerCase).toSet
        // Check if the tweet contains exactly two keywords from the input
        keywordSet.subsets(2).exists { subset =>
          subset.forall(keyword => text.toLowerCase.split("\\s+").contains(keyword))
        }
      }

    val fpercentage = (tweetsWithKeywords.toDouble / totalTweets.toDouble) * 100.0
    val spercentage = (twoKeywordTweets.count().toDouble / totalTweets.toDouble) * 100.0

    val (averageLength, stdDevLength) = calculateAvgAndStdDev(tweets, keywords)

    println(s"Total Tweets: $totalTweets")
    println(s"Tweets with At Least One Keyword: $tweetsWithKeywords")
    println(f"Percentage of Tweets with At Least One Keyword: $fpercentage%.2f%%")
    println(s"Tweets with Exactly Two Keywords: ${twoKeywordTweets.count()}")
    println(f"Percentage of Tweets with Exactly Two Keywords: $spercentage%.2f%%")
    println(s"Average Length of Tweets with Keywords: $averageLength")
    println(s"Standard Deviation of Length of Tweets with Keywords: $stdDevLength")

    sc.stop()
  }
}
