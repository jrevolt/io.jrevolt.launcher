#!/bin/bash

set -eu

case $(whoami) in
	root)	dst="/etc/springboot" ;;
	*)	dst="$HOME/.springboot" ;;
esac

echo "Creating $dst directory..."
mkdir -p $dst

repo="https://raw.githubusercontent.com/jrevolt/io.jrevolt.launcher"
branch="develop"

echo "Downloading updater..."
wget -q -O $dst/update.sh $repo/$branch/bin/update.sh

echo "Updating launcher..."
chmod a+x $dst/update.sh
$dst/update.sh

echo "Downloading launcher script..."
wget -q -O $dst/springboot.sh $repo/$branch/bin/springboot.sh
chmod a+x $dst/springboot.sh

if [ -d ~/bin ]; then
	[ -L ~/bin/springboot ] || ln -s ~/.springboot/springboot.sh ~/bin/springboot
	ls -l ~/bin/springboot
fi
