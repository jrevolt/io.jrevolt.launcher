#!/bin/bash

set -eu
error() { echo "ERROR: $*"; exit 1; }
trap 'error ${LINENO}' ERR

launcher="${JREVOLT_LAUNCHER_JAR:-$HOME/.jrevolt/io.jrevolt.launcher.jar}"

realpath() {
	p="$(cd $(dirname $1) && pwd -L)/$(basename $1)"
	which cygpath >/dev/null 2>&1 && cygpath -w $p || echo $p
}

# NOTE:
# 1. Using arrays to properly handle white space
# 2. "${jvmopts[@]:+${jvmopts[@]}}" syntax ix needed to properly handle empty arrays with set -u
# 3. not all JVM options are supported

jvmopts=()
launcheropts=()

while true; do
	case "${1:-}" in
		-D*|-X*|-verbose*|-ea*|-da*|-agent*|-javaagent*)
			jvmopts+=("$1"); shift ;;
		--*)
			launcheropts+=("$1"); shift ;;
		*)
			break ;;
	esac
done

java=$(which java)
file=$(realpath $launcher)

$java \
	"${jvmopts[@]:+${jvmopts[@]}}" \
	-jar $file \
	"${launcheropts[@]:+${launcheropts[@]}}" \
	"$@" \
	|| exit $?

