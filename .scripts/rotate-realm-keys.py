#!/usr/bin/env python3
"""Replace key providers imported from the retired realm exports."""
import argparse
import getpass
import json
import os
from pathlib import Path

import requests


def rotate(session, endpoint, realm, retired, refresh=None):
    base = f"{endpoint}/admin/realms/{realm}"
    response = session.get(base, timeout=10)
    response.raise_for_status()
    realm_id = response.json()["id"]
    response = session.get(f"{base}/components", params={"type": "org.keycloak.keys.KeyProvider"}, timeout=10)
    response.raise_for_status()
    existing = {item["id"]: item for item in response.json()}
    changed = 0
    for template in retired:
        old_id = template["id"]
        if old_id not in existing:
            continue
        # Create the replacement before removing the old provider, including in master.
        replacement = {key: value for key, value in template.items() if key != "id"}
        replacement.update(parentId=realm_id, providerType="org.keycloak.keys.KeyProvider")
        response = session.post(f"{base}/components", json=replacement, timeout=10)
        response.raise_for_status()
        response = session.delete(f"{base}/components/{old_id}", timeout=10)
        response.raise_for_status()
        if refresh:
            refresh()
        changed += 1
    response = session.get(f"{base}/components", params={"type": "org.keycloak.keys.KeyProvider"}, timeout=10)
    response.raise_for_status()
    remaining = {item["id"] for item in response.json()}
    if remaining.intersection(item["id"] for item in retired):
        raise RuntimeError(f"Retired key providers remain in realm {realm}")
    return changed


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--endpoint', required=True, help='Keycloak base URL')
    parser.add_argument('--username', default='admin')
    args = parser.parse_args()
    endpoint = args.endpoint.rstrip('/')
    password = os.environ.get('AUTH_SERVICE_ADMIN_PASSWORD') or getpass.getpass('Keycloak admin password: ')
    session = requests.Session()

    def authenticate():
        response = requests.post(f'{endpoint}/realms/master/protocol/openid-connect/token',
                                 data={'client_id': 'admin-cli', 'grant_type': 'password',
                                       'username': args.username, 'password': password}, timeout=10)
        response.raise_for_status()
        session.headers['Authorization'] = 'Bearer ' + response.json()['access_token']

    authenticate()
    retired = json.loads(Path(__file__).with_name('retired-realm-key-providers.json').read_text())
    # Rotate master last: its old admin token becomes invalid after replacement.
    for realm in ('dbrepo', 'master'):
        changed = rotate(session, endpoint, realm, retired[realm], authenticate if realm == 'master' else None)
        print(f'{realm}: replaced {changed} retired key providers')



if __name__ == '__main__':
    main()
