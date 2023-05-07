FROM python:3.9-alpine
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

RUN apk update && apk --no-cache add build-base gcc python3-dev libpq-dev libffi-dev mariadb-dev mariadb-connector-c \
    mariadb-connector-c-dev curl bash py3-pandas py3-sqlalchemy py3-requests py3-gevent py3-psycopg2

COPY ./requirements.txt ./requirements.txt
RUN pip install -r ./requirements.txt

WORKDIR /app

ENV FLASK_APP=app.py
ENV FLASK_RUN_HOST=0.0.0.0
ENV PORT_APP=5010
ENV FLASK_DEBUG=0
ENV HOSTNAME=semantics-service
ENV LOG_LEVEL=debug
ENV METADATA_DB=fda
ENV METADATA_USERNAME=root
ENV METADATA_PASSWORD=dbrepo
ENV JWT_ISSUER=http://localhost:8080/realms/dbrepo
ENV JWT_PUBKEY=public-key

COPY ./requirements.txt ./requirements.txt
COPY ./healthcheck.sh ./healthcheck.sh

RUN pip install -r requirements.txt > /dev/null

HEALTHCHECK --interval=10s --timeout=5s --retries=12 CMD ["bash", "/app/healthcheck.sh"]

EXPOSE $PORT_APP

COPY ./us-yml ./us-yml
COPY ./ontologies ./ontologies
COPY ./*.py ./

ENTRYPOINT [ "python", "-u", "./pywsgi.py" ]
