import org.apache.log4j.BasicConfigurator
import org.apache.log4j.varia.NullAppender
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object SparkDfDemo {

  def main(args: Array[String]): Unit = {

    val nullAppender = new NullAppender
    BasicConfigurator.configure(nullAppender)

    val spark = SparkSession
      .builder()
      .master("local")
      .appName("Spark SQL")
      .getOrCreate()

    // using spark session to read a file from the file system and create a dataframe accordingly
    val df = spark
      .read
      .json("customers.txt")

    df.show(10)

    //System.exit(0)

    // calculating the number of customers per city
    // groupedByCity: dataframe
    val groupedByCity = df
      .groupBy("customer_city")
      .count()
    groupedByCity.show(10)

    //System.exit(0)

    // implicits object gives implicit conversions for converting Scala objects (incl. RDDs)
    // into a Dataset, DataFrame, Columns or supporting such conversions
    import spark.implicits._

    // 1- Show how you can save the resulting dataframe into json file(s)
    // Save the groupedByCity DataFrame to a JSON file
    // groupedByCity.write.json("C:\\Users\\Think\\OneDrive\\Documents\\BigData\\SparkDemoDF\\grouped_customers.json")
    // 2- Sort the cities according to the number of customers in a descending order.
    // Sort the groupedByCity DataFrame in descending order of 'count'
    val sortedCities = groupedByCity
      .orderBy($"count".desc)
    sortedCities.show(10)

    // count the number of customers whose first name is "Robert" and last name is "Smith"
    // filteredDF: DateSet[Row]
    val filteredDF =
    df.filter('customer_fname === "Robert" && $"customer_lname" === "Smith");
    println("count the number of customers whose first name is \"Robert\" and last name is \"Smith\" is : " + filteredDF.count())

    //1- Count the number of cities that has street names ending with 'Plaza'
    // Assuming there's a column 'customer_street' containing street names
    val citiesWithPlaza = df.filter($"customer_street".endsWith("Plaza"))
    val countCitiesWithPlaza = citiesWithPlaza.select("customer_city").distinct().count()
    println("Number of cities with street names ending with 'Plaza': " + countCitiesWithPlaza)

    // print the name of the city where the name "Robert Smith" has the largest frequency.
    val cityWithMostFreq =
    filteredDF
      .groupBy("customer_city")
      .count()
      .sort($"count".desc)
      .select("customer_city").take(1)(0)

    println("City with most frequent 'Robert Smith' is " + cityWithMostFreq)

    //System.exit(0)
    // Here, you need to know how to join two dataframes with each others in order to find the top-5 customers
    // in terms of the number of cancelled orders
    // Note: you need to utilize the 'order' dataframe that can be loaded from
    // data/retail_db_json/orders
    // Load the 'orders' DataFrame from a JSON file, adjust the path accordingly
    val orders = spark.read.json("orders.txt")

    // Join 'df' with 'orders' using the 'customer_id' and 'order_customer_id' columns
    val joinedDF = df.join(orders, df("customer_id") === orders("order_customer_id"))

    // Group by customer and count the number of canceled orders
    val canceledOrdersCount = joinedDF
      .filter($"order_status" === "CANCELED")
      .groupBy("customer_id", "customer_fname", "customer_lname")
      .agg(count("order_id").alias("canceled_order_count"))

    // Sort by the count in descending order and select the top 5 customers
    val top5Customers = canceledOrdersCount
      .orderBy($"canceled_order_count".desc)
      .limit(5)

    top5Customers.show()
  }
}