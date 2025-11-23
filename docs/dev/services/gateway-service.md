---
author: Martin Weise
---

## tl;dr

!!! debug "Debug Information"

    Image: [`docker.io/nginx:1.27.3-alpine3.20-slim`](https://hub.docker.com/r/nginx)

    * Ports: 80/tcp

## Overview

Provides a single point of access to the *application programming interface* (API) and configures a
standard [NGINX](https://www.nginx.com/) reverse proxy for load balancing. This component is optional if you already
have a load balancer or reverse proxy running.

## Settings

### SSL/TLS Security

To setup SSL/TLS encryption, mount your TLS certificate and TLS private key into the container directly into the
`/etc/nginx/` directory.

```yaml title="docker-compose.yml"
services:
  ...
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

### Connection Timeouts

The reverse proxy has a defined timeout of 90 seconds on all requests (these are also enforced in the 
[User Interface](/infrastructures/dbrepo/1.13/dev/services/ui) using the [`axios`](https://www.npmjs.com/package/axios) 
module). For large databases these timeouts may need to be increased, e.g. the timeout for creating subsets is by 
default already increased to 600 seconds.

### User Interface

To serve the [User Interface](/infrastructures/dbrepo/1.13/dev/services/ui/) under different port than `80`, change the port
mapping in
the `docker-compose.yml` to e.g. port `8000`:

```yaml title="docker-compose.yml"
services:
  ...
  dbrepo-gateway-service:
    image: docker.io/nginx:1.27.0-alpine3.19-slim
    ports:
      - "8000:80"
  ...
```

## Monitoring (Optional)

By default the Gateway Service is not monitored. You need to add the following to the `docker-compose.yml` file.

```yaml title="docker-compose.yml"
services:
  ...
  dbrepo-gateway-service-sidecar:
    restart: "no"
    container_name: dbrepo-gateway-service-sidecar
    hostname: dbrepo-gateway-service-sidecar
    image: docker.io/nginx/nginx-prometheus-exporter:1.3.0
    command:
      - "-nginx.scrape-uri=http://gateway-service/basic_status"
    ports:
      - "9113:9113"
    depends_on:
      dbrepo-gateway-service:
        condition: service_started
    logging:
      driver: json-file
```

Then, uncomment the scrape config from the `prometheus.yml` file.

```yaml title="prometheus.yml"
scrape_configs:
  ...
  - job_name: 'gateway scrape'
    metrics_path: '/metrics'
    static_configs:
      - targets: ['dbrepo-gateway-service-sidecar:9113']
```

## Limitations

(none relevant to DBRepo)

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](/infrastructures/dbrepo/1.13/contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

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
