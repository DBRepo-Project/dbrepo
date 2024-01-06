FROM python:3.10-alpine

RUN apk add bash curl && adduser -D alpine

WORKDIR /home/alpine

COPY Pipfile Pipfile.lock ./

RUN pip install pipenv && \
    pipenv install gunicorn && \
    pipenv install --system --deploy

COPY ./app ./app
COPY ./omlib ./omlib
COPY ./scripts ./scripts
COPY ./us-yml ./us-yml
COPY config.py wsgi.py friendly_names_overrides.json ./

ENV FLASK_APP=wsgi.py
ENV COLLECTION="['database','table','column','identifier','unit','concept','user','view']"
ENV OPENSEARCH_HOST=localhost
ENV OPENSEARCH_PORT=9200
ENV OPENSEARCH_USERNAME=admin
ENV OPENSEARCH_PASSWORD=admin
ENV LOG_LEVEL=info

RUN chown -R alpine:alpine ./
USER alpine

ENTRYPOINT ["sh", "./scripts/docker-entrypoint.sh"]
