#!/bin/sh

cd "$(dirname $0)"
rm -f *.jks *.crt

# Create a self signed key pair root CA certificate.
keytool -genkeypair -v \
  -alias localhost \
  -dname "CN=localhost, OU=monolithic, O=mtday, C=US" \
  -keystore keystore.jks \
  -keypass changeit \
  -storepass changeit \
  -keyalg RSA \
  -keysize 4096 \
  -ext KeyUsage="keyCertSign" \
  -ext BasicConstraints:"critical=ca:true" \
  -validity 9999

# Export the public certificate so that it can be used in trust stores.
keytool -export -v \
  -alias localhost \
  -file localhost.crt \
  -keypass changeit \
  -storepass changeit \
  -keystore keystore.jks \
  -rfc

# Import the public certificate into a trust store.
keytool -importcert \
  -file localhost.crt \
  -keystore truststore.jks \
  -storepass changeit \
  -alias localhost \
  -noprompt

# For unit tests.
keytool -genkey -alias a -keyalg RSA -keystore multiple.jks -storepass changeit -dname "CN=a" -keypass changeit
keytool -genkey -alias b -keyalg RSA -keystore multiple.jks -storepass changeit -dname "CN=b" -keypass changeit

