import logging
import os
from dataclasses import dataclass
from typing import List

import requests
from dbrepo.api.dto import ApiError
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
            f"{os.getenv('AUTH_SERVICE_ENDPOINT', 'http://auth-service:8080')}/realms/dbrepo/protocol/openid-connect/token",
            data={
                "username": username,
                "password": password,
                "grant_type": "password",
                "client_id": os.getenv("AUTH_SERVICE_CLIENT", "dbrepo-client"),
                "client_secret": os.getenv("AUTH_SERVICE_CLIENT_SECRET", "MUwRc7yfXSJwX8AdRMWaQC3Nep1VjwgG")
            })
        body = response.json()
        if "access_token" not in body:
            raise AssertionError("Failed to obtain user token(s)")
        return response.json()["access_token"]

    def verify_jwt(self, access_token: str) -> ApiError | User:
        public_key = jwk_from_pem(str('-----BEGIN PUBLIC KEY-----\n' + os.getenv("JWT_PUBKEY",
                                                                                 "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqqnHQ2BWWW9vDNLRCcxD++xZg/16oqMo/c1l+lcFEjjAIJjJp/HqrPYU/U9GvquGE6PbVFtTzW1KcKawOW+FJNOA3CGo8Q1TFEfz43B8rZpKsFbJKvQGVv1Z4HaKPvLUm7iMm8Hv91cLduuoWx6Q3DPe2vg13GKKEZe7UFghF+0T9u8EKzA/XqQ0OiICmsmYPbwvf9N3bCKsB/Y10EYmZRb8IhCoV9mmO5TxgWgiuNeCTtNCv2ePYqL/U0WvyGFW0reasIK8eg3KrAUj8DpyOgPOVBn3lBGf+3KFSYi+0bwZbJZWqbC/Xlk20Go1YfeJPRIt7ImxD27R/lNjgDO/MwIDAQAB") + '\n-----END PUBLIC KEY-----').encode(
            'utf-8'))
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
