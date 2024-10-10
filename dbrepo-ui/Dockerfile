FROM oven/bun:1.1.20-alpine AS build

WORKDIR /app

COPY ./bun.lockb ./bun.lockb
COPY ./package.json ./package.json

RUN bun install

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

RUN bun run build

FROM node:18.19.0-alpine3.19 AS runtime

ARG APP_VERSION="latest"
ARG COMMIT=""

RUN apk add --no-cache curl bash

USER 1000

WORKDIR /app

COPY --from=build --chown=1000 /app/.output /app/.output

RUN chmod -R 755 /app/.output

ENV NUXT_PUBLIC_VERSION="${APP_VERSION:-}"
ENV NUXT_PUBLIC_COMMIT="${COMMIT:-}"
ENV NODE_OPTIONS="--max_old_space_size=256"

EXPOSE 3000

ENTRYPOINT [ "node", ".output/server/index.mjs" ]
