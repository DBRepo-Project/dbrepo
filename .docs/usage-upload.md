---
author: Martin Weise
---

# Upload Service

Uploads a file `file.csv` in 200 byte chunks.

=== "Python"

    ```python
    #!/bin/env python3
    from tusclient import client
    my_client = client.TusClient('http://localhost/api/upload/files')
    uploader = my_client.uploader('/path/to/file.csv', chunk_size=200)
    uploader.upload()
    ```
