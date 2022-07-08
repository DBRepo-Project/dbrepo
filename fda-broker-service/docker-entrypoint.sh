#!/bin/bash
rabbitmq-server &
java -Dlog4j2.formatMsgNoLookups=true -Duser.timezone=UTC -jar ./rest-service.jar