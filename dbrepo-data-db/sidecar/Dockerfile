FROM python:3.10-alpine

RUN apk add bash curl jq && adduser -D alpine

WORKDIR /home/alpine

COPY Pipfile Pipfile.lock ./

RUN pip install pipenv && \
    pipenv install gunicorn && \
    pipenv install --system --deploy

COPY ./ds-yml ./ds-yml
COPY ./app.py ./app.py

ENV UPLOAD_ENDPOINT="http://upload-service:1080/api/upload"

RUN chown -R alpine:alpine ./
USER alpine

EXPOSE 5000

ENTRYPOINT [ "gunicorn", "-w", "4", "-b", ":5000", "app:app" ]
