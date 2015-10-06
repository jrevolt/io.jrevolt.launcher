#!/bin/bash

set -eu
error() { echo "ERROR: $*"; exit 1; }
trap 'error ${LINENO}' ERR

realpath() {
	echo "$(cd $(dirname $1) && pwd -L)/$(basename $1)"
}

java=$(which java)
jvmoptions="${JVM_OPTIONS:-}"
file=$(realpath ~/.jrevolt/io.jrevolt.launcher.jar)
launcheropts="${JREVOLT_LAUNCHER_OTPIONS:-}"
cat << EOF
java:         $java
file:         $file
jvmoptions:   $jvmoptions
launcheropts: $launcheropts
EOF

$java $jvmoptions -jar $file $launcheropts $@
