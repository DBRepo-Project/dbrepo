FROM python:3.11-alpine
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"

RUN apk add --no-cache curl bash jq

WORKDIR /home/alpine

COPY Pipfile Pipfile.lock ./

COPY ./lib ./lib

RUN pip install pipenv && \
    pipenv install gunicorn && \
    pipenv install --system --deploy

USER 1001

WORKDIR /app

COPY --chown=1001 ./clients ./clients
COPY --chown=1001 ./omlib ./omlib
COPY --chown=1001 ./app.py ./app.py
COPY --chown=1001 ./database.json ./database.json

ENTRYPOINT [ "python", "./app.py" ]
