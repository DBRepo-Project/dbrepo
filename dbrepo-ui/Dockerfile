FROM node:14-alpine as build
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

ARG TAG=latest
ARG GIT_HASH_SHORT=git_hash

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
ENV SEARCH="http://search-db:9200"
ENV SHARED_FILESYSTEM="/tmp"
ENV LOGO="/logo.png"
ENV SEARCH_USERNAME="admin"
ENV SEARCH_PASSWORD="admin"
ENV VERSION="${TAG}"
ENV TITLE="Database Repository"
ENV ICON="/favicon.ico"
ENV DBREPO_CLIENT_ID="dbrepo-client"
ENV DBREPO_CLIENT_SECRET="MUwRc7yfXSJwX8AdRMWaQC3Nep1VjwgG"
ENV GIT_HASH="${GIT_HASH_SHORT}"
ENV UPLOAD_ENDPOINT="http://upload-service:1080/files/"
ENV UPLOAD_PATH="/tmp/"

WORKDIR /app

COPY --from=build /app /app

RUN ls -la

ENTRYPOINT [ "yarn", "start" ]
