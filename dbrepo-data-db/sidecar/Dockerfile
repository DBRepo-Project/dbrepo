FROM python:3.10-alpine

RUN apk add bash curl jq

WORKDIR /app

COPY Pipfile Pipfile.lock ./

RUN pip install pipenv && \
    pipenv install gunicorn && \
    pipenv install --system --deploy

USER 1000

COPY ./clients ./clients
COPY ./ds-yml ./ds-yml
COPY ./app.py ./app.py

ENV S3_STORAGE_ENDPOINT="http://storage-service:9000"
ENV S3_ACCESS_KEY_ID="minioadmin"
ENV S3_SECRET_ACCESS_KEY="minioadmin"

RUN ls -la ./clients

EXPOSE 5000

ENTRYPOINT [ "gunicorn", "-w", "4", "-b", ":5000", "app:app" ]
