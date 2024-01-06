FROM node:14-alpine as build
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

ARG TAG=latest

ENV HOST=0.0.0.0
ENV API=http://:80

WORKDIR /app

COPY ./package.json ./package.json
COPY ./yarn.lock ./yarn.lock

# Install yarn dependencies
RUN yarn install --frozen-lockfile

COPY ./nuxt.config.js ./nuxt.config.js
COPY ./ava.config.cjs ./ava.config.cjs
COPY ./babel.config.js ./babel.config.js
COPY ./dbrepo.config.json ./dbrepo.config.json
COPY ./assets ./assets
COPY ./api ./api
COPY ./components ./components
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

WORKDIR /app

COPY --from=build /app /app

ENTRYPOINT [ "yarn", "start" ]
