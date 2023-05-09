###### FIRST STAGE ######
FROM dbrepo-metadata-db:latest as dependency
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

###### SECOND STAGE ######
FROM rabbitmq:3-management-alpine as runtime

ENV RABBITMQ_DEFAULT_VHOST=dbrepo

RUN apk --no-cache add curl

COPY ./rabbitmq.conf /etc/rabbitmq/rabbitmq.conf

WORKDIR /app

ENV JWT_PUBKEY=public-key
ENV JWT_CERT=cert

COPY ./init.sh ./init.sh
COPY ./service_ready /usr/bin/service_ready
COPY ./docker-entrypoint.sh ./docker-entrypoint.sh

HEALTHCHECK --interval=10s --timeout=5s --retries=12 CMD service_ready

ENTRYPOINT [ "bash", "/app/docker-entrypoint.sh" ]