#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
SERIAL=${1:?Uso: sh scripts/install.sh SERIAL}
ADB=${ADB:-adb}
[ -f out/tvhome.apk ] || { echo "Compila prima con sh build.sh"; exit 1; }
[ "$("$ADB" -s "$SERIAL" get-state </dev/null)" = device ] || exit 1
printf 'Installare soltanto Sketch TV su %s? Scrivi SI: ' "$SERIAL"
read -r answer
[ "$answer" = SI ] || { echo "Annullato"; exit 0; }
"$ADB" -s "$SERIAL" install -r out/tvhome.apk </dev/null
echo "APK installato. Nessuna app disabilitata, Home e Accessibilità non modificate."
