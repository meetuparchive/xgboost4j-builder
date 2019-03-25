# XGBoost in Spark #

## Update ##

Since mid-2018, XGBoost is available in [maven central](https://mvnrepository.com/artifact/ml.dmlc/xgboost4j-spark "XGBoost in Maven").

## Spark & Boosted decision trees ##

The machine learning team at [Meetup](https://www.meetup.com/ "Meetup") uses [Apache Spark](https://spark.apache.org/ "Apache Spark") for most of its batch data processing and modeling needs. To keep things simple and robust, we share code between training and production scoring for things like feature generation. In addition, model scoring is an embarrassingly parallel task which Spark can easily exploit. Both of these aspects give us an incentive to have everything running within Spark.

[Gradient boosted](https://en.wikipedia.org/wiki/Gradient_boosting "Wikipedia: Gradient boosting") decision trees with regularization have had a lot of success in supervised learning problems on tabular data in recent years with consistent best-in-class performance on a broad set of problem domains, including ones of interest to Meetup. While Spark does implement a [boosted trees](https://github.com/apache/spark/blob/master/mllib/src/main/scala/org/apache/spark/ml/regression/GBTRegressor.scala "GBTRegressor") algorithm (without regularization), we have made improvements in model quality and faster training time by using alternatives outside of the Spark project. [XGBoost](https://github.com/dmlc/xgboost "XGBoost") is one of the most popular open source manifestations of regularized boosted trees and is an especially good fit for our requirements given the great work that has already been done to package it for the [jvm](https://github.com/dmlc/xgboost/tree/master/jvm-packages "XGBoost4J") and Spark.

## Docker ##

XGBoost relies on native components for performance and so it needs to be built from source for the relevant target system. We use Docker as a build container to create the "fat JAR" that we then ship to our Spark clusters. (As mentioned above, this same JAR is used for training and scoring, and is the versioned artifact that represents a model in production.) For our jobs running on Google Cloud, this means [Debian-based](https://cloud.google.com/dataproc/docs/resources/faq#what_operating_system_os_is_used_for_dataproc "GCP Dataproc FAQ") images and so our [Dockerfile reflects that](Dockerfile#L1 "FROM debian:8.9"). The advantage of Docker is that it is then very easy to make the modification to build for a different target system using a different base.

We've published the Dockerfile which does the build--together with a small Scala test to verify that Spark was able to use XGBoost--to serve as a demonstration and documentation for this process. We have this being built daily on the latest commits to XGBoost to catch more quickly any future changes to the XGBoost project that will require updates.

## Usage ##

### Adding XGBoost to your Spark project ###

1. `make assembly` -- use a build container:
    - Clone this repo and add in target Scala-Spark project
    - The `sbt assembly` instruction in the `ENTRYPOINT` packages the project
    - Invocations of `make package` will assemble a fat jar for use in `./target/scala-2.11/`
    - This is the simplest way to incorporate another project
2. `make publish-local` -- save the `ml.dmlc.xgboost4j-spark` artifact to the project directory:
    - This places all of the artifacts under `./target/jvm-packages/` for local reuse
    - This could be published to the local resolver for other projects
3. `make publish-nexus` -- publish to an internal repository manager:
    - This is how Meetup uses this project.
    - This publishes to Meetup's internal [repository manager](https://maven.apache.org/repository-management.html "Maven Repository Management")
    - [Travis CI](https://travis-ci.com "Travis CI") is scheduled for nightly builds
    - Those are put into an internal [Nexus repository](https://www.sonatype.com/nexus-repository-oss "Sonatype Nexus")

We also have a couple of other `make` targets for convenience. [make package](#make-package) (which creates the docker build container for XGBoost and makes that project) and [make test](#make-test) (which validates that build using the Scala tests included here).

### make package ###

The very first run will be the slowest as the base layers of the Docker image -- downloading everything, building cmake, building XGBoost -- need to be created (these are reused in subsequent invocations). We allow for the option to do some ["cache busting"](https://docs.docker.com/develop/develop-images/dockerfile_best-practices/#run "Docker RUN") using the `ARG XGBOOST_REF` which ensures that XGBoost is pulled from remote if needed. We pass in the latest commit from XGBoost so that we avoid unnecessary work if there hasn't been an update to that project. (Avoiding passing in any argument will mean that the build container holds the version of XGBoost that was cloned the first time the container was built -- which might be sufficient and will ensure that the build container is static across runs.)

Consecutive runs (using the cache for everything) should complete within a few seconds:

    Sending build context to Docker daemon  744.4kB
    Step 1/13 : FROM debian:8.9
     ---> 25fc9eb3417f
    Step 2/13 : ARG CMAKE_VERSION=3.10
     ---> Using cache
     ---> b911ca3f0edb
    Step 3/13 : ARG CMAKE_BUILD=1
     ---> Using cache
     ---> d01792c08928
    Step 4/13 : ARG SBT_VERSION=1.1.1
     ---> Using cache
     ---> 387ddff02511
    Step 5/13 : RUN echo 'deb http://http.debian.net/debian jessie-backports main' >> /etc/apt/sources.list
     ---> Using cache
     ---> ff63b8d799cb
    Step 6/13 : RUN apt-get update && apt-get install -y -t     jessie-backports     build-essential     git     make     maven     openjdk-8-jdk     python     tar     wget
     ---> Using cache
     ---> a9a3ed820c83
    Step 7/13 : RUN update-java-alternatives -s java-1.8.0-openjdk-amd64
     ---> Using cache
     ---> 36850c56ffd9
    Step 8/13 : RUN mkdir ~/temp_cmake &&     cd ~/temp_cmake &&     wget https://cmake.org/files/v$CMAKE_VERSION/cmake-$CMAKE_VERSION.$CMAKE_BUILD.tar.gz &&     tar -xzvf cmake-$CMAKE_VERSION.$CMAKE_BUILD.tar.gz &&     cd cmake-$CMAKE_VERSION.$CMAKE_BUILD/ &&     ./bootstrap &&     make -j4 &&     make install &&     ln -s /usr/local/bin/cmake /usr/bin/cmake
     ---> Using cache
     ---> 214a4456ff43
    Step 9/13 : RUN cd /usr/local &&     wget -O sbt-$SBT_VERSION.tgz https://github.com/sbt/sbt/releases/download/v$SBT_VERSION/sbt-$SBT_VERSION.tgz &&     tar -xf sbt-$SBT_VERSION.tgz &&     rm sbt-$SBT_VERSION.tgz &&     ln -s /usr/local/sbt/bin/sbt /usr/bin/sbt
     ---> Using cache
     ---> 8ffa550da35e
    Step 10/13 : RUN sbt reload
     ---> Using cache
     ---> aba2827cffab
    Step 11/13 : ARG XGBOOST_REF="HEAD"
     ---> Using cache
     ---> 30e8c4bf3a3c
    Step 12/13 : RUN cd ~ &&     git clone --recursive https://github.com/dmlc/xgboost &&     cd xgboost &&     git checkout $XGBOOST_REF &&     make -j4 &&     cd jvm-packages &&     mvn -q -Dclean -DskipTests install package
     ---> Using cache
     ---> 3307646459af
    Step 13/13 : ENTRYPOINT cd /xgboost4j-builder-repo &&     sbt --error test
     ---> Using cache
     ---> bcd856574492
    Successfully built bcd856574492
    Successfully tagged xgboost4j-builder-container:latest

### make test ###

The entrypoint here will build and run the tests for the Scala project -- which demonstrates that all is well with the Spark-XGBoost integration. This will log quite a  bit at the INFO level from Spark and should end with the statement of tests passing.

    18/05/11 16:22:07 INFO XGBoostSpark: Rabit returns with exit code 0
    [info] XgboostTest:
    [info] - Model shows low RMSE for simple problem
    [info] Run completed in 10 seconds, 46 milliseconds.
    [info] Total number of tests run: 1
    [info] Suites: completed 1, aborted 0
    [info] Tests: succeeded 1, failed 0, canceled 0, ignored 0, pending 0
    [info] All tests passed.
    [success] Total time: 12 s, completed May 11, 2018 4:22:11 PM

## Happy Boosting ##

The project uses the [MIT License](LICENSE.txt) -- free to use and adapt it as needed!
