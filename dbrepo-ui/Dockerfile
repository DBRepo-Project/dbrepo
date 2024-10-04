FROM node:18.20.4-alpine3.20 AS build

WORKDIR /app

COPY ./package.json ./package.json

RUN npm install

ENV NODE_ENV="production"

COPY ./assets ./assets
COPY ./components ./components
COPY ./composables ./composables
COPY ./dto ./dto
COPY ./layouts ./layouts
COPY ./locales ./locales
COPY ./pages ./pages
COPY ./plugins ./plugins
COPY ./public ./public
COPY ./server ./server
COPY ./stores ./stores
COPY ./utils ./utils
COPY ./nuxt.config.ts ./nuxt.config.ts

RUN npm run build

FROM node:18.20.4-alpine3.20 AS runtime

ARG APP_VERSION="latest"
ARG COMMIT=""

USER 1000

WORKDIR /app

COPY --from=build --chown=1000 /app/.output /app/.output

RUN chmod -R 755 /app/.output

ENV NUXT_PUBLIC_VERSION="${APP_VERSION:-}"
ENV NUXT_PUBLIC_COMMIT="${COMMIT:-}"
ENV NODE_OPTIONS="--max_old_space_size=4096"

EXPOSE 3000

ENTRYPOINT [ "node", ".output/server/index.mjs" ]
