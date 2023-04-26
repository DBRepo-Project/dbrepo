import requests

auth = requests.post("http://localhost/api/auth/realms/dbrepo/protocol/openid-connect/token", data={
    "grant_type": "refresh_token",
    "client_id": "dbrepo-client",
    "client_secret": "MUwRc7yfXSJwX8AdRMWaQC3Nep1VjwgG",
    "refresh_token": "THE_REFRESH_TOKEN"
})
print(auth.json()["access_token"])
