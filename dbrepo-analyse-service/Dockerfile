FROM python:3.9-slim
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

RUN apt update && apt install -y curl

WORKDIR /app

COPY ./requirements.txt ./requirements.txt
RUN pip install -r requirements.txt

ENV FLASK_APP=app.py
ENV FLASK_RUN_HOST=0.0.0.0
ENV PORT_APP=5000
ENV FLASK_ENV=production
ENV HOSTNAME=analyse-service
ENV UPLOAD_ENDPOINT=http://upload-service:1080/api/upload/files

COPY ./as-yml/ ./as-yml/
COPY ./*.py ./

RUN mkdir -p /data

EXPOSE $PORT_APP

ENTRYPOINT [ "python", "./pywsgi.py" ]

CMD sh -c /wait && flask run