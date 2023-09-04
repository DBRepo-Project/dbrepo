FROM gcr.io/kaniko-project/executor:v1.15.0-debug

ENTRYPOINT [""]

COPY ./dbrepo-metadata-service ./dbrepo-metadata-service

RUN /kaniko/executor --context ./dbrepo-metadata-service --dockerfile "./dbrepo-metadata-service/Dockerfile" --no-push