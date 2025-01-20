FROM python:3.11-alpine
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"

RUN apk add --no-cache alpine-sdk \
    gcc \
    python3-dev \
    mariadb-connector-c-dev

WORKDIR /home/alpine

COPY Pipfile Pipfile.lock ./

RUN pip install pipenv && \
    pipenv install --system --deploy

USER 1001

WORKDIR /app

COPY --chown=1001 ./app.py ./app.py

ENTRYPOINT [ "python", "app.py" ]