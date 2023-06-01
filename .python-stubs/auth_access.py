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
