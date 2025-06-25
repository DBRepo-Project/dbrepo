FROM --platform=$BUILDPLATFORM bitnami/spark:4.0.0
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"

USER root

RUN install_packages curl

USER 1001

ENV AWS_SDK_VERSION="2.20.18"
ENV MARIADB_JAVA_CLIENT_VERSION="3.4.1"
ENV HADOOP_AWS_VERSION="3.4.0"

WORKDIR /opt/bitnami/spark/jars

RUN curl -fsSL https://repo1.maven.org/maven2/software/amazon/awssdk/bundle/$AWS_SDK_VERSION/bundle-$AWS_SDK_VERSION.jar \
    --output ./hadoop-aws-$AWS_SDK_VERSION.jar
RUN curl -fsSL https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/$MARIADB_JAVA_CLIENT_VERSION/mariadb-java-client-$MARIADB_JAVA_CLIENT_VERSION.jar \
    --output ./mariadb-java-client-$MARIADB_JAVA_CLIENT_VERSION.jar
RUN curl -fsSL https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-aws/$HADOOP_AWS_VERSION/hadoop-aws-$HADOOP_AWS_VERSION.jar \
    --output ./hadoop-aws-$HADOOP_AWS_VERSION.jar