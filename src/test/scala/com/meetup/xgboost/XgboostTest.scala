package com.meetup.xgboost

import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import org.scalatest.{Matchers, PropSpec}

class XgboostTest extends PropSpec with Matchers {

  implicit val sc: SparkContext = new SparkContext("local[2]", "test-sc")
  implicit val ss: SparkSession = new SparkSession.Builder().config(sc.getConf).appName(sc.appName).master(sc.master).getOrCreate()

  property("Model shows low RMSE for simple problem") {
    val rmse = Xgboost.sampleTraining()
    rmse should be < 2d
  }

  property("Model shows low RMSE for simple xval training") {
    val rmse = Xgboost.sampleXvalTraining()
    rmse should be < 2d
  }

}
