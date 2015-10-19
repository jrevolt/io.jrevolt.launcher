#!/bin/bash

set -eu
error() { echo "ERROR: $*"; exit 1; }
trap 'error ${LINENO}' ERR

realpath() {
	p="$(cd $(dirname $1) && pwd -L)/$(basename $1)"
	which cygpath >/dev/null && cygpath -w $p || echo $p
}

java=$(which java)
jvmoptions="${JVM_OPTIONS:-}"
file=$(realpath ~/.jrevolt/io.jrevolt.launcher.jar)
launcheropts="${JREVOLT_LAUNCHER_OTPIONS:-}"

$java $jvmoptions -jar $file $launcheropts "$@" || exit $?
