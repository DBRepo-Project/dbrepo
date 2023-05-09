#!/bin/bash
# ----------------
# https://blogs.oracle.com/blogbypuneeth/post/create-an-internal-certification-authority-ca-using-keytool-and-sign-your-server-certificate
# ----------------
STORE_PASS=password
KEY_PASS=password

declare -A services
services[443]=gateway
services[8443]=authentication
services[9091]=container
services[9092]=database
services[9093]=query
services[9094]=table
services[9096]=identifier
services[9098]=user
services[9099]=metadata

function generate () {
  if [ -z "$2" ]; then
    CN="$1"
  else
    CN="$1-$2"
  fi
  echo "... generate $CN certificate"
  keytool -genkeypair -storepass ${STORE_PASS} -keypass ${KEY_PASS} -storetype PKCS12 -keyalg RSA -keysize 2048 \
    -dname "CN=$CN, OU=DS-IFS, O=TU Wien, C=AT" -alias "$CN" -ext "SAN:c=DNS:localhost,IP:127.0.0.1" \
    -keystore ./server.keystore
}

function sign () {
  if [ -z "$2" ]; then
    CN="$1"
  else
    CN="$1-$2"
  fi
  echo "... sign $CN certificate"
  keytool -alias "$CN" -certreq -storepass ${STORE_PASS} -keyalg RSA \
    -keystore ./server.keystore | keytool -alias intermediate -gencert -storepass ${STORE_PASS} \
    -keyalg RSA | keytool -alias "$CN" -importcert -storepass ${STORE_PASS} -keyalg RSA \
    -keystore ./server.keystore -noprompt -trustcacerts
}

function crt () {
  echo "... export $1 certificate"
  keytool -exportcert -alias "$1" -rfc -storepass ${STORE_PASS} -keystore "$2" > "$3"
}

function key () {
  echo "... export $1 key"
  rm -f ./tmp.12 && keytool -importkeystore -srckeystore "$2" -destkeystore ./tmp.p12 -deststoretype PKCS12 \
    -srcalias "$1" -srcstorepass ${STORE_PASS} -deststorepass ${STORE_PASS} -destkeypass ${STORE_PASS}
  openssl pkcs12 -in ./tmp.p12 -nodes -nocerts -out server.key -password pass:${STORE_PASS}
}

function move () {
  if [ -z "$2" ]; then
    CN="$1"
  else
    CN="$1-$2"
  fi
  echo "... move jks to the $CN"
  cp ./server.keystore "../dbrepo-$CN/server.keystore"
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
  generate "${services[$key]}" "service"
done

echo "Sign the certificates with intermediate certificate"
for key in "${!services[@]}"; do
  sign "${services[$key]}" "service"
done

echo "Export the trusted keystore"
keytool -export -alias intermediate -storepass ${STORE_PASS} | keytool -import -alias intermediate \
  -keystore ./chain.jks -storepass ${STORE_PASS} -trustcacerts -noprompt
keytool -export -alias root -storepass ${STORE_PASS} | keytool -import -alias root -keystore ./chain.jks \
  -storepass ${STORE_PASS} -trustcacerts -noprompt

echo "Export CRTs"
crt root ./chain.jks ./root.crt
crt intermediate ./chain.jks ./intermediate.crt
crt gateway-service ./server.keystore ./gateway-service.crt
cp ./gateway-service.crt ../dbrepo-gateway-service/server.crt
cat ./root.crt ./intermediate.crt ./gateway-service.crt > ../dbrepo-gateway-service/fullchain.crt

echo "Export private key"
key gateway-service ./server.keystore
cp ./server.key ../dbrepo-gateway-service/server.key

echo "Copy the JKS(s)"
for key in "${!services[@]}"; do
  move "${services[$key]}" "service"
done

echo "Create the authentication service JKS"
echo "... import private key into the key store"
keytool -importkeystore -srckeystore ./server.keystore -srcstorepass ${STORE_PASS} -srcalias "authentication-service" \
  -destkeystore ./auth.keystore -deststorepass ${STORE_PASS} -deststoretype PKCS12 -destalias "server" -trustcacerts \
  -noprompt
rm -f ../dbrepo-authentication-service/auth.keystore && cp ./auth.keystore ../dbrepo-authentication-service/auth.keystore
