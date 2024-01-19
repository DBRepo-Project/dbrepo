---
author: Martin Weise
---

# Authentication Service

## Obtain Access Token

Access tokens are needed for almost all operations.

=== "Terminal"

    ``` console
    curl -X POST \
      -d "username=foo&password=bar&grant_type=password&client_id=dbrepo-client&scope=openid&client_secret=MUwRc7yfXSJwX8AdRMWaQC3Nep1VjwgG" \
      http://localhost/api/auth/realms/dbrepo/protocol/openid-connect/token
    ```

=== "Python"

    ``` py
    import requests

    auth = requests.post("http://localhost/api/auth/realms/dbrepo/protocol/openid-connect/token", data={
        "username": "foo",
        "password": "bar",
        "grant_type": "password",
        "client_id": "dbrepo-client",
        "scope": "openid",
        "client_secret": "MUwRc7yfXSJwX8AdRMWaQC3Nep1VjwgG"
    })
    print(auth.json()["access_token"])
    ```

## Refresh Access Token

Using the response from above, a new access token can be created via the refresh token provided.

=== "Terminal"

    ``` console
    curl -X POST \
      -d "grant_type=refresh_token&client_id=dbrepo-client&refresh_token=THE_REFRESH_TOKEN&client_secret=MUwRc7yfXSJwX8AdRMWaQC3Nep1VjwgG" \
      http://localhost/api/auth/realms/dbrepo/protocol/openid-connect/token
    ```

=== "Python"

    ``` py
    import requests

    auth = requests.post("http://localhost/api/auth/realms/dbrepo/protocol/openid-connect/token", data={
        "grant_type": "refresh_token",
        "client_id": "dbrepo-client",
        "client_secret": "MUwRc7yfXSJwX8AdRMWaQC3Nep1VjwgG",
        "refresh_token": "THE_REFRESH_TOKEN"
    })
    print(auth.json()["access_token"])
    ```

