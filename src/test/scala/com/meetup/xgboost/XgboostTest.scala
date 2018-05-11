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
import org.scalatest.{Matchers, PropSpec}

class XgboostTest extends PropSpec with Matchers {

  implicit val sc: SparkContext = new SparkContext("local[2]", "test-sc")
  implicit val ss: SparkSession = new Builder().config(sc.getConf).appName(sc.appName).master(sc.master).getOrCreate()

  property("Model shows low RMSE for simple problem") {
    val rmse = Xgboost.sampleTraining()
    rmse should be < 2d
  }

}
