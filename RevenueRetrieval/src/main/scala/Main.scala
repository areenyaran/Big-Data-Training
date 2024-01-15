import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.log4j.BasicConfigurator
import org.apache.log4j.varia.NullAppender

object RevenueRetrieval {

  def main(args: Array[String]): Unit = {

    val nullAppender = new NullAppender
    BasicConfigurator.configure(nullAppender)
    // creating conf object holding spark context information required in the driver application
    val conf = new SparkConf().
      setMaster("local[4]").
      setAppName("Revenue Retrieval")

    // creating spark context
    val sc = new SparkContext(conf)

    // To avoid logging too many debugging messages
    sc.setLogLevel("ERROR")

    // using spark context to read a file from the file system and create an RDD accordingly
    // id, order_id, product_id, quantity, total_price, unit_price
    val orderItems = sc.textFile("C:\\Users\\Think\\OneDrive\\Documents\\BigData\\RevenueRetrieval\\input.txt");
    println(orderItems.count());

    // doing a couple of transformation operations:
    // creating a new RDD 'revenuePerOrder' which is supposed to hold the accumulated revenue
    //  associated with each order.
    //  This is done first by selecting the second and fifth columns. Then, we group by key (order_id) using
    // reduceByKey. After that we generate a comma-separated records composing of 'order_id, revenue' to be
    // written to file

    val revenuePerOrder = orderItems
      .map(oi => (oi.split(",")(1).toInt, oi.split(",")(4).toFloat))
      .reduceByKey((x,y) => (x + y))
      //.reduceByKey(_ + _)
      .map(oi => oi._1 + "," + oi._2)

    //revenuePerOrder.foreach(println)
    println("----------------------------------------------------------------")

    // This piece of scala code finds the average revenue for each order
    val average_by_key = orderItems
      .map(oi => (oi.split(",")(1).toInt, (oi.split(",")(4).toFloat, 1)))
      .reduceByKey((x,y) => ((x._1+y._1),(x._2+y._2)))
      .mapValues(x=>(x._1/ x._2))
      .map(oi => oi._1 + "," + oi._2)

    //average_by_key.saveAsTextFile(args(1))
    average_by_key.collect().take(10).foreach(println);
    println("----------------------------------------------------------------")

    //Practice-1.1) replace the above mapValues by map to achieve the same functionality.
    val average_by_key2 = orderItems
      .map(oi => (oi.split(",")(1).toInt, (oi.split(",")(4).toFloat, 1)))
      .reduceByKey((x, y) => ((x._1 + y._1), (x._2 + y._2)))
      .map(oi => (oi._1, oi._2._1 / oi._2._2))
      .map(oi => oi._1 + "," + oi._2)

    average_by_key2.collect().take(10).foreach(println)
    println("----------------------------------------------------------------")

    //Practice-1.2) Find the total number of items sold for each order
    val totalItemsPerOrder = orderItems
      .map(oi => (oi.split(",")(1).toInt, oi.split(",")(3).toInt))
      .reduceByKey(_ + _)
      .map(oi => oi._1 + "," + oi._2)

    totalItemsPerOrder.collect().take(10).foreach(println)
    println("----------------------------------------------------------------")

    //Practice-1.3) Find the average number of items sold for each product
    val averageItemsPerProduct = orderItems
      .map(oi => (oi.split(",")(2).toInt, (oi.split(",")(3).toInt, 1)))
      .reduceByKey((x, y) => ((x._1 + y._1), (x._2 + y._2)))
      .mapValues(x => x._1.toFloat / x._2)
      .map(oi => oi._1 + "," + oi._2)

    averageItemsPerProduct.collect().take(10).foreach(println)
    println("----------------------------------------------------------------")

  }
}