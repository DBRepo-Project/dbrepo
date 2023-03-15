FROM elasticsearch:7.13.4 AS runtime
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

ENV discovery.type=single-node
ENV ES_JAVA_OPTS="-Xms2g -Xmx2g"
ENV logger.level=WARN
ENV bootstrap.memory_lock=true
ENV xpack.security.enabled=true
ENV ELASTIC_PASSWORD=elastic