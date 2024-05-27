---
author: Martin Weise
---

## tl;dr

!!! debug "Debug Information"

    Image: [`bitnami/rabbitmq:3.12.13-debian-12-r2`](https://hub.docker.com/r/bitnami/rabbitmq)

    * Ports: 5672/tcp, 15672/tcp, 15692/tcp
    * AMQP: `amqp://<hostname>:5672`
    * Prometheus: `http://<hostname>:15692/metrics`
    * Management: `http://<hostname>/admin/broker`

## Overview

It holds exchanges and topics responsible for holding AMQP messages for later consumption. We
use [RabbitMQ](https://www.rabbitmq.com/) in the implementation. By default, the endpoint listens to the insecure port `5672` for incoming 
AMQP tuples and insecure port `15672` for the management UI.

The default configuration creates a user with administrative privileges on the default virtual host `dbrepo`:

* Username: `fda`
* Password: `fda`
* Roles: `["administrator"]`

The Broker Service allows two ways of authentication:

1. Plain
2. OAuth2

For detailed examples how to authenticate with the Broker Service see 
the [usage](/usage-broker) page.

The architecture of the Broker Service is very simple. There is only one durable, topic exchange `dbrepo` and one quorum
queue `dbrepo`, connected with a binding of `dbrepo.#` which routes all tuples with routing key prefix `dbrepo.` (mind 
the dot!) to this queue.

<figure markdown>
   ![Data ingest](./images/queue-quorum.png)
   <figcaption>Replicated quorum queue dbrepo in a cluster with three nodes</figcaption>
</figure>

The consumer takes care of writing it to the correct table in the [Data Service](./system-services-data).

<figure markdown>
   ![Data ingest](./images/exchange-binding.png)
   <figcaption>Architecture Broker Service</figcaption>
</figure>

## Limitations

* No support for MQTT in the [Metadata Service](./system-services-metadata) 
  and [Data Service](./system-services-data) because of MQTT's missing permission system.

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](./contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

For a secure deployment it is necessary to configure the Broker Service as follows:

1. Download the [`rabbitmq.conf`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/dev/dbrepo-broker-service/rabbitmq.conf.secure) and
   change the `default_user` and `default_pass` lines before mounting it to `/etc/rabbitmq/rabbitmq.conf`.
2. Mount your previously generated certificate and RSA public key pair (PEM-encoded) to `/app/cert.pem` 
   and `/app/pubkey.pem`. Note that these are *not* used for TLS encryption, but only for authentication of users. It
   is not recommended to use "real" TLS certificates, self-signed certificates with *sufficient keylength* are best-practice.
3. Mount your TLS certificate authority file into `/etc/rabbitmq/cacert.crt` and your TLS certificate / private key pair
   into `/etc/tls/tls.crt` and `/etc/tls/tls.key`.
