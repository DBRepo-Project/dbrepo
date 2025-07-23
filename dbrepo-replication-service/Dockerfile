###### FIRST STAGE ######
FROM --platform=$BUILDPLATFORM maven:3-amazoncorretto-17 AS build
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"

COPY ./pom.xml ./

RUN mvn -fn dependency:go-offline

COPY ./lib ./lib
COPY ./replication-service ./replication-service

# Make sure it compiles
RUN mvn -q clean package -DskipTests

###### SECOND STAGE ######
FROM --platform=$BUILDPLATFORM bitnami/java:17 AS runtime
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"

ARG DUCKDB_VERSION="v1.2.2"

RUN apt-get update && \
    apt-get install -y \
    curl \
    jq && \
    apt-get clean

COPY ./lib/httpfs.duckdb_extension.gz /tmp/httpfs.duckdb_extension.gz

WORKDIR /home/dbrepo/.duckdb/extensions/$DUCKDB_VERSION/linux_amd64

RUN gunzip /tmp/httpfs.duckdb_extension.gz && \
    mv /tmp/httpfs.duckdb_extension /home/dbrepo/.duckdb/extensions/$DUCKDB_VERSION/linux_amd64/httpfs.duckdb_extension && \
    rm -f /tmp/httpfs.duckdb_extension.gz

RUN mkdir -p /var/log/app/service/data && \
    chown -R 1001:1001 /var/log/app/service/data

WORKDIR /app

RUN adduser dbrepo --uid 1001 && \
    chown -R 1001:1001 /app

USER 1001

COPY --from=build --chown=1001 ./replication-service/target/*service-*.jar ./replication-service.jar

# non-root port
EXPOSE 8080

ENV JAVA_OPTS="-XX:+IgnoreUnrecognizedVMOptions --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED --add-opens=java.base/jdk.internal.ref=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/sun.nio.cs=ALL-UNNAMED --add-opens=java.base/sun.security.action=ALL-UNNAMED --add-opens=java.base/sun.util.calendar=ALL-UNNAMED --add-opens=java.security.jgss/sun.security.krb5=ALL-UNNAMED -Djdk.reflect.useDirectMethodHandle=false -Dio.netty.tryReflectionSetAccessible=true"

ENTRYPOINT exec java $JAVA_OPTS -jar ./replication-service.jar