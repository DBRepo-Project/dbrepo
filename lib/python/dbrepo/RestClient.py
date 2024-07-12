import os
import sys
import logging
import time

import requests
from pydantic import TypeAdapter
from tusclient.client import TusClient
from pandas import DataFrame

from dbrepo.UploadClient import UploadClient
from dbrepo.api.dto import *
from dbrepo.api.exceptions import ResponseCodeError, UsernameExistsError, EmailExistsError, NotExistsError, \
    ForbiddenError, MalformedError, NameExistsError, QueryStoreError, ExternalSystemError, \
    AuthenticationError, UploadError, FormatNotAvailable, RequestError, ServiceError, ServiceConnectionError

logger = logging.getLogger("RestClient")


class RestClient:
    """
    The RestClient class for communicating with the DBRepo REST API. All parameters can be set also via environment \
    variables, e.g. set endpoint with REST_API_ENDPOINT, username with REST_API_USERNAME, etc. You can override \
    the constructor parameters with the environment variables.

    :param endpoint: The REST API endpoint. Optional. Default: "http://gateway-service"
    :param username: The REST API username. Optional.
    :param password: The REST API password. Optional.
    :param secure: When set to false, the requests library will not verify the authenticity of your TLS/SSL
        certificates (i.e. when using self-signed certificates). Default: true.
    """
    endpoint: str = None
    username: str = None
    password: str = None
    secure: bool = None

    def __init__(self,
                 endpoint: str = 'http://gateway-service',
                 username: str = None,
                 password: str = None,
                 secure: bool = True) -> None:
        logging.basicConfig(format='%(asctime)s %(name)-12s %(levelname)-6s %(message)s', level=logging.DEBUG,
                            stream=sys.stdout)
        self.endpoint = os.environ.get('REST_API_ENDPOINT', endpoint)
        self.username = os.environ.get('REST_API_USERNAME', username)
        self.password = os.environ.get('REST_API_PASSWORD', password)
        if os.environ.get('REST_API_SECURE') is not None:
            self.secure = os.environ.get('REST_API_SECURE') == 'True'
        else:
            self.secure = secure
        logger.debug(
            f'initialized rest client with endpoint={self.endpoint}, username={username}, verify_ssl={secure}')

    def _wrapper(self, method: str, url: str, params: [(str,)] = None, payload=None, headers: dict = None,
                 force_auth: bool = False, stream: bool = False) -> requests.Response:
        if force_auth and (self.username is None and self.password is None):
            raise AuthenticationError(f"Failed to perform request: authentication required")
        url = f'{self.endpoint}{url}'
        logger.debug(f'method: {method}')
        logger.debug(f'url: {url}')
        if params is not None:
            logger.debug(f'params: {params}')
        if stream is not None:
            logger.debug(f'stream: {stream}')
        logger.debug(f'secure: {self.secure}')
        if headers is not None:
            logger.debug(f'headers: {headers}')
        else:
            headers = dict()
            logger.debug(f'no headers set')
        if payload is not None:
            payload = payload.model_dump()
        auth = None
        if self.username is None and self.password is not None:
            headers["Authorization"] = f"Bearer {self.password}"
            logger.debug(f'configured for oidc/bearer auth')
        elif self.username is not None and self.password is not None:
            auth = (self.username, self.password)
            logger.debug(f'configured for basic auth: username={self.username}, password=(hidden)')
        return requests.request(method=method, url=url, auth=auth, verify=self.secure,
                                json=payload, headers=headers, params=params, stream=stream)

    def upload(self, file_path: str) -> str:
        """
        Uploads a file located at file_path to the Upload Service.

        :param file_path: The location of the file on the local filesystem.

        :returns: Filename on the S3 backend of the Upload Service, if successful.
        """
        my_client = TusClient(url=f'{self.endpoint}/api/upload/files/')
        uploader = my_client.uploader(file_path=file_path)
        uploader.upload()
        filename = uploader.url[uploader.url.rfind('/') + 1:uploader.url.rfind('+')]
        if filename is None or len(filename) == 0:
            raise UploadError(f'Failed to upload the file to {self.endpoint}')
        return filename

    def get_jwt_auth(self, username: str = None, password: str = None) -> JwtAuth:
        """
        Obtains a JWT auth object from the auth service containing e.g. the access token and refresh token.

        :param username: The username used to authenticate with the auth service. Optional. Default: username from the `RestClient` constructor.
        :param password: The password used to authenticate with the auth service. Optional. Default: password from the `RestClient` constructor.

        :returns: JWT auth object from the auth service, if successful.

        :raises MalformedError: If the payload was rejected by the service.
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises AuthenticationError: If something went wrong with the authentication.
        :raises ServiceConnectionError: If something went wrong with connection to the auth service.
        :raises ServiceError: If something went wrong with obtaining the information in the auth service.
        :raises ResponseCodeError: If something went wrong with the authentication.
        """
        if username is None:
            username = self.username
        if password is None:
            password = self.password
        url = f'{self.endpoint}/api/user/token'
        response = requests.post(url=url, json=dict({"username": username, "password": password}))
        if response.status_code == 202:
            body = response.json()
            return JwtAuth.model_validate(body)
        if response.status_code == 400:
            raise MalformedError(f'Failed to get JWT: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to get JWT: not allowed')
        if response.status_code == 428:
            raise AuthenticationError(f'Failed to get JWT: account not fully setup (requires password change?)')
        if response.status_code == 502:
            raise ServiceConnectionError(f'Failed to get JWT: failed to establish connection with auth service')
        if response.status_code == 503:
            raise ServiceError(f'Failed to get JWT: failed to get user in auth service')
        raise ResponseCodeError(f'Failed to get JWT: response code: {response.status_code} is not '
                                f'202 (ACCEPTED): {response.text}')

    def refresh_jwt_auth(self, refresh_token: str) -> JwtAuth:
        """
        Refreshes a JWT auth object from the auth service containing e.g. the access token and refresh token.

        :param refresh_token: The refresh token.

        :returns: JWT auth object from the auth service, if successful.

        :raises MalformedError: If the payload was rejected by the service.
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises ServiceConnectionError: If something went wrong with the connection to the auth service.
        :raises ResponseCodeError: If something went wrong with the authentication.
        """
        url = f'{self.endpoint}/api/user/token'
        response = self._wrapper(method="put", url=url, payload={"refresh_token": refresh_token})
        if response.status_code == 202:
            body = response.json()
            return JwtAuth.model_validate(body)
        if response.status_code == 400:
            raise MalformedError(f'Failed to refresh JWT: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to refresh JWT: not allowed')
        if response.status_code == 502:
            raise ServiceConnectionError(f'Failed to refresh JWT: failed to establish connection with auth service')
        raise ResponseCodeError(f'Failed to refresh JWT: response code: {response.status_code} is not '
                                f'202 (ACCEPTED): {response.text}')

    def whoami(self) -> str | None:
        """
        Print the username.

        :returns: The username, if set.
        """
        if self.username is not None:
            print(f"{self.username}")
            return self.username
        print(f"No username set!")
        return None

    def get_users(self) -> List[UserBrief]:
        """
        Get all users.

        :returns: List of users, if successful.

        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/user'
        response = self._wrapper(method="get", url=url)
        if response.status_code == 200:
            body = response.json()
            return TypeAdapter(List[UserBrief]).validate_python(body)
        raise ResponseCodeError(f'Failed to find users: response code: {response.status_code} is not '
                                f'200 (OK): {response.text}')

    def get_units(self) -> List[Unit]:
        """
        Get all units known to the metadata database.

        :returns: List of units, if successful.

        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/unit'
        response = self._wrapper(method="get", url=url)
        if response.status_code == 200:
            body = response.json()
            return TypeAdapter(List[Unit]).validate_python(body)
        raise ResponseCodeError(f'Failed to find units: response code: {response.status_code} is not '
                                f'200 (OK): {response.text}')

    def get_user(self, user_id: str) -> User:
        """
        Get a user with given user id.

        :returns: The user, if successful.

        :raises ResponseCodeError: If something went wrong with the retrieval.
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the user does not exist.
        """
        url = f'/api/user/{user_id}'
        response = self._wrapper(method="get", url=url)
        if response.status_code == 200:
            body = response.json()
            return User.model_validate(body)
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to find user: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to find user: not found')
        raise ResponseCodeError(f'Failed to find user: response code: {response.status_code} is not '
                                f'200 (OK): {response.text}')

    def create_user(self, username: str, password: str, email: str) -> UserBrief:
        """
        Creates a new user.

        :param username: The username of the new user. Must be unique.
        :param password: The password of the new user.
        :param email: The email of the new user. Must be unique.

        :returns: The user, if successful.

        :raises MalformedError: If the payload was rejected by the service.
        :raises ForbiddenError: If the internal authentication to the auth service is invalid.
        :raises UsernameExistsError: The username exists already.
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the created user was not found in the auth service.
        :raises EmailExistsError: The email exists already.
        :raises ServiceConnectionError: If something went wrong with connection to the auth service.
        :raises ServiceError: If something went wrong with obtaining the information in the auth service.
        """
        url = f'/api/user'
        response = self._wrapper(method="post", url=url,
                                 payload=CreateUser(username=username, password=password, email=email))
        if response.status_code == 201:
            body = response.json()
            return UserBrief.model_validate(body)
        if response.status_code == 400:
            raise MalformedError(f'Failed to create user: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to create user: internal authentication to the auth service is invalid')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to create user: created user not found in auth service')
        if response.status_code == 409:
            raise UsernameExistsError(f'Failed to create user: user with username exists')
        if response.status_code == 417:
            raise EmailExistsError(f'Failed to create user: user with e-mail exists')
        if response.status_code == 502:
            raise ServiceConnectionError(f'Failed to create user: failed to establish connection with auth service')
        if response.status_code == 503:
            raise ServiceError(f'Failed to create user: failed to create in auth service')
        raise ResponseCodeError(f'Failed to create user: response code: {response.status_code} is not '
                                f'201 (CREATED): {response.text}')

    def update_user(self, user_id: str, theme: str, language: str, firstname: str = None, lastname: str = None,
                    affiliation: str = None, orcid: str = None) -> User:
        """
        Updates a user with given user id.

        :param user_id: The user id of the user that should be updated.
        :param theme: The user theme. One of "light", "dark", "light-contrast", "dark-contrast".
        :param language: The user language localization. One of "en", "de".
        :param firstname: The updated given name. Optional.
        :param lastname: The updated family name. Optional.
        :param affiliation: The updated affiliation identifier. Optional.
        :param orcid: The updated ORCID identifier. Optional.

        :returns: The user, if successful.

        :raises MalformedError: If the payload was rejected by the service.
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the user does not exist.
        :raises ResponseCodeError: If something went wrong with the update.
        """
        url = f'/api/user/{user_id}'
        response = self._wrapper(method="put", url=url, force_auth=True,
                                 payload=UpdateUser(theme=theme, language=language, firstname=firstname,
                                                    lastname=lastname, affiliation=affiliation, orcid=orcid))
        if response.status_code == 202:
            body = response.json()
            return User.model_validate(body)
        if response.status_code == 400:
            raise MalformedError(f'Failed to update user: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to update user password: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to update user: user not found')
        raise ResponseCodeError(f'Failed to update user: response code: {response.status_code} is not '
                                f'202 (ACCEPTED): {response.text}')

    def update_user_password(self, user_id: str, password: str) -> User:
        """
        Updates the password of a user with given user id.

        :param user_id: The user id of the user that should be updated.
        :param password: The updated user password.

        :returns: The user, if successful.

        :raises MalformedError: If the payload was rejected by the service.
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the user does not exist.
        :raises ServiceConnectionError: If something went wrong with connection to the auth service.
        :raises ServiceError: If something went wrong with obtaining the information in the auth service.
        :raises ResponseCodeError: If something went wrong with the update.
        """
        url = f'/api/user/{user_id}/password'
        response = self._wrapper(method="put", url=url, force_auth=True, payload=UpdateUserPassword(password=password))
        if response.status_code == 202:
            body = response.json()
            return User.model_validate(body)
        if response.status_code == 400:
            raise MalformedError(f'Failed to update user password: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to update user password: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to update user password: not found')
        if response.status_code == 502:
            raise ServiceConnectionError(
                f'Failed to update user password: failed to establish connection with auth service')
        if response.status_code == 503:
            raise ServiceError(f'Failed to update user password: failed to update in auth service')
        raise ResponseCodeError(f'Failed to update user theme: response code: {response.status_code} is not '
                                f'202 (ACCEPTED): {response.text}')

    def get_containers(self) -> List[ContainerBrief]:
        """
        Get all containers.

        :returns: List of containers, if successful.

        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/container'
        response = self._wrapper(method="get", url=url)
        if response.status_code == 200:
            body = response.json()
            return TypeAdapter(List[ContainerBrief]).validate_python(body)
        raise ResponseCodeError(f'Failed to find containers: response code: {response.status_code} is not '
                                f'200 (OK): {response.text}')

    def get_container(self, container_id: int) -> Container:
        """
        Get a container with given id.

        :returns: List of containers, if successful.

        :raises NotExistsError: If the container does not exist.
        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/container/{container_id}'
        response = self._wrapper(method="get", url=url)
        if response.status_code == 200:
            body = response.json()
            return Container.model_validate(body)
        if response.status_code == 404:
            raise NotExistsError(f'Failed to get container: not found')
        raise ResponseCodeError(f'Failed to get container: response code: {response.status_code} is not '
                                f'200 (OK): {response.text}')

    def get_databases(self) -> List[DatabaseBrief]:
        """
        Get all databases.

        :returns: List of databases, if successful.

        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/database'
        response = self._wrapper(method="get", url=url)
        if response.status_code == 200:
            body = response.json()
            return TypeAdapter(List[DatabaseBrief]).validate_python(body)
        raise ResponseCodeError(f'Failed to find databases: response code: {response.status_code} is not '
                                f'200 (OK): {response.text}')

    def get_databases_count(self) -> int:
        """
        Count all databases.

        :returns: Count of databases if successful.
        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/database'
        response = self._wrapper(method="head", url=url)
        if response.status_code == 200:
            return int(response.headers.get("x-count"))
        raise ResponseCodeError(f'Failed to find databases: response code: {response.status_code} is not '
                                f'200 (OK): {response.text}')

    def get_database(self, database_id: int) -> Database:
        """
        Get a databases with given id.

        :param database_id: The database id.

        :returns: The database, if successful.

        :raises NotExistsError: If the container does not exist.
        :raises ServiceConnectionError: If something went wrong with connection to the broker service.
        :raises ServiceError: If something went wrong with obtaining the information in the broker service.
        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/database/{database_id}'
        response = self._wrapper(method="get", url=url)
        if response.status_code == 200:
            body = response.json()
            return Database.model_validate(body)
        if response.status_code == 404:
            raise NotExistsError(f'Failed to find database: not found')
        if response.status_code == 502:
            raise ServiceConnectionError(f'Failed to find database: failed to establish connection with broker service')
        if response.status_code == 503:
            raise ServiceError(f'Failed to find database: failed to obtain queue metadata from broker service')
        raise ResponseCodeError(f'Failed to find database: response code: {response.status_code} is not '
                                f'200 (OK): {response.text}')

    def create_database(self, name: str, container_id: int, is_public: bool) -> Database:
        """
        Create a databases in a container with given container id.

        :param name: The name of the database.
        :param container_id: The container id.
        :param is_public: The visibility of the database. If set to true everything will be visible, otherwise only
                the metadata (schema, identifiers) will be visible to the public.

        :returns: The database, if successful.

        :raises MalformedError: If the payload was rejected by the service.
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the container does not exist.
        :raises QueryStoreError: If something went wrong with the query store.
        :raises ServiceConnectionError: If something went wrong with connection to the search service.
        :raises ServiceError: If something went wrong with obtaining the information in the search service.
        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/database'
        response = self._wrapper(method="post", url=url, force_auth=True,
                                 payload=CreateDatabase(name=name, container_id=container_id, is_public=is_public))
        if response.status_code == 201:
            body = response.json()
            return Database.model_validate(body)
        if response.status_code == 400:
            raise MalformedError(f'Failed to create database: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to create database: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to create database: container not found')
        if response.status_code == 409:
            raise QueryStoreError(f'Failed to create database: failed to create query store in data database')
        if response.status_code == 502:
            raise ServiceConnectionError(f'Failed to create database: failed to establish connection to search service')
        if response.status_code == 503:
            raise ServiceError(f'Failed to create database: failed to create in search service')
        raise ResponseCodeError(f'Failed to create database: response code: {response.status_code} is not '
                                f'201 (CREATED): {response.text}')

    def create_container(self, name: str, host: str, image_id: int, sidecar_host: str, sidecar_port: int,
                         privileged_username: str, privileged_password: str, port: int = None, ui_host: str = None,
                         ui_port: int = None) -> Container:
        """
        Register a container instance executing a given container image. Note that this does not create a container,
        but only saves it in the metadata database to be used within DBRepo. The container still needs to be created
        through e.g. `docker run image:tag -d`.

        :param name: The container name.
        :param host: The container hostname.
        :param image_id: The container image id.
        :param sidecar_host: The container sidecar hostname.
        :param sidecar_port: The container sidecar port.
        :param privileged_username: The container privileged user username.
        :param privileged_password: The container privileged user password.
        :param port: The container port bound to the host. Optional.
        :param ui_host: The container hostname displayed in the user interface. Optional. Default: value of `host`
        :param ui_port: The container port displayed in the user interface. Optional. Default: `default_port` of image.

        :returns: The container, if successful.

        :raises MalformedError: If the payload was rejected by the service.
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the container does not exist.
        :raises NameExistsError: If a container with this name already exists.
        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/container'
        response = self._wrapper(method="post", url=url, force_auth=True,
                                 payload=CreateContainer(name=name, host=host, image_id=image_id,
                                                         sidecar_host=sidecar_host, sidecar_port=sidecar_port,
                                                         privileged_username=privileged_username,
                                                         privileged_password=privileged_password, port=port,
                                                         ui_host=ui_host, ui_port=ui_port))
        if response.status_code == 201:
            body = response.json()
            return Container.model_validate(body)
        if response.status_code == 400:
            raise MalformedError(f'Failed to create container: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to create container: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to create container: container not found')
        if response.status_code == 409:
            raise NameExistsError(f'Failed to create container: container name already exists')
        raise ResponseCodeError(f'Failed to create container: response code: {response.status_code} is not '
                                f'201 (CREATED): {response.text}')

    def update_database_visibility(self, database_id: int, is_public: bool) -> Database:
        """
        Updates the database visibility of a database with given database id.

        :param database_id: The database id.
        :param is_public: The visibility of the database. If set to true everything will be visible, otherwise only
                the metadata (schema, identifiers) will be visible to the public.

        :returns: The database, if successful.

        :raises MalformedError: If the payload was rejected by the service.
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the database does not exist.
        :raises ServiceConnectionError: If something went wrong with connection to the search service.
        :raises ServiceError: If something went wrong with obtaining the information in the search service.
        :raises ResponseCodeError: If something went wrong with the update.
        """
        url = f'/api/database/{database_id}'
        response = self._wrapper(method="put", url=url, force_auth=True, payload=ModifyVisibility(is_public=is_public))
        if response.status_code == 202:
            body = response.json()
            return Database.model_validate(body)
        if response.status_code == 400:
            raise MalformedError(f'Failed to update database visibility: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to update database visibility: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to update database visibility: not found')
        if response.status_code == 502:
            raise ServiceConnectionError(
                f'Failed to update database visibility: failed to establish connection to search service')
        if response.status_code == 503:
            raise ServiceError(f'Failed to update database visibility: failed to update in search service')
        raise ResponseCodeError(
            f'Failed to update database visibility: response code: {response.status_code} is not 202 (ACCEPTED)')

    def update_database_owner(self, database_id: int, user_id: str) -> Database:
        """
        Updates the database owner of a database with given database id.

        :param database_id: The database id.
        :param user_id: The user id of the new owner.

        :returns: The database, if successful.

        :raises MalformedError: If the payload was rejected by the service.
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the database does not exist.
        :raises ServiceConnectionError: If something went wrong with connection to the search service.
        :raises ServiceError: If something went wrong with obtaining the information in the search service.
        :raises ResponseCodeError: If something went wrong with the update.
        """
        url = f'/api/database/{database_id}/owner'
        response = self._wrapper(method="put", url=url, force_auth=True, payload=ModifyOwner(id=user_id))
        if response.status_code == 202:
            body = response.json()
            return Database.model_validate(body)
        if response.status_code == 400:
            raise MalformedError(f'Failed to update database visibility: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to update database visibility: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to update database visibility: not found')
        if response.status_code == 502:
            raise ServiceConnectionError(
                f'Failed to update database visibility: failed to establish connection to search service')
        if response.status_code == 503:
            raise ServiceError(
                f'Failed to update database visibility: failed to update in search service')
        raise ResponseCodeError(
            f'Failed to update database visibility: response code: {response.status_code} is not 202 (ACCEPTED)')

    def update_database_schema(self, database_id: int) -> Database:
        """
        Updates the database table and view metadata of a database with given database id.

        :param database_id: The database id.

        :returns: The updated database, if successful.

        :raises MalformedError: If the payload was rejected by the service.
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the database does not exist.
        :raises ServiceConnectionError: If something went wrong with connection to the data service.
        :raises ServiceError: If something went wrong with obtaining the information in the data service.
        :raises ResponseCodeError: If something went wrong with the update.
        """
        url = f'/api/database/{database_id}/metadata/table'
        response = self._wrapper(method="put", url=url, force_auth=True)
        if response.status_code == 200:
            response.json()
            url = f'/api/database/{database_id}/metadata/view'
            response = self._wrapper(method="put", url=url, force_auth=True)
            if response.status_code == 200:
                body = response.json()
                return Database.model_validate(body)
        if response.status_code == 400:
            raise MalformedError(f'Failed to update database schema: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to update database schema: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to update database schema: not found')
        if response.status_code == 502:
            raise ServiceConnectionError(
                f'Failed to update database schema: failed to establish connection to search service')
        if response.status_code == 503:
            raise ServiceError(f'Failed to update database schema: failed to update in search service')
        raise ResponseCodeError(
            f'Failed to update database schema: response code: {response.status_code} is not 200 (OK)')

    def create_table(self, database_id: int, name: str, columns: List[CreateTableColumn],
                     constraints: CreateTableConstraints, description: str = None) -> Table:
        """
        Updates the database owner of a database with given database id.

        :param database_id: The database id.
        :param name: The name of the created table.
        :param constraints: The constraints of the created table.
        :param columns: The columns of the created table.
        :param description: The description of the created table. Optional.

        :returns: The table, if successful.

        :raises MalformedError: If the payload is rejected by the service.
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the database does not exist.
        :raises NameExistsError: If a table with this name already exists.
        :raises ServiceConnectionError: If something went wrong with connection to the data service.
        :raises ServiceError: If something went wrong with obtaining the information in the data service.
        :raises ResponseCodeError: If something went wrong with the creation.
        """
        url = f'/api/database/{database_id}/table'
        response = self._wrapper(method="post", url=url, force_auth=True,
                                 payload=CreateTable(name=name, description=description,
                                                     columns=columns, constraints=constraints))
        if response.status_code == 201:
            body = response.json()
            return Table.model_validate(body)
        if response.status_code == 400:
            raise MalformedError(f'Failed to create table: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to create table: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to create table: not found')
        if response.status_code == 409:
            raise NameExistsError(f'Failed to create table: table name exists')
        if response.status_code == 502:
            raise ServiceConnectionError(f'Failed to create table: failed to establish connection to data service')
        if response.status_code == 503:
            raise ServiceError(f'Failed to create table: failed to create table in data service')
        raise ResponseCodeError(f'Failed to create table: response code: {response.status_code} is not '
                                f'201 (CREATED): {response.text}')

    def get_tables(self, database_id: int) -> List[TableBrief]:
        """
        Get all tables.

        :param database_id: The database id.

        :returns: List of tables, if successful.

        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the database does not exist.
        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/database/{database_id}/table'
        response = self._wrapper(method="get", url=url)
        if response.status_code == 200:
            body = response.json()
            return TypeAdapter(List[TableBrief]).validate_python(body)
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to get tables: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to get tables: database not found')
        raise ResponseCodeError(f'Failed to get tables: response code: {response.status_code} is not '
                                f'200 (OK): {response.text}')

    def get_table(self, database_id: int, table_id: int) -> Table:
        """
        Get a table with given database id and table id.

        :param database_id: The database id.
        :param table_id: The table id.

        :returns: List of tables, if successful.

        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the table does not exist.
        :raises ServiceConnectionError: If something went wrong with connection to the metadata service.
        :raises ServiceError: If something went wrong with obtaining the information in the metadata service.
        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/database/{database_id}/table/{table_id}'
        response = self._wrapper(method="get", url=url)
        if response.status_code == 200:
            body = response.json()
            return Table.model_validate(body)
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to find table: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to find table: not found')
        if response.status_code == 502:
            raise ServiceConnectionError(f'Failed to find table: failed to establish connection to broker service')
        if response.status_code == 503:
            raise ServiceError(f'Failed to find table: failed to obtain queue information from broker service')
        raise ResponseCodeError(f'Failed to find table: response code: {response.status_code} is not '
                                f'200 (OK): {response.text}')

    def delete_table(self, database_id: int, table_id: int) -> None:
        """
        Delete a table with given database id and table id.

        :param database_id: The database id.
        :param table_id: The table id.

        :raises MalformedError: If the payload is rejected by the service.
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the container does not exist.
        :raises ServiceConnectionError: If something went wrong with connection to the data service.
        :raises ServiceError: If something went wrong with obtaining the information in the data service.
        :raises ResponseCodeError: If something went wrong with the deletion.
        """
        url = f'/api/database/{database_id}/table/{table_id}'
        response = self._wrapper(method="delete", url=url, force_auth=True)
        if response.status_code == 202:
            return
        if response.status_code == 400:
            raise MalformedError(f'Failed to delete table: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to delete table: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to delete table: not found')
        if response.status_code == 502:
            raise ServiceConnectionError(f'Failed to delete table: failed to establish connection to search service')
        if response.status_code == 503:
            raise ServiceError(f'Failed to delete table: failed to delete in search service')
        raise ResponseCodeError(f'Failed to delete table: response code: {response.status_code} is not '
                                f'202 (ACCEPTED): {response.text}')

    def delete_container(self, container_id: int) -> None:
        """
        Deletes a container with given id. Note that this does not delete the container, but deletes the entry in the
        metadata database. The container still needs to be removed, e.g. `docker container stop hash` and then
        `docker container rm hash`.

        :param container_id: The container id.

        :raises MalformedError: If the payload is rejected by the service.
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the container does not exist.
        :raises ResponseCodeError: If something went wrong with the deletion.
        """
        url = f'/api/container/{container_id}'
        response = self._wrapper(method="delete", url=url, force_auth=True)
        if response.status_code == 202:
            return
        if response.status_code == 400:
            raise MalformedError(f'Failed to delete container: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to delete container: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to delete container: not found')
        raise ResponseCodeError(f'Failed to delete container: response code: {response.status_code} is not '
                                f'202 (ACCEPTED): {response.text}')

    def get_table_metadata(self, database_id: int) -> Database:
        """
        Generate metadata of all system-versioned tables in a database with given id.

        :param database_id: The database id.

        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the table does not exist.
        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/database/{database_id}/metadata/table'
        response = self._wrapper(method="put", url=url, force_auth=True)
        if response.status_code == 200:
            body = response.json()
            return Database.model_validate(body)
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to get tables metadata: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to get tables metadata: not found')
        raise ResponseCodeError(f'Failed to get tables metadata: response code: {response.status_code} is not '
                                f'200 (OK): {response.text}')

    def get_table_history(self, database_id: int, table_id: int, size: int = 100) -> Database:
        """
        Get the table history of insert/delete operations.

        :param database_id: The database id.
        :param table_id: The table id.
        :param size: The number of operations. Optional. Default: 100.

        :raises MalformedError: If the payload is rejected by the service.
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the table does not exist.
        :raises ServiceError: If something went wrong with obtaining the information in the data service.
        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/database/{database_id}/table/{table_id}/history?size={size}'
        response = self._wrapper(method="get", url=url, force_auth=True)
        if response.status_code == 200:
            body = response.json()
            return Database.model_validate(body)
        if response.status_code == 400:
            raise MalformedError(f'Failed to get table history: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to get table history: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to get table history: not found')
        if response.status_code == 503:
            raise ServiceError(f'Failed to get table history: failed to establish connection with metadata service')
        raise ResponseCodeError(f'Failed to get table history: response code: {response.status_code} is not '
                                f'200 (OK): {response.text}')

    def get_views(self, database_id: int) -> List[View]:
        """
        Gets views of a database with given database id.

        :param database_id: The database id.

        :returns: The list of views, if successful.

        :raises NotExistsError: If the container does not exist.
        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/database/{database_id}/view'
        response = self._wrapper(method="get", url=url)
        if response.status_code == 200:
            body = response.json()
            return TypeAdapter(List[View]).validate_python(body)
        if response.status_code == 404:
            raise NotExistsError(f'Failed to find views: not found')
        raise ResponseCodeError(f'Failed to find views: response code: {response.status_code} is not '
                                f'200 (OK): {response.text}')

    def get_view(self, database_id: int, view_id: int) -> View:
        """
        Get a view of a database with given database id and view id.

        :param database_id: The database id.
        :param view_id: The view id.

        :returns: The view, if successful.

        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the container does not exist.
        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/database/{database_id}/view/{view_id}'
        response = self._wrapper(method="get", url=url)
        if response.status_code == 200:
            body = response.json()
            return View.model_validate(body)
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to find view: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to find view: not found')
        raise ResponseCodeError(f'Failed to find view: response code: {response.status_code} is not '
                                f'200 (OK): {response.text}')

    def create_view(self, database_id: int, name: str, query: str, is_public: bool) -> View:
        """
        Create a view in a database with given database id.

        :param database_id: The database id.
        :param name: The name of the created view.
        :param query: The query of the created view.
        :param is_public: The visibility of the view. If set to true everything will be visible, otherwise only
                the metadata (schema, identifiers) will be visible to the public.

        :returns: The created view, if successful.

        :raises MalformedError: If the payload is rejected by the service.
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the database does not exist.
        :raises ExternalSystemError: If the mapped view creation query is erroneous.
        :raises ServiceConnectionError: If something went wrong with connection to the search service.
        :raises ServiceError: If something went wrong with obtaining the information in the search service.
        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/database/{database_id}/view'
        response = self._wrapper(method="post", url=url, force_auth=True,
                                 payload=CreateView(name=name, query=query, is_public=is_public))
        if response.status_code == 201:
            body = response.json()
            return View.model_validate(body)
        if response.status_code == 400:
            raise MalformedError(f'Failed to create view: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to create view: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to create view: not found')
        if response.status_code == 423:
            raise ExternalSystemError(f'Failed to create view: mapped invalid query: {response.text}')
        if response.status_code == 502:
            raise ServiceConnectionError(f'Failed to create view: failed to establish connection to search service')
        if response.status_code == 503:
            raise ServiceError(f'Failed to create view: failed to save in search service')
        raise ResponseCodeError(f'Failed to create view: response code: {response.status_code} is not '
                                f'201 (CREATED): {response.text}')

    def delete_view(self, database_id: int, view_id: int) -> None:
        """
        Deletes a view in a database with given database id and view id.

        :param database_id: The database id.
        :param view_id: The view id.

        :raises MalformedError: If the payload is rejected by the service.
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the container does not exist.
        :raises ExternalSystemError: If the mapped view deletion query is erroneous.
        :raises ServiceConnectionError: If something went wrong with connection to the search service.
        :raises ServiceError: If something went wrong with obtaining the information in the search service.
        :raises ResponseCodeError: If something went wrong with the deletion.
        """
        url = f'/api/database/{database_id}/view/{view_id}'
        response = self._wrapper(method="delete", url=url, force_auth=True)
        if response.status_code == 202:
            return
        if response.status_code == 400:
            raise MalformedError(f'Failed to delete view: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to delete view: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to delete view: not found')
        if response.status_code == 423:
            raise ExternalSystemError(f'Failed to delete view: mapped invalid delete query')
        if response.status_code == 502:
            raise ServiceConnectionError(f'Failed to delete view: failed to establish connection to search service')
        if response.status_code == 503:
            raise ServiceError(f'Failed to delete view: failed to save in search service')
        raise ResponseCodeError(f'Failed to delete view: response code: {response.status_code} is not '
                                f'202 (ACCEPTED): {response.text}')

    def get_view_data(self, database_id: int, view_id: int, page: int = 0, size: int = 10,
                      df: bool = False) -> Result | DataFrame:
        """
        Get data of a view in a database with given database id and view id.

        :param database_id: The database id.
        :param view_id: The view id.
        :param page: The result pagination number. Optional. Default: 0.
        :param size: The result pagination size. Optional. Default: 10.
        :param df: If true, the result is returned as Pandas DataFrame. Optional. Default: False.

        :returns: The result of the view query, if successful.

        :raises MalformedError: If the payload is rejected by the service.
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the view does not exist.
        :raises ExternalSystemError: If the mapped view selection query is erroneous.
        :raises ServiceError: If something went wrong with obtaining the information in the metadata service.
        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/database/{database_id}/view/{view_id}/data'
        params = []
        if page is not None and size is not None:
            params.append(('page', page))
            params.append(('size', size))
        response = self._wrapper(method="get", url=url, params=params)
        if response.status_code == 200:
            body = response.json()
            res = Result.model_validate(body)
            if df:
                return DataFrame.from_records(res.result)
            return res
        if response.status_code == 400:
            raise MalformedError(f'Failed to get view data: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to get view data: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to get view data: not found')
        if response.status_code == 409:
            raise ExternalSystemError(f'Failed to get view data: mapping failed: {response.text}')
        if response.status_code == 503:
            raise ServiceError(f'Failed to get view data: data service failed to establish connection to '
                               f'metadata service')
        raise ResponseCodeError(f'Failed to get view data: response code: {response.status_code} is not '
                                f'200 (OK):{response.text}')

    def get_views_metadata(self, database_id: int) -> Database:
        """
        Generate metadata of all views in a database with given id.

        :param database_id: The database id.

        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the container does not exist.
        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/database/{database_id}/metadata/view'
        response = self._wrapper(method="put", url=url, force_auth=True)
        if response.status_code == 200:
            body = response.json()
            return Database.model_validate(body)
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to get views metadata: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to get views metadata: not found')
        raise ResponseCodeError(f'Failed to get views metadata: response code: {response.status_code} is not '
                                f'200 (OK): {response.text}')

    def get_table_data(self, database_id: int, table_id: int, page: int = 0, size: int = 10,
                       timestamp: datetime.datetime = None, df: bool = False) -> Result | DataFrame:
        """
        Get data of a table in a database with given database id and table id.

        :param database_id: The database id.
        :param table_id: The table id.
        :param page: The result pagination number. Optional. Default: 0.
        :param size: The result pagination size. Optional. Default: 10.
        :param timestamp: The query execution time. Optional.
        :param df: If true, the result is returned as Pandas DataFrame. Optional. Default: False.

        :returns: The result of the view query, if successful.

        :raises MalformedError: If the payload is rejected by the service.
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the table does not exist.
        :raises ServiceError: If something went wrong with obtaining the information in the metadata service.
        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/database/{database_id}/table/{table_id}/data'
        params = []
        if page is not None and size is not None:
            params.append(('page', page))
            params.append(('size', size))
        if timestamp is not None:
            params.append(('timestamp', timestamp))
        response = self._wrapper(method="get", url=url, params=params)
        if response.status_code == 200:
            body = response.json()
            res = Result.model_validate(body)
            if df:
                return DataFrame.from_records(res.result)
            return res
        if response.status_code == 400:
            raise MalformedError(f'Failed to get table data: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to get table data: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to get table data: not found')
        if response.status_code == 503:
            raise ServiceError(f'Failed to get table data: data service failed to establish connection to '
                               f'metadata service')
        raise ResponseCodeError(f'Failed to get table data: response code: {response.status_code} is not '
                                f'200 (OK): {response.text}')

    def create_table_data(self, database_id: int, table_id: int, data: dict) -> None:
        """
        Insert data into a table in a database with given database id and table id.

        :param database_id: The database id.
        :param table_id: The table id.
        :param data: The data dictionary to be inserted into the table with the form column=value of the table.

        :raises MalformedError: If the payload is rejected by the service (e.g. LOB could not be imported).
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the table does not exist.
        :raises ServiceError: If something went wrong with obtaining the information in the metadata service.
        :raises ResponseCodeError: If something went wrong with the insert.
        """
        url = f'/api/database/{database_id}/table/{table_id}/data'
        response = self._wrapper(method="post", url=url, force_auth=True, payload=CreateData(data=data))
        if response.status_code == 201:
            return
        if response.status_code == 400:
            raise MalformedError(f'Failed to insert table data: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to insert table data: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to insert table data: not found')
        if response.status_code == 503:
            raise ServiceError(
                f'Failed to insert table data: data service failed to establish connection to metadata service')
        raise ResponseCodeError(f'Failed to insert table data: response code: {response.status_code} is not '
                                f'201 (CREATED): {response.text}')

    def import_table_data(self, database_id: int, table_id: int, file_name_or_data_frame: str | DataFrame,
                          separator: str = None, quote: str = None, skip_lines: int = 0,
                          false_encoding: str = None, true_encoding: str = None, null_encoding: str = None,
                          line_encoding: str = "\n") -> None:
        """
        Import a csv dataset from a file into a table in a database with given database id and table id.

        :param database_id: The database id.
        :param table_id: The table id.
        :param file_name_or_data_frame: The path of the file that is imported on the storage service or pandas dataframe.
        :param separator: The csv column separator. Optional.
        :param quote: The column data quotation character. Optional.
        :param skip_lines: The number of lines to skip. Optional. Default: 0.
        :param false_encoding: The encoding of boolean false. Optional.
        :param true_encoding: The encoding of boolean true. Optional.
        :param null_encoding: The encoding of null. Optional.
        :param line_encoding: The encoding of the line termination. Optional. Default: CR (Windows).

        :raises MalformedError: If the payload is rejected by the service (e.g. LOB could not be imported).
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the table does not exist.
        :raises ServiceError: If something went wrong with obtaining the information in the metadata service.
        :raises ResponseCodeError: If something went wrong with the insert.
        """
        client = UploadClient(endpoint=f"{self.endpoint}/api/upload/files")
        if type(file_name_or_data_frame) is DataFrame:
            file_path: str = f"./tmp-{time.time()}"
            df: DataFrame = file_name_or_data_frame
            df.to_csv(path_or_buf=file_path, index=False, header=False)
        else:
            file_path: str = file_name_or_data_frame
        filename = client.upload(file_path=file_path)
        url = f'/api/database/{database_id}/table/{table_id}/data/import'
        response = self._wrapper(method="post", url=url, force_auth=True,
                                 payload=Import(location=filename, separator=separator, quote=quote,
                                                skip_lines=skip_lines, false_element=false_encoding,
                                                true_element=true_encoding, null_element=null_encoding,
                                                line_termination=line_encoding))
        if response.status_code == 202:
            return
        if response.status_code == 400:
            raise MalformedError(f'Failed to import table data: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to import table data: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to import table data: not found')
        if response.status_code == 503:
            raise ServiceError(
                f'Failed to insert table data: data service failed to establish connection to metadata service')
        raise ResponseCodeError(f'Failed to import table data: response code: {response.status_code} is not '
                                f'202 (ACCEPTED): {response.text}')

    def analyse_datatypes(self, file_path: str, separator: str, enum: bool = None,
                          enum_tol: int = None, upload: bool = True) -> DatatypeAnalysis:
        """
        Import a csv dataset from a file and analyse it for the possible enums, line encoding and column data types.

        :param file_path: The path of the file that is imported on the storage service.
        :param separator: The csv column separator.
        :param enum: If set to true, enumerations should be guessed, otherwise no guessing. Optional.
        :param enum_tol: The tolerance for guessing enumerations (ignored if enum=False). Optional.
        :param upload: If set to true, the file from file_path will be uploaded, otherwise no upload will be performed \
            and the file_path will be treated as S3 filename and analysed instead. Optional. Default: true.

        :returns: The determined data types, if successful.

        :raises MalformedError: If the payload is rejected by the service.
        :raises NotExistsError: If the file was not found by the Analyse Service.
        :raises ResponseCodeError: If something went wrong with the analysis.
        """
        if upload:
            client = UploadClient(endpoint=f"{self.endpoint}/api/upload/files")
            filename = client.upload(file_path=file_path)
        else:
            filename = file_path
        params = [
            ('filename', filename),
            ('separator', separator),
            ('enum', enum),
            ('enum_tol', enum_tol)
        ]
        url = f'/api/analyse/datatypes'
        response = self._wrapper(method="get", url=url, params=params)
        if response.status_code == 202:
            body = response.json()
            return DatatypeAnalysis.model_validate(body)
        if response.status_code == 400:
            raise MalformedError(f'Failed to analyse data types: {response.text}')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to analyse data types: failed to find file in storage service')
        raise ResponseCodeError(f'Failed to analyse data types: response code: {response.status_code} is not '
                                f'202 (ACCEPTED): {response.text}')

    def analyse_keys(self, file_path: str, separator: str, upload: bool = True) -> KeyAnalysis:
        """
        Import a csv dataset from a file and analyse it for the possible primary key.

        :param file_path: The path of the file that is imported on the storage service.
        :param separator: The csv column separator.
        :param upload: If set to true, the file from file_path will be uploaded, otherwise no upload will be performed \
            and the file_path will be treated as S3 filename and analysed instead. Optional. Default: true.

        :returns: The determined ranking of the primary key candidates, if successful.

        :raises MalformedError: If the payload is rejected by the service.
        :raises NotExistsError: If the file was not found by the Analyse Service.
        :raises ResponseCodeError: If something went wrong with the analysis.
        """
        if upload:
            client = UploadClient(endpoint=f"{self.endpoint}/api/upload/files")
            filename = client.upload(file_path=file_path)
        else:
            filename = file_path
        params = [
            ('filename', filename),
            ('separator', separator),
        ]
        url = f'/api/analyse/keys'
        response = self._wrapper(method="get", url=url, params=params)
        if response.status_code == 202:
            body = response.json()
            return KeyAnalysis.model_validate(body)
        if response.status_code == 400:
            raise MalformedError(f'Failed to analyse data keys: {response.text}')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to analyse data keys: failed to find file in Storage Service')
        raise ResponseCodeError(f'Failed to analyse data types: response code: {response.status_code} is not '
                                f'202 (ACCEPTED): {response.text}')

    def analyse_table_statistics(self, database_id: int, table_id: int) -> TableStatistics:
        """
        Analyses the numerical contents of a table in a database with given database id and table id.

        :param database_id: The database id.
        :param table_id: The table id.

        :returns: The table statistics, if successful.

        :raises MalformedError: If the payload is rejected by the service.
        :raises NotExistsError: If the file was not found by the Analyse Service.
        :raises ServiceConnectionError: If something went wrong with connection to the metadata service.
        :raises ServiceError: If something went wrong with obtaining the information in the search service.
        :raises ResponseCodeError: If something went wrong with the analysis.
        """
        url = f'/api/analyse/database/{database_id}/table/{table_id}/statistics'
        response = self._wrapper(method="get", url=url)
        if response.status_code == 202:
            body = response.json()
            return TableStatistics.model_validate(body)
        if response.status_code == 400:
            raise MalformedError(f'Failed to analyse table statistics: {response.text}')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to analyse table statistics: separator error')
        if response.status_code == 502:
            raise NotExistsError(
                f'Failed to analyse table statistics: data service failed to establish connection to metadata service')
        if response.status_code == 503:
            raise ServiceError(f'Failed to analyse table statistics: failed to save statistic in search service')
        raise ResponseCodeError(f'Failed to analyse table statistics: response code: {response.status_code} is not '
                                f'202 (ACCEPTED): {response.text}')

    def update_table_data(self, database_id: int, table_id: int, data: dict, keys: dict) -> None:
        """
        Update data in a table in a database with given database id and table id.

        :param database_id: The database id.
        :param table_id: The table id.
        :param data: The data dictionary to be updated into the table with the form column=value of the table.
        :param keys: The key dictionary matching the rows in the form column=value.

        :raises MalformedError: If the payload is rejected by the service (e.g. LOB data could not be imported).
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the table does not exist.
        :raises ServiceError: If something went wrong with obtaining the information in the metadata service.
        :raises ResponseCodeError: If something went wrong with the update.
        """
        url = f'/api/database/{database_id}/table/{table_id}/data'
        response = self._wrapper(method="put", url=url, force_auth=True, payload=UpdateData(data=data, keys=keys))
        if response.status_code == 202:
            return
        if response.status_code == 400:
            raise MalformedError(f'Failed to update table data: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to update table data: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to update table data: not found')
        if response.status_code == 503:
            raise ServiceError(
                f'Failed to update table data: data service failed to establish connection to metadata service')
        raise ResponseCodeError(f'Failed to update table data: response code: {response.status_code} is not '
                                f'202 (ACCEPTED): {response.text}')

    def delete_table_data(self, database_id: int, table_id: int, keys: dict) -> None:
        """
        Delete data in a table in a database with given database id and table id.

        :param database_id: The database id.
        :param table_id: The table id.
        :param keys: The key dictionary matching the rows in the form column=value.

        :raises MalformedError: If the payload is rejected by the service.
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the table does not exist.
        :raises ServiceError: If something went wrong with obtaining the information in the metadata service.
        :raises ResponseCodeError: If something went wrong with the deletion.
        """
        url = f'/api/database/{database_id}/table/{table_id}/data'
        response = self._wrapper(method="delete", url=url, force_auth=True, payload=DeleteData(keys=keys))
        if response.status_code == 202:
            return
        if response.status_code == 400:
            raise MalformedError(f'Failed to delete table data: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to delete table data: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to delete table data: not found')
        if response.status_code == 503:
            raise ServiceError(
                f'Failed to update table data: data service failed to establish connection to metadata service')
        raise ResponseCodeError(f'Failed to delete table data: response code: {response.status_code} is not '
                                f'202 (ACCEPTED): {response.text}')

    def get_table_data_count(self, database_id: int, table_id: int, page: int = 0, size: int = 10,
                             timestamp: datetime.datetime = None) -> int:
        """
        Get data count of a table in a database with given database id and table id.

        :param database_id: The database id.
        :param table_id: The table id.
        :param page: The result pagination number. Optional. Default: 0.
        :param size: The result pagination size. Optional. Default: 10.
        :param timestamp: The query execution time. Optional.

        :returns: The result of the view query, if successful.

        :raises MalformedError: If the payload is rejected by the service.
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the table does not exist.
        :raises ExternalSystemError: If the mapped view selection query is erroneous.
        :raises ServiceError: If something went wrong with obtaining the information in the metadata service.
        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/database/{database_id}/table/{table_id}/data'
        if page is not None and size is not None:
            url += f'?page={page}&size={size}'
        if timestamp is not None:
            if page is not None and size is not None:
                url += '&'
            else:
                url += '?'
            url += f'timestamp={timestamp}'
        response = self._wrapper(method="head", url=url)
        if response.status_code == 200:
            return int(response.headers.get('X-Count'))
        if response.status_code == 400:
            raise MalformedError(f'Failed to count table data: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to count table data: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to count table data: not found')
        if response.status_code == 409:
            raise ExternalSystemError(f'Failed to count table data: mapping failed: {response.text}')
        if response.status_code == 503:
            raise ServiceError(
                f'Failed to count table data: data service failed to establish connection to metadata service')
        raise ResponseCodeError(f'Failed to count table data: response code: {response.status_code} is not '
                                f'200 (OK): {response.text}')

    def get_view_data_count(self, database_id: int, view_id: int) -> int:
        """
        Get data count of a view in a database with given database id and view id.

        :param database_id: The database id.
        :param view_id: The view id.

        :returns: The result count of the view query, if successful.

        :raises MalformedError: If the payload is rejected by the service.
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the view does not exist.
        :raises ExternalSystemError: If the mapped view selection query is erroneous.
        :raises ServiceError: If something went wrong with obtaining the information in the metadata service.
        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/database/{database_id}/view/{view_id}/data'
        response = self._wrapper(method="head", url=url)
        if response.status_code == 200:
            return int(response.headers.get('X-Count'))
        if response.status_code == 400:
            raise MalformedError(f'Failed to count view data: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to count view data: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to count view data: not found')
        if response.status_code == 409:
            raise ExternalSystemError(f'Failed to count view data: mapping failed: {response.text}')
        if response.status_code == 503:
            raise ServiceError(
                f'Failed to count view data: data service failed to establish connection to metadata service')
        raise ResponseCodeError(f'Failed to count view data: response code: {response.status_code} is not '
                                f'200 (OK): {response.text}')

    def get_database_access(self, database_id: int) -> AccessType:
        """
        Get access of a view in a database with given database id and view id.

        :param database_id: The database id.

        :returns: The access type, if successful.

        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the container does not exist.
        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/database/{database_id}/access'
        response = self._wrapper(method="get", url=url)
        if response.status_code == 200:
            body = response.json()
            return DatabaseAccess.model_validate(body).type
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to get database access: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to get database access: not found')
        raise ResponseCodeError(f'Failed to get database access: response code: {response.status_code} is not '
                                f'200 (OK): {response.text}')

    def check_database_access(self, database_id: int) -> bool:
        """
        Checks access of a view in a database with given database id and view id.

        :param database_id: The database id.

        :returns: The access type, if successful.

        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the container does not exist.
        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/database/{database_id}/access'
        response = self._wrapper(method="get", url=url)
        if response.status_code == 200:
            return True
        if response.status_code == 403:
            return False
        if response.status_code == 404:
            raise NotExistsError(f'Failed to check database access: not found')
        raise ResponseCodeError(f'Failed to check database access: response code: {response.status_code} is not '
                                f'200 (OK): {response.text}')

    def create_database_access(self, database_id: int, user_id: str, type: AccessType) -> AccessType:
        """
        Create access to a database with given database id and user id.

        :param database_id: The database id.
        :param user_id: The user id.
        :param type: The access type.

        :returns: The access type, if successful.

        :raises MalformedError: If the payload is rejected by the service.
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the database or user does not exist.
        :raises ServiceConnectionError: If something went wrong with connection to the data service.
        :raises ServiceError: If something went wrong with obtaining the information in the data service.
        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/database/{database_id}/access/{user_id}'
        response = self._wrapper(method="post", url=url, force_auth=True, payload=CreateAccess(type=type))
        if response.status_code == 202:
            body = response.json()
            return DatabaseAccess.model_validate(body).type
        if response.status_code == 400:
            raise MalformedError(f'Failed to create database access: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to create database access: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to create database access: not found')
        if response.status_code == 502:
            raise ServiceConnectionError(
                f'Failed to create database access: failed to establish connection to data service')
        if response.status_code == 503:
            raise ServiceError(f'Failed to create database access: failed to create access in data service')
        raise ResponseCodeError(f'Failed to create database access: response code: {response.status_code} is not '
                                f'202 (ACCEPTED): {response.text}')

    def update_database_access(self, database_id: int, user_id: str, type: AccessType) -> AccessType:
        """
        Updates the access for a user to a database with given database id and user id.

        :param database_id: The database id.
        :param user_id: The user id.
        :param type: The access type.

        :returns: The access type, if successful.

        :raises MalformedError: If the payload is rejected by the service.
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the database or user does not exist.
        :raises ServiceConnectionError: If something went wrong with connection to the data service.
        :raises ServiceError: If something went wrong with obtaining the information in the data service.
        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/database/{database_id}/access/{user_id}'
        response = self._wrapper(method="put", url=url, force_auth=True, payload=UpdateAccess(type=type))
        if response.status_code == 202:
            body = response.json()
            return DatabaseAccess.model_validate(body).type
        if response.status_code == 400:
            raise MalformedError(f'Failed to update database access: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to update database access: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to update database access: not found')
        if response.status_code == 502:
            raise ServiceConnectionError(
                f'Failed to update database access: failed to establish connection to data service')
        if response.status_code == 503:
            raise ServiceError(f'Failed to update database access: failed to update access in data service')
        raise ResponseCodeError(f'Failed to update database access: response code: {response.status_code} is not '
                                f'202 (ACCEPTED): {response.text}')

    def delete_database_access(self, database_id: int, user_id: str) -> None:
        """
        Deletes the access for a user to a database with given database id and user id.

        :param database_id: The database id.
        :param user_id: The user id.

        :raises MalformedError: If the payload is rejected by the service.
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the database or user does not exist.
        :raises ServiceConnectionError: If something went wrong with connection to the data service.
        :raises ServiceError: If something went wrong with obtaining the information in the data service.
        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/database/{database_id}/access/{user_id}'
        response = self._wrapper(method="delete", url=url, force_auth=True)
        if response.status_code == 202:
            return
        if response.status_code == 400:
            raise MalformedError(f'Failed to delete database access: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to delete database access: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to delete database access: not found')
        if response.status_code == 502:
            raise ServiceConnectionError(
                f'Failed to delete database access: failed to establish connection to data service')
        if response.status_code == 503:
            raise ServiceError(f'Failed to delete database access: failed to delete access in data service')
        raise ResponseCodeError(f'Failed to delete database access: response code: {response.status_code} is not '
                                f'201 (CREATED): {response.text}')

    def create_subset(self, database_id: int, query: str, page: int = 0, size: int = 10,
                      df: bool = False) -> Result | DataFrame:
        """
        Executes a SQL query in a database where the current user has at least read access with given database id. The
        result set can be paginated with setting page and size (both). Historic data can be queried by setting
        timestamp.

        :param database_id: The database id.
        :param query: The query statement.
        :param page: The result pagination number. Optional. Default: 0.
        :param size: The result pagination size. Optional. Default: 10.
        :param df: If true, the result is returned as Pandas DataFrame. Optional. Default: False.

        :returns: The result set, if successful.

        :raises MalformedError: If the payload is rejected by the service.
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the database, table or user does not exist.
        :raises QueryStoreError: The query store rejected the query.
        :raises FormatNotAvailable: The subset query contains non-supported keywords.
        :raises ServiceError: If something went wrong with obtaining the information in the data service.
        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/database/{database_id}/subset'
        if page is not None and size is not None:
            url += f'?page={page}&size={size}'
        response = self._wrapper(method="post", url=url, force_auth=True, headers={"Accept": "application/json"},
                                 payload=ExecuteQuery(statement=query))
        if response.status_code == 201:
            body = response.json()
            res = Result.model_validate(body)
            if df:
                return DataFrame.from_records(res.result)
            return res
        if response.status_code == 400:
            raise MalformedError(f'Failed to create subset: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to create subset: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to create subset: not found')
        if response.status_code == 417:
            raise QueryStoreError(f'Failed to create subset: query store rejected query')
        if response.status_code == 501:
            raise FormatNotAvailable(f'Failed to create subset: contains non-supported keywords: {response.text}')
        if response.status_code == 503:
            raise ServiceError(f'Failed to create subset: failed to establish connection with data database')
        raise ResponseCodeError(f'Failed to create subset: response code: {response.status_code} is not '
                                f'201 (CREATED): {response.text}')

    def get_subset_data(self, database_id: int, subset_id: int, page: int = 0, size: int = 10,
                        df: bool = False) -> Result | DataFrame:
        """
        Re-executes a query in a database with given database id and query id.

        :param database_id: The database id.
        :param subset_id: The subset id.
        :param page: The result pagination number. Optional. Default: 0.
        :param size: The result pagination size. Optional. Default: 10.
        :param size: The result pagination size. Optional. Default: 10.
        :param df: If true, the result is returned as Pandas DataFrame. Optional. Default: False.

        :returns: The result set, if successful.

        :raises MalformedError: If the payload is rejected by the service.
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the database, query or user does not exist.
        :raises ServiceError: If something went wrong with obtaining the information in the data service.
        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        headers = {}
        url = f'/api/database/{database_id}/subset/{subset_id}/data'
        if page is not None and size is not None:
            url += f'?page={page}&size={size}'
        response = self._wrapper(method="get", url=url, headers=headers)
        if response.status_code == 200:
            body = response.json()
            res = Result.model_validate(body)
            if df:
                return DataFrame.from_records(res.result)
            return res
        if response.status_code == 400:
            raise MalformedError(f'Failed to get query data: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to get query data: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to get query data: not found')
        if response.status_code == 503:
            raise ServiceError(f'Failed to get query data: failed to establish connection with data database')
        raise ResponseCodeError(f'Failed to get query data: response code: {response.status_code} is not '
                                f'200 (OK): {response.text}')

    def get_subset_data_count(self, database_id: int, subset_id: int, page: int = 0, size: int = 10) -> int:
        """
        Re-executes a query in a database with given database id and query id and only counts the results.

        :param database_id: The database id.
        :param subset_id: The subset id.
        :param page: The result pagination number. Optional. Default: 0.
        :param size: The result pagination size. Optional. Default: 10.

        :returns: The result set, if successful.

        :raises MalformedError: If the payload is rejected by the service.
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the database, query or user does not exist.
        :raises ServiceError: If something went wrong with obtaining the information in the data service.
        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/database/{database_id}/subset/{subset_id}/data'
        if page is not None and size is not None:
            url += f'?page={page}&size={size}'
        response = self._wrapper(method="head", url=url)
        if response.status_code == 200:
            return int(response.headers.get('X-Count'))
        if response.status_code == 400:
            raise MalformedError(f'Failed to get query count: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to get query count: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to get query count: not found')
        if response.status_code == 503:
            raise ServiceError(f'Failed to get query count: failed to establish connection with data database')
        raise ResponseCodeError(
            f'Failed to get query count: response code: {response.status_code} is not 200 (OK)')

    def get_subset(self, database_id: int, subset_id: int) -> Query:
        """
        Get query from a database with given database id and query id.

        :param database_id: The database id.
        :param subset_id: The subset id.

        :returns: The query, if successful.

        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the database, query or user does not exist.
        :raises FormatNotAvailable: If the service could not represent the output.
        :raises ServiceError: If something went wrong with obtaining the information in the data service.
        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/database/{database_id}/subset/{subset_id}'
        response = self._wrapper(method="get", url=url)
        if response.status_code == 200:
            body = response.json()
            return Query.model_validate(body)
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to find subset: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to find subset: not found')
        if response.status_code == 406:
            raise FormatNotAvailable(f'Failed to find subset: failed to provide acceptable representation')
        if response.status_code == 503:
            raise ServiceError(f'Failed to find subset: failed to establish connection with data database')
        raise ResponseCodeError(f'Failed to find subset: response code: {response.status_code} is not '
                                f'200 (OK): {response.text}')

    def get_queries(self, database_id: int) -> List[Query]:
        """
        Get queries from a database with given database id.

        :param database_id: The database id.

        :returns: List of queries, if successful.

        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the database or user does not exist.
        :raises ServiceError: If something went wrong with obtaining the information in the data service.
        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/database/{database_id}/subset'
        response = self._wrapper(method="get", url=url)
        if response.status_code == 200:
            body = response.json()
            return TypeAdapter(List[Query]).validate_python(body)
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to find queries: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to find queries: not found')
        if response.status_code == 503:
            raise ServiceError(f'Failed to find queries: failed to establish connection with data database')
        raise ResponseCodeError(f'Failed to find query: response code: {response.status_code} is not '
                                f'200 (OK): {response.text}')

    def update_subset(self, database_id: int, subset_id: int, persist: bool) -> Query:
        """
        Save query or mark it for deletion (at a later time) in a database with given database id and query id.

        :param database_id: The database id.
        :param subset_id: The subset id.
        :param persist: If set to true, the query will be saved and visible in the user interface, otherwise the query \
                is marked for deletion in the future and not visible in the user interface.

        :returns: The query, if successful.

        :raises MalformedError: If the payload is rejected by the service.
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the database or user does not exist.
        :raises QueryStoreError: The query store rejected the update.
        :raises ServiceError: If something went wrong with obtaining the information in the data service.
        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/database/{database_id}/subset/{subset_id}'
        response = self._wrapper(method="put", url=url, force_auth=True, payload=UpdateQuery(persist=persist))
        if response.status_code == 202:
            body = response.json()
            return Query.model_validate(body)
        if response.status_code == 400:
            raise MalformedError(f'Failed to update query: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to update query: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to update query: not found')
        if response.status_code == 417:
            raise QueryStoreError(f'Failed to update query: query store rejected update')
        if response.status_code == 503:
            raise ServiceError(f'Failed to update query: failed to establish connection with data database')
        raise ResponseCodeError(f'Failed to update query: response code: {response.status_code} is not '
                                f'202 (ACCEPTED): {response.text}')

    def create_identifier(self, database_id: int, type: IdentifierType, titles: List[CreateIdentifierTitle],
                          publisher: str, creators: List[CreateIdentifierCreator], publication_year: int,
                          descriptions: List[CreateIdentifierDescription] = None,
                          funders: List[CreateIdentifierFunder] = None, licenses: List[License] = None,
                          language: Language = None, subset_id: int = None, view_id: int = None, table_id: int = None,
                          publication_day: int = None, publication_month: int = None,
                          related_identifiers: List[CreateRelatedIdentifier] = None) -> Identifier:
        """
        Create an identifier draft.

        :param database_id: The database id of the created identifier.
        :param type: The type of the created identifier.
        :param titles: The titles of the created identifier.
        :param publisher: The publisher of the created identifier.
        :param creators: The creator(s) of the created identifier.
        :param publication_year: The publication year of the created identifier.
        :param descriptions: The description(s) of the created identifier. Optional.
        :param funders: The funders(s) of the created identifier. Optional.
        :param licenses: The license(s) of the created identifier. Optional.
        :param language: The language of the created identifier. Optional.
        :param subset_id: The subset id of the created identifier. Required when type=SUBSET, otherwise invalid. Optional.
        :param view_id: The view id of the created identifier. Required when type=VIEW, otherwise invalid. Optional.
        :param table_id: The table id of the created identifier. Required when type=TABLE, otherwise invalid. Optional.
        :param publication_day: The publication day of the created identifier. Optional.
        :param publication_month: The publication month of the created identifier. Optional.
        :param related_identifiers: The related identifier(s) of the created identifier. Optional.

        :returns: The identifier, if successful.

        :raises MalformedError: If the payload is rejected by the service.
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the database, table/view/subset or user does not exist.
        :raises ServiceConnectionError: If something went wrong with connection to the search service.
        :raises ServiceError: If something went wrong with obtaining the information in the search service.
        :raises ResponseCodeError: If something went wrong with the creation of the identifier.
        """
        url = f'/api/identifier'
        payload = CreateIdentifier(database_id=database_id, type=type, titles=titles, publisher=publisher,
                                   creators=creators, publication_year=publication_year, descriptions=descriptions,
                                   funders=funders, licenses=licenses, language=language, subset_id=subset_id,
                                   view_id=view_id, table_id=table_id, publication_day=publication_day,
                                   publication_month=publication_month, related_identifiers=related_identifiers)
        response = self._wrapper(method="post", url=url, force_auth=True, payload=payload)
        if response.status_code == 201:
            body = response.json()
            return Identifier.model_validate(body)
        if response.status_code == 400:
            raise MalformedError(f'Failed to create identifier: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to create identifier: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to create identifier: not found')
        if response.status_code == 502:
            raise ServiceConnectionError(
                f'Failed to create identifier: failed to establish connection with search service')
        if response.status_code == 503:
            raise ServiceError(f'Failed to create identifier: failed to save in search service')
        raise ResponseCodeError(f'Failed to create identifier: response code: {response.status_code} is not '
                                f'201 (CREATED): {response.text}')

    def save_identifier(self, identifier_id: int, database_id: int, type: IdentifierType,
                        titles: List[CreateIdentifierTitle], publisher: str, creators: List[CreateIdentifierCreator],
                        publication_year: int, descriptions: List[CreateIdentifierDescription] = None,
                        funders: List[CreateIdentifierFunder] = None, licenses: List[License] = None,
                        language: Language = None, subset_id: int = None, view_id: int = None, table_id: int = None,
                        publication_day: int = None, publication_month: int = None,
                        related_identifiers: List[CreateRelatedIdentifier] = None) -> Identifier:
        """
        Save an existing identifier and update the metadata attached to it.

        :param identifier_id: The identifier id.
        :param database_id: The database id of the created identifier.
        :param type: The type of the created identifier.
        :param titles: The titles of the created identifier.
        :param publisher: The publisher of the created identifier.
        :param creators: The creator(s) of the created identifier.
        :param publication_year: The publication year of the created identifier.
        :param descriptions: The description(s) of the created identifier. Optional.
        :param funders: The funders(s) of the created identifier. Optional.
        :param licenses: The license(s) of the created identifier. Optional.
        :param language: The language of the created identifier. Optional.
        :param subset_id: The subset id of the created identifier. Required when type=SUBSET, otherwise invalid. Optional.
        :param view_id: The view id of the created identifier. Required when type=VIEW, otherwise invalid. Optional.
        :param table_id: The table id of the created identifier. Required when type=TABLE, otherwise invalid. Optional.
        :param publication_day: The publication day of the created identifier. Optional.
        :param publication_month: The publication month of the created identifier. Optional.
        :param related_identifiers: The related identifier(s) of the created identifier. Optional.

        :returns: The identifier, if successful.

        :raises MalformedError: If the payload is rejected by the service.
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the database, table/view/subset or user does not exist.
        :raises ServiceConnectionError: If something went wrong with connection to the search service.
        :raises ServiceError: If something went wrong with obtaining the information in the search service.
        :raises ResponseCodeError: If something went wrong with the creation of the identifier.
        """
        url = f'/api/identifier/{identifier_id}'
        payload = CreateIdentifier(database_id=database_id, type=type, titles=titles, publisher=publisher,
                                   creators=creators, publication_year=publication_year, descriptions=descriptions,
                                   funders=funders, licenses=licenses, language=language, subset_id=subset_id,
                                   view_id=view_id, table_id=table_id, publication_day=publication_day,
                                   publication_month=publication_month, related_identifiers=related_identifiers)
        response = self._wrapper(method="put", url=url, force_auth=True, payload=payload)
        if response.status_code == 202:
            body = response.json()
            return Identifier.model_validate(body)
        if response.status_code == 400:
            raise MalformedError(f'Failed to save identifier: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to save identifier: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to save identifier: not found')
        if response.status_code == 502:
            raise ServiceConnectionError(
                f'Failed to save identifier: failed to establish connection with search service')
        if response.status_code == 503:
            raise ServiceError(f'Failed to save identifier: failed to update in search service')
        raise ResponseCodeError(f'Failed to save identifier: response code: {response.status_code} is not '
                                f'202 (ACCEPTED): {response.text}')

    def publish_identifier(self, identifier_id: int) -> Identifier:
        """
        Publish an identifier with given id.

        :param identifier_id: The identifier id.

        :returns: The identifier, if successful.

        :raises MalformedError: If the payload is rejected by the service.
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the database, table/view/subset or user does not exist.
        :raises ServiceConnectionError: If something went wrong with connection to the search service.
        :raises ServiceError: If something went wrong with obtaining the information in the search service.
        :raises ResponseCodeError: If something went wrong with the creation of the identifier.
        """
        url = f'/api/identifier/{identifier_id}/publish'
        response = self._wrapper(method="put", url=url, force_auth=True)
        if response.status_code == 202:
            body = response.json()
            return Identifier.model_validate(body)
        if response.status_code == 400:
            raise MalformedError(f'Failed to publish identifier: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to publish identifier: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to publish identifier: not found')
        if response.status_code == 502:
            raise ServiceConnectionError(
                f'Failed to publish identifier: failed to establish connection with search service')
        if response.status_code == 503:
            raise ServiceError(f'Failed to publish identifier: failed to update in search service')
        raise ResponseCodeError(f'Failed to publish identifier: response code: {response.status_code} is not '
                                f'202 (ACCEPTED): {response.text}')

    def get_licenses(self) -> List[License]:
        """
        Get list of licenses allowed.

        :returns: List of licenses, if successful.
        """
        url = f'/api/database/license'
        response = self._wrapper(method="get", url=url)
        if response.status_code == 200:
            body = response.json()
            return TypeAdapter(List[License]).validate_python(body)
        raise ResponseCodeError(f'Failed to get licenses: response code: {response.status_code} is not '
                                f'200 (OK): {response.text}')

    def get_concepts(self) -> List[Concept]:
        """
        Get list of concepts known to the metadata database.

        :returns: List of concepts, if successful.
        """
        url = f'/api/concept'
        response = self._wrapper(method="get", url=url)
        if response.status_code == 200:
            body = response.json()
            return TypeAdapter(List[Concept]).validate_python(body)
        raise ResponseCodeError(f'Failed to get concepts: response code: {response.status_code} is not '
                                f'200 (OK): {response.text}')

    def get_identifiers(self, database_id: int = None, subset_id: int = None, view_id: int = None,
                        table_id: int = None) -> List[Identifier] | str:
        """
        Get list of identifiers, filter by the remaining optional arguments.

        :param database_id: The database id. Optional.
        :param subset_id: The subset id. Optional. Requires `database_id` to be set.
        :param view_id: The view id. Optional. Requires `database_id` to be set.
        :param table_id: The table id. Optional. Requires `database_id` to be set.

        :returns: List of identifiers, if successful.

        :raises NotExistsError: If the accept header is neither application/json nor application/ld+json.
        :raises FormatNotAvailable: If the service could not represent the output.
        :raises ResponseCodeError: If something went wrong with the retrieval of the identifiers.
        """
        url = f'/api/identifiers'
        if database_id is not None:
            url += f'?dbid={database_id}'
        if subset_id is not None:
            if database_id is None:
                raise RequestError(f'Filtering by subset_id requires database_id to be set')
            url += f'&qid={subset_id}'
        if view_id is not None:
            if database_id is None:
                raise RequestError(f'Filtering by view_id requires database_id to be set')
            url += f'&vid={view_id}'
        if table_id is not None:
            if database_id is None:
                raise RequestError(f'Filtering by table_id requires database_id to be set')
            url += f'&tid={table_id}'
        response = self._wrapper(method="get", url=url, headers={'Accept': 'application/json'})
        if response.status_code == 200:
            body = response.json()
            return TypeAdapter(List[Identifier]).validate_python(body)
        if response.status_code == 404:
            raise NotExistsError(f'Failed to get identifiers: requested style is not known')
        if response.status_code == 406:
            raise MalformedError(
                f'Failed to get identifiers: accept header must be application/json or application/ld+json')
        raise ResponseCodeError(f'Failed to get identifiers: response code: {response.status_code} is not '
                                f'200 (OK): {response.text}')

    def update_table_column(self, database_id: int, table_id: int, column_id: int, concept_uri: str = None,
                            unit_uri: str = None) -> Column:
        """
        Update semantic information of a table column by given database id and table id and column id.

        :param database_id: The database id.
        :param table_id: The table id.
        :param column_id: The column id.
        :param concept_uri: The concept URI. Optional.
        :param unit_uri: The unit URI. Optional.

        :returns: The column, if successful.

        :raises MalformedError: If the payload is rejected by the service.
        :raises ForbiddenError: If something went wrong with the authorization.
        :raises NotExistsError: If the accept header is neither application/json nor application/ld+json.
        :raises ServiceConnectionError: If something went wrong with connection to the search service.
        :raises ServiceError: If something went wrong with obtaining the information in the search service.
        :raises ResponseCodeError: If something went wrong with the retrieval of the identifiers.
        """
        url = f'/api/database/{database_id}/table/{table_id}/column/{column_id}'
        response = self._wrapper(method="put", url=url, force_auth=True,
                                 payload=UpdateColumn(concept_uri=concept_uri, unit_uri=unit_uri))
        if response.status_code == 202:
            body = response.json()
            return Column.model_validate(body)
        if response.status_code == 400:
            raise MalformedError(f'Failed to update column: {response.text}')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to update colum: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to update colum: not found')
        if response.status_code == 502:
            raise ServiceConnectionError(f'Failed to update colum: failed to establish connection to search service')
        if response.status_code == 503:
            raise ServiceError(f'Failed to update colum: failed to save in search service')
        raise ResponseCodeError(f'Failed to update colum: response code: {response.status_code} is not 202 (ACCEPTED)')
