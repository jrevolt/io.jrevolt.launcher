#!/bin/bash

set -eu
error() { echo "ERROR: $*"; exit 1; }
trap 'error ${LINENO}' ERR

repo="https://raw.githubusercontent.com/jrevolt/io.jrevolt.launcher"
branch="${1:-master}"

dst="${2:-$HOME/.jrevolt}"
echo "Initializing $dst directory..."
mkdir -p $dst

cd $dst
url="$repo/$branch/bin/update.sh"
echo "Downloading JRevolt Updater: $url"
wget -q -N $url
chmod a+x update.sh

./update.sh

