#!/bin/bash

# enable prometheus plugin
(sleep 10; rabbitmq-plugins enable rabbitmq_prometheus) &

# register with discovery service
python3 ./init.py
(while sleep 60; do python3 ./init.py; done) &

rabbitmq-server