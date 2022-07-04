#!/bin/bash
rabbitmq-server &
java -Dlog4j2.formatMsgNoLookups=true -jar ./rest-service.jar