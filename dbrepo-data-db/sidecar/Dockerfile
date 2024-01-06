FROM python:3.10-alpine

RUN apk add bash curl jq mariadb-client

ENV PYTHONFAULTHANDLER=1

COPY Pipfile Pipfile.lock ./

RUN pip install pipenv && \
    pipenv install gunicorn && \
    pipenv install --system --deploy

USER 1001

WORKDIR /app

COPY --chown=1001 ./clients ./clients
COPY --chown=1001 ./ds-yml ./ds-yml
COPY --chown=1001 ./app.py ./app.py

ENV S3_STORAGE_ENDPOINT="http://storage-service:9000"
ENV S3_ACCESS_KEY_ID="seaweedfsadmin"
ENV S3_SECRET_ACCESS_KEY="seaweedfsadmin"

EXPOSE 3305

ENTRYPOINT [ "gunicorn", "--log-level", "DEBUG", "--workers", "4", "--bind", ":3305", "app:app" ]
