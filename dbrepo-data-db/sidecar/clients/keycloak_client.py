import logging
from dataclasses import dataclass
import requests
from flask import current_app
from typing import List

from jwt import jwk_from_pem, JWT


@dataclass(init=True, eq=True)
class User:
    username: str
    roles: List[str]


class KeycloakClient:

    def obtain_user_token(self, username: str, password: str) -> str:
        response = requests.post(f"{current_app.config['KEYCLOAK_HOST']}/realms/dbrepo/protocol/openid-connect/token",
                                 data={
                                     "username": username,
                                     "password": password,
                                     "grant_type": "password",
                                     "client_id": current_app.config["AUTH_SERVICE_CLIENT"],
                                     "client_secret": current_app.config["KEYCLOAK_CLIENT_SECRET"]
                                 })
        body = response.json()
        if "access_token" not in body:
            raise AssertionError("Failed to obtain user token(s)")
        return response.json()["access_token"]

    def verify_jwt(self, access_token: str) -> User:
        public_key = jwk_from_pem(str(current_app.config["JWT_PUBKEY"]).encode('utf-8'))
        payload = JWT().decode(message=access_token, key=public_key, do_time_check=True)
        return User(username=payload.get('client_id'), roles=payload.get('realm_access')["roles"])
