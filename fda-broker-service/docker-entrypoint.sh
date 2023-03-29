#!/bin/bash

# load jwt certificates
python3 ./init.py

# enable prometheus plugin
(sleep 10; rabbitmq-plugins enable rabbitmq_prometheus rabbitmq_mqtt rabbitmq_auth_backend_oauth2 rabbitmq_auth_mechanism_ssl; touch /ready) &

# register with discovery service
python3 ./register.py
(while sleep 60; do python3 ./register.py; done) &

rabbitmq-server