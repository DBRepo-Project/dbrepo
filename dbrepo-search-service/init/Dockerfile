FROM python:3.11-alpine

RUN apk add bash curl

WORKDIR /home/alpine

COPY Pipfile Pipfile.lock ./

RUN pip install pipenv && \
    pipenv install gunicorn && \
    pipenv install --system --deploy

USER 1001

WORKDIR /app

COPY --chown=1001 ./app.py ./app.py
COPY --chown=1001 ./database.json ./database.json

ENTRYPOINT [ "python", "./app.py" ]
