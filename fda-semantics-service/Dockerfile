FROM python:3.9-slim
MAINTAINER Cornelia Michlits <cornelia.michlits@tuwien.ac.at>

RUN apt-get update && apt-get install -y python3-dev libmariadb3 libmariadb-dev gcc

ENV FLASK_APP=app.py
ENV FLASK_RUN_HOST=0.0.0.0
ENV PORT_APP=5010
ENV FLASK_ENV=production
ENV HOSTNAME=semantics-service
ENV EUREKA_SERVER=http://discovery-service:9090/eureka/
ENV READY_FILE=/ready
ENV LOG_LEVEL=debug
ENV METADATA_DB=fda
ENV METADATA_USERNAME=root
ENV METADATA_PASSWORD=dbrepo

WORKDIR /app

COPY requirements.txt requirements.txt

RUN pip install -r requirements.txt > /dev/null

COPY ./service_ready /usr/bin
RUN chmod +x /usr/bin/service_ready

HEALTHCHECK --interval=10s --timeout=5s --retries=12 CMD service_ready

EXPOSE $PORT_APP

COPY ./us-yml/*.yml ./
COPY ./*.py ./

ENTRYPOINT [ "python", "-u", "./pywsgi.py" ]
