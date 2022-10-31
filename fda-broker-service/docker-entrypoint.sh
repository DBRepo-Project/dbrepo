#!/bin/bash
rabbitmq-server &

# enable prometheus plugin
sleep 10 && rabbitmq-plugins enable rabbitmq_prometheus

# register with discovery service
python3 ./init.py