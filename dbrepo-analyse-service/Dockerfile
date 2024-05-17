FROM python:3.11-alpine
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

RUN apk add bash curl

WORKDIR /home/alpine

COPY Pipfile Pipfile.lock ./

COPY ./lib ./lib

RUN pip install pipenv && \
    pipenv install gunicorn && \
    pipenv install --system --deploy

USER 1001

WORKDIR /app

COPY --chown=1001 ./api ./api
COPY --chown=1001 ./as-yml ./as-yml
COPY --chown=1001 ./clients ./clients
COPY --chown=1001 ./*.py ./

# non-root port
EXPOSE 8080

ENTRYPOINT [ "gunicorn", "--log-level", "DEBUG", "--workers", "4", "--bind", ":8080", "app:app" ]
