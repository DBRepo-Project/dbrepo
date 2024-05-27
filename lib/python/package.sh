#!/bin/bash
python -m build --sdist ./lib/python
python -m build --wheel ./lib/python
