package com.meetup.xgboost

import ml.dmlc.xgboost4j.scala.spark.{XGBoostRegressionModel, XGBoostRegressor}
import org.apache.spark.SparkContext
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.feature.LabeledPoint
import org.apache.spark.ml.linalg
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.ml.tuning.{CrossValidator, CrossValidatorModel, ParamGridBuilder}
import org.apache.spark.mllib.random.RandomRDDs.normalVectorRDD
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}

object Xgboost {

  def main(args: Array[String]): Unit = {

    implicit val sc: SparkContext = new SparkContext()
    implicit val ss: SparkSession = SparkSession.builder().config(sc.getConf).appName(sc.appName).master(sc.master).getOrCreate()

    val rmse = sampleTraining()(sc, ss)
    println(s"Training completed with RMSE $rmse")

    val xvrmse = sampleXvalTraining()(sc, ss)
    println(s"Training completed with RMSE $xvrmse")
    sc.stop()

  }

  def sampleTraining(numRows: Long = 1000, numCols: Int = 4, numPartitions: Int = 0, seed: Long = 1)(implicit sc: SparkContext, ss: SparkSession): Double = {
    import ss.implicits._

    val normalVectorRdd = normalVectorRDD(sc, numRows, numCols, numPartitions, seed).map(_.asML)
    val dataRdd: Dataset[LabeledPoint] = normalVectorRdd.map(v => LabeledPoint(math.abs(v.toArray.take(2).sum), v)).toDF.as[LabeledPoint]
    val Array(training, test): Array[Dataset[LabeledPoint]] = dataRdd.randomSplit(Array(0.9, 0.1), seed).map(_.cache())

    val paramMap = Map(
      "eta" -> "1",
      "silent" -> "1",
      "objective" -> "reg:linear",
      "num_round" -> 5,
      "num_workers" -> 2)

    val metric: RegressionEvaluator = new RegressionEvaluator().setMetricName("rmse")

    val regressor: XGBoostRegressor = new XGBoostRegressor(paramMap)
    val model: XGBoostRegressionModel = regressor.fit(training)

    val xgboostPrediction: DataFrame = model.transform(test)
    model.summary.trainObjectiveHistory.last
  }

  def sampleXvalTraining(numRows: Long = 1000, numCols: Int = 4, numPartitions: Int = 0, seed: Long = 1)(implicit sc: SparkContext, ss: SparkSession): Double = {
    import ss.implicits._

    val normalVectorRdd: RDD[linalg.Vector] = normalVectorRDD(sc, numRows, numCols, numPartitions, seed).map(_.asML)
    val dataRdd: Dataset[LabeledPoint] = normalVectorRdd.map(v => LabeledPoint(math.abs(v.toArray.take(2).sum), v)).toDF.as[LabeledPoint]
    val Array(training, test): Array[Dataset[LabeledPoint]] = dataRdd.randomSplit(Array(0.9, 0.1), seed).map(_.cache())

    val paramMap: Map[String, Any] = Map(
      "eta" -> "1",
      "silent" -> "1",
      "objective" -> "reg:linear",
      "num_round" -> 5,
      "num_workers" -> 2)

    val model: XGBoostRegressor = new XGBoostRegressor(paramMap)
    val metric: RegressionEvaluator = new RegressionEvaluator().setMetricName("rmse")

    val paramGrid: Array[ParamMap] = new ParamGridBuilder().
      addGrid(model.maxDepth, Array(2, 6)).
      build()

    val cv: CrossValidator = new CrossValidator()
      .setEstimator(model)
      .setEvaluator(metric)
      .setEstimatorParamMaps(paramGrid)
      .setNumFolds(2)

    val cvModel: CrossValidatorModel = cv.fit(training)
    val prediction: DataFrame = cvModel.bestModel.transform(test)
    cvModel.bestModel.asInstanceOf[XGBoostRegressionModel].summary.trainObjectiveHistory.last
  }
}
