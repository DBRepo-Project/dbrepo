FROM chrislusf/seaweedfs:3.59 as runtime

RUN apk add curl

WORKDIR /app

COPY ./create-buckets.sh ./create-buckets.sh
COPY ./docker-entrypoint.sh ./docker-entrypoint.sh

ENTRYPOINT [ "/bin/sh", "./docker-entrypoint.sh" ]