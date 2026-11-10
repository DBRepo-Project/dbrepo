import logging
import base64
import hashlib
import json
import time
from functools import lru_cache
from typing import List, Any

import requests
from jwt import jwk_from_dict, JWT
from jwt.exceptions import JWTDecodeError

from dbrepo.api.dto import ApiError
from dbrepo.core.api.dto import User


# Public RSA moduli from the retired realm exports must never authenticate requests.
_RETIRED_KEY_HASHES = frozenset({
    "88b617bc95683ece6e94b121496b3632a9587fe5fa6ce42fa871f4894ebb8ec3",
    "8ae7135e87c5beb23ce41dc1051814acfc7276e4ae81c8a1f57b9a57a0eda9f5",
    "94bba692503386291d7f2cafe6b6d8a6cd009a91e365246c3685f22421834d73",
    "adbc16f6af46cdf08c7b70233e4405055224ef857d276893cd94863176e93fc1",
})


@lru_cache(maxsize=16)
def _realm_keys(endpoint: str, interval: int) -> list:
    response = requests.get(f"{endpoint.rstrip('/')}/realms/dbrepo/protocol/openid-connect/certs", timeout=5)
    response.raise_for_status()
    return response.json()["keys"]


class AuthServiceClient:

    def __init__(self, endpoint: str, client_id: str, client_secret: str, jwt_public_key: str | None = None):
        self.endpoint = endpoint
        self.client_id = client_id
        self.client_secret = client_secret
        # Retained as an argument for callers upgrading from the static-key client.
        # Verification always uses this installation's Keycloak keys.

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

    def _user_from_header(self, auth_header: str | None):
        if auth_header is None:
            return None, None, None
        parts = auth_header.split()
        if len(parts) != 2 or parts[0].lower() != 'bearer':
            return None, ApiError(status='UNAUTHORIZED', message='A Bearer token is required',
                                  code='search.user.unauthorized').model_dump(), 401
        try:
            return self.verify_jwt(parts[1]), None, None
        except JWTDecodeError as e:
            logging.error(f'Failed to decode JWT: {e}')
            if str(e) == 'JWT Expired':
                return None, ApiError(status='UNAUTHORIZED', message='Token expired',
                                      code='search.user.unauthorized').model_dump(), 401
            return None, ApiError(status='FORBIDDEN', message=str(e), code='search.user.forbidden').model_dump(), 403

    def get_user_id(self, auth_header: str | None) -> (str | None, ApiError, int):
        user, error, status = self._user_from_header(auth_header)
        return user.id if user is not None else None, error, status

    def get_username(self, auth_header: str | None) -> (str | None, ApiError, int):
        user, error, status = self._user_from_header(auth_header)
        return user.username if user is not None else None, error, status

    def verify_jwt(self, access_token: str) -> User:
        try:
            encoded_header = access_token.split(".")[0]
            header = json.loads(base64.urlsafe_b64decode(encoded_header + "=" * (-len(encoded_header) % 4)))
            if not isinstance(header, dict) or header.get("alg") != "RS256" or not isinstance(header.get("kid"), str):
                raise JWTDecodeError("Unsupported signing key")
            keys = _realm_keys(self.endpoint, int(time.monotonic() // 60))
            key = next((item for item in keys if isinstance(item, dict) and item.get("kid") == header["kid"]
                        and item.get("kty") == "RSA" and item.get("use", "sig") == "sig"
                        and item.get("alg", "RS256") == "RS256"), None)
            if key is None or hashlib.sha256(key["n"].encode()).hexdigest() in _RETIRED_KEY_HASHES:
                raise JWTDecodeError("Unknown or retired signing key")
            payload = JWT().decode(message=access_token, key=jwk_from_dict(key),
                                   algorithms={"RS256"}, do_time_check=True)
            return User(id=payload.get('uid'), username=payload.get('preferred_username'),
                        roles=payload.get('realm_access')["roles"])
        except (requests.RequestException, ValueError, KeyError, TypeError) as error:
            raise JWTDecodeError("Token verification failed") from error

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
