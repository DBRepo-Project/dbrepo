---
author: Martin Weise
---

## tl;dr

!!! debug "Debug Information"

    Image: [`docker.io/openldap:2.6.8-debian-12-r1`](https://hub.docker.com/r/openldap)

    * Ports: 1389/tcp, 1636/tcp

## Overview

This service holds the user identities which we simply call identities in the following. It is integrated into the 
[Auth Service](../auth-service) through an LDAP federation, allowing any identity to authenticate through the Auth
Service. The LDAP protocol is not used for authentication. You can use your own identity provider, e.g. Active
Directory.

## Identities

Any identity is identified by its `entryUUID` by default in the Auth Service. Note that Keycloak (the software running
the Auth Service) may assign a different UUID to a user. DBRepo **always** uses the UUID provided through the Identity
Service.

## Limitations

* Limited support for scaling in Kubernetes, see the
  [guide](https://github.com/jp-gouin/helm-openldap?tab=readme-ov-file#scaling-your-cluster) of the chart developers.

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](../contact) with us, we happily answer requests for collaboration with attached CV and your programming 
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