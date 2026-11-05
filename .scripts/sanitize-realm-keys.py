#!/usr/bin/env python3
"""Remove generated key material and provider IDs from exported realm templates."""
import json
import sys
from pathlib import Path

for name in sys.argv[1:]:
    path = Path(name)
    realm = json.loads(path.read_text())
    for provider in realm.get('components', {}).get('org.keycloak.keys.KeyProvider', []):
        provider.pop('id', None)
        for key in ('privateKey', 'certificate', 'secret', 'kid'):
            provider.get('config', {}).pop(key, None)
    path.write_text(json.dumps(realm, indent=2) + '\n')
