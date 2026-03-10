FROM --platform=$BUILDPLATFORM docker.io/chrislusf/seaweedfs:4.13 AS runtime
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"

WORKDIR /app

COPY ./init.sh ./init.sh

RUN chmod +x ./init.sh

ENTRYPOINT [ "sh", "./init.sh" ]
