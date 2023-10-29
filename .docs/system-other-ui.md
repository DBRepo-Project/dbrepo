---
author: Martin Weise
---

# UI

## tl;dr

!!! debug "Debug Information"

    Image: [`dbrepo/ui:latest`](https://hub.docker.com/r/dbrepo/ui)

    * Ports: 3000/tcp, 9100/tcp
    * Prometheus: `http://:9100/metrics`
    * UI: `http://:3000/`

## Overview

It provides a *user interface* (UI) for a researcher to interact with the database repository's API.

<figure markdown>
   ![Data ingest](images/ui.png){ .img-border }
   <figcaption>User Interface</figcaption>
</figure>

<figure markdown>
![UI microservice architecture detailed](images/architecture-ui.png)
<figcaption>Architecture of the UI microservice</figcaption>
</figure>

## Limitations

(none)

## Security

(none)
