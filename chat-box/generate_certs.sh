#!/bin/bash

# This script can be run from anywhere.
# It will determine its own location and place certs in the correct relative path.
SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

# A script to generate certificates for the gRPC chat-box example.

CERT_DIR="$SCRIPT_DIR/src/main/resources/certs"
SERVER_CN="localhost"
CLIENT_CN="my-client"
SERVER_PASS="server_password"
CLIENT_PASS="client_password"

# --- Cleanup and Setup ---
echo "Cleaning up old certificates..."
rm -rf "$CERT_DIR"
mkdir -p "$CERT_DIR"
echo "Certificate directory created at $CERT_DIR"

# --- CA Generation ---
echo "Generating Certificate Authority (CA)..."
openssl genpkey -algorithm RSA -out "$CERT_DIR/ca.key"
openssl req -new -x509 -key "$CERT_DIR/ca.key" -out "$CERT_DIR/ca.crt" -days 365 -subj "/CN=My Test CA"
echo "CA generated."

# --- Server Certificate ---
echo "Generating server certificate for CN=$SERVER_CN..."
# Create a config file for server SAN to fix "No name matching localhost found"
cat > "$CERT_DIR/server_ext.cnf" <<EOF
[req]
distinguished_name = dn
[dn]
[ext]
subjectAltName = @alt_names
[alt_names]
DNS.1 = ${SERVER_CN}
EOF

openssl genpkey -algorithm RSA -out "$CERT_DIR/server.key"
openssl req -new -key "$CERT_DIR/server.key" -out "$CERT_DIR/server.csr" -subj "/CN=${SERVER_CN}" -config "$CERT_DIR/server_ext.cnf" -reqexts ext
openssl x509 -req -in "$CERT_DIR/server.csr" -CA "$CERT_DIR/ca.crt" -CAkey "$CERT_DIR/ca.key" -CAcreateserial -out "$CERT_DIR/server.crt" -days 365 -extfile "$CERT_DIR/server_ext.cnf" -extensions ext
echo "Server certificate generated."

# --- Client Certificate ---
echo "Generating client certificate for CN=$CLIENT_CN..."
openssl genpkey -algorithm RSA -out "$CERT_DIR/client.key"
openssl req -new -key "$CERT_DIR/client.key" -out "$CERT_DIR/client.csr" -subj "/CN=${CLIENT_CN}"
openssl x509 -req -in "$CERT_DIR/client.csr" -CA "$CERT_DIR/ca.crt" -CAkey "$CERT_DIR/ca.key" -out "$CERT_DIR/client.crt" -days 365
echo "Client certificate generated."

# --- PKCS12 Bundles ---
echo "Creating PKCS12 bundles..."
# Server
openssl pkcs12 -export -in "$CERT_DIR/server.crt" -inkey "$CERT_DIR/server.key" \
    -name server -out "$CERT_DIR/server.p12" -passout "pass:${SERVER_PASS}"
# Client
openssl pkcs12 -export -in "$CERT_DIR/client.crt" -inkey "$CERT_DIR/client.key" \
    -name client -out "$CERT_DIR/client.p12" -passout "pass:${CLIENT_PASS}"
echo "PKCS12 bundles created."

# --- Cleanup ---
echo "Cleaning up intermediate files..."
rm "$CERT_DIR/server_ext.cnf"
rm "$CERT_DIR"/*.csr
rm "$CERT_DIR"/*.srl
echo "Certificate generation complete! You can find the .p12 files in $CERT_DIR" 