import os

import mariadb
from keycloak import KeycloakAdmin

system_username = os.getenv('SYSTEM_USERNAME', 'admin')

admin = KeycloakAdmin(server_url=os.getenv('AUTH_SERVICE_ENDPOINT', 'http://localhost:8080'),
                      username=os.getenv('AUTH_SERVICE_ADMIN', 'admin'),
                      password=os.getenv('AUTH_SERVICE_ADMIN_PASSWORD', 'admin'),
                      verify=True)
keycloak_user_id = admin.get_user_id(username=system_username)
print(f'Successfully fetched keycloak user id: {keycloak_user_id}')
ldap_user_id = admin.get_user(user_id=keycloak_user_id).get('attributes')['LDAP_ID'][0]
print(f'Successfully fetched ldap user id: {ldap_user_id}')

try:
    conn = mariadb.connect(user=os.getenv('METADATA_USERNAME', 'root'),
                           password=os.getenv('METADATA_DB_PASSWORD', 'dbrepo'),
                           host="metadata-db",
                           port=3306,
                           database=os.getenv('METADATA_DB', 'dbrepo'))
    cursor = conn.cursor()
    cursor.execute(
        "INSERT IGNORE INTO `mdb_users` (`id`, `username`, `email`, `mariadb_password`) VALUES (?, ?, ?, PASSWORD(?))",
        (ldap_user_id, system_username, 'some@admin', '1234567890'))
    conn.commit()
    conn.close()
except mariadb.Error as e:
    print(f"Error connecting to MariaDB Platform: {e}")
    exit(1)

print(f'Successfully inserted user')
exit(0)
