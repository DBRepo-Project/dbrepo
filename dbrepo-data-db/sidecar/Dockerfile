FROM python:3.11-alpine
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"

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

EXPOSE 8080

ENTRYPOINT [ "gunicorn", "--log-level", "DEBUG", "--workers", "4", "--bind", ":8080", "app:app" ]
