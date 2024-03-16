FROM oven/bun:1.0.26-alpine as build
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

WORKDIR /app

COPY ./package.json ./package.json
COPY ./bun.lockb ./bun.lockb

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

FROM oven/bun:1.0.26-alpine as runtime
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

ARG TAG=""
ARG CI_COMMIT_SHA=""

USER 1000

WORKDIR /app

COPY --from=build --chown=1000:1000 /app/.output /app/.output

ENV NUXT_PUBLIC_VERSION="${TAG:-latest}"
ENV NUXT_PUBLIC_COMMIT="${COMMIT:-deadbeef}"

EXPOSE 3000

ENTRYPOINT [ "bun", "run", ".output/server/index.mjs" ]
