#!/bin/bash
# Bellek sınırıyla derleme: sınır aşılırsa oyun sunucuları değil bu derleme ölür.
cd "$(dirname "$0")/mod" || exit 1
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
exec systemd-run --user --scope --quiet -p MemoryMax=${HAPI_BELLEK:-2600M} -p MemorySwapMax=0 \
  nice -n 15 ./gradlew --no-daemon --console=plain "${@:-build}"
