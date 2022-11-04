# FROM node:lts as build
FROM node:14-alpine as build
MAINTAINER Kirill Stytsenko <kirill.stytsenko@univie.ac.at>

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

ENV API=http://gateway-service:9095
ENV SEARCH=http://search-service:9200
ENV BROKER_USERNAME=fda
ENV BROKER_PASSWORD=fda
ENV SANDBOX=false
ENV SHARED_FILESYSTEM=/tmp
ENV VERSION=1.1
ENV LOGO=/logo.png
ENV MAIL_VERIFY=false

ENTRYPOINT [ "yarn", "start" ]
