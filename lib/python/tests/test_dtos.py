import inspect
import sys
import unittest

from yaml import safe_load

from dbrepo.api import dto


class AnalyseUnitTest(unittest.TestCase):
    schemas = None
    models: [()] = []
    found: int = 0
    skipped: int = 0

    def setUp(self):
        with open('../../../.docs/.openapi/api.yaml', 'r') as f:
            self.schemas = safe_load(f)['components']['schemas']
            for name, obj in inspect.getmembers(sys.modules[dto.__name__]):
                if not inspect.isclass(obj):
                    self.found += 1
                    continue
                if f'{name}Dto' not in self.schemas:
                    self.skipped += 1
                    continue
                self.models.append((name, obj))

    def build_model(self, name: str, obj: any, definition: any) -> dict:
        model_dict = dict()
        for property in definition['properties']:
            if 'example' not in definition['properties'][property]:
                if '$ref' not in definition['properties'][property]:
                    self.fail(f'OpenAPI model {name}Dto does not have example for property: {property}')
                ref = definition['properties'][property]['$ref'][len('#/components/schemas/'):-3]
                # recursive call
                model_dict[property] = self.build_model(ref, obj, self.schemas[f'{name}Dto'])
            model_dict[property] = definition['properties'][property]['example']
        model = obj(**model_dict)

    def test_dtos_succeeds(self):
        for name, obj in self.models:
            self.build_model(name, obj, self.schemas[f'{name}Dto'])

    pass
