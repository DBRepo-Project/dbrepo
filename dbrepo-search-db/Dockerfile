FROM opensearchproject/opensearch:2.10.0 AS runtime

USER root

RUN yum install -y jq

COPY ./limits.conf /etc/security/limits.conf

WORKDIR /usr/share/opensearch

RUN chmod 0700 ./config
COPY --chown=opensearch:opensearch ./opensearch.yml ./config/opensearch.yml
COPY --chown=opensearch:opensearch ./config.yml ./config/opensearch-security/config.yml

RUN chmod 0600 ./config/opensearch.yml
RUN chmod 0600 ./config/opensearch-security/config.yml

COPY --chown=opensearch:opensearch ./pem/admin.pem ./config/admin.pem
COPY --chown=opensearch:opensearch ./pem/admin-key.pem ./config/admin-key.pem
RUN chmod 0600 ./config/admin*.pem
COPY --chown=opensearch:opensearch ./pem/node1.pem ./config/node1.pem
COPY --chown=opensearch:opensearch ./pem/node1-key.pem ./config/node1-key.pem
RUN chmod 0600 ./config/node1*.pem
COPY --chown=opensearch:opensearch ./pem/root-ca.pem ./config/root-ca.pem
COPY --chown=opensearch:opensearch ./pem/root-ca-key.pem ./config/root-ca-key.pem
RUN chmod 0600 ./config/root-ca*.pem

USER opensearch

ENV DISABLE_INSTALL_DEMO_CONFIG=true
