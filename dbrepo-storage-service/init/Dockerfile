FROM chrislusf/seaweedfs:3.59 AS runtime

WORKDIR /app

COPY ./create-buckets.sh ./create-buckets.sh

ENTRYPOINT [ "/bin/sh", "/app/create-buckets.sh" ]