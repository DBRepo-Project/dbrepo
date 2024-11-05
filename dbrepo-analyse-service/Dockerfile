FROM python:3.11-alpine
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"

RUN apk --no-cache \
    add \
    bash \
    curl \
    openjdk8-jre

COPY Pipfile Pipfile.lock ./

COPY ./lib ./lib

RUN pip install pipenv && \
    pipenv install gunicorn && \
    pipenv install --system --deploy

RUN adduser -D analyse-service --uid 1000

WORKDIR /app

USER 1000

COPY --chown=1000 ./api ./api
COPY --chown=1000 ./as-yml ./as-yml
COPY --chown=1000 ./clients ./clients
COPY --chown=1000 ./*.py ./

# non-root port
EXPOSE 8080

ENTRYPOINT [ "gunicorn", "--log-level", "DEBUG", "--workers", "4", "--bind", ":8080", "app:app" ]
