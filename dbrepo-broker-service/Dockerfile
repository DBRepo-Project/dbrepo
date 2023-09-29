###### FIRST STAGE ######
FROM bitnami/rabbitmq:3.10 as runtime
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

COPY ./rabbitmq.conf /etc/rabbitmq/rabbitmq.conf
COPY ./enabled_plugins /etc/rabbitmq/enabled_plugins

WORKDIR /app

COPY ./cert.pem ./cert.pem
COPY ./pubkey.pem ./pubkey.pem
