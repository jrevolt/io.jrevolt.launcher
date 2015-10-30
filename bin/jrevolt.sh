#!/bin/bash
set -eu

realpath() {
	echo "$(cd $(dirname $1) && pwd -L)/$(basename $1)"
}

java=$(which java)
jvmoptions="${JVM_OPTIONS:-}"
file=$(realpath ~/.jrevolt/io.jrevolt.launcher.jar)
launcheropts="${JREVOLT_LAUNCHER_OTPIONS:-}"

$java $jvmoptions -jar $file $launcheropts $@
