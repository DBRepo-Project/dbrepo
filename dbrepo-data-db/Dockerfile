FROM docker.io/postgres:17-alpine AS build


RUN apk --no-cache add make \
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

FROM docker.io/postgres:17-alpine AS runtime
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"

RUN apk --no-cache add py-boto3

ARG LIBDIR="/usr/local/lib/postgresql"

# EXT: periods
COPY --from=build /periods/periods.bc $LIBDIR/bitcode/periods/periods.bc
COPY --from=build /periods/periods.so $LIBDIR/periods.so

ARG SHAREDIR="/usr/local/share/postgresql"

COPY --from=build /periods/periods--1.0.sql $SHAREDIR/extension/periods--1.0.sql
COPY --from=build /periods/periods--1.0--1.1.sql $SHAREDIR/extension/periods--1.0--1.1.sql
COPY --from=build /periods/periods--1.1.sql $SHAREDIR/extension/periods--1.1.sql
COPY --from=build /periods/periods--1.0--1.1.sql $SHAREDIR/extension/periods--1.0--1.2.sql
COPY --from=build /periods/periods--1.2.sql $SHAREDIR/extension/periods--1.2.sql
COPY --from=build /periods/periods.control $SHAREDIR/extension/periods.control

# EXT: aws_s3
COPY --from=build /aws_s3/aws_s3--0.0.1.sql $SHAREDIR/extension/aws_s3--0.0.1.sql
COPY --from=build /aws_s3/aws_s3.control $SHAREDIR/extension/aws_s3.control

# EXT: dbrepo
COPY ./dbrepo--1.13.sql $SHAREDIR/extension/dbrepo--1.13.sql
COPY ./dbrepo.control $SHAREDIR/extension/dbrepo.control
