---
author: Martin Weise
---

# Upload Service

We recommend using a TUS-compatible client:

* [tus-py-client](https://github.com/tus/tus-py-client) (Python)
* [tus-java-client](https://github.com/tus/tus-java-client) (Java)
* [tus-js-client](https://github.com/tus/tus-js-client) (JavaScript/Node.js)
* [tusd](https://github.com/tus/tusd) (Go)

Upload a file to the `dbrepo-upload` bucket in the [Storage Service](./system-services-storage/) using the Node.js
middleware.

=== "Terminal"
    
    ```shell
    curl -X POST \
      -H "Content-Type: multipart/form-data" \
      -F "file=@path/to/file/gps.csv" \
      http://<hostname>/server-middleware/upload
    ```

    !!! info "TUS protocol from terminal"

        Alternatively, use the [`tusc.sh`](https://github.com/adhocore/tusc.sh) terminal client to directly upload to the
        Upload Service.

        ```shell
        tusc -H <hostname>/api/upload/files -f path/to/file/gps.csv
        ```

=== "Python"

    ```python
    from tusclient import client

    my_client = client.TusClient('http://<hostname>/api/upload/files')
    uploader = my_client.uploader('/path/to/file.csv', chunk_size=200)
    uploader.upload()
    ```
