FROM python:3.10-alpine

RUN apk add bash curl && adduser -D alpine

WORKDIR /home/alpine

COPY Pipfile Pipfile.lock ./

RUN pip install pipenv && \
    pipenv install gunicorn && \
    pipenv install --system --deploy

COPY ./app ./app
COPY ./scripts ./scripts
COPY ./us-yml ./us-yml
COPY config.py wsgi.py ./

ENV FLASK_APP=wsgi.py

RUN chown -R alpine:alpine ./
USER alpine

ENTRYPOINT ["sh", "./scripts/docker-entrypoint.sh"]
