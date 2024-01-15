import org.apache.log4j.BasicConfigurator
import org.apache.log4j.varia.NullAppender
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.feature.{VectorAssembler}
import org.apache.spark.ml.regression.{LinearRegression, RandomForestRegressor}
import org.apache.spark.sql.SparkSession

object PricePrediction {
  def main(args: Array[String]): Unit = {
    val nullAppender = new NullAppender
    BasicConfigurator.configure(nullAppender)

    val spark = SparkSession.builder()
      .master("local[4]")
      .appName("Spark Regression basic example")
      .getOrCreate()

    // load data
    // For more information about the data
    // https://www.kaggle.com/c/boston-housing
    // data: dataFrame
    val data = spark.read.format("csv").
      option("header", "true").
      option("inferSchema", "true" ).
      load("boston_housing.csv")

    //create features vector
    // Excluding the label column (medv)
    val feature_columns = data.columns.slice(0,data.columns.length-1)

    //printing the feature columns to make sure we are targeting the right columns
    //feature_columns.foreach(println);

    // assembling the features as one feature column called "features",
    // Note: assembler is a transformer
    val assembler = new VectorAssembler().
      setInputCols(feature_columns).
      setOutputCol("features");

    //combining the "features" column with the original dataset columns including 'medv'
    val data_2 = assembler.transform(data)

    data.printSchema();

    //have a look at the main schema of the transformed dataset
    data_2.printSchema();
    data_2.show(10, false)
    //System.exit(0);

    // splitting dataset into 70% of the data for the trainig and 30% testing datasets
    val splits = data_2.randomSplit(Array(0.7, 0.3), seed = 11L)
    val train = splits(0)
    val test = splits(1)

    // Random Forest Regressor
    val linearRegressor = new LinearRegression().
      setFeaturesCol("features").
      setLabelCol("medv")

    // train the model
    val lrModel = linearRegressor.fit(train)

    // Print the coefficients and intercept for linear regression
    println(s"Coefficients: ${lrModel.coefficients} Intercept: ${lrModel.intercept}")

    // Summarize the model over the training set and print out some metrics
    val trainingSummary = lrModel.summary

    // residuals --> compare between actual values & predicted values
    trainingSummary.residuals.show() // Whatever the value is close to zero is the model will be more accurate
    print(s"Linear Regression: Evaluation of test data: R2 =  ${trainingSummary.r2}")
    println(s", RMSE: ${trainingSummary.rootMeanSquaredError}") //The less the better

    //System.exit(0)

    //another try with new ML algorithim to make RMSE less
    val randomForestRegressor = new RandomForestRegressor().
      setFeaturesCol("features").
      setLabelCol("medv");

    val model = randomForestRegressor.fit(train)
    val predictions = model.transform(test)

    // 2 evaluation metrics to try -> r2 & rmse
    val r2_evaluator = new RegressionEvaluator()
      .setLabelCol("medv")
      .setPredictionCol("prediction").setMetricName("r2")
    //Range of r2 (0-100), whatever the r2 value close to 100 the performance will be better

    val rmse_evaluator = new RegressionEvaluator()
      .setLabelCol("medv")
      .setPredictionCol("prediction").setMetricName("rmse")

    println("Random Forest: Evaluation of test data: R2 = "
      + r2_evaluator.evaluate(predictions) + ", RMSE = "
      + rmse_evaluator.evaluate(predictions))

    // Random forest better then regression
    predictions.show()
  }
}