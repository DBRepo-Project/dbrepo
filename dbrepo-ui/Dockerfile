FROM node:14-alpine as build
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

RUN apk --no-cache add curl
RUN cd /tmp && curl -LO https://github.com/prometheus/node_exporter/releases/download/v0.18.1/node_exporter-0.18.1.linux-amd64.tar.gz && tar -xvf node_exporter-0.18.1.linux-amd64.tar.gz && mv node_exporter-0.18.1.linux-amd64/node_exporter /usr/local/bin/

ARG TAG=1.2

ENV NODE_ENV=production
ENV HOST=0.0.0.0
ENV API=http://:80
ENV BASE_URL=http://:80

WORKDIR /app

COPY ./package.json ./
COPY ./yarn.lock ./

# Install yarn dependencies
RUN yarn install --frozen-lockfile

COPY ./nuxt.config.js ./
COPY ./ava.config.cjs ./
COPY ./babel.config.js ./
COPY ./config.js ./
COPY ./assets ./assets
COPY ./api ./api
COPY ./components ./components
COPY ./lang ./lang
COPY ./layouts ./layouts
COPY ./locales ./locales
COPY ./pages ./pages
COPY ./plugins ./plugins
COPY ./server-middleware ./server-middleware
COPY ./static ./static
COPY ./store ./store
COPY ./utils ./utils

RUN yarn build

FROM node:14-alpine as runtime
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

EXPOSE 3000
EXPOSE 9100

ENV BROKER_USERNAME="fda"
ENV BROKER_PASSWORD="fda"
ENV SANDBOX=false
ENV SEARCH="http://search-service:9200"
ENV SHARED_FILESYSTEM="/tmp"
ENV LOGO="/logo.png"
ENV ELASTIC_USERNAME="elastic"
ENV ELASTIC_PASSWORD="elastic"
ENV VERSION="${TAG:-latest}"
ENV TITLE="Database Repository"
ENV ICON="/favicon.ico"
ENV DBREPO_CLIENT_ID="dbrepo-client"
ENV DBREPO_CLIENT_SECRET="MUwRc7yfXSJwX8AdRMWaQC3Nep1VjwgG"
ENV GIT_HASH="deadbeef"

WORKDIR /app

COPY --from=build /usr/local/bin/node_exporter/ /usr/local/bin/node_exporter/
COPY --from=build /app /app

RUN ls -la

COPY ./docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

ENTRYPOINT [ "sh", "/app/docker-entrypoint.sh" ]
