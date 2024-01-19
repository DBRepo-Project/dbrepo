FROM alpine:3.18 as runtime

RUN apk --no-cache add bash jq curl

ENV OPENSEARCH_HOST="http://search-db:9200"
ENV OPENSEARCH_USERNAME="admin"
ENV OPENSEARCH_PASSWORD="admin"

WORKDIR /app

COPY ./indices/* .
COPY ./create-indices.sh ./create-indices.sh

CMD [ "bash", "/app/create-indices.sh" ]