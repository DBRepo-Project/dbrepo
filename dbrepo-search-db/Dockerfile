FROM opensearchproject/opensearch:2.8.0 as runtime

USER root

RUN yum install -y jq

USER opensearch

WORKDIR /app

COPY ./limits.conf /etc/security/limits.conf

COPY ./indices/*.json .

COPY ./create-indices.sh ./create-indices.sh
COPY ./docker-entrypoint.sh ./docker-entrypoint.sh
COPY ./healthcheck.sh ./healthcheck.sh

ENTRYPOINT [ "bash", "./docker-entrypoint.sh" ]