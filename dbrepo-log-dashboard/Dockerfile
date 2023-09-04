FROM opensearchproject/opensearch-dashboards:2.8.0
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

RUN /usr/share/opensearch-dashboards/bin/opensearch-dashboards-plugin remove securityDashboards

COPY --chown=opensearch-dashboards:opensearch-dashboards ./opensearch_dashboards.yml /usr/share/opensearch-dashboards/config/

COPY ./config.yml /usr/share/opensearch-dashboards/config/opensearch-security/config.yml

EXPOSE 5601