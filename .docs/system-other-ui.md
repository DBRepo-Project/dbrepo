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

It provides a *user interface* (UI) for a researcher to interact with the database repository's API.

<figure markdown>
![Data ingest](images/ui.png){ .img-border }
<figcaption>User Interface</figcaption>
</figure>

<figure markdown>
![UI microservice architecture detailed](images/architecture-ui.svg)
<figcaption>Architecture of the UI microservice</figcaption>
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
