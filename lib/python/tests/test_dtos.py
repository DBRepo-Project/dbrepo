import inspect
import logging
import sys
import unittest
from logging.config import dictConfig
from math import floor

from yaml import safe_load

from dbrepo.api import dto

logging.addLevelName(level=logging.NOTSET, levelName='TRACE')
logging.basicConfig(level=logging.DEBUG)

# logging configuration
dictConfig({
    'version': 1,
    'formatters': {
        'default': {
            'format': '[%(asctime)s] %(levelname)s in %(module)s: %(message)s',
        },
        'simple': {
            'format': '[%(asctime)s] %(levelname)s: %(message)s',
        },
    },
    'handlers': {'console': {
        'class': 'logging.StreamHandler',
        'stream': 'ext://sys.stdout',
        'formatter': 'simple'  # default
    }},
    'root': {
        'level': 'DEBUG',
        'handlers': ['console']
    }
})


class DtoUnitTest(unittest.TestCase):
    schemas = None
    models: [()] = []
    exclusions: [str] = ['EnumType']
    found: int = 0
    skipped: [str] = []

    def setUp(self):
        with open('../../.docs/.openapi/api.yaml') as fh:
            self.schemas = safe_load(fh)['components']['schemas']
            for name, obj in inspect.getmembers(sys.modules[dto.__name__]):
                self.found += 1
                if not inspect.isclass(obj):
                    continue
                if f'{name}Dto' not in self.schemas or name not in self.exclusions:
                    logging.debug(f'skip model {name}: OpenAPI schema definition {name}Dto not found')
                    self.skipped.append(f'{name}Dto')
                    continue
                self.models.append((name, obj))

    def build_model(self, name: str, obj: any, definition: any) -> dict:
        model_dict = dict()
        for property in definition['properties']:
            if 'example' in definition['properties'][property]:
                if '$ref' not in definition['properties'][property]:
                    model_dict[property] = definition['properties'][property]['example']
                    continue
                ref = definition['properties'][property]['$ref'][len('#/components/schemas/'):-3]
                # recursive call
                model_dict[property] = self.build_model(ref, self.get_model(ref), self.schemas[f'{ref}Dto'])
                continue
            if 'items' in definition['properties'][property]:
                if '$ref' not in definition['properties'][property]['items']:
                    continue
                ref = definition['properties'][property]['items']['$ref'][len('#/components/schemas/'):-3]
                # recursive call
                model_dict[property] = [self.build_model(ref, self.get_model(ref), self.schemas[f'{ref}Dto'])]
                continue
            if '$ref' in definition['properties'][property]:
                ref = definition['properties'][property]['$ref'][len('#/components/schemas/'):-3]
                # recursive call
                model_dict[property] = self.build_model(ref, self.get_model(ref), self.schemas[f'{ref}Dto'])
        return model_dict

    def get_model(self, ref: str):
        for name, obj in self.models:
            if name == ref:
                return obj
        return None

    def test_dtos_succeeds(self):
        logging.info(f'Found {self.found} model(s) in {dto.__name__}')
        for name, obj in self.models:
            logging.debug(f'building model: {name} against OpenAPI schema definition {name}Dto')
            model = obj(**self.build_model(name, obj, self.schemas[f'{name}Dto']))
        logging.warning(f'Unable to find {len(self.skipped)} OpenAPI schema definition(s): {self.skipped}')
        logging.info(f'Coverage: {floor((1 - len(self.skipped) / self.found) * 100)}%')
        pass
