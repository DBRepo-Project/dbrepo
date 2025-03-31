###### FIRST STAGE ######
FROM maven:3-amazoncorretto-17 AS build
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"

COPY ./pom.xml ./

RUN mvn dependency:go-offline

COPY ./src/ ./src/

# Make sure it compiles
RUN mvn clean install -DskipTests