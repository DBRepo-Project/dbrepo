---
author: Martin Weise
---

# Upload Service

Upload a CSV-file into the `dbrepo-upload` bucket with the console
via `http://<hostname>/admin/storage/browser/dbrepo-upload`.

We recommend using a TUS-compatible client:

* [tus-py-client](https://github.com/tus/tus-py-client) (Python)
* [tus-java-client](https://github.com/tus/tus-java-client) (Java)
* [tus-js-client](https://github.com/tus/tus-js-client) (JavaScript/Node.js)
* [tusd](https://github.com/tus/tusd) (Go)

You can also upload a file `file.csv` in 200 byte chunks with Python:

=== "Python"

    ```python
    #!/bin/env python3
    from tusclient import client
    my_client = client.TusClient('http://localhost/api/upload/files')
    uploader = my_client.uploader('/path/to/file.csv', chunk_size=200)
    uploader.upload()
    ```
