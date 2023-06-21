###### FIRST STAGE ######
FROM dbrepo-metadata-db:latest as dependency
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

###### SECOND STAGE ######
FROM maven:3-openjdk-17 as build
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

COPY ./pom.xml ./

RUN mvn -fn -B dependency:go-offline > /dev/null

COPY --from=dependency /root/.m2/repository/at/tuwien /root/.m2/repository/at/tuwien

COPY ./rest-service ./rest-service
COPY ./services ./services
COPY ./report ./report
COPY ./api ./api

# Make sure it compiles
RUN mvn -q clean package -DskipTests

###### THIRD STAGE ######
FROM openjdk:17-alpine as runtime
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

ENV METADATA_DB=fda
ENV METADATA_USERNAME=root
ENV METADATA_PASSWORD=dbrepo
ENV BROKER_USERNAME=fda
ENV BROKER_PASSWORD=fda
ENV GATEWAY_ENDPOINT=http://gateway-service
ENV BROKER_ENDPOINT=http://broker-service:15672
ENV SHARED_FILESYSTEM=/tmp
ENV BROKER_CONSUMERS=2
ENV LOG_LEVEL=debug
ENV CLIENT_ID="dbrepo-client"
ENV DBREPO_CLIENT_SECRET="MUwRc7yfXSJwX8AdRMWaQC3Nep1VjwgG"
ENV JWT_ISSUER="http://localhost/realms/dbrepo"
ENV JWT_PUBKEY="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqqnHQ2BWWW9vDNLRCcxD++xZg/16oqMo/c1l+lcFEjjAIJjJp/HqrPYU/U9GvquGE6PbVFtTzW1KcKawOW+FJNOA3CGo8Q1TFEfz43B8rZpKsFbJKvQGVv1Z4HaKPvLUm7iMm8Hv91cLduuoWx6Q3DPe2vg13GKKEZe7UFghF+0T9u8EKzA/XqQ0OiICmsmYPbwvf9N3bCKsB/Y10EYmZRb8IhCoV9mmO5TxgWgiuNeCTtNCv2ePYqL/U0WvyGFW0reasIK8eg3KrAUj8DpyOgPOVBn3lBGf+3KFSYi+0bwZbJZWqbC/Xlk20Go1YfeJPRIt7ImxD27R/lNjgDO/MwIDAQAB"
ENV NOT_SUPPORTED_KEYWORDS=\\*,AVG,BIT_AND,BIT_OR,BIT_XOR,COUNT,COUNTDISTINCT,GROUP_CONCAT,JSON_ARRAYAGG,JSON_OBJECTAGG,MAX,MIN,STD,STDDEV,STDDEV_POP,STDDEV_SAMP,SUM,VARIANCE,VAR_POP,VAR_SAMP,--

WORKDIR /app

COPY --from=build ./rest-service/target/rest-service-*.jar ./query-service.jar

EXPOSE 9093

ENTRYPOINT ["java", "-Dlog4j2.formatMsgNoLookups=true",  "-jar", "./query-service.jar"]
