---
author: Martin Weise
---

## Prometheus Metrics

We expose Prometheus metrics on all endpoints of the REST API. [Prometheus](https://prometheus.io/) is a cloud-native
monitoring system using a time-series database. In the default deployment (Docker Compose / Kubernetes) no Prometheus
instance is started.

You need can setup Prometheus in a few minutes using
a [Docker container](https://prometheus.io/docs/prometheus/latest/installation/).

## Dashboards

<figure markdown>
![DBRepo Dashboard](/infrastructures/dbrepo/images/screenshots/grafana4.png)
</figure>

<figure markdown>
![MariaDB Galera Dashboard](/infrastructures/dbrepo/images/screenshots/grafana3.png)
</figure>

<figure markdown>
![RabbitMQ Dashboard](/infrastructures/dbrepo/images/screenshots/grafana5.png)
</figure>