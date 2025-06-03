#!/usr/bin/env python3
from distutils.core import setup

setup(name="dbrepo",
      version="1.9.2",
      description="A library for communicating with DBRepo",
      url="https://www.ifs.tuwien.ac.at/infrastructures/dbrepo/1.9/",
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
