FROM python:3.11-alpine3.21
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"

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

USER 1001

COPY --chown=1001 ./app.py ./app.py
COPY --chown=1001 ./ds-yml ./ds-yml

# non-root port
EXPOSE 8080

ENTRYPOINT [ "gunicorn", "--log-level", "debug", "--workers", "4", "--bind", ":8080", "app:app" ]
