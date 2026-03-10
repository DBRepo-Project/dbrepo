FROM docker.io/postgres:18-alpine AS build

RUN apk --no-cache add \
    make \
    gcc \
    clang19 \
    git \
    openssh

# EXT: periods
WORKDIR /periods

RUN git clone https://github.com/xocolatl/periods.git && \
    mv -f periods/* .

RUN make

# EXT: aws_s3
WORKDIR /aws_s3

RUN git clone https://github.com/chimpler/postgres-aws-s3.git && \
    mv -f postgres-aws-s3/* .

FROM docker.io/postgres:18-alpine AS runtime
LABEL org.opencontainers.image.authors="martin.weise@student.tuwien.ac.at"

RUN apk --no-cache add \
    py-boto3

# EXT: periods
COPY --from=build /periods/periods.bc /usr/local/lib/postgresql/bitcode/periods/periods.bc
COPY --from=build /periods/periods.so /usr/local/lib/postgresql/periods.so

WORKDIR /usr/local/share/postgresql/extension

COPY --from=build /periods/periods--1.0.sql ./periods--1.0.sql
COPY --from=build /periods/periods--1.0--1.1.sql ./periods--1.0--1.1.sql
COPY --from=build /periods/periods--1.1.sql ./periods--1.1.sql
COPY --from=build /periods/periods--1.0--1.1.sql ./periods--1.0--1.2.sql
COPY --from=build /periods/periods--1.2.sql ./periods--1.2.sql
COPY --from=build /periods/periods.control ./periods.control

# EXT: aws_s3
COPY --from=build /aws_s3/aws_s3--0.0.1.sql ./aws_s3--0.0.1.sql
COPY --from=build /aws_s3/aws_s3.control ./aws_s3.control

WORKDIR /
