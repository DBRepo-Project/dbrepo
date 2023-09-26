#!/bin/bash

# Generate the private key of the root CA
openssl genrsa -out ./pem/root-ca-key.pem 4096
# Generate the self-signed root CA certificate
openssl req -x509 -sha256 -new -nodes -key ./pem/root-ca-key.pem -days 3650 -out ./pem/root-ca.pem -subj "/C=AT/O=Technische Universität Wien/CN=test.dbrepo.tuwien.ac.at"

# Create the certificate key
openssl genrsa -out ./pem/admin-key.pem 4096
# Create the signing (csr)
openssl req -new -sha256 -key ./pem/admin-key.pem -subj "/C=AT/O=Technische Universität Wien/CN=test.dbrepo.tuwien.ac.at" -out ./pem/admin.csr
# Generate the certificate using the csr and key along with the CA Root key
openssl x509 -req -in ./pem/admin.csr -CA ./pem/root-ca.pem -CAkey ./pem/root-ca-key.pem -CAcreateserial -out ./pem/admin.pem -days 365 -sha256

# Create the certificate key
openssl genrsa -out ./pem/node1-key.pem 4096
# Create the signing (csr)
openssl req -new -sha256 -key ./pem/node1-key.pem -subj "/C=AT/O=Technische Universität Wien/CN=test.dbrepo.tuwien.ac.at" -out ./pem/node1.csr
# Generate the certificate using the csr and key along with the CA Root key
openssl x509 -req -in ./pem/node1.csr -CA ./pem/root-ca.pem -CAkey ./pem/root-ca-key.pem -CAcreateserial -out ./pem/node1.pem -days 365 -sha256