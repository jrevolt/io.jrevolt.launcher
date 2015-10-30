#!/bin/bash

set -eu

error() {
	echo "ERROR: $*"
	exit
}

realpath() {
	echo "$(cd $(dirname $1) && pwd -L)/$(basename $1)"
}

trap 'error ${LINENO}' ERR

repo="${1:-http://repo.jrevolt.io}"
version="${2:-develop-SNAPSHOT}"

grepo="jrevolt/io.jrevolt.launcher"
gbranch="develop"

groupid="io.jrevolt.launcher"
artifactid="io.jrevolt.launcher"

urljar="${repo}/service/local/artifact/maven/redirect?r=snapshots&g=${groupid}&a=${artifactid}&v=${version}&e=jar"
urlsh="https://raw.githubusercontent.com/${grepo}/${gbranch}/bin/jrevolt.sh"
fjar="io.jrevolt.launcher.jar"

echo "Updating JRevolt Launcher..."
cd $(dirname $0)

url=$(curl -sk --head "$urljar" | grep "Location:" | sed "s/Location: //" | tr -d '\r')

wget -qN $url

[ -L $fjar ] && rm $fjar
ln -s $(ls -1 io.jrevolt.launcher-*.jar | tail -n1) $fjar
chmod 640 $fjar

wget -qN $urlsh
chmod a+x $(basename $urlsh)

ls -l $fjar $(basename $urlsh)
