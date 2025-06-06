---
author: Martin Weise
---

## tl;dr

!!! debug "Debug Information"

    Image: [`bitnami/rabbitmq:3.13.7`](https://hub.docker.com/r/bitnami/rabbitmq)

    * Ports: 5672/tcp, 15672/tcp, 15692/tcp
    * AMQP: `amqp://<hostname>:5672`
    * Prometheus: `http://<hostname>:15692/metrics`
    * Management: `http://<hostname>:15672`

    To directly access in Kubernetes (for e.g. debugging), forward the svc port to your local machine:

    ```shell
    kubectl [-n namespace] port-forward svc/broker-service 15672:15672
    ```

## Overview

It holds exchanges and topics responsible for holding AMQP messages for later consumption. We
use [RabbitMQ](https://www.rabbitmq.com/) in the implementation. By default, the endpoint listens to the insecure port `5672` for incoming 
AMQP tuples and insecure port `15672` for the management UI.

## Supported Protocols

* AMQP (v0.9.1, v1.0), see [RabbitMQ docs](https://www.rabbitmq.com/docs/next/amqp).
* MQTT (v3.1, v3.1.1, v5), see [RabbitMQ docs](https://www.rabbitmq.com/docs/mqtt).

## Authentication

The default configuration allows any user in the `cn=system,ou=users,dc=dbrepo,dc=at` from the 
[Identity Service](../identity-service) to access the Broker Service as user with `administrator` role, i.e. the
`cn=admin,dc=dbrepo,dc=at` user that is created by default.

The Broker Service allows two ways of authentication for AMQP tuples:

1. LDAP
2. Plain (RabbitMQ's internal authentication)

## Architecture

The queue architecture of the Broker Service is very simple. There is only one durable, topic exchange `dbrepo` and one
quorum queue `dbrepo`, connected with a binding of `dbrepo.#` which routes all tuples with routing key prefix `dbrepo.`
to this queue.

<figure markdown>
   ![Data ingest](../images/queue-quorum.png)
   <figcaption>Replicated quorum queue dbrepo in a cluster with three nodes</figcaption>
</figure>

The consumer takes care of writing it to the correct table in the [Data Service](../system-services-data).

<figure markdown>
   ![Data ingest](../images/exchange-binding.png)
   <figcaption>Architecture Broker Service</figcaption>
</figure>

## Limitations

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](../contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

For a secure deployment it is necessary to configure the Broker Service as follows:

1. Once you change the admin password of the [Identity Service](../identity-service), you need to change it in the
   `rabbitmq.conf` as well: `auth_ldap.dn_lookup_bind.password=newpassword`.
2. Enable TLS and mount your previously generated certificate and RSA public key pair (PEM-encoded) to `/app/cert.pem` 
   and `/app/pubkey.pem`. Note that these are *not* used for TLS encryption, but only for authentication of users. It
   is not recommended to use "real" TLS certificates, self-signed certificates with *sufficient keylength* are 
   best-practice. Mount your TLS certificate authority file into `/etc/rabbitmq/cacert.crt` and your TLS certificate 
   / private key pair into `/etc/tls/tls.crt` and `/etc/tls/tls.key`.
