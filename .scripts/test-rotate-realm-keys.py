import importlib.util
import unittest
from pathlib import Path

import requests
import requests_mock

spec = importlib.util.spec_from_file_location('rotation', Path(__file__).with_name('rotate-realm-keys.py'))
rotation = importlib.util.module_from_spec(spec)
spec.loader.exec_module(rotation)


class RotationTest(unittest.TestCase):
    def test_replace_then_delete_and_repeat_without_changes(self):
        old = {'id': 'old', 'name': 'rsa', 'providerId': 'rsa-generated', 'config': {'priority': ['100']}}
        base = 'http://keycloak/admin/realms/master'
        with requests_mock.Mocker() as mock:
            mock.get(base, json={'id': 'realm-id'})
            mock.get(base + '/components', [{'json': [old]}, {'json': []}, {'json': []}, {'json': []}])
            create = mock.post(base + '/components', status_code=201)
            remove = mock.delete(base + '/components/old', status_code=204)
            refreshed = []
            session = requests.Session()
            self.assertEqual(1, rotation.rotate(session, 'http://keycloak', 'master', [old], lambda: refreshed.append(True)))
            self.assertEqual(0, rotation.rotate(session, 'http://keycloak', 'master', [old]))
            self.assertEqual(1, create.call_count)
            self.assertEqual(1, remove.call_count)
            self.assertEqual([True], refreshed)
            replacement = create.last_request.json()
            self.assertNotIn('id', replacement)
            self.assertEqual('realm-id', replacement['parentId'])
            self.assertLess([r.method for r in mock.request_history].index('POST'),
                            [r.method for r in mock.request_history].index('DELETE'))


if __name__ == '__main__':
    unittest.main()
