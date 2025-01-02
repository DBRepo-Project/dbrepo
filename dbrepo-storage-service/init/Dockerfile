FROM docker.io/python:3.11-alpine3.21 AS runtime
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"

RUN apk add --no-cache bash \
    && pip install s3cmd

USER 1001

WORKDIR /app

COPY --chown=1001 ./init.sh /app/init.sh

RUN chmod 0744 /app/init.sh \
    && mkdir -p /app/config

ENTRYPOINT [ "bash", "-c", "/app/init.sh" ]
