import logging
import os
import sys

import mariadb
from keycloak import KeycloakAdmin

system_username = os.getenv('SYSTEM_USERNAME', 'admin')

admin = KeycloakAdmin(server_url=os.getenv('AUTH_SERVICE_ENDPOINT', 'http://localhost:8080'),
                      username=os.getenv('AUTH_SERVICE_ADMIN', 'admin'),
                      password=os.getenv('AUTH_SERVICE_ADMIN_PASSWORD', 'admin'),
                      verify=True)
keycloak_user_id = admin.get_user_id(username=system_username)
logging.info(f'Successfully fetched keycloak user id: {keycloak_user_id}')
ldap_user = admin.get_user(user_id=keycloak_user_id)
if ldap_user is None:
    logging.error(f'Failed to obtain user')
    sys.exit(1)
ldap_user_attrs = ldap_user.get('attributes')
if ldap_user_attrs is None:
    logging.error(f'Failed to obtain user attributes')
    sys.exit(1)
if 'LDAP_ID' not in ldap_user_attrs:
    logging.error(f'Failed to obtain ldap id: LDAP_ID not in attributes {ldap_user_attrs}')
    sys.exit(1)
if len(ldap_user_attrs['LDAP_ID']) != 1:
    logging.error(f'Failed to obtain ldap id: wrong length {len(ldap_user_attrs["LDAP_ID"])} != 1')
    sys.exit(1)
ldap_user_id = ldap_user_attrs['LDAP_ID'][0]
logging.info(f'Successfully fetched ldap user id: {ldap_user_id}')

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
    logging.info(f"Error connecting to MariaDB Platform: {e}")
    exit(1)

logging.info(f'Successfully inserted user')
exit(0)
