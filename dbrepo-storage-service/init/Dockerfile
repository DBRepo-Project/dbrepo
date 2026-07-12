FROM --platform=$BUILDPLATFORM docker.io/python:3.11-alpine3.23 AS runtime
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"
LABEL org.opencontainers.image.source="https://github.com/DBRepo-Project/dbrepo"

RUN apk --no-cache add \
    bash \
    s3cmd

WORKDIR /app

RUN mkdir -p /app/config && \
    chown -R 1001:1001 /app/config

RUN mkdir -p /var/log/app/service/storage && \
    chown -R 1001:1001 /var/log/app/service/storage

RUN adduser -D dbrepo --uid 1001

COPY --chown=1001 --chmod=0744 ./init.sh /app/init.sh

ENTRYPOINT [ "bash", "-c", "/app/init.sh" ]
