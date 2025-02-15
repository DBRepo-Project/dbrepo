FROM python:3.11-alpine3.21
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

COPY --chown=1001 ./init/clients ./clients
COPY --chown=1001 ./init/omlib ./omlib
COPY --chown=1001 ./init/friendly_names_overrides.json ./friendly_names_overrides.json
COPY --chown=1001 ./os-yml ./os-yml
COPY --chown=1001 ./app.py ./app.py

# non-root port
EXPOSE 8080

ENTRYPOINT [ "gunicorn", "--log-level", "debug", "--workers", "4", "--bind", ":8080", "app:app" ]
