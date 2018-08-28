import sbt.Resolver

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.meetup",
      scalaVersion := "2.11.8",
      version      := "0.0.1-SNAPSHOT"
    )),
    name := "xgboost4j-builder-test"
  )

resolvers += Resolver.mavenLocal

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "2.2.0" % "provided", // See: https://github.com/sbt/sbt-assembly#excluding-jars-and-files
  "org.apache.spark" %% "spark-mllib" % "2.2.0",
  "org.scalatest" %% "scalatest" % "3.0.3",
  "ml.dmlc" % "xgboost4j-spark" % "0.80"
)

// Code below is necessary only if `sbt assembly` is desired to packed up the Scala code here.
test in assembly := {} // `sbt assembly` will not run tests first

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case _ => MergeStrategy.last // Not necessarily sufficient: see https://github.com/sbt/sbt-assembly#merge-strategy
}
