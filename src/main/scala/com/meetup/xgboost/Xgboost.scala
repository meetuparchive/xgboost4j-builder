package com.meetup.xgboost

import ml.dmlc.xgboost4j.scala.spark.{TrackerConf, XGBoostModel, XGBoost}
import org.apache.spark.SparkContext
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.feature.LabeledPoint
import org.apache.spark.ml.linalg.Vector
import org.apache.spark.mllib.random.RandomRDDs.normalVectorRDD
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession.Builder
import org.apache.spark.sql.{Dataset, SparkSession}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.SparkSession

object Xgboost {


  def main(args: Array[String]): Unit = {

    implicit val sc: SparkContext = new SparkContext()
    implicit val ss: SparkSession = SparkSession.builder().config(sc.getConf).appName(sc.appName).master(sc.master).getOrCreate()

    val rmse = sampleTraining()(sc, ss)
    println(s"Training completed with RMSE $rmse")
    sc.stop()

  }

  def sampleTraining(numRows: Long = 1000, numCols: Int = 4, numPartitions: Int = 0, seed: Long = 0)(implicit sc: SparkContext, ss: SparkSession): Double = {
    import ss.implicits._
    val normalVectorRdd = normalVectorRDD(sc, numRows, numCols, numPartitions, seed).map(_.asML)
    val dataRdd: RDD[LabeledPoint] = normalVectorRdd.map(v => LabeledPoint(math.abs(v.toArray.take(2).sum), v))
    val Array(trainingRdd, testRdd) = dataRdd.randomSplit(Array(0.9, 0.1), seed)

    val xgbGlobalParams = Map("objective" -> "reg:linear", "tracker_conf" -> TrackerConf(600 * 1000, "scala"))

    val xgboostModel: XGBoostModel = XGBoost.trainWithRDD(trainingRdd, xgbGlobalParams, round = 10, nWorkers = 1)
    val xgboostPrediction: Dataset[(Double, Vector, Double)] = xgboostModel.transform(testRdd.toDF).as[(Double, Vector, Double)]

    val rmseEval = new RegressionEvaluator()
    rmseEval.evaluate(xgboostPrediction)
  }

}
