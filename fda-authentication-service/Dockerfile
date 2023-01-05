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
COPY ./report ./report

# Make sure it compiles
RUN mvn -q clean package -DskipTests

###### THIRD STAGE ######
FROM openjdk:11-jre-slim as runtime

ENV METADATA_DB=fda
ENV METADATA_USERNAME=root
ENV METADATA_PASSWORD=dbrepo
ENV BROKER_USERNAME=fda
ENV BROKER_PASSWORD=fda
ENV WEBSITE=http://example.com
ENV GATEWAY_ENDPOINT=http://gateway-service:9095/api/broker
ENV TOKEN_MAX=5
ENV MAIL_FROM="Database Repository <noreply@example.com>"
ENV MAIL_REPLY_TO="Somebody <somebody@example.com>"
ENV MAIL_VERIFY=false
ENV JWT_ISSUER=dbrepo
ENV JWT_SECRET=secret
ENV JWT_EXPIRATION=86400000
ENV SMTP_HOST=""
ENV SMTP_PORT=""
ENV SMTP_USERNAME=""
ENV SMTP_PASSWORD=""
ENV LOG_LEVEL=debug
ENV DEFAULT_ROLES=ROLE_RESEARCHER
ENV DEVELOPER_USERNAMES=""

VOLUME /tmp

COPY ./service_ready /usr/bin
RUN chmod +x /usr/bin/service_ready

HEALTHCHECK --interval=10s --timeout=5s --retries=12 CMD service_ready

COPY --from=build ./rest-service/target/rest-service-*.jar ./authentication-service.jar

EXPOSE 9097

ENTRYPOINT ["java", "-Dlog4j2.formatMsgNoLookups=true", "-jar", "./authentication-service.jar"]
