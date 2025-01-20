import os
import sys

import mariadb

from requests import post, get

endpoint = os.getenv('AUTH_SERVICE_ENDPOINT', 'http://localhost:8080')
system_username = os.getenv('SYSTEM_USERNAME', 'admin')

print(f'Fetching user id of internal user with username: {system_username}')
response = post(url=f'{endpoint}/realms/master/protocol/openid-connect/token', data=dict({
    'username': os.getenv('AUTH_SERVICE_ADMIN', 'admin'),
    'password': os.getenv('AUTH_SERVICE_ADMIN_PASSWORD', 'admin'),
    'grant_type': 'password',
    'client_id': 'admin-cli'
}))

if response.status_code != 200:
    print(f'Failed to obtain admin token: {response.status_code}')

response = get(url=f'{endpoint}/admin/realms/dbrepo/users/?username={system_username}', headers=dict({
    'Authorization': f'Bearer {response.json()["access_token"]}'
}))
if len(response.json()) != 1:
    print(f'Failed to obtain user')
    sys.exit(1)
ldap_user = response.json()[0]
print(f'Successfully fetched user id: {ldap_user["id"]}')
ldap_user_attrs = ldap_user['attributes']
if ldap_user_attrs is None:
    print(f'Failed to obtain user attributes: {ldap_user}')
    sys.exit(1)
if 'LDAP_ID' not in ldap_user_attrs:
    print(f'Failed to obtain ldap id: LDAP_ID not in attributes {ldap_user_attrs}')
    sys.exit(1)
if len(ldap_user_attrs['LDAP_ID']) != 1:
    print(f'Failed to obtain ldap id: wrong length {len(ldap_user_attrs["LDAP_ID"])} != 1')
    sys.exit(1)
ldap_user_id = ldap_user_attrs['LDAP_ID'][0]
print(f'Successfully fetched ldap user id: {ldap_user_id}')

try:
    conn = mariadb.connect(user=os.getenv('METADATA_USERNAME', 'root'),
                           password=os.getenv('METADATA_DB_PASSWORD', 'dbrepo'),
                           host="metadata-db",
                           port=3306,
                           database=os.getenv('METADATA_DB', 'dbrepo'))
    cursor = conn.cursor()
    cursor.execute(
        "INSERT IGNORE INTO `mdb_users` (`id`, `username`, `email`, `mariadb_password`, `is_internal`) VALUES (?, ?, LEFT(UUID(), 20), PASSWORD(LEFT(UUID(), 20)), true)",
        (ldap_user_id, system_username))
    conn.commit()
    conn.close()
except mariadb.Error as e:
    print(f"Error connecting to MariaDB Platform: {e}")
    exit(1)

print(f'Successfully inserted user')
exit(0)
