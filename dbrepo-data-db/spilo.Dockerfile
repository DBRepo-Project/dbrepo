FROM ghcr.io/zalando/spilo-17:4.0-p3 AS build

USER 0

RUN apt update && \
    apt install -y make \
                   gcc \
                   postgresql-server-dev-17

WORKDIR /app

COPY ./periods/Makefile ./Makefile
COPY ./periods/periods.c ./periods.c
COPY ./periods/periods.control ./periods.control

RUN make

FROM ghcr.io/zalando/spilo-17:4.0-p3 AS runtime
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"
LABEL org.opencontainers.image.url="https://github.com/xocolatl/periods"

ARG LIBDIR="/usr/lib/postgresql/17/lib"

COPY --from=build /app/periods.bc $LIBDIR/bitcode/periods/periods.bc
COPY --from=build /app/periods.so $LIBDIR/periods.so

ARG SHAREDIR="/usr/share/postgresql/17"

COPY ./periods/periods--1.0.sql $SHAREDIR/extension/periods--1.0.sql
COPY ./periods/periods--1.0--1.1.sql $SHAREDIR/extension/periods--1.0--1.1.sql
COPY ./periods/periods--1.1.sql $SHAREDIR/extension/periods--1.1.sql
COPY ./periods/periods--1.0--1.1.sql $SHAREDIR/extension/periods--1.0--1.2.sql
COPY ./periods/periods--1.2.sql $SHAREDIR/extension/periods--1.2.sql
COPY ./periods/periods.control $SHAREDIR/extension/periods.control

WORKDIR /app
