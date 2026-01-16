#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
CERTS_DIR="$PROJECT_DIR/certs"

echo "========================================"
echo "Kafka SSL Certificate Generator"
echo "========================================"
echo ""

VALIDITY_DAYS=365
KEY_SIZE=2048
KEYSTORE_PASSWORD="yourSecretKey"
TRUSTSTORE_PASSWORD="yourSecretKey"
KEY_PASSWORD="yourSecretKey"

mkdir -p "$CERTS_DIR"
cd "$CERTS_DIR"

echo "$KEYSTORE_PASSWORD" > keystore_creds
echo "$TRUSTSTORE_PASSWORD" > truststore_creds
echo "$KEY_PASSWORD" > key_creds

echo "1. Generating CA (Certificate Authority)..."
openssl req -new -x509 \
    -keyout ca-key \
    -out ca-cert \
    -days $VALIDITY_DAYS \
    -passin pass:$KEY_PASSWORD \
    -passout pass:$KEY_PASSWORD \
    -subj "/C=US/ST=CA/L=SanFrancisco/O=KafkaSSL/OU=IT/CN=kafka-ca"

echo ""
echo "2. Creating Kafka server keystore..."
keytool -genkey \
    -keystore kafka.server.keystore.jks \
    -validity $VALIDITY_DAYS \
    -storepass $KEYSTORE_PASSWORD \
    -keypass $KEY_PASSWORD \
    -dname "CN=localhost,OU=IT,O=KafkaSSL,L=SanFrancisco,ST=CA,C=US" \
    -ext "SAN=DNS:kafka,DNS:localhost,IP:127.0.0.1" \
    -keyalg RSA \
    -keysize $KEY_SIZE \
    -storetype JKS

echo ""
echo "3. Creating certificate signing request..."
keytool -keystore kafka.server.keystore.jks \
    -certreq \
    -file cert-file \
    -storepass $KEYSTORE_PASSWORD \
    -keypass $KEY_PASSWORD \
    -ext "SAN=DNS:kafka,DNS:localhost,IP:127.0.0.1"

echo ""
echo "4. Signing certificate with CA..."
openssl x509 -req \
    -CA ca-cert \
    -CAkey ca-key \
    -in cert-file \
    -out cert-signed \
    -days $VALIDITY_DAYS \
    -CAcreateserial \
    -passin pass:$KEY_PASSWORD \
    -extfile <(printf "subjectAltName=DNS:kafka,DNS:localhost,IP:127.0.0.1")

echo ""
echo "5. Creating server truststore and importing CA..."
keytool -keystore kafka.server.truststore.jks \
    -alias CARoot \
    -import \
    -file ca-cert \
    -storepass $TRUSTSTORE_PASSWORD \
    -noprompt

echo ""
echo "6. Importing CA into server keystore..."
keytool -keystore kafka.server.keystore.jks \
    -alias CARoot \
    -import \
    -file ca-cert \
    -storepass $KEYSTORE_PASSWORD \
    -keypass $KEY_PASSWORD \
    -noprompt

echo ""
echo "7. Importing signed certificate into server keystore..."
keytool -keystore kafka.server.keystore.jks \
    -import \
    -file cert-signed \
    -storepass $KEYSTORE_PASSWORD \
    -keypass $KEY_PASSWORD \
    -noprompt

echo ""
echo "8. Creating client truststore..."
cp kafka.server.truststore.jks kafka.client.truststore.jks

echo ""
echo "========================================"
echo "Certificate Generation Complete"
echo "========================================"
echo ""
echo "Generated files in $CERTS_DIR:"
echo "  - kafka.server.keystore.jks"
echo "  - kafka.server.truststore.jks"
echo "  - kafka.client.truststore.jks"
echo "  - ca-cert"
echo ""
echo "Credentials:"
echo "  - Keystore Password:   yourSecretKey"
echo "  - Truststore Password: yourSecretKey"
echo "  - Key Password:        yourSecretKey"
echo ""
echo "Next: Run ./scripts/02-start.sh"
echo ""