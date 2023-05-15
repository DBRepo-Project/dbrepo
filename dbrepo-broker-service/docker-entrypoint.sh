#!/bin/bash

# load jwt certificates
bash ./init.sh

# enable prometheus plugin
(sleep 10; rabbitmq-plugins enable rabbitmq_prometheus rabbitmq_mqtt rabbitmq_auth_backend_oauth2 rabbitmq_auth_mechanism_ssl; touch /ready) &

rabbitmq-server