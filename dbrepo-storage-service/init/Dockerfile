FROM chrislusf/seaweedfs:3.59 as runtime

WORKDIR /app

COPY ./create-buckets.sh ./create-buckets.sh

ENTRYPOINT [ "/bin/sh", "/app/create-buckets.sh" ]