FROM debian:8.9

# cmake and sbt defaults
ARG CMAKE_VERSION=3.10
ARG CMAKE_BUILD=1
ARG SBT_VERSION=1.1.1

RUN echo 'deb http://http.debian.net/debian jessie-backports main' >> /etc/apt/sources.list
RUN apt-get update && apt-get install -y -t \
    jessie-backports \
    build-essential \
    git \
    make \
    maven \
    openjdk-8-jdk \
    python \
    tar \
    wget

RUN update-java-alternatives -s java-1.8.0-openjdk-amd64

# install cmake
RUN mkdir ~/temp_cmake && \
    cd ~/temp_cmake && \
    wget https://cmake.org/files/v$CMAKE_VERSION/cmake-$CMAKE_VERSION.$CMAKE_BUILD.tar.gz && \
    tar -xzvf cmake-$CMAKE_VERSION.$CMAKE_BUILD.tar.gz && \
    cd cmake-$CMAKE_VERSION.$CMAKE_BUILD/ && \
    ./bootstrap && \
    make -j4 && \
    make install && \
    ln -s /usr/local/bin/cmake /usr/bin/cmake

# install sbt
RUN cd /usr/local && \
    wget -O sbt-$SBT_VERSION.tgz https://github.com/sbt/sbt/releases/download/v$SBT_VERSION/sbt-$SBT_VERSION.tgz && \
    tar -xf sbt-$SBT_VERSION.tgz && \
    rm sbt-$SBT_VERSION.tgz && \
    ln -s /usr/local/sbt/bin/sbt /usr/bin/sbt

RUN sbt reload

# install xgboost (intended use with --build-arg and the latest commit hash ensures that build is up-to-date)
ARG XGBOOST_REF="release_0.80"
RUN cd ~ && \
    git clone --recursive https://github.com/dmlc/xgboost && \
    cd xgboost && \
    git checkout $XGBOOST_REF && \
    make -j4 && \
    cd jvm-packages && \
    mvn -q -Dclean -DskipTests install package

ENTRYPOINT cd /xgboost4j-builder-repo && \
    # --error and src/test/resouces/log4j.properties are to reduce log output for travis and can be adapted locally
    sbt --error test
