FROM oven/bun:1.0.26-alpine AS build

WORKDIR /app

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

FROM oven/bun:1.0.26-alpine AS runtime

ARG APP_VERSION="latest"
ARG COMMIT=""

USER 1000

WORKDIR /app

COPY --from=build --chown=1000 /app/.output /app/.output

RUN chmod -R 755 /app/.output

ENV NUXT_PUBLIC_VERSION="${APP_VERSION:-}"
ENV NUXT_PUBLIC_COMMIT="${COMMIT:-}"

EXPOSE 3000

ENTRYPOINT [ "bun", "run", ".output/server/index.mjs" ]
