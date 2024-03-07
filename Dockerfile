FROM python:3.11-slim as build

WORKDIR /app

RUN apt-get update && apt-get install -y git

ENV VERSIONS="1.4.1,1.4.0,1.3.0"
ENV APP_VERSION="1.4.1"

COPY .git/ .git/
COPY .docs/ .docs/
COPY ./requirements.txt ./requirements.txt

RUN bash .docs/build-website.sh

FROM nginx as runtime

WORKDIR /usr/share/nginx/html/infrastructures/dbrepo/

COPY --from=build /app/final /usr/share/nginx/html/infrastructures/dbrepo/
