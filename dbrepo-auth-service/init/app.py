import os

import mariadb
from requests import post, get

endpoint = os.getenv('AUTH_SERVICE_ENDPOINT', 'http://localhost:8080')
system_username = os.getenv('SYSTEM_USERNAME', 'admin')


def fetch() -> (str, str):
    print(f'Fetching user id of internal user with username: {system_username}')
    response = post(url=f'{endpoint}/realms/master/protocol/openid-connect/token', data=dict({
        'username': os.getenv('AUTH_SERVICE_ADMIN', 'admin'),
        'password': os.getenv('AUTH_SERVICE_ADMIN_PASSWORD', 'admin'),
        'grant_type': 'password',
        'client_id': 'admin-cli'
    }))

    if response.status_code != 200:
        raise IOError(f'Failed to obtain admin token: {response.status_code}')

    response = get(url=f'{endpoint}/admin/realms/dbrepo/users/?username={system_username}', headers=dict({
        'Authorization': f'Bearer {response.json()["access_token"]}'
    }))
    if response.status_code != 200 or len(response.json()) != 1:
        raise FileNotFoundError(f'Failed to obtain user')
    ldap_user = response.json()[0]
    user_id = ldap_user["id"]
    print(f'Successfully fetched user id: {user_id}')
    if 'attributes' not in ldap_user or ldap_user['attributes'] is None:
        raise ModuleNotFoundError(f'Failed to obtain user attributes: {ldap_user}')
    ldap_user_attrs = ldap_user['attributes']
    if 'LDAP_ID' not in ldap_user_attrs:
        raise ImportError(f'Failed to obtain ldap id: LDAP_ID not in attributes {ldap_user_attrs}')
    if len(ldap_user_attrs['LDAP_ID']) != 1:
        raise EnvironmentError(f'Failed to obtain ldap id: wrong length {len(ldap_user_attrs["LDAP_ID"])} != 1')
    ldap_user_id = ldap_user_attrs['LDAP_ID'][0]
    print(f'Successfully fetched ldap user id: {ldap_user_id}')
    return (ldap_user_id, user_id)


def save(user_id: str, keycloak_id: str) -> None:
    conn = mariadb.connect(user=os.getenv('METADATA_USERNAME', 'root'),
                           password=os.getenv('METADATA_DB_PASSWORD', 'dbrepo'),
                           host="metadata-db",
                           port=3306,
                           database=os.getenv('METADATA_DB', 'dbrepo'))
    cursor = conn.cursor()
    cursor.execute(
        "INSERT IGNORE INTO `mdb_users` (`id`, `keycloak_id`, `username`, `mariadb_password`, `is_internal`) VALUES (?, ?, ?, PASSWORD(LEFT(UUID(), 20)), true)",
        (user_id, keycloak_id, system_username))
    conn.commit()
    conn.close()


if __name__ == '__main__':
    user_id, keycloak_id = fetch()
    save(user_id, keycloak_id)
    print(f'Successfully inserted user')
