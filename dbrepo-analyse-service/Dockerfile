FROM python:3.9-slim
MAINTAINER Cornelia Michlits <cornelia.michlits@tuwien.ac.at>

ENV FLASK_APP=app.py
ENV FLASK_RUN_HOST=0.0.0.0
ENV PORT_APP=5000
ENV FLASK_ENV=production
ENV HOSTNAME=analyse-service
ENV EUREKA_SERVER=http://discovery-service:9090/eureka/

WORKDIR /app

COPY requirements.txt requirements.txt

RUN pip install -r requirements.txt > /dev/null

HEALTHCHECK --interval=10s --timeout=5s --retries=12 CMD ./service_ready

COPY ./as-yml/ ./as-yml/
COPY ./*.py ./
COPY ./service_ready ./

RUN chmod +x ./service_ready

EXPOSE $PORT_APP

ENTRYPOINT [ "python", "./pywsgi.py" ]

CMD sh -c /wait && flask run