---
author: Martin Weise
---

# Authentication Service

## Obtain Access Token

Access tokens are needed for almost all operations.

=== "Terminal"

    ```shell
    curl -X POST \
      -d "username=foo&password=bar&grant_type=password&client_id=dbrepo-client&scope=openid&client_secret=MUwRc7yfXSJwX8AdRMWaQC3Nep1VjwgG" \
      http://localhost/api/auth/realms/dbrepo/protocol/openid-connect/token
    ```

=== "Python"

    ```python
    from keycloak import KeycloakOpenID

    # Configure client
    openid = KeycloakOpenID(server_url="http://<hostname>/api/auth",
        realm_name="dbrepo", client_id="dbrepo-client",
        client_secret_key="MUwRc7yfXSJwX8AdRMWaQC3Nep1VjwgG")

    # Get Token
    token = openid.token("username", "password")
    access_token = token['access_token']
    refresh_token = token['refresh_token']
    ```

## Refresh Access Token

Using the response from above, a new access token can be created via the refresh token provided.

=== "Terminal"

    ```shell
    curl -X POST \
      -d "grant_type=refresh_token&client_id=dbrepo-client&refresh_token=THE_REFRESH_TOKEN&client_secret=MUwRc7yfXSJwX8AdRMWaQC3Nep1VjwgG" \
      http://localhost/api/auth/realms/dbrepo/protocol/openid-connect/token
    ```

=== "Python"

    ```python
    from keycloak import KeycloakOpenID

    # Configure client
    openid = KeycloakOpenID(server_url="http://<hostname>/api/auth",
        realm_name="dbrepo", client_id="dbrepo-client",
        client_secret_key="MUwRc7yfXSJwX8AdRMWaQC3Nep1VjwgG")

    # Get Token
    token = keycloak_openid.refresh_token(refresh_token)
    ```

