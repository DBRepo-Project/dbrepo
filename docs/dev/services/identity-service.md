---
author: Martin Weise
---

## tl;dr

!!! debug "Debug Information"

    Image: [`docker.io/bitnamilegacy/openldap:2.6.8`](https://hub.docker.com/r/openldap)

    * Ports: 1389/tcp, 1636/tcp

    To directly access in Kubernetes (for e.g. debugging), forward the svc port to your local machine:

    ```shell
    kubectl [-n namespace] port-forward svc/identity-service 1389:389
    ```

## Overview

This optional service holds the user identities which we simply call identities in the following. It is integrated into
the [Auth Service](/infrastructures/dbrepo/1.10/dev/services/auth-service) through an LDAP federation, allowing any identity
to authenticate through the Auth Service. The LDAP protocol is not used for authentication. 

The Identity Service can be optionally replaced with your existing LDAP solution. Your LDAP solution should store
users using the RFC 2798 [`InetOrgPerson`](https://datatracker.ietf.org/doc/html/rfc2798) schema which is standard
to most LDAP solutions.

## Identities

Any identity is identified by its `entryUUID` by default in the Auth Service. Note that Keycloak (the software running
the Auth Service) may assign a different UUID to a user. DBRepo **always** uses the UUID provided through the Identity
Service.

The field `uid` is the username and is used for bind/unbind operations. The fields `cn` and `sn` are ignored by the
Auth Service and can be empty `""`.

## Limitations

* Limited support for scaling in Kubernetes, see the
  [guide](https://github.com/jp-gouin/helm-openldap?tab=readme-ov-file#scaling-your-cluster) of the chart developers.
* Currently no support for LDAP in the Data Database.
* Currently no support for LDAP in the Search Database.

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](/infrastructures/dbrepo/1.10/contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

1. By default, no ingress is enabled. If you need ingress on LTP Password and phpLDAPadmin, configure the ingress
   to use your TLS secret `tls-cert-secret` containing the `tls.crt` and `tls.key`, e.g.:

       ```yaml title="values.yaml"
       identityservice:
         ltb-passwd:
           ingress:
             enabled: true
             tls:
               - secretName: tls-cert-secret
                 hosts:
                   - example.com
         phpldapadmin:
           ingress:
             enabled: true
             tls:
               - secretName: tls-cert-secret
                 hosts:
                   - example.com
       ```