import logging
import os

import ldap
import mariadb
from ldap.controls import RelaxRulesControl
from requests import post, get

logging.addLevelName(level=logging.NOTSET, levelName='TRACE')
logging.basicConfig(level=logging.DEBUG)

from logging.config import dictConfig

# logging configuration
dictConfig({
    'version': 1,
    'formatters': {
        'default': {
            'format': '[%(asctime)s] %(levelname)s in %(module)s: %(message)s',
        },
        'simple': {
            'format': '[%(asctime)s] [%(levelname)s] %(message)s',
        },
        "ecs": {
            "()": "ecs_logging.StdlibFormatter"
        },
    },
    'handlers': {
        'console': {
            'class': 'logging.StreamHandler',
            'stream': 'ext://sys.stdout',
            'formatter': 'simple'
        },
        # 'file': {
        #     'class': 'logging.handlers.TimedRotatingFileHandler',
        #     'formatter': 'ecs',
        #     'filename': '/var/log/app/service/auth/init.log',
        #     'when': 'm',
        #     'interval': 1,
        #     'backupCount': 5,
        #     'encoding': 'utf8'
        # },
    },
    'root': {
        'level': 'DEBUG',
        'handlers': ['console']
    }
})


def fetch_keycloak_master_access_token() -> str:
    """
    Fetch admin access token from the master realm.
    :return: The access token.
    """
    endpoint = os.getenv('AUTH_SERVICE_ENDPOINT', 'http://localhost:8080')
    response = post(url=f'{endpoint}/realms/master/protocol/openid-connect/token', data=dict({
        'username': os.getenv('SYSTEM_USERNAME', 'admin'),
        'password': os.getenv('SYSTEM_PASSWORD', 'admin'),
        'grant_type': 'password',
        'client_id': 'admin-cli'
    }))
    if response.status_code != 200:
        raise IOError(f'Failed to obtain admin token: {response.status_code}')
    return response.json()["access_token"]


def modify_identity(username: str, user_id: str) -> None:
    endpoint = os.getenv('IDENTITY_SERVICE_ENDPOINT', 'ldap://identity-service:1389')
    instance = ldap.initialize(endpoint)
    instance.simple_bind_s(f"{os.getenv('IDENTITY_SERVICE_ADMIN_DN', 'cn=admin,dc=dbrepo,dc=at')}",
                           os.getenv('IDENTITY_SERVICE_ADMIN_PASSWORD', 'admin'))
    logging.debug(f'modify user id {user_id} in identity service for user: {username}')
    try:
        instance.modify_ext_s(f"uid={username},ou=users,{os.getenv('IDENTITY_SERVICE_ROOT', 'dc=dbrepo,dc=at')}",
                              [(ldap.MOD_REPLACE, 'entryUUID', user_id.encode("utf-8"))],
                              serverctrls=[RelaxRulesControl()])
        logging.info(f'Modified user id {user_id} for user: {username}')
    except ldap.NO_SUCH_OBJECT:
        logging.warning(f'User {username} not found in identity service, skip')


def get_auth_users() -> list[tuple[str, str]]:
    logging.debug(f'get user ids from auth service')
    endpoint = os.getenv('AUTH_SERVICE_ENDPOINT', 'http://auth-service:8080')
    response = get(url=f'{endpoint}/admin/realms/dbrepo/users', headers=dict({
        'Authorization': f'Bearer {fetch_keycloak_master_access_token()}'
    }))
    if response.status_code != 200 or len(response.json()) == 0:
        raise FileNotFoundError(f'Failed to obtain users: {response.status_code}')
    users = []
    for ldap_user in response.json():
        user_id = ldap_user["id"]
        if 'attributes' not in ldap_user or ldap_user['attributes'] is None:
            raise ModuleNotFoundError(f'Failed to obtain user attributes: {ldap_user}')
        ldap_user_attrs = ldap_user['attributes']
        if 'LDAP_ID' not in ldap_user_attrs:
            raise ImportError(f'Failed to obtain ldap id: LDAP_ID not in attributes {ldap_user_attrs}')
        if len(ldap_user_attrs['LDAP_ID']) != 1:
            raise EnvironmentError(f'Failed to obtain ldap id: wrong length {len(ldap_user_attrs["LDAP_ID"])} != 1')
        ldap_user_id = ldap_user_attrs['LDAP_ID'][0]
        logging.debug(f'found user id: {user_id} and ldap id {ldap_user_id}')
        users.append((ldap_user_id, ldap_user["username"]))
    return users


def get_auth_user(username: str) -> (str, str):
    logging.debug(f'get user id of {username} from auth service')
    endpoint = os.getenv('AUTH_SERVICE_ENDPOINT', 'http://localhost:8080')
    response = get(url=f'{endpoint}/admin/realms/dbrepo/users/?username={username}', headers=dict({
        'Authorization': f'Bearer {fetch_keycloak_master_access_token()}'
    }))
    if response.status_code != 200 or len(response.json()) != 1:
        raise FileNotFoundError(f'Failed to obtain user')
    ldap_user = response.json()[0]
    user_id = ldap_user["id"]
    if 'attributes' not in ldap_user or ldap_user['attributes'] is None:
        raise ModuleNotFoundError(f'Failed to obtain user attributes: {ldap_user}')
    ldap_user_attrs = ldap_user['attributes']
    if 'LDAP_ID' not in ldap_user_attrs:
        raise ImportError(f'Failed to obtain ldap id: LDAP_ID not in attributes {ldap_user_attrs}')
    if len(ldap_user_attrs['LDAP_ID']) != 1:
        raise EnvironmentError(f'Failed to obtain ldap id: wrong length {len(ldap_user_attrs["LDAP_ID"])} != 1')
    ldap_user_id = ldap_user_attrs['LDAP_ID'][0]
    logging.debug(f'found user id: {user_id} and ldap id {ldap_user_id}')
    return (ldap_user_id, user_id)


def save_metadata_user(user_id: str, keycloak_id: str, username: str, password: str) -> None:
    conn = mariadb.connect(user=os.getenv('METADATA_USERNAME', 'root'),
                           password=os.getenv('METADATA_DB_PASSWORD', 'dbrepo'),
                           host=os.getenv('METADATA_HOST', 'metadata-db'),
                           port=int(os.getenv('METADATA_PORT', '3306')),
                           database=os.getenv('METADATA_DB', 'dbrepo'))
    cursor = conn.cursor()
    cursor.execute(
        "INSERT IGNORE INTO `mdb_users` (`id`, `keycloak_id`, `username`, `mariadb_password`, `is_internal`) VALUES (?, ?, ?, PASSWORD(?), true)",
        (user_id, keycloak_id, username, password))
    conn.commit()
    conn.close()
    logging.info(f'Successfully inserted internal user: {username}')


if __name__ == '__main__':
    system_username = os.getenv('SYSTEM_USERNAME', 'admin')
    system_password = os.getenv('SYSTEM_PASSWORD', 'admin')
    readonly_username = os.getenv('READONLY_USERNAME', 'user')
    readonly_password = os.getenv('READONLY_PASSWORD', 'user')
    logging.debug(f'initializing internal users ...')
    user_id, keycloak_id = get_auth_user(system_username)
    save_metadata_user(user_id, keycloak_id, system_username, system_password)
    user_id, keycloak_id = get_auth_user(readonly_username)
    save_metadata_user(user_id, keycloak_id, readonly_username, readonly_password)
    logging.debug(f'initializing normal users ...')
    for user_id, username in get_auth_users():
        modify_identity(username, user_id)
    logging.info(f'Finished')
