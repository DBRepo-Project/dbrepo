FROM --platform=$BUILDPLATFORM python:3.11-alpine3.23
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"
LABEL org.opencontainers.image.source="https://github.com/DBRepo-Project/dbrepo"

RUN apk add --no-cache \
    curl \
    bash \
    jq

COPY Pipfile Pipfile.lock ./

COPY ./lib ./lib

RUN pip install pipenv && \
    pipenv install gunicorn && \
    pipenv install --system --deploy

RUN adduser -D dbrepo --uid 1001

WORKDIR /app

RUN mkdir -p /var/log/app/service/dashboard && \
    chown -R 1001:1001 /var/log/app

USER 1001

COPY --chown=1001 ./init.py ./init.py

ENTRYPOINT [ "python", "./init.py" ]
