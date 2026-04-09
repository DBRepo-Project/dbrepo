FROM ghcr.io/cloudnative-pg/postgresql:17.9-minimal-trixie AS build

USER 0

RUN apt update && \
    apt install -y make \
                   gcc \
                   git \
                   postgresql-server-dev-17 \
                   python3-boto3

# EXT: periods
WORKDIR /periods

RUN git clone https://github.com/xocolatl/periods.git && \
    mv -f periods/* .

RUN make

# EXT: aws_s3
WORKDIR /aws_s3

RUN git clone https://github.com/chimpler/postgres-aws-s3.git && \
    mv -f postgres-aws-s3/* .

FROM ghcr.io/cloudnative-pg/postgresql:17.9-minimal-trixie AS runtime
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"

USER 0

RUN apt update && \
    apt install -y python3-boto3 && \
    apt clean

ARG LIBDIR="/usr/lib/postgresql/17/lib"

# EXT: periods
COPY --from=build /periods/periods.bc $LIBDIR/bitcode/periods/periods.bc
COPY --from=build /periods/periods.so $LIBDIR/periods.so

ARG SHAREDIR="/usr/share/postgresql/17"

COPY --from=build /periods/periods--1.0.sql $SHAREDIR/extension/periods--1.0.sql
COPY --from=build /periods/periods--1.0--1.1.sql $SHAREDIR/extension/periods--1.0--1.1.sql
COPY --from=build /periods/periods--1.1.sql $SHAREDIR/extension/periods--1.1.sql
COPY --from=build /periods/periods--1.0--1.1.sql $SHAREDIR/extension/periods--1.0--1.2.sql
COPY --from=build /periods/periods--1.2.sql $SHAREDIR/extension/periods--1.2.sql
COPY --from=build /periods/periods.control $SHAREDIR/extension/periods.control

# EXT: aws_s3
COPY --from=build /aws_s3/aws_s3--0.0.1.sql ./aws_s3--0.0.1.sql
COPY --from=build /aws_s3/aws_s3.control ./aws_s3.control

USER postgres