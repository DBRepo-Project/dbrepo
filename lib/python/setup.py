#!/usr/bin/env python3
from distutils.core import setup

setup(name="dbrepo",
      version="1.4.2rc1",
      description="A library for communicating with DBRepo",
      url="https://www.ifs.tuwien.ac.at/infrastructures/dbrepo//",
      author="Martin Weise",
      license="Apache-2.0",
      author_email="martin.weise@tuwien.ac.at",
      packages=[
            "dbrepo",
            "dbrepo.api"
      ])
