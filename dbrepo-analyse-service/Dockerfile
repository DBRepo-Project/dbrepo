FROM python:3.11-alpine3.21
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"

RUN apk --no-cache add \
    bash \
    curl \
    openjdk8-jre

COPY Pipfile Pipfile.lock ./

COPY ./lib ./lib

RUN pip install pipenv && \
    pipenv install gunicorn && \
    pipenv install --system --deploy

RUN adduser -D dbrepo --uid 1001

WORKDIR /app

USER 1001

COPY --chown=1001 ./api ./api
COPY --chown=1001 ./as-yml ./as-yml
COPY --chown=1001 ./*.py ./

# non-root port
EXPOSE 8080

ENTRYPOINT [ "gunicorn", "--log-level", "DEBUG", "--workers", "4", "--bind", ":8080", "app:app" ]
