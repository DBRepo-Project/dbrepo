#!/bin/bash
echo "init pubkey ..."
rm -f /app/pubkey.pem /app/cert.pem
cat << EOF > /app/pubkey.pem
-----BEGIN RSA PUBLIC KEY-----
${JWT_PUBKEY}
-----END RSA PUBLIC KEY-----
EOF
echo "init cert ..."
cat << EOF > /app/cert.pem
-----BEGIN CERTIFICATE-----
${JWT_CERT}
-----END CERTIFICATE-----
EOF