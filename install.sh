#!/bin/bash
set -u
mkdir -p $HOME/.jrevolt
cd $HOME/.jrevolt
VERSION="config-SNAPSHOT"
URL="https://oss.sonatype.org/service/local/artifact/maven/content"
QUERY="r=snapshots&g=io.jrevolt.launcher&a=io.jrevolt.launcher&v=${VERSION}&e=jar"
curl -v "${URL}?${QUERY}" > $HOME/.jrevolt/launcher.jar
