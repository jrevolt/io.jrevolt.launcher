#!/bin/bash
set -eu

realpath() {
	echo "$(cd $(dirname $1) && pwd -L)/$(basename $1)"
}

file=~/.jrevolt/io.jrevolt.launcher.jar
file=$(which cygpath > /dev/null 2>&1 && cygpath -w $file || $(realpath $file))
java ${JVM_OPTIONS:-} -jar $file ${SPRINGBOOT_LAUNCHER_OPTIONS:-} $*
