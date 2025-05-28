import logging
import os

import mariadb
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
        'file': {
            'class': 'logging.handlers.TimedRotatingFileHandler',
            'formatter': 'ecs',
            'filename': '/var/log/app/service/auth/init.log',
            'when': 'm',
            'interval': 1,
            'backupCount': 5,
            'encoding': 'utf8'
        },
    },
    'root': {
        'level': 'DEBUG',
        'handlers': ['console', 'file']
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


def fetch(username) -> (str, str):
    logging.debug(f'fetching user id of internal user with username: {username}')
    endpoint = os.getenv('AUTH_SERVICE_ENDPOINT', 'http://localhost:8080')
    response = get(url=f'{endpoint}/admin/realms/dbrepo/users/?username={username}', headers=dict({
        'Authorization': f'Bearer {fetch_keycloak_master_access_token()}'
    }))
    if response.status_code != 200 or len(response.json()) != 1:
        raise FileNotFoundError(f'Failed to obtain user')
    ldap_user = response.json()[0]
    user_id = ldap_user["id"]
    logging.debug(f'obtained user id for username {username} from auth service: {user_id}')
    if 'attributes' not in ldap_user or ldap_user['attributes'] is None:
        raise ModuleNotFoundError(f'Failed to obtain user attributes: {ldap_user}')
    ldap_user_attrs = ldap_user['attributes']
    if 'LDAP_ID' not in ldap_user_attrs:
        raise ImportError(f'Failed to obtain ldap id: LDAP_ID not in attributes {ldap_user_attrs}')
    if len(ldap_user_attrs['LDAP_ID']) != 1:
        raise EnvironmentError(f'Failed to obtain ldap id: wrong length {len(ldap_user_attrs["LDAP_ID"])} != 1')
    ldap_user_id = ldap_user_attrs['LDAP_ID'][0]
    return (ldap_user_id, user_id)


def save(user_id: str, keycloak_id: str, username: str) -> None:
    conn = mariadb.connect(user=os.getenv('METADATA_USERNAME', 'root'),
                           password=os.getenv('METADATA_DB_PASSWORD', 'dbrepo'),
                           host=os.getenv('METADATA_HOST', 'metadata-db'),
                           port=int(os.getenv('METADATA_PORT', '3306')),
                           database=os.getenv('METADATA_DB', 'dbrepo'))
    cursor = conn.cursor()
    cursor.execute(
        "INSERT IGNORE INTO `mdb_users` (`id`, `keycloak_id`, `username`, `mariadb_password`, `is_internal`) VALUES (?, ?, ?, PASSWORD(LEFT(UUID(), 20)), true)",
        (user_id, keycloak_id, username))
    conn.commit()
    conn.close()
    logging.info(f'Successfully inserted user: {username}')


if __name__ == '__main__':
    system_username = os.getenv('SYSTEM_USERNAME', 'admin')
    readonly_username = os.getenv('READONLY_USERNAME', 'user')
    user_id, keycloak_id = fetch(system_username)
    save(user_id, keycloak_id, system_username)
    user_id, keycloak_id = fetch(readonly_username)
    save(user_id, keycloak_id, readonly_username)
