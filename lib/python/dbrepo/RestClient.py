import dataclasses
import datetime
import sys
import os
import logging

import requests
from pydantic import TypeAdapter
from tusclient.client import TusClient

from dbrepo.api.dto import *
from dbrepo.api.exceptions import ResponseCodeError, UsernameExistsError, EmailExistsError, NotExistsError, \
    ForbiddenError, MalformedError, NameExistsError, QueryStoreError, MetadataConsistencyError, ExternalSystemError, \
    AuthenticationError, UploadError


class RestClient:
    """
    The RestClient class for communicating with the DBRepo REST API. All parameters can be set also via environment \
    variables, e.g. set endpoint with DBREPO_ENDPOINT, username with DBREPO_USERNAME, etc. You can override the \
    constructor parameters with the environment variables.

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
        logging.getLogger('requests').setLevel(logging.INFO)
        logging.getLogger('urllib3').setLevel(logging.INFO)
        logging.basicConfig(format='%(asctime)s %(name)-12s %(levelname)-6s %(message)s', level=logging.DEBUG,
                            stream=sys.stdout)
        self.endpoint = os.environ.get('DBREPO_ENDPOINT', endpoint)
        self.username = os.environ.get('DBREPO_USERNAME', username)
        self.password = os.environ.get('DBREPO_PASSWORD', password)
        if os.environ.get('DBREPO_SECURE') is not None:
            self.secure = os.environ.get('DBREPO_SECURE') == 'True'
        else:
            self.secure = secure

    def _wrapper(self, method: str, url: str, params: [(str,)] = None, payload=None, headers=None,
                 force_auth: bool = False, stream: bool = False) -> requests.Response:
        if force_auth and (self.username is None or self.password is None):
            raise AuthenticationError(f"Failed to perform request: authentication required")
        url = f'{self.endpoint}{url}'
        logging.debug(f'method: {method}')
        logging.debug(f'url: {url}')
        if params is not None:
            logging.debug(f'params: {params}')
        if stream is not None:
            logging.debug(f'stream: {stream}')
        logging.debug(f'secure: {self.secure}')
        if headers is not None:
            logging.debug(f'headers: {headers}')
        if payload is not None:
            logging.debug(f'payload: {payload}')
            payload = payload.model_dump_json()
        if self.username is not None and self.password is not None:
            logging.debug(f'username: {self.username}, password: (hidden)')
            return requests.request(method=method, url=url, auth=(self.username, self.password), verify=self.secure,
                                    json=payload, headers=headers, params=params, stream=stream)
        return requests.request(method=method, url=url, verify=self.secure, json=payload, headers=headers,
                                params=params, stream=stream)

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

    def whoami(self) -> str:
        """
        Print the username.
        """
        if self.username is not None:
            logging.info(f"{self.username}")
            return self.username
        logging.info(f"No username set!")
        return None

    def get_users(self) -> List[User]:
        """
        Get all users.

        :returns: List of users, if successful.

        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/user'
        response = self._wrapper(method="get", url=url)
        if response.status_code == 200:
            body = response.json()
            return TypeAdapter(List[User]).validate_json(body)
        raise ResponseCodeError(f'Failed to find users: response code: {response.status_code} is not 200 (OK)')

    def get_user(self, user_id: str) -> User:
        """
        Get a user with given user id.

        :returns: The user, if successful.

        :raises ResponseCodeError: If something went wrong with the retrieval.
        :raises NotExistsError: If theuser does not exist.
        """
        url = f'/api/user/{user_id}'
        response = self._wrapper(method="get", url=url)
        if response.status_code == 200:
            body = response.json()
            return User.parse_raw(body)
        if response.status_code == 404:
            raise NotExistsError(f'Failed to find user with id {user_id}')
        raise ResponseCodeError(
            f'Failed to find user with id {user_id}: response code: {response.status_code} is not 200 (OK)')

    def create_user(self, username: str, password: str, email: str) -> UserBrief:
        """
        Creates a new user.

        :param username: The username of the new user. Must be unique.
        :param password: The password of the new user.
        :param email: The email of the new user. Must be unique.

        :returns: The user, if successful.

        :raises ResponseCodeError: If something went wrong with the creation.
        :raises UsernameExistsError: The username exists already.
        :raises ForbiddenError: If the action is not allowed.
        :raises NotExistsError: If thedefault role was not found.
        :raises EmailExistsError: The email exists already.
        """
        url = f'/api/user'
        response = self._wrapper(method="post", url=url,
                                 payload=CreateUser(username=username, password=password, email=email))
        if response.status_code == 201:
            body = response.json()
            return UserBrief.parse_raw(body)
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to update user password: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to create user: default role not found')
        if response.status_code == 409:
            raise UsernameExistsError(f'Failed to create user: user with username exists')
        if response.status_code == 417:
            raise EmailExistsError(f'Failed to create user: user with e-mail exists')
        raise ResponseCodeError(
            f'Failed to create user: response code: {response.status_code} is not 201 (CREATED)')

    def update_user(self, user_id: str, firstname: str = None, lastname: str = None, affiliation: str = None,
                    orcid: str = None) -> User:
        """
        Updates a user with given user id.

        :param user_id: The user id of the user that should be updated.
        :param firstname: The updated given name. Optional.
        :param lastname: The updated family name. Optional.
        :param affiliation: The updated affiliation identifier. Optional.
        :param orcid: The updated ORCID identifier. Optional.

        :returns: The user, if successful.

        :raises ResponseCodeError: If something went wrong with the update.
        :raises ForbiddenError: If the action is not allowed.
        :raises NotExistsError: If theuser does not exist.
        """
        url = f'/api/user/{user_id}'
        response = self._wrapper(method="put", url=url, force_auth=True,
                                 payload=UpdateUser(firstname=firstname, lastname=lastname, affiliation=affiliation,
                                                    orcid=orcid))
        if response.status_code == 202:
            body = response.json()
            return User.parse_raw(body)
        if response.status_code == 400:
            raise ResponseCodeError(f'Failed to update user: invalid values')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to update user password: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to update user: user not found')
        if response.status_code == 405:
            raise ForbiddenError(f'Failed to update user: foreign user')
        raise ResponseCodeError(
            f'Failed to update user: response code: {response.status_code} is not 202 (ACCEPTED)')

    def update_user_theme(self, user_id: str, theme: str) -> User:
        """
        Updates the theme of a user with given user id.

        :param user_id: The user id of the user that should be updated.
        :param theme: The updated user theme name.

        :returns: The user, if successful.

        :raises ResponseCodeError: If something went wrong with the update.
        :raises ForbiddenError: If the action is not allowed.
        :raises NotExistsError: If theuser does not exist.
        """
        url = f'/api/user/{user_id}/theme'
        response = self._wrapper(method="put", url=url, force_auth=True, payload=UpdateUserTheme(theme=theme))
        if response.status_code == 202:
            body = response.json()
            return User.parse_raw(body)
        if response.status_code == 400:
            raise ResponseCodeError(f'Failed to update user theme: invalid values')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to update user password: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to update user theme: user not found')
        if response.status_code == 405:
            raise ResponseCodeError(f'Failed to update user theme: foreign user')
        raise ResponseCodeError(
            f'Failed to update user theme: response code: {response.status_code} is not 202 (ACCEPTED)')

    def update_user_password(self, user_id: str, password: str) -> User:
        """
        Updates the password of a user with given user id.

        :param user_id: The user id of the user that should be updated.
        :param password: The updated user password.

        :returns: The user, if successful.

        :raises ResponseCodeError: If something went wrong with the update.
        :raises ForbiddenError: If the action is not allowed.
        :raises NotExistsError: If theuser does not exist.
        """
        url = f'/api/user/{user_id}/password'
        response = self._wrapper(method="put", url=url, force_auth=True, payload=UpdateUserPassword(password=password))
        if response.status_code == 202:
            body = response.json()
            return User.parse_raw(body)
        if response.status_code == 400:
            raise ResponseCodeError(f'Failed to update user password: invalid values')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to update user password: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to update user password: not found')
        if response.status_code == 405:
            raise ForbiddenError(f'Failed to update user password: foreign user')
        if response.status_code == 503:
            raise ResponseCodeError(f'Failed to update user password: keycloak error')
        raise ResponseCodeError(
            f'Failed to update user theme: response code: {response.status_code} is not 202 (ACCEPTED)')

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
            return TypeAdapter(List[ContainerBrief]).validate_json(body)
        raise ResponseCodeError(f'Failed to find containers: response code: {response.status_code} is not 200 (OK)')

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
            return Container.parse_raw(body)
        if response.status_code == 404:
            raise NotExistsError(f'Failed to get container: not found')
        raise ResponseCodeError(f'Failed to get container: response code: {response.status_code} is not 200 (OK)')

    def get_databases(self) -> List[Database]:
        """
        Get all databases.

        :returns: List of databases, if successful.

        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/database'
        response = self._wrapper(method="get", url=url)
        if response.status_code == 200:
            body = response.json()
            return TypeAdapter(List[Database]).validate_json(body)
        raise ResponseCodeError(f'Failed to find databases: response code: {response.status_code} is not 200 (OK)')

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
        raise ResponseCodeError(f'Failed to find databases: response code: {response.status_code} is not 200 (OK)')

    def get_database(self, database_id: int) -> Database:
        """
        Get a databases with given id.

        :param database_id: The database id.

        :returns: The database, if successful.

        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/database/{database_id}'
        response = self._wrapper(method="get", url=url)
        if response.status_code == 200:
            body = response.json()
            return Database.parse_raw(body)
        if response.status_code == 404:
            raise NotExistsError(f'Failed to find database with id {database_id}')
        raise ResponseCodeError(
            f'Failed to find database with id {database_id}: response code: {response.status_code} is not 200 (OK)')

    def create_database(self, name: str, container_id: int, is_public: bool) -> Database:
        """
        Create a databases in a container with given container id.

        :param name: The name of the database.
        :param container_id: The container id.
        :param is_public: The visibility of the database. If set to true everything will be visible, otherwise only
                the metadata (schema, identifiers) will be visible to the public.

        :returns: The database, if successful.

        :raises ResponseCodeError: If something went wrong with the retrieval.
        :raises ForbiddenError: If the action is not allowed.
        :raises NotExistsError: If the container does not exist.
        """
        url = f'/api/database'
        response = self._wrapper(method="post", url=url, force_auth=True,
                                 payload=CreateDatabase(name=name, container_id=container_id, is_public=is_public))
        if response.status_code == 201:
            body = response.json()
            return Database.parse_raw(body)
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to create database: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to create database: container not found')
        raise ResponseCodeError(
            f'Failed to create database: response code: {response.status_code} is not 201 (CREATED)')

    def update_database_visibility(self, database_id: int, is_public: bool) -> Database:
        """
        Updates the database visibility of a database with given database id.

        :param database_id: The database id.
        :param is_public: The visibility of the database. If set to true everything will be visible, otherwise only
                the metadata (schema, identifiers) will be visible to the public.

        :returns: The database, if successful.

        :raises ResponseCodeError: If something went wrong with the retrieval.
        :raises ForbiddenError: If the action is not allowed.
        :raises NotExistsError: If thedatabase does not exist.
        """
        url = f'/api/database/{database_id}'
        response = self._wrapper(method="put", url=url, force_auth=True, payload=ModifyVisibility(is_public=is_public))
        if response.status_code == 202:
            body = response.json()
            return Database.parse_raw(body)
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to update database visibility: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to update database visibility: not found')
        raise ResponseCodeError(
            f'Failed to update database visibility: response code: {response.status_code} is not 202 (ACCEPTED)')

    def update_database_owner(self, database_id: int, user_id: str) -> Database:
        """
        Updates the database owner of a database with given database id.

        :param database_id: The database id.
        :param user_id: The user id of the new owner.

        :returns: The database, if successful.

        :raises ResponseCodeError: If something went wrong with the retrieval.
        :raises NotExistsError: If thedatabase does not exist.
        """
        url = f'/api/database/{database_id}/owner'
        response = self._wrapper(method="put", url=url, force_auth=True, payload=ModifyOwner(id=user_id))
        if response.status_code == 202:
            body = response.json()
            return Database.parse_raw(body)
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to update database visibility: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to update database visibility: not found')
        raise ResponseCodeError(
            f'Failed to update database visibility: response code: {response.status_code} is not 202 (ACCEPTED)')

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

        :raises ResponseCodeError: If something went wrong with the retrieval.
        :raises NameExistsError: If a table with this name already exists.
        :raises ForbiddenError: If the action is not allowed.
        :raises MalformedError: If the payload is rejected by the service.
        :raises NotExistsError: If thecontainer does not exist.
        """
        url = f'/api/database/{database_id}/table'
        response = self._wrapper(method="post", url=url, force_auth=True,
                                 payload=CreateTable(name=name, description=description,
                                                     columns=columns, constraints=constraints))
        if response.status_code == 201:
            body = response.json()
            return Table.parse_raw(body)
        if response.status_code == 400:
            raise MalformedError(f'Failed to create table: service rejected malformed payload')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to create table: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to create table: container not found')
        if response.status_code == 409:
            raise NameExistsError(f'Failed to create table: table name exists')
        raise ResponseCodeError(
            f'Failed to create table: response code: {response.status_code} is not 201 (CREATED)')

    def get_tables(self, database_id: int) -> List[Table]:
        """
        Get all tables.

        :param database_id: The database id.

        :returns: List of tables, if successful.

        :raises ResponseCodeError: If something went wrong with the retrieval.
        """
        url = f'/api/database/{database_id}/table'
        response = self._wrapper(method="get", url=url)
        if response.status_code == 200:
            body = response.json()
            return TypeAdapter(List[Table]).validate_json(body)
        raise ResponseCodeError(f'Failed to find tables: response code: {response.status_code} is not 200 (OK)')

    def get_table(self, database_id: int, table_id: int) -> Table:
        """
        Get a table with given database id and table id.

        :param database_id: The database id.
        :param table_id: The table id.

        :returns: List of tables, if successful.

        :raises ResponseCodeError: If something went wrong with the retrieval.
        :raises ForbiddenError: If the action is not allowed.
        :raises NotExistsError: If thecontainer does not exist.
        """
        url = f'/api/database/{database_id}/table/{table_id}'
        response = self._wrapper(method="get", url=url)
        if response.status_code == 200:
            body = response.json()
            return Table.parse_raw(body)
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to find table: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to find table: not found')
        raise ResponseCodeError(f'Failed to find table: response code: {response.status_code} is not 200 (OK)')

    def delete_table(self, database_id: int, table_id: int) -> None:
        """
        Delete a table with given database id and table id.

        :param database_id: The database id.
        :param table_id: The table id.

        :raises ResponseCodeError: If something went wrong with the retrieval.
        :raises ForbiddenError: If the action is not allowed.
        :raises NotExistsError: If thecontainer does not exist.
        """
        url = f'/api/database/{database_id}/table/{table_id}'
        response = self._wrapper(method="delete", url=url, force_auth=True)
        if response.status_code == 202:
            return
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to delete table: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to delete table: not found')
        raise ResponseCodeError(f'Failed to delete table: response code: {response.status_code} is not 202 (ACCEPTED)')

    def get_views(self, database_id: int) -> List[View]:
        """
        Gets views of a database with given database id.

        :param database_id: The database id.

        :returns: The list of views, if successful.

        :raises ResponseCodeError: If something went wrong with the retrieval.
        :raises ForbiddenError: If the action is not allowed.
        :raises NotExistsError: If thecontainer does not exist.
        """
        url = f'/api/database/{database_id}/view'
        response = self._wrapper(method="get", url=url)
        if response.status_code == 200:
            body = response.json()
            return TypeAdapter(List[View]).validate_json(body)
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to find views: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to find views: not found')
        raise ResponseCodeError(f'Failed to find views: response code: {response.status_code} is not 200 (OK)')

    def get_view(self, database_id: int, view_id: int) -> View:
        """
        Get a view of a database with given database id and view id.

        :param database_id: The database id.
        :param view_id: The view id.

        :returns: The view, if successful.

        :raises ResponseCodeError: If something went wrong with the retrieval.
        :raises ForbiddenError: If the action is not allowed.
        :raises NotExistsError: If thecontainer does not exist.
        """
        url = f'/api/database/{database_id}/view/{view_id}'
        response = self._wrapper(method="get", url=url)
        if response.status_code == 200:
            body = response.json()
            return View.parse_raw(body)
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to find view: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to find view: not found')
        raise ResponseCodeError(f'Failed to find view: response code: {response.status_code} is not 200 (OK)')

    def create_view(self, database_id: int, name: str, query: str, is_public: bool) -> View:
        """
        Create a view in a database with given database id.

        :param database_id: The database id.
        :param name: The name of the created view.
        :param query: The query of the created view.
        :param is_public: The visibility of the view. If set to true everything will be visible, otherwise only
                the metadata (schema, identifiers) will be visible to the public.

        :returns: The created view, if successful.

        :raises ResponseCodeError: If something went wrong with the retrieval.
        :raises ForbiddenError: If the action is not allowed.
        :raises NotExistsError: If thecontainer does not exist.
        """
        url = f'/api/database/{database_id}/view'
        response = self._wrapper(method="post", url=url, force_auth=True,
                                 payload=CreateView(name=name, query=query, is_public=is_public))
        if response.status_code == 201:
            body = response.json()
            return View.parse_raw(body)
        if response.status_code == 400 or response.status_code == 423:
            raise MalformedError(f'Failed to create view: service rejected malformed payload')
        if response.status_code == 403 or response.status_code == 405:
            raise ForbiddenError(f'Failed to create view: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to create view: not found')
        raise ResponseCodeError(f'Failed to create view: response code: {response.status_code} is not 201 (CREATED)')

    def delete_view(self, database_id: int, view_id: int) -> None:
        """
        Deletes a view in a database with given database id and view id.

        :param database_id: The database id.
        :param view_id: The view id.

        :raises ResponseCodeError: If something went wrong with the retrieval.
        :raises ForbiddenError: If the action is not allowed.
        :raises NotExistsError: If thecontainer does not exist.
        """
        url = f'/api/database/{database_id}/view/{view_id}'
        response = self._wrapper(method="delete", url=url, force_auth=True)
        if response.status_code == 202:
            return
        if response.status_code == 400 or response.status_code == 423:
            raise MalformedError(f'Failed to delete view: service rejected malformed payload')
        if response.status_code == 403 or response.status_code == 405:
            raise ForbiddenError(f'Failed to delete view: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to delete view: not found')
        raise ResponseCodeError(f'Failed to delete view: response code: {response.status_code} is not 202 (ACCEPTED)')

    def get_view_data(self, database_id: int, view_id: int, page: int = 0, size: int = 10) -> Result:
        """
        Get data of a view in a database with given database id and view id.

        :param database_id: The database id.
        :param view_id: The view id.
        :param page: The result pagination number. Optional. Default: 0.
        :param size: The result pagination size. Optional. Default: 10.

        :returns: The result of the view query, if successful.

        :raises ResponseCodeError: If something went wrong with the retrieval.
        :raises ForbiddenError: If the action is not allowed.
        :raises NotExistsError: If the view does not exist.
        """
        url = f'/api/database/{database_id}/view/{view_id}/data'
        params = []
        if page is not None and size is not None:
            params.append(('page', page))
            params.append(('size', size))
        response = self._wrapper(method="get", url=url, params=params)
        if response.status_code == 200:
            body = response.json()
            return Result.parse_raw(body)
        if response.status_code == 400:
            raise MalformedError(f'Failed to get view data: service rejected malformed payload')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to get view data: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to get view data: not found')
        raise ResponseCodeError(f'Failed to get view data: response code: {response.status_code} is not 200 (OK)')

    def get_table_data(self, database_id: int, table_id: int, page: int = 0, size: int = 10,
                       timestamp: datetime.datetime = None) -> Result:
        """
        Get data of a table in a database with given database id and table id.

        :param database_id: The database id.
        :param table_id: The table id.
        :param page: The result pagination number. Optional. Default: 0.
        :param size: The result pagination size. Optional. Default: 10.
        :param timestamp: The query execution time. Optional.

        :returns: The result of the view query, if successful.

        :raises ResponseCodeError: If something went wrong with the retrieval.
        :raises ForbiddenError: If the action is not allowed.
        :raises NotExistsError: If the table does not exist.
        :raises QueryStoreError: If the result set could not be counted.
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
            return Result.parse_raw(body)
        if response.status_code == 400:
            raise MalformedError(f'Failed to get table data: service rejected malformed payload')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to get table data: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to get table data: not found')
        if response.status_code == 409:
            raise QueryStoreError(f'Failed to get table data: service rejected result count')
        raise ResponseCodeError(f'Failed to get table data: response code: {response.status_code} is not 200 (OK)')

    def create_table_data(self, database_id: int, table_id: int, data: dict) -> None:
        """
        Insert data into a table in a database with given database id and table id.

        :param database_id: The database id.
        :param table_id: The table id.
        :param data: The data dictionary to be inserted into the table with the form column=value of the table.

        :raises ResponseCodeError: If something went wrong with the insert.
        :raises ForbiddenError: If the action is not allowed.
        :raises NotExistsError: If the table does not exist.
        :raises MalformedError: If the payload is rejected by the service (e.g. LOB data could not be imported).
        """
        url = f'/api/database/{database_id}/table/{table_id}/data'
        response = self._wrapper(method="post", url=url, force_auth=True, payload=CreateData(data=data))
        if response.status_code == 202:
            return
        if response.status_code == 400 or response.status_code == 410:
            raise MalformedError(f'Failed to insert table data: service rejected malformed payload')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to insert table data: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to insert table data: not found')
        raise ResponseCodeError(
            f'Failed to insert table data: response code: {response.status_code} is not 202 (ACCEPTED)')

    def import_table_data(self, database_id: int, table_id: int, separator: str, file_path: str,
                          quote: str = None, skip_lines: int = None, false_encoding: str = None,
                          true_encoding: str = None, null_encoding: str = None,
                          line_encoding: str = None) -> None:
        """
        Import a csv dataset from a file into a table in a database with given database id and table id.

        :param database_id: The database id.
        :param table_id: The table id.
        :param separator: The csv column separator.
        :param file_path: The path of the file that is imported on the storage service.
        :param quote: The column data quotation character. Optional.
        :param skip_lines: The number of lines to skip. Optional.
        :param false_encoding: The encoding of boolean false. Optional.
        :param true_encoding: The encoding of boolean true. Optional.
        :param null_encoding: The encoding of null. Optional. Default: empty string "".
        :param line_encoding: The encoding of the line termination. Optional. Default: CR (Windows).

        :raises ResponseCodeError: If something went wrong with the insert.
        :raises ForbiddenError: If the action is not allowed.
        :raises NotExistsError: If the table does not exist.
        :raises MalformedError: If the payload is rejected by the service (e.g. LOB data could not be imported).
        """
        upload = UploadClient(endpoint=self.endpoint)
        filename = upload.upload(file_path=file_path)
        url = f'/api/database/{database_id}/table/{table_id}/data/import'
        response = self._wrapper(method="post", url=url, force_auth=True,
                                 payload=Import(location=filename, separator=separator, quote=quote,
                                                skip_lines=skip_lines, false_element=false_encoding,
                                                true_element=true_encoding, null_element=null_encoding,
                                                line_termination=line_encoding))
        if response.status_code == 202:
            return
        if response.status_code == 400:
            raise MalformedError(f'Failed to import table data: service rejected malformed payload')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to import table data: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to import table data: not found')
        if response.status_code == 409 or response.status_code == 422:
            raise ExternalSystemError(f'Failed to import table data: sidecar rejected the import')
        raise ResponseCodeError(
            f'Failed to import table data: response code: {response.status_code} is not 202 (ACCEPTED)')

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

        :raises ResponseCodeError: If something went wrong with the analysis.
        :raises MalformedError: If the payload is rejected by the service.
        :raises NotExistsError: If the file was not found by the Analyse Service.
        """
        if upload:
            client = UploadClient(endpoint=self.endpoint)
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
            return DatatypeAnalysis.parse_raw(body)
        if response.status_code == 400 or response.status_code == 500:
            raise MalformedError(f'Failed to analyse data types: service rejected malformed payload')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to analyse data types: failed to find file in Storage Service')
        raise ResponseCodeError(
            f'Failed to analyse data types: response code: {response.status_code} is not 202 (ACCEPTED)')

    def analyse_keys(self, file_path: str, separator: str, upload: bool = True) -> KeyAnalysis:
        """
        Import a csv dataset from a file and analyse it for the possible primary key.

        :param file_path: The path of the file that is imported on the storage service.
        :param separator: The csv column separator.
        :param upload: If set to true, the file from file_path will be uploaded, otherwise no upload will be performed \
            and the file_path will be treated as S3 filename and analysed instead. Optional. Default: true.

        :returns: The determined ranking of the primary key candidates, if successful.

        :raises ResponseCodeError: If something went wrong with the analysis.
        :raises MalformedError: If the payload is rejected by the service.
        :raises NotExistsError: If the file was not found by the Analyse Service.
        """
        if upload:
            client = UploadClient(endpoint=self.endpoint)
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
            return KeyAnalysis.parse_raw(body)
        if response.status_code == 400 or response.status_code == 500:
            raise MalformedError(f'Failed to analyse data types: service rejected malformed payload')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to analyse data types: failed to find file in Storage Service')
        raise ResponseCodeError(
            f'Failed to analyse data types: response code: {response.status_code} is not 202 (ACCEPTED)')

    def analyse_table_statistics(self, database_id: int, table_id: int) -> TableStatistics:
        """
        Analyses the numerical contents of a table in a database with given database id and table id.

        :param database_id: The database id.
        :param table_id: The table id.

        :returns: The table statistics, if successful.

        :raises ResponseCodeError: If something went wrong with the analysis.
        :raises MalformedError: If the payload is rejected by the service.
        :raises NotExistsError: If the file was not found by the Analyse Service.
        """
        url = f'/api/analyse/database/{database_id}/table/{table_id}/statistics'
        response = self._wrapper(method="get", url=url)
        if response.status_code == 202:
            body = response.json()
            return TableStatistics.parse_raw(body)
        if response.status_code == 400:
            raise MalformedError(f'Failed to analyse table statistics: service rejected malformed payload')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to analyse table statistics: separator error')
        raise ResponseCodeError(
            f'Failed to analyse table statistics: response code: {response.status_code} is not 202 (ACCEPTED)')

    def update_table_data(self, database_id: int, table_id: int, data: dict, keys: dict) -> None:
        """
        Update data in a table in a database with given database id and table id.

        :param database_id: The database id.
        :param table_id: The table id.
        :param data: The data dictionary to be updated into the table with the form column=value of the table.
        :param keys: The key dictionary matching the rows in the form column=value.

        :raises ResponseCodeError: If something went wrong with the update.
        :raises ForbiddenError: If the action is not allowed.
        :raises NotExistsError: If the table does not exist.
        :raises MalformedError: If the payload is rejected by the service (e.g. LOB data could not be imported).
        """
        url = f'/api/database/{database_id}/table/{table_id}/data'
        response = self._wrapper(method="put", url=url, force_auth=True, payload=UpdateData(data=data, keys=keys))
        if response.status_code == 202:
            return
        if response.status_code == 400 or response.status_code == 410:
            raise MalformedError(f'Failed to update table data: service rejected malformed payload')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to update table data: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to update table data: not found')
        raise ResponseCodeError(
            f'Failed to update table data: response code: {response.status_code} is not 202 (ACCEPTED)')

    def delete_table_data(self, database_id: int, table_id: int, keys: dict) -> None:
        """
        Delete data in a table in a database with given database id and table id.

        :param database_id: The database id.
        :param table_id: The table id.
        :param keys: The key dictionary matching the rows in the form column=value.

        :raises ResponseCodeError: If something went wrong with the deletion.
        :raises ForbiddenError: If the action is not allowed.
        :raises NotExistsError: If the table does not exist.
        :raises MalformedError: If the payload is rejected by the service.
        """
        url = f'/api/database/{database_id}/table/{table_id}/data'
        response = self._wrapper(method="delete", url=url, force_auth=True, payload=DeleteData(keys=keys))
        if response.status_code == 202:
            return
        if response.status_code == 400:
            raise MalformedError(f'Failed to delete table data: service rejected malformed payload')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to delete table data: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to delete table data: not found')
        raise ResponseCodeError(
            f'Failed to delete table data: response code: {response.status_code} is not 202 (ACCEPTED)')

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

        :raises ResponseCodeError: If something went wrong with the retrieval.
        :raises ForbiddenError: If the action is not allowed.
        :raises NotExistsError: If the table does not exist.
        :raises QueryStoreError: If the result set could not be counted.
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
            raise MalformedError(f'Failed to get table data: service rejected malformed payload')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to get table data: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to get table data: not found')
        if response.status_code == 409:
            raise QueryStoreError(f'Failed to get table data: service rejected result count')
        raise ResponseCodeError(f'Failed to get table data: response code: {response.status_code} is not 200 (OK)')

    def get_view_data_count(self, database_id: int, view_id: int) -> int:
        """
        Get data count of a view in a database with given database id and view id.

        :param database_id: The database id.
        :param view_id: The view id.

        :returns: The result count of the view query, if successful.

        :raises ResponseCodeError: If something went wrong with the retrieval.
        :raises MalformedError: If the payload is rejected by the service.
        :raises ForbiddenError: If the action is not allowed.
        :raises NotExistsError: If thecontainer does not exist.
        """
        url = f'/api/database/{database_id}/view/{view_id}/data'
        response = self._wrapper(method="head", url=url)
        if response.status_code == 200:
            return int(response.headers.get('X-Count'))
        if response.status_code == 400:
            raise MalformedError(f'Failed to get view data count: service rejected malformed payload')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to get view data count: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to get view data count: not found')
        raise ResponseCodeError(f'Failed to get view data count: response code: {response.status_code} is not 200 (OK)')

    def get_database_access(self, database_id: int) -> AccessType:
        """
        Get access of a view in a database with given database id and view id.

        :param database_id: The database id.

        :returns: The access type, if successful.

        :raises ResponseCodeError: If something went wrong with the retrieval.
        :raises ForbiddenError: If the action is not allowed.
        :raises NotExistsError: If thecontainer does not exist.
        """
        url = f'/api/database/{database_id}/access'
        response = self._wrapper(method="get", url=url)
        if response.status_code == 200:
            body = response.json()
            return DatabaseAccess.parse_raw(body).type
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to get database access: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to get database access: not found')
        raise ResponseCodeError(f'Failed to get database access: response code: {response.status_code} is not 200 (OK)')

    def create_database_access(self, database_id: int, user_id: str, type: AccessType) -> AccessType:
        """
        Create access to a database with given database id and user id.

        :param database_id: The database id.
        :param user_id: The user id.
        :param type: The access type.

        :returns: The access type, if successful.

        :raises ResponseCodeError: If something went wrong with the retrieval.
        :raises MalformedError: If the payload is rejected by the service.
        :raises ForbiddenError: If the action is not allowed.
        :raises NotExistsError: If thedatabase or user does not exist.
        """
        url = f'/api/database/{database_id}/access/{user_id}'
        response = self._wrapper(method="post", url=url, force_auth=True, payload=CreateAccess(type=type))
        if response.status_code == 202:
            body = response.json()
            return DatabaseAccess.parse_raw(body).type
        if response.status_code == 400:
            raise MalformedError(f'Failed to create database access: service rejected malformed payload')
        if response.status_code == 403 or response.status_code == 405:
            raise ForbiddenError(f'Failed to create database access: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to create database access: not found')
        raise ResponseCodeError(
            f'Failed to create database access: response code: {response.status_code} is not 202 (ACCEPTED)')

    def update_database_access(self, database_id: int, user_id: str, type: AccessType) -> AccessType:
        """
        Updates the access for a user to a database with given database id and user id.

        :param database_id: The database id.
        :param user_id: The user id.
        :param type: The access type.

        :returns: The access type, if successful.

        :raises ResponseCodeError: If something went wrong with the retrieval.
        :raises MalformedError: If the payload is rejected by the service.
        :raises ForbiddenError: If the action is not allowed.
        :raises NotExistsError: If thedatabase or user does not exist.
        """
        url = f'/api/database/{database_id}/access/{user_id}'
        response = self._wrapper(method="put", url=url, force_auth=True, payload=UpdateAccess(type=type))
        if response.status_code == 202:
            body = response.json()
            return DatabaseAccess.parse_raw(body).type
        if response.status_code == 400:
            raise MalformedError(f'Failed to update database access: service rejected malformed payload')
        if response.status_code == 403 or response.status_code == 405:
            raise ForbiddenError(f'Failed to update database access: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to update database access: not found')
        raise ResponseCodeError(
            f'Failed to update database access: response code: {response.status_code} is not 202 (ACCEPTED)')

    def delete_database_access(self, database_id: int, user_id: str) -> None:
        """
        Deletes the access for a user to a database with given database id and user id.

        :param database_id: The database id.
        :param user_id: The user id.

        :raises ResponseCodeError: If something went wrong with the retrieval.
        :raises MalformedError: If the payload is rejected by the service.
        :raises ForbiddenError: If the action is not allowed.
        :raises NotExistsError: If thedatabase or user does not exist.
        """
        url = f'/api/database/{database_id}/access/{user_id}'
        response = self._wrapper(method="delete", url=url, force_auth=True)
        if response.status_code == 202:
            return
        if response.status_code == 400:
            raise MalformedError(f'Failed to delete database access: service rejected malformed payload')
        if response.status_code == 403 or response.status_code == 405:
            raise ForbiddenError(f'Failed to delete database access: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to delete database access: not found')
        raise ResponseCodeError(
            f'Failed to delete database access: response code: {response.status_code} is not 202 (ACCEPTED)')

    def execute_query(self, database_id: int, query: str, page: int = 0, size: int = 10,
                      timestamp: datetime.datetime = datetime.datetime.now()) -> Result:
        """
        Executes a SQL query in a database where the current user has at least read access with given database id. The
        result set can be paginated with setting page and size (both). Historic data can be queried by setting
        timestamp.

        :param database_id: The database id.
        :param query: The query statement.
        :param page: The result pagination number. Optional. Default: 0.
        :param size: The result pagination size. Optional. Default: 10.
        :param timestamp: The query execution time. Optional.

        :returns: The result set, if successful.

        :raises ResponseCodeError: If something went wrong with the retrieval.
        :raises MalformedError: If the payload is rejected by the service.
        :raises ForbiddenError: If the action is not allowed.
        :raises NotExistsError: If the database, table or user does not exist.
        :raises QueryStoreError: The query store rejected the query.
        :raises MetadataConsistencyError: The service failed to parse columns from the metadata database.
        """
        url = f'/api/database/{database_id}/query'
        if page is not None and size is not None:
            url += f'?page={page}&size={size}'
        response = self._wrapper(method="post", url=url, force_auth=True,
                                 payload=ExecuteQuery(statement=query, timestamp=timestamp))
        if response.status_code == 202:
            body = response.json()
            return Result.parse_raw(body)
        if response.status_code == 400:
            raise MalformedError(f'Failed to execute query: service rejected malformed payload')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to execute query: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to execute query: not found')
        if response.status_code == 409:
            raise QueryStoreError(f'Failed to execute query: query store rejected query')
        if response.status_code == 417:
            raise MetadataConsistencyError(f'Failed to execute query: service expected other metadata')
        raise ResponseCodeError(
            f'Failed to execute query: response code: {response.status_code} is not 202 (ACCEPTED)')

    def get_query_data(self, database_id: int, query_id: int, page: int = 0, size: int = 10,
                       file_path: str = None) -> Result:
        """
        Re-executes a query in a database with given database id and query id.

        :param database_id: The database id.
        :param query_id: The query id.
        :param page: The result pagination number. Optional. Default: 0.
        :param size: The result pagination size. Optional. Default: 10.
        :param size: The result pagination size. Optional. Default: 10.
        :param file_path: The file path where the result should be saved. Optional.

        :returns: The result set, if successful.

        :raises ResponseCodeError: If something went wrong with the retrieval.
        :raises MalformedError: If the payload is rejected by the service.
        :raises ForbiddenError: If the action is not allowed.
        :raises NotExistsError: If the database, query or user does not exist.
        :raises QueryStoreError: The query store rejected the query.
        :raises MetadataConsistencyError: The service failed to parse columns from the metadata database.
        """
        stream = False
        headers = {}
        url = f'/api/database/{database_id}/query/{query_id}/data'
        if page is not None and size is not None:
            url += f'?page={page}&size={size}'
        if file_path is not None:
            stream = True
            headers = {'Accept': 'text/csv'}
        response = self._wrapper(method="get", url=url, headers=headers, stream=stream)
        if response.status_code == 200:
            if file_path is None:
                body = response.json()
                return Result.parse_raw(body)
            else:
                with open(file_path, "w") as f:
                    f.write(response.content.decode("utf-8"))
        if response.status_code == 400:
            raise MalformedError(f'Failed to re-execute query: service rejected malformed payload')
        if response.status_code == 403 or response.status_code == 405:
            raise ForbiddenError(f'Failed to re-execute query: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to re-execute query: not found')
        if response.status_code == 409:
            raise QueryStoreError(f'Failed to re-execute query: query store rejected query')
        if response.status_code == 417:
            raise MetadataConsistencyError(f'Failed to re-execute query: service expected other metadata')
        raise ResponseCodeError(
            f'Failed to re-execute query: response code: {response.status_code} is not 200 (OK)')

    def get_query_data_count(self, database_id: int, query_id: int, page: int = 0, size: int = 10) -> int:
        """
        Re-executes a query in a database with given database id and query id and only counts the results.

        :param database_id: The database id.
        :param query_id: The query id.
        :param page: The result pagination number. Optional. Default: 0.
        :param size: The result pagination size. Optional. Default: 10.

        :returns: The result set, if successful.

        :raises ResponseCodeError: If something went wrong with the retrieval.
        :raises MalformedError: If the payload is rejected by the service.
        :raises ForbiddenError: If the action is not allowed.
        :raises NotExistsError: If the database, query or user does not exist.
        :raises QueryStoreError: The query store rejected the query.
        :raises MetadataConsistencyError: The service failed to parse columns from the metadata database.
        """
        url = f'/api/database/{database_id}/query/{query_id}/data'
        if page is not None and size is not None:
            url += f'?page={page}&size={size}'
        response = self._wrapper(method="head", url=url)
        if response.status_code == 200:
            return int(response.headers.get('X-Count'))
        if response.status_code == 400:
            raise MalformedError(f'Failed to re-execute query: service rejected malformed payload')
        if response.status_code == 403 or response.status_code == 405:
            raise ForbiddenError(f'Failed to re-execute query: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to re-execute query: not found')
        if response.status_code == 409:
            raise QueryStoreError(f'Failed to re-execute query: query store rejected query')
        if response.status_code == 417:
            raise MetadataConsistencyError(f'Failed to re-execute query: service expected other metadata')
        raise ResponseCodeError(
            f'Failed to re-execute query: response code: {response.status_code} is not 200 (OK)')

    def get_query(self, database_id: int, query_id: int) -> Query:
        """
        Get query from a database with given database id and query id.

        :param database_id: The database id.
        :param query_id: The query id.

        :returns: The query, if successful.

        :raises ResponseCodeError: If something went wrong with the retrieval.
        :raises ForbiddenError: If the action is not allowed.
        :raises NotExistsError: If thedatabase, query or user does not exist.
        :raises QueryStoreError: The query store rejected the query.
        :raises MetadataConsistencyError: The service failed to parse columns from the metadata database.
        """
        url = f'/api/database/{database_id}/query/{query_id}'
        response = self._wrapper(method="get", url=url)
        if response.status_code == 200:
            body = response.json()
            return Query.parse_raw(body)
        if response.status_code == 404:
            raise NotExistsError(f'Failed to find query: not found')
        if response.status_code == 403 or response.status_code == 405:
            raise ForbiddenError(f'Failed to find query: not allowed')
        if response.status_code == 417:
            raise MetadataConsistencyError(f'Failed to find query: service expected other metadata')
        if response.status_code == 501 or response.status_code == 503 or response.status_code == 504:
            raise QueryStoreError(f'Failed to find query: query store rejected query')
        raise ResponseCodeError(
            f'Failed to find query: response code: {response.status_code} is not 200 (OK)')

    def get_queries(self, database_id: int) -> List[Query]:
        """
        Get queries from a database with given database id.

        :param database_id: The database id.

        :returns: List of queries, if successful.

        :raises ResponseCodeError: If something went wrong with the retrieval.
        :raises MalformedError: If the query is rejected by the service.
        :raises ForbiddenError: If the action is not allowed.
        :raises NotExistsError: If thedatabase or user does not exist.
        :raises QueryStoreError: The query store rejected the query.
        """
        url = f'/api/database/{database_id}/query'
        response = self._wrapper(method="get", url=url)
        if response.status_code == 200:
            body = response.json()
            return TypeAdapter(List[Query]).validate_json(body)
        if response.status_code == 403 or response.status_code == 405:
            raise ForbiddenError(f'Failed to find queries: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to find queries: not found')
        if response.status_code == 423:
            raise MalformedError(f'Failed to find queries: service rejected malformed query')
        if response.status_code == 501 or response.status_code == 503 or response.status_code == 504:
            raise QueryStoreError(f'Failed to find queries: query store rejected query')
        raise ResponseCodeError(
            f'Failed to find query: response code: {response.status_code} is not 200 (OK)')

    def update_query(self, database_id: int, query_id: int, persist: bool) -> Query:
        """
        Update query from a database with given database id and query id.

        :param database_id: The database id.
        :param query_id: The query id.
        :param persist: If set to true, the query will be saved and visible in the User Interface, otherwise the query \
                is marked for deletion in the future and not visible in the User Interface.

        :returns: The query, if successful.

        :raises ResponseCodeError: If something went wrong with the retrieval.
        :raises ForbiddenError: If the action is not allowed.
        :raises NotExistsError: If thedatabase or user does not exist.
        :raises QueryStoreError: The query store rejected the update.
        """
        url = f'/api/database/{database_id}/query/{query_id}'
        response = self._wrapper(method="put", url=url, force_auth=True, payload=UpdateQuery(persist=persist))
        if response.status_code == 202:
            body = response.json()
            return Query.parse_raw(body)
        if response.status_code == 403 or response.status_code == 405:
            raise ForbiddenError(f'Failed to update query: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to update query: not found')
        if response.status_code == 412:
            raise QueryStoreError(f'Failed to update query: query store rejected update')
        raise ResponseCodeError(
            f'Failed to update query: response code: {response.status_code} is not 200 (OK)')

    def create_identifier(self, database_id: int, type: IdentifierType, titles: List[CreateIdentifierTitle],
                          publisher: str, creators: List[CreateIdentifierCreator], publication_year: int,
                          descriptions: List[CreateIdentifierDescription] = None,
                          funders: List[CreateIdentifierFunder] = None, licenses: List[License] = None,
                          language: Language = None, query_id: int = None, view_id: int = None, table_id: int = None,
                          publication_day: int = None, publication_month: int = None,
                          related_identifiers: List[CreateRelatedIdentifier] = None) -> Identifier:
        """
        Create an identifier

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
        :param query_id: The query id of the created identifier. Required when type=SUBSET, otherwise invalid. Optional.
        :param view_id: The view id of the created identifier. Required when type=VIEW, otherwise invalid. Optional.
        :param table_id: The table id of the created identifier. Required when type=TABLE, otherwise invalid. Optional.
        :param publication_day: The publication day of the created identifier. Optional.
        :param publication_month: The publication month of the created identifier. Optional.
        :param related_identifiers: The related identifier(s) of the created identifier. Optional.

        :returns: The identifier, if successful.

        :raises ResponseCodeError: If something went wrong with the creation of the identifier.
        :raises ForbiddenError: If the action is not allowed.
        :raises MalformedError: If the payload is rejected by the service.
        :raises NotExistsError: If the database, table/view/query or user does not exist.
        :raises ExternalSystemError: If the external system (DataCite) refused communication with the service.
        """
        url = f'/api/identifier'
        payload = CreateIdentifier(database_id=database_id, type=type, titles=titles, publisher=publisher,
                                   creators=creators, publication_year=publication_year, descriptions=descriptions,
                                   funders=funders, licenses=licenses, language=language, query_id=query_id,
                                   view_id=view_id, table_id=table_id, publication_day=publication_day,
                                   publication_month=publication_month, related_identifiers=related_identifiers)
        response = self._wrapper(method="post", url=url, force_auth=True, payload=payload)
        if response.status_code == 201:
            body = response.json()
            return Identifier.parse_raw(body)
        if response.status_code == 400:
            raise MalformedError(f'Failed to create identifier: service rejected malformed payload')
        if response.status_code == 403 or response.status_code == 405:
            raise ForbiddenError(f'Failed to create identifier: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to create identifier: not found')
        if response.status_code == 503:
            raise ExternalSystemError(f'Failed to create identifier: external system rejected communication')
        raise ResponseCodeError(
            f'Failed to create identifier: response code: {response.status_code} is not 201 (CREATED)')

    def suggest_identifier(self, uri: str) -> Identifier:
        """
        Suggest identifier metadata for a given identifier URI. Example: ROR, ORCID, ISNI, GND, DOI.

        :param uri: The identifier URI.

        :returns: The identifier, if successful.

        :raises ResponseCodeError: If something went wrong with the suggestion of the identifier.
        :raises NotExistsError: If no metadata can be found or the identifier type is not supported.
        """
        url = f'/api/identifier?url={uri}'
        response = self._wrapper(method="get", url=url)
        if response.status_code == 200:
            body = response.json()
            return Identifier.parse_raw(body)
        if response.status_code == 404:
            raise NotExistsError(f'Failed to suggest identifier: not found or not supported')
        raise ResponseCodeError(f'Failed to suggest identifier: response code: {response.status_code} is not 200 (OK)')

    def get_licenses(self) -> List[License]:
        """
        Get list of licenses allowed.

        :returns: List of licenses, if successful.
        """
        url = f'/api/database/license'
        response = self._wrapper(method="get", url=url)
        if response.status_code == 200:
            body = response.json()
            return TypeAdapter(List[License]).validate_json(body)
        raise ResponseCodeError(f'Failed to get licenses: response code: {response.status_code} is not 200 (OK)')

    def get_identifiers(self, ld: bool = False) -> List[Identifier] | str:
        """
        Get list of identifiers.

        :param ld: If set to true, identifiers are requested as JSON-LD. Optional. Default: false.

        :returns: List of identifiers, if successful.

        :raises ResponseCodeError: If something went wrong with the retrieval of the identifiers.
        :raises NotExistsError: If the accept header is neither application/json nor application/ld+json.
        """
        url = f'/api/pid'
        headers = None
        if ld:
            headers = {'Accept': 'application/ld+json'}
        response = self._wrapper(method="get", url=url, headers=headers)
        if response.status_code == 200:
            if ld:
                return response.json()
            else:
                body = response.json()
                return TypeAdapter(List[Identifier]).validate_json(body)
        if response.status_code == 406:
            raise MalformedError(
                f'Failed to get identifiers: accept header must be application/json or application/ld+json')
        raise ResponseCodeError(f'Failed to get identifiers: response code: {response.status_code} is not 200 (OK)')

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

        :raises ResponseCodeError: If something went wrong with the retrieval of the identifiers.
        :raises NotExistsError: If the accept header is neither application/json nor application/ld+json.
        """
        url = f'/api/database/{database_id}/table/{table_id}/column/{column_id}'
        response = self._wrapper(method="put", url=url, force_auth=True,
                                 payload=UpdateColumn(concept_uri=concept_uri, unit_uri=unit_uri))
        if response.status_code == 202:
            body = response.json()
            return Column.parse_raw(body)
        if response.status_code == 400:
            raise MalformedError(f'Failed to update column: service rejected malformed payload')
        if response.status_code == 403:
            raise ForbiddenError(f'Failed to update colum: not allowed')
        if response.status_code == 404:
            raise NotExistsError(f'Failed to update colum: not found')
        raise ResponseCodeError(f'Failed to update colum: response code: {response.status_code} is not 202 (ACCEPTED)')
