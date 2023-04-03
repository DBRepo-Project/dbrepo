#!/bin/bash
# ----------------
# https://blogs.oracle.com/blogbypuneeth/post/create-an-internal-certification-authority-ca-using-keytool-and-sign-your-server-certificate
# ----------------
STORE_PASS=password
KEY_PASS=password

declare -A services
services[9091]=container
services[9092]=database
services[9093]=query
services[9094]=table
services[9095]=gateway
services[9096]=identifier
services[9097]=authentication
services[9098]=user
services[9099]=metadata

function generate () {
  echo "... generate $1-service certificate"
  keytool -genkeypair -storepass ${STORE_PASS} -keypass ${KEY_PASS} -storetype PKCS12 -keyalg RSA -keysize 2048 \
    -dname "CN=$1-service, OU=DS-IFS, O=TU Wien, C=AT" -alias "$1-service" -ext "SAN:c=DNS:localhost,IP:127.0.0.1" \
    -keystore ./server.keystore
}

function sign () {
  echo "... sign $1-service certificate"
  keytool -alias "$1-service" -certreq -storepass ${STORE_PASS} -keyalg RSA \
    -keystore ./server.keystore | keytool -alias intermediate -gencert -storepass ${STORE_PASS} \
    -keyalg RSA | keytool -alias "$1-service" -importcert -storepass ${STORE_PASS} -keyalg RSA \
    -keystore ./server.keystore -noprompt -trustcacerts
}

function crt () {
  echo "... export $1 certificate"
  keytool -exportcert -alias "$1" -rfc -storepass ${STORE_PASS} -keystore "$2" > "./$1.crt"
}

function move () {
  echo "... move jks to the $1-service"
  cp ./server.keystore "../fda-$1-service/server.keystore"
  rm -f "../fda-$1-service/intermediate.crt" && cp ./intermediate.crt "../fda-$1-service/intermediate.crt"
}

echo "Remove old JKS(s)"
rm -f ./server.keystore ./auth.keystore ./chain.jks ./*.crt

echo "Generate root certificate"
keytool -alias root -dname "CN=RootCA, OU=DS-IFS, O=TU Wien, C=AT" -genkeypair -ext KeyUsage="keyCertSign" \
  -ext BasicConstraints:"critical=ca:true" -validity 3600 -storepass ${STORE_PASS} -keyalg RSA

echo "Generate intermediate certificate"
keytool -alias intermediate -dname "CN=IntermediateCA, OU=DS-IFS, O=TU Wien, C=AT" -genkeypair \
  -ext KeyUsage="keyCertSign" -ext BasicConstraints:"critical=ca:true" -validity 1800 -storepass ${STORE_PASS} \
  -keyalg RSA

echo "Sign the intermediate certificate"
keytool -alias intermediate -certreq -storepass ${STORE_PASS} -keyalg RSA | keytool -alias root -gencert \
  -ext KeyUsage="keyCertSign" -ext BasicConstraints:"critical=ca:true" -storepass ${STORE_PASS} \
  -keyalg RSA | keytool -alias intermediate -importcert -storepass ${STORE_PASS} -keyalg RSA

echo "Import the root certificate to the JKS"
keytool -export -alias root -storepass ${STORE_PASS} | keytool -import -alias root -keystore ./server.keystore \
  -storepass ${STORE_PASS} -noprompt -trustcacerts

echo "Import the intermediate certificate to the JKS"
keytool -export -alias intermediate -storepass ${STORE_PASS} | keytool -import -alias intermediate -keystore ./server.keystore \
  -storepass ${STORE_PASS} -noprompt -trustcacerts

echo "Generating the certificate key pairs"
for key in "${!services[@]}"; do
  generate "${services[$key]}"
done

echo "Sign the certificates with intermediate certificate"
for key in "${!services[@]}"; do
  sign "${services[$key]}"
done

echo "Export the trusted keystore"
keytool -export -alias intermediate -storepass ${STORE_PASS} | keytool -import -alias intermediate \
  -keystore ./chain.jks -storepass ${STORE_PASS} -trustcacerts -noprompt
keytool -export -alias root -storepass ${STORE_PASS} | keytool -import -alias root -keystore ./chain.jks \
  -storepass ${STORE_PASS} -trustcacerts -noprompt

echo "Export CRTs"
crt root ./chain.jks
crt intermediate ./chain.jks

echo "Copy the JKS(s)"
for key in "${!services[@]}"; do
  move "${services[$key]}"
done

echo "Create the authentication service JKS"
echo "... import private key into the key store"
keytool -importkeystore -srckeystore ./server.keystore -srcstorepass ${STORE_PASS} -srcalias "authentication-service" \
  -destkeystore ./auth.keystore -deststorepass ${STORE_PASS} -deststoretype PKCS12 -destalias "server" -trustcacerts \
  -noprompt
rm -f ../fda-authentication-service/auth.keystore && cp ./auth.keystore ../fda-authentication-service/auth.keystore
