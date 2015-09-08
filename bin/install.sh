#!/bin/bash

set -eu

error() {
	echo "ERROR: $*"
	exit
}

trap 'error ${LINENO}' ERR

case $(whoami) in
	root)	dst="/etc/jrevolt" ;;
	*)	    dst="$HOME/.jrevolt" ;;
esac

echo "Creating $dst directory..."
mkdir -p $dst

repo="https://raw.githubusercontent.com/jrevolt/io.jrevolt.launcher"
branch="develop"

cd $dst

echo "Downloading JRevolt Updater..."
wget -q -N $repo/$branch/bin/update.sh
chmod a+x update.sh

echo "Updating JRevolt Launcher..."
./update.sh

