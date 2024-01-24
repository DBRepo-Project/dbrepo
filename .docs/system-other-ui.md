---
author: Martin Weise
---

# User Interface

## tl;dr

!!! debug "Debug Information"

    Image: [`dbrepo/ui:$TAG`](https://hub.docker.com/r/dbrepo/ui)

    * Ports: 3000/tcp, 9100/tcp
    * Prometheus: `http://<hostname>:9100/metrics`
    * UI: `http://<hostname>/`

## Overview

It provides a graphical interface for a researcher to interact with the API (c.f. Figure 1). 

<figure markdown>
![User Interface](images/screenshots/ui.png){ .img-border }
<figcaption>Figure 1: User Interface</figcaption>
</figure>

For examples on how to use the User Interface, visit the [Usage Overview](../usage-overview/) to find out how to create
users, databases and how to import your data.

### Settings

The User Interface can be configured extensively with 
the [`dbrepo.config.json`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/master/dbrepo-ui/dbrepo.config.json)
configuration file, mounted directly into the container with e.g. docker compose. As a small example, you can configure
the logo :material-numeric-1-circle-outline: in Figure 2. Make sure you mount the logo as image as well, in this example
we want to mount a custom logo `my_logo.png` into the container and specify the name.

<figure markdown>
![Architecture of the UI microservice](images/screenshots/ui-config-step-1.png){ .img-border }
<figcaption>Figure 2: Architecture of the UI microservice</figcaption>
</figure>

Text values like the version :material-numeric-2-circle-outline: and title :material-numeric-3-circle-outline: can be
configured as well via the `dbrepo.config.json` values file. The important links section 
:material-numeric-4-circle-outline: can be modified or removed entirely by setting `page.information.links` to `[]`.

```json title="dbrepo.config.json"
{
  "title": "Database Repository",
  "version": "$TAG_DOCKER-COMPOSE",
  "logo": {
    "path": "/my_logo.png"
  },
  "page": {
    "information": {
      "links": []
    }
  },
  ...
}
```

To work, you need to mount the `my_logo.png` file into the `dbrepo-ui` container via the `docker-compose.yml` file (or
if you use a Kubernetes deployment via ConfigMap and Volumes).

```yaml title="docker-compose.yml"
services:
  dbrepo-ui:
    image: docker.io/dbrepo/ui:$TAG
    volumes:
      - ./my_logo.png:/app/static/my_logo.png
      - ./dbrepo.conf.json:/app/dbrepo.conf.json
  ...
```

### Architecture

<figure markdown>
![Architecture of the UI microservice](images/architecture-ui.svg)
<figcaption>Figure 3: Architecture of the UI microservice</figcaption>
</figure>

### Example

Upload a file to the `dbrepo-upload` bucket in the [Storage Service](../system-services-storage/) using the Node.js
middleware. The request must be sent with the `Content-Type: multipart/form-data` header and the file must be placed
in the `file` field of the form. For example:

```shell
curl -X POST \
  -F "file=@path/to/file/gps.csv" \
  http://<hostname>/server-middleware/upload
```

The response looks like this:

```json
{
  "fieldname": "file",
  "originalname": "gps.csv",
  "encoding": "7bit",
  "mimetype": "text/csv",
  "buffer": {
    "type": "Buffer",
    "data": [
      34,
      73,
      ...
    ]
  },
  "size": 130279,
  "etag": "9d23e73f4ed9f7e5afc80e696db69ebb"
}
```

## Limitations

(none)

## Security

(none)
