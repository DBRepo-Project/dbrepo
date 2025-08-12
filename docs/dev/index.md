---
author: Martin Weise
---

Development of DBRepo.

## Build from Source

Requirements:

* [Docker CE](https://docs.docker.com/engine/install/)

=== "Debian 12"

    ```shell
    apt install make \
      git \
      maven \
      openjdk-17-jdk \
      python3 \
      python3-venv
    ```

Clone the repository and `cd` into it:

```shell
git clone https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services.git && cd ./fda-services
```

Build only the images from source:

```shell
make build-images
```

!!! example "Build everything from source"

    You can of course build all auxiliary files and the images from source:

    ```bash
    #!/bin/bash
    python3 -m venv venv && \
      source ./venv/bin/activate && \
      pip install pipenv && \
      pipenv install --dev
    make build-java-lib build-python-lib build-auth-event-listener build-images
    ```

## Support

| Branch                                                        | Initial Release | End of Life |
|---------------------------------------------------------------|-----------------|-------------|
| <span class="support-active">:material-circle:</span> 1.10.x  | 2025-06-27      | 2025-09-27  |
| <span class="support-active">:material-circle:</span> 1.9.x   | 2025-05-30      | 2025-08-30  |
| <span class="support-inactive">:material-circle:</span> 1.8.x | 2025-04-04      | 2025-07-04  |
| <span class="support-inactive">:material-circle:</span> 1.7.x | 2025-03-07      | 2025-06-07  |
| <span class="support-inactive">:material-circle:</span> 1.6.x | 2025-01-07      | 2025-04-07  |
| <span class="support-inactive">:material-circle:</span> 1.5.x | 2024-11-07      | 2025-01-07  |
| <span class="support-inactive">:material-circle:</span> 1.4.x | 2024-01-19      | 2025-04-19  |