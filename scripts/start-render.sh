#!/bin/sh
set -eu

# Managed MySQL providers can supply a private CA as a Render Secret File.
# Keep the existing JVM public CAs, and add the database CA to a writable copy.
if [ -n "${MYSQL_CA_CERT_PATH:-}" ]; then
  if [ ! -r "$MYSQL_CA_CERT_PATH" ]; then
    echo 'MYSQL_CA_CERT_PATH is not readable. Check the Render Secret File path.' >&2
    exit 1
  fi
  trust_directory=$(mktemp -d /tmp/fishfarm-trust.XXXXXX)
  cp "$JAVA_HOME/lib/security/cacerts" "$trust_directory/cacerts"
  chmod 600 "$trust_directory/cacerts"
  keytool -importcert -noprompt -alias fishfarm-mysql-provider-ca \
    -file "$MYSQL_CA_CERT_PATH" -keystore "$trust_directory/cacerts" \
    -storepass changeit >/dev/null
  # This password protects a store of public certificates, not a private key.
  set -- "-Djavax.net.ssl.trustStore=$trust_directory/cacerts" \
    '-Djavax.net.ssl.trustStorePassword=changeit' "$@"
fi
exec java -Duser.timezone=UTC -XX:MaxRAMPercentage=70.0 "$@" -jar /app/app.jar
