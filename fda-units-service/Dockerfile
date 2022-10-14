FROM python:3.8-slim
MAINTAINER Cornelia Michlits <cornelia.michlits@tuwien.ac.at>

ENV FLASK_APP=app.py
ENV FLASK_RUN_HOST=0.0.0.0
ENV PORT_APP=5010
ENV FLASK_ENV=production
ENV HOSTNAME=units-service
ENV EUREKA_SERVER=http://discovery-service:9090/eureka/
ENV READY_FILE=/ready

WORKDIR /app

COPY requirements.txt requirements.txt

RUN pip install -r requirements.txt > /dev/null

COPY ./service_ready /usr/bin
RUN chmod +x /usr/bin/service_ready

HEALTHCHECK --interval=10s --timeout=5s --retries=12 CMD service_ready

EXPOSE $PORT_APP

COPY ./us-yml/*.yml ./
COPY ./onto/*.ttl ./onto/
COPY ./*.py ./

ENTRYPOINT [ "python", "./pywsgi.py" ]
CMD ["flask", "run"]
