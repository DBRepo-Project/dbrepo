FROM python:3.11-slim as build

WORKDIR /app

ENV VERSIONS="latest,1.4.1,1.4.0"
ENV APP_VERSION="1.4.1"

RUN apt-get update && apt-get install -y git

COPY .git/ .git/
COPY .docs/ .docs/
COPY ./requirements.txt ./requirements.txt

RUN bash .docs/build-website.sh

FROM nginx as runtime

WORKDIR /usr/share/nginx/html/infrastructures/dbrepo/

COPY --from=build /app/final /usr/share/nginx/html/infrastructures/dbrepo/

RUN rm -f ./index.html

RUN pwd

RUN ls -la