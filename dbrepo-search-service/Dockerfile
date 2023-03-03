FROM elasticsearch:7.13.4 AS runtime
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

ENV discovery.type=single-node
ENV ES_JAVA_OPTS="-Xms2g -Xmx2g"
ENV logger.level=WARN
ENV bootstrap.memory_lock=true
ENV xpack.security.enabled=true
ENV ELASTIC_PASSWORD=elastic

HEALTHCHECK --interval=5s --timeout=3s CMD curl -u "elastic:${ELASTIC_PASSWORD}" -f -X GET "localhost:9200/_cluster/health?wait_for_status=green&timeout=1s" || exit 1