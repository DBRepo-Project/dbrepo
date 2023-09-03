FROM fluentd:v1.16-1

USER root

RUN gem install fluent-plugin-opensearch
RUN fluent-gem install fluent-plugin-rewrite-tag-filter fluent-plugin-multi-format-parser

USER fluent