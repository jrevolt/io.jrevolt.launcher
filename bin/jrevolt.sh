#!/bin/bash

set -eu

java="${JAVA_HOME:-/usr/java/default}/bin/java"

file=~/.jrevolt/jrevolt.jar
file=$(which cygpath > /dev/null 2>&1 && cygpath -w $file || realpath $file)
java ${JVM_OPTIONS:-} -jar $file ${SPRINGBOOT_LAUNCHER_OPTIONS:-} $*
