#!/bin/bash
HOSTNAME="dbrepo.local"

openssl genrsa -out ./tls/ca.key 2048
openssl req -new -x509 -days 365 -key ./tls/ca.key -subj "/C=AT/O=Acme, Inc./CN=Acme Root CA" -out ./tls/ca.crt
openssl req -newkey rsa:2048 -nodes -keyout ./tls/tls.key -subj "/C=AT/O=DBRepo/CN=${HOSTNAME}" -out ./tls/tls.csr
openssl x509 -req -extfile <(printf "subjectAltName=DNS:${HOSTNAME},DNS:www.${HOSTNAME}") -days 365 -in ./tls/tls.csr \
  -CA ./tls/ca.crt -CAkey ./tls/ca.key -CAcreateserial -out ./tls/tls.crt