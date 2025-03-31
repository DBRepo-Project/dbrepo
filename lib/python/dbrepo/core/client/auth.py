import logging
from typing import List, Any

import requests
from jwt import jwk_from_pem, JWT
from jwt.exceptions import JWTDecodeError

from dbrepo.api.dto import ApiError
from dbrepo.core.api.dto import User


class AuthServiceClient:

    def __init__(self, endpoint: str, client_id: str, client_secret: str, jwt_public_key: str):
        self.endpoint = endpoint
        self.client_id = client_id
        self.client_secret = client_secret
        self.jwt_public_key = jwt_public_key

    def get_user_token(self, username: str, password: str) -> str:
        response = requests.post(f"{self.endpoint}/realms/dbrepo/protocol/openid-connect/token",
                                 data={
                                     "username": username,
                                     "password": password,
                                     "grant_type": "password",
                                     "client_id": self.client_id,
                                     "client_secret": self.client_secret
                                 })
        body = response.json()
        if "access_token" not in body:
            raise AssertionError("Failed to obtain user token(s)")
        return response.json()["access_token"]

    def get_user_id(self, auth_header: str | None) -> (str | None, ApiError, int):
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

    def verify_jwt(self, access_token: str) -> User:
        public_key = jwk_from_pem(self.jwt_public_key.encode('utf-8'))
        payload = JWT().decode(message=access_token, key=public_key, do_time_check=True)
        return User(id=payload.get('uid'), username=payload.get('preferred_username'),
                    roles=payload.get('realm_access')["roles"])

    def is_valid_token(self, token: str) -> bool | User:
        if token is None or token == "":
            return False
        try:
            return self.verify_jwt(access_token=token)
        except JWTDecodeError:
            return False

    def is_valid_password(self, username: str, password: str) -> Any:
        if username is None or username == "" or password is None or password == "":
            return False
        try:
            return self.verify_jwt(access_token=self.get_user_token(username=username, password=password))
        except AssertionError as error:
            logging.error(error)
            return False
        except requests.exceptions.ConnectionError as error:
            logging.error(f"Failed to connect to Authentication Service {error}")
            return False

    def get_user_roles(self, user: User) -> List[str]:
        return user.roles
