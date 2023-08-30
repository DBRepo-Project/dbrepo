###### FIRST STAGE ######
FROM rabbitmq:3-management-alpine as runtime
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

COPY ./rabbitmq.conf /etc/rabbitmq/rabbitmq.conf

WORKDIR /app

COPY ./cert.pem ./cert.pem
COPY ./pubkey.pem ./pubkey.pem

RUN rabbitmq-plugins enable --offline rabbitmq_prometheus rabbitmq_mqtt rabbitmq_auth_backend_oauth2 rabbitmq_auth_mechanism_ssl

HEALTHCHECK --interval=10s --timeout=5s --retries=12 CMD wget --spider http://localhost:15672/broker/
