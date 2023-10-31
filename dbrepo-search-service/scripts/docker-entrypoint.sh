#!/bin/bash

if [[ "$FLASK_DEBUG" == true ]]; then
  exec flask run --host 0.0.0.0 --port=4000
else
  exec gunicorn -w 4 -b :4000 wsgi:app
fi
