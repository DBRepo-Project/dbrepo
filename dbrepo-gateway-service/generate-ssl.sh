#!/bin/bash
openssl req -x509 -sha256 -days 365 -nodes -newkey rsa:2048 -subj "/CN=localhost/C=AT/O=Technische Universität Wien/OU=Research Unit Data Science" \
  -keyout insecure.key -out insecure.crt
