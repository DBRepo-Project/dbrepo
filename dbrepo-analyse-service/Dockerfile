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
ENV SHARED_FILESYSTEM=/tmp

COPY ./as-yml/ ./as-yml/
COPY ./*.py ./

EXPOSE $PORT_APP

ENTRYPOINT [ "python", "./pywsgi.py" ]

CMD sh -c /wait && flask run