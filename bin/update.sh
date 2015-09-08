#!/bin/bash

set -eu

error() {
	echo "ERROR: $*"
	exit
}

trap 'error ${LINENO}' ERR

grepo="jrevolt/io.jrevolt.launcher"
gbranch="develop"

repo="${1:-http://repo.jrevolt.io}"
groupid="io.jrevolt.launcher"
artifactid="io.jrevolt.launcher"
version="${2:-develop-SNAPSHOT}"

urljar="${repo}/service/local/artifact/maven/redirect?r=snapshots&g=${groupid}&a=${artifactid}&v=${version}&e=jar"
urlsh="https://raw.githubusercontent.com/${grepo}/${gbranch}/bin/jrevolt.sh"
fjar="io.jrevolt.launcher.jar"

echo "Updating JRevolt Launcher..."
cd $(dirname $0)

url=$(curl -sk --head "$urljar" \
	| perl -n -e '/^Location: (.*)$/ && print "$1\n"' \
	| sed 's/\r//'
)
curl -sk --remote-name $url
[ -L $fjar ] && rm $fjar
ln -s $(ls -1 io.jrevolt.launcher-*.jar | tail -n1) $fjar
ls -l $fjar
chmod 640 *.jar

wget -q -N $urlsh
chmod a+x $(basename $urlsh)