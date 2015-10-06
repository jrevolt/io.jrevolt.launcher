#!/bin/bash

set -eu
error() { echo "ERROR: $*"; exit 1; }
trap 'error ${LINENO}' ERR

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

