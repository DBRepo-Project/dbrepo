#!/usr/bin/env python3
import os
from distutils.core import setup

app_version = os.environ.get("APP_VERSION", "1.13.1")
doc_version = os.environ.get("DOC_VERSION", "1.13")

setup(name="dbrepo",
      version=str(app_version),
      description="A library for communicating with DBRepo",
      url=f"https://www.ifs.tuwien.ac.at/infrastructures/dbrepo/{doc_version}/",
      author="Martin Weise",
      license="Apache-2.0",
      author_email="martin.weise@tuwien.ac.at",
      packages=[
          "dbrepo",
          "dbrepo.api",
          "dbrepo.core",
          "dbrepo.core.api",
          "dbrepo.core.client",
          "dbrepo.core.omlib",
          "dbrepo.core.omlib.exceptions",
          "dbrepo.core.omlib.rdf",
      ])
