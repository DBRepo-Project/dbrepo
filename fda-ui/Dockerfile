# FROM node:lts as build
FROM node:14-alpine as build
MAINTAINER Kirill Stytsenko <kirill.stytsenko@univie.ac.at>

RUN apk --no-cache add curl
RUN cd /tmp && curl -LO https://github.com/prometheus/node_exporter/releases/download/v0.18.1/node_exporter-0.18.1.linux-amd64.tar.gz && tar -xvf node_exporter-0.18.1.linux-amd64.tar.gz && mv node_exporter-0.18.1.linux-amd64/node_exporter /usr/local/bin/

ENV NODE_ENV=production
ENV HOST=0.0.0.0
ENV API=http://gateway-service:9095

WORKDIR /app

COPY ./package.json ./
COPY ./yarn.lock ./

# Install yarn dependencies
RUN yarn install --frozen-lockfile > /dev/null 2>&1

COPY ./nuxt.config.js ./
COPY ./ava.config.cjs ./
COPY ./babel.config.js ./
COPY ./assets ./assets
COPY ./components ./components
COPY ./lang ./lang
COPY ./layouts ./layouts
COPY ./pages ./pages
COPY ./plugins ./plugins
COPY ./server-middleware ./server-middleware
COPY ./static ./static
COPY ./store ./store
COPY ./utils ./utils

RUN yarn build > /dev/null

EXPOSE 3000
EXPOSE 9100

ENV API=http://gateway-service:9095
ENV BROKER_USERNAME=fda
ENV BROKER_PASSWORD=fda
ENV SANDBOX=false
ENV SEARCH=http://search-service:9200
ENV SHARED_FILESYSTEM=/tmp
ENV VERSION=1.2
ENV LOGO=/logo.png
ENV MAIL_VERIFY=false

COPY ./docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

ENTRYPOINT [ "/app/docker-entrypoint.sh" ]
