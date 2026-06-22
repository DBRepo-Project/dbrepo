FROM --platform=$BUILDPLATFORM docker.io/python:3.14-alpine3.21 AS runtime
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"

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
