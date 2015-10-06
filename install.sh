#!/bin/bash

set -eu
error() { echo "ERROR: $*"; exit 1; }
trap 'error ${LINENO}' ERR

basedir="$HOME/.jrevolt"

git init $basedir && cd $basedir
git remote add -t dist origin https://github.com/jrevolt/io.jrevolt.launcher.git
git fetch
git checkout dist

