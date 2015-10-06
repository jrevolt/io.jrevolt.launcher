#!/bin/bash

set -eu
error() { echo "ERROR: $*"; exit 1; }
trap 'error ${LINENO}' ERR

basedir="$HOME/.jrevolt"

mkdir -p $basedir

if [[ -d .git ]]; then
  git init .
  git remote add -t dist origin https://github.com/jrevolt/io.jrevolt.launcher.git
  git fetch
  git checkout dist
else
  git pull
fi

