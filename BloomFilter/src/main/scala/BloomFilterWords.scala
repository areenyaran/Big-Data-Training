import org.apache.log4j.BasicConfigurator
import org.apache.log4j.varia.NullAppender
import org.apache.spark.{SparkConf, SparkContext}
import breeze.util.BloomFilter

object BloomFilterWords {
  def createBloomFilter(wordsRDD: org.apache.spark.rdd.RDD[String], numElements: Long): BloomFilter[String] = {
    val bloomFilters = wordsRDD.mapPartitions { iter =>
      val bf = BloomFilter.optimallySized[String](numElements.toInt, 0.01.toInt)
      iter.foreach(i => bf += i)
      Iterator(bf)
    }
    bloomFilters.reduce(_ | _)
  }

  def calculateErrorRate(results: Array[(String, Boolean)], docsResults: Set[String]): (Int, Int, Double) = {
    val falsePositives = results.count { case (word, isPresent) => isPresent && !docsResults.contains(word) }
    val falseNegatives = results.count { case (word, isPresent) => !isPresent && docsResults.contains(word) }
    val errorRate = (falsePositives + falseNegatives).toDouble / results.length.toDouble
    (falsePositives, falseNegatives, errorRate)
  }

  def printResults(description: String, testWords: Array[String], results: Array[(String, Boolean)], falsePositives: Int, falseNegatives: Int, errorRate: Double): Unit = {
    println(s"$description results: ")
    println(s"Test Words: ${testWords.mkString(", ")}")
    println(s"Results from Bloom Filter: ${results.mkString(", ")}")
    println(s"False Positives: $falsePositives")
    println(s"False Negatives: $falseNegatives")
    println(s"Error Rate: $errorRate")
  }

  def main(args: Array[String]): Unit = {
    val nullAppender = new NullAppender
    BasicConfigurator.configure(nullAppender)

    val conf = new SparkConf().setMaster("local[4]").setAppName("BloomFilter")
    val sc = new SparkContext(conf)

    val documents = sc.wholeTextFiles("documents") // each file is a document
    val wordsRDD = documents.flatMap { case (_, content) => content.split("\\s+") }

    val numElements = wordsRDD.count()

    val bloomFilter = createBloomFilter(wordsRDD, numElements)
    val sbloomFilter = new BloomFilter[String](numElements.toInt, 0.01.toInt)
    // wordsRDD.collect().foreach(word => sbloomFilter.+=(word)) // the correct result

    val testWords = Array("asdfghjkl", "test", "word", "spark", "penthouselkf", "scalaa", "tigerldkmv", "saludhvdu")

    val resultsFromBloomFilter = testWords.map(word => (word, bloomFilter.contains(word)))
    val resultsFromSBloomFilter = testWords.map(word => (word, sbloomFilter.contains(word)))

    val docsResult = wordsRDD.collect().toSet

    val (falsePositives, falseNegatives, errorRate) = calculateErrorRate(resultsFromBloomFilter, docsResult)
    printResults("Bloom filter as object", testWords, resultsFromBloomFilter, falsePositives, falseNegatives, errorRate)

    val (sfalsePositives, sfalseNegatives, serrorRate) = calculateErrorRate(resultsFromSBloomFilter, docsResult)
    printResults("Bloom filter", testWords, resultsFromSBloomFilter, sfalsePositives, sfalseNegatives, serrorRate)
    sc.stop()
  }
}