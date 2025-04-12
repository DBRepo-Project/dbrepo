FROM docker.io/python:3.11-alpine3.21 AS runtime
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"

<<<<<<< HEAD
RUN apk --no-cache add \
    bash \
    s3cmd
=======
RUN apk add --no-cache bash \
    && pip install s3cmd
>>>>>>> release-1.8

WORKDIR /app

RUN adduser -D dbrepo --uid 1001

COPY --chown=1001 --chmod=0744 ./init.sh /app/init.sh

ENTRYPOINT [ "bash", "-c", "/app/init.sh" ]
