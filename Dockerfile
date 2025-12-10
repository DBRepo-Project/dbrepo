FROM docker.io/python:3.11-alpine3.21

RUN apk --no-cache add python3-dev
RUN python -m pip install build

COPY ./lib/python ./lib/python

RUN python3 -m build --sdist ./lib/python
RUN python3 -m build --wheel ./lib/python

RUN ls -la ./lib/python/dist