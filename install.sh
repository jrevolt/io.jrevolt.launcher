#!/bin/bash

set -eu
error() { echo "ERROR: $*"; exit 1; }
trap 'error ${LINENO}' ERR

version="${1:-master}"

basedir="$HOME/.jrevolt"

mkdir -p $basedir && cd $basedir

if [[ -d .git ]]; then
  git pull
else
  git init .
  git checkout -b dist
  git remote add -t dist origin https://github.com/jrevolt/io.jrevolt.launcher.git
  git pull 
fi
echo $version > .version
./update.sh
