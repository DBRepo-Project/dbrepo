---
author: Martin Weise
---

# Gateway Service

## tl;dr

!!! debug "Debug Information"

    Image: [`nginx:1.25-alpine-slim`](https://hub.docker.com/r/nginx)

    * Ports: 80/tcp

## Overview

Provides a single point of access to the *application programming interface* (API) and configures a
standard [NGINX](https://www.nginx.com/) reverse proxy for load balancing. This component is optional if you already have a load balancer
or reverse proxy running.

### Settings

To setup SSL/TLS encryption, mount your TLS certificate and TLS private key into the container directly into the 
`/etc/nginx/` directory.

```yaml title="docker-compose.yml"
services:
  dbrepo-gateway-service:
    image: docker.io/nginx:1.25-alpine-slim
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./fullchain.pem:/etc/nginx/fullchain.pem
      - ./privkey.pem:/etc/nginx/privkey.pem
  ...
```

If your TLS private key as a password, you need to specify it in the `dbrepo.conf` file.

## Limitations

(none relevant to DBRepo)

## Security

1. Enable TLS encryption by downloading 
   the [`dbrepo.conf`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/master/dbrepo-gateway-service/dbrepo.conf)
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
