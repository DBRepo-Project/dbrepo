FROM docker.io/bitnami/seaweedfs:3.80.0-debian-12-r1 AS runtime

WORKDIR /app

COPY ./create-buckets.sh ./create-buckets.sh

ENTRYPOINT [ "/bin/sh", "/app/create-buckets.sh" ]