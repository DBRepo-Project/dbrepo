###### FIRST STAGE ######
FROM fda-metadata-db:latest as dependency
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

###### SECOND STAGE ######
FROM maven:slim as build

COPY ./pom.xml ./

RUN mvn -fn -B dependency:go-offline > /dev/null

COPY --from=dependency /root/.m2/repository/at/tuwien /root/.m2/repository/at/tuwien

COPY ./rest-service ./rest-service
COPY ./services ./services

# Make sure it compiles
RUN mvn -q clean package -DskipTests > /dev/null

###### THIRD STAGE ######
FROM rabbitmq:3-management-alpine

RUN apk --no-cache add openjdk11

COPY ./service_ready /usr/bin
COPY ./docker-entrypoint.sh /usr/bin

RUN chmod +x /usr/bin/service_ready
RUN chmod +x /usr/bin/docker-entrypoint.sh

COPY ./rabbitmq.conf /etc/rabbitmq/

HEALTHCHECK --interval=10s --timeout=5s --retries=12 CMD service_ready

COPY --from=build ./rest-service/target/rest-service-*.jar ./rest-service.jar

EXPOSE 9098

ENTRYPOINT ["/usr/bin/docker-entrypoint.sh"]