import logging
from dataclasses import dataclass
from typing import List

import requests
from dbrepo.api.dto import ApiError
from flask import current_app
from jwt import jwk_from_pem, JWT
from jwt.exceptions import JWTDecodeError


@dataclass(init=True, eq=True)
class User:
    id: str
    username: str
    roles: List[str]


class KeycloakClient:

    def obtain_user_token(self, username: str, password: str) -> str:
        response = requests.post(
            f"{current_app.config['AUTH_SERVICE_ENDPOINT']}/realms/dbrepo/protocol/openid-connect/token",
            data={
                "username": username,
                "password": password,
                "grant_type": "password",
                "client_id": current_app.config["AUTH_SERVICE_CLIENT"],
                "client_secret": current_app.config["AUTH_SERVICE_CLIENT_SECRET"]
            })
        body = response.json()
        if "access_token" not in body:
            raise AssertionError("Failed to obtain user token(s)")
        return response.json()["access_token"]

    def verify_jwt(self, access_token: str) -> ApiError | User:
        public_key = jwk_from_pem(str(current_app.config["JWT_PUBKEY"]).encode('utf-8'))
        payload = JWT().decode(message=access_token, key=public_key, do_time_check=True)
        return User(id=payload.get('uid'), username=payload.get('client_id'),
                    roles=payload.get('realm_access')["roles"])

    def userId(self, auth_header: str | None) -> (str | None, ApiError, int):
        if auth_header is None:
            return None, None, None
        try:
            user = self.verify_jwt(auth_header.split(" ")[1])
            logging.debug(f'mapped JWT to user.id {user.id}')
            return user.id, None, None
        except JWTDecodeError as e:
            logging.error(f'Failed to decode JWT: {e}')
            if str(e) == 'JWT Expired':
                return None, ApiError(status='UNAUTHORIZED', message=f'Token expired',
                                      code='search.user.unauthorized').model_dump(), 401
            return None, ApiError(status='FORBIDDEN', message=str(e), code='search.user.forbidden').model_dump(), 403
