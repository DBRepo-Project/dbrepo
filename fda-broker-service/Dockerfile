###### FIRST STAGE ######
FROM fda-metadata-db:latest as dependency
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

###### SECOND STAGE ######
FROM rabbitmq:3-management-alpine as runtime

ENV PYTHONUNBUFFERED=1

COPY ./rabbitmq.conf /etc/rabbitmq/

RUN apk --no-cache add python3 py3-pip
COPY ./requirements.txt ./requirements.txt
RUN pip3 install -r ./requirements.txt

WORKDIR /app

COPY ./init.py ./init.py
COPY ./register.py ./register.py
COPY ./service_ready /usr/bin/service_ready
COPY ./docker-entrypoint.sh ./docker-entrypoint.sh

HEALTHCHECK --interval=10s --timeout=5s --retries=12 CMD service_ready

ENTRYPOINT [ "bash", "/app/docker-entrypoint.sh" ]