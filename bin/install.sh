#!/bin/bash

set -eu

case $(whoami) in
	root)	dst="/etc/jrevolt" ;;
	*)	dst="$HOME/.jrevolt" ;;
esac

echo "Creating $dst directory..."
mkdir -p $dst

repo="https://raw.githubusercontent.com/jrevolt/io.jrevolt.launcher"
branch="develop"

echo "Downloading updater..."
wget -q -O $dst/update.sh $repo/$branch/bin/update.sh

echo "Updating JRevolt Launcher..."
chmod a+x $dst/update.sh
$dst/update.sh

echo "Downloading JRevolt Launcher script..."
name="jrevolt.sh"
wget -q -O $dst/$name $repo/$branch/bin/$name
chmod a+x $dst/$name

shortcut="jrevolt"
if [ -d ~/bin ]; then
	[ -L ~/bin/$shortcut ] || ln -s ~/.jrevolt/$shortcut ~/bin/$shortcut
	ls -l ~/bin/$shortcut
fi
