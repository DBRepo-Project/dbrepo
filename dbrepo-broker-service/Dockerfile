###### FIRST STAGE ######
FROM dbrepo-metadata-db:latest as dependency
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

###### SECOND STAGE ######
FROM rabbitmq:3-management-alpine as runtime

ENV PYTHONUNBUFFERED=1

COPY ./rabbitmq.conf /etc/rabbitmq/
COPY ./docker-entrypoint.sh ./docker-entrypoint.sh
RUN chmod +x ./docker-entrypoint.sh

RUN apk --no-cache add python3 py3-pip
COPY ./requirements.txt ./requirements.txt
RUN pip3 install -r ./requirements.txt

COPY ./init.py ./init.py

EXPOSE 15692

ENTRYPOINT [ "./docker-entrypoint.sh" ]