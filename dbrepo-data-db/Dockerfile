FROM docker.io/postgres:18-alpine AS build

RUN apk --no-cache add \
    make \
    gcc \
    clang19

WORKDIR /app

COPY ./periods/Makefile ./Makefile
COPY ./periods/periods.c ./periods.c
COPY ./periods/periods.control ./periods.control

RUN make

FROM docker.io/postgres:18-alpine AS runtime
LABEL org.opencontainers.image.authors="martin.weise@student.tuwien.ac.at"

COPY --from=build /app/periods.bc /usr/local/lib/postgresql/bitcode/periods/periods.bc
COPY --from=build /app/periods.so /usr/local/lib/postgresql/periods.so

WORKDIR /usr/local/share/postgresql/extension

COPY ./periods/periods--1.0.sql ./periods--1.0.sql
COPY ./periods/periods--1.0--1.1.sql ./periods--1.0--1.1.sql
COPY ./periods/periods--1.1.sql ./periods--1.1.sql
COPY ./periods/periods--1.0--1.1.sql ./periods--1.0--1.2.sql
COPY ./periods/periods--1.2.sql ./periods--1.2.sql
COPY ./periods/periods.control ./periods.control

WORKDIR /app
