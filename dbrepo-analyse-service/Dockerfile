FROM python:3.9-slim
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

WORKDIR /app

COPY ./requirements.txt ./requirements.txt
RUN pip install -r requirements.txt > /dev/null

COPY ./healthcheck.sh ./healthcheck.sh

ENV FLASK_APP=app.py
ENV FLASK_RUN_HOST=0.0.0.0
ENV PORT_APP=5000
ENV FLASK_ENV=production
ENV HOSTNAME=analyse-service

HEALTHCHECK --interval=10s --timeout=5s --retries=12 CMD ["bash", "/app/healthcheck.sh"]

COPY ./as-yml/ ./as-yml/
COPY ./*.py ./

EXPOSE $PORT_APP

ENTRYPOINT [ "python", "./pywsgi.py" ]

CMD sh -c /wait && flask run
