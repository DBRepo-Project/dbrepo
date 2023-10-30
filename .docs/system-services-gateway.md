---
author: Martin Weise
---

# Gateway Service

## tl;dr

!!! debug "Debug Information"

    Image: [`nginx:1.25-alpine-slim`](https://hub.docker.com/r/nginx)

    * Ports: 80/tcp, 443/tcp

## Overview

Provides a single point of access to the *application programming interface* (API) and configures a
standard [NGINX](https://www.nginx.com/) reverse proxy for load balancing, SSL/TLS configuration.

## Limitations

(none relevant to DBRepo)

## Security

1. Enable TLS encryption by downloading 
   the [`dbrepo.conf`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/dev/dbrepo-gateway-service/dbrepo.conf)
   and editing the *server* block to include your TLS certificate (with trust chain) `fullchain.pem` and TLS private key
   `privkey.pem` (PEM-encoded).

       ```nginx
       server {
         listen 443 ssl;
         server_name _;
         ssl_certificate     /etc/nginx/fullchain.pem;
         ssl_certificate_key /etc/nginx/privkey.pem;
         ...
       }
       ```
