#!/bin/bash

set -eu
error() { echo "ERROR: $*"; exit 1; }
trap 'error ${LINENO}' ERR

realpath() {
	echo "$(cd $(dirname $1) && pwd -L)/$(basename $1)"
}

dflt=master
[ -f .version ] && dflt="$(cat .version)"

# github
grepo="jrevolt/io.jrevolt.launcher"
gversion="${1:-$dflt}"

if [[ $gversion =~ "^tag:.*" ]]; then
	gversion="${gversion//tag:}"
	mversion=$gversion
	reponame=releases
else
	mversion="${gversion//*\/}-SNAPSHOT"
	reponame=snapshots
fi

# maven
repo="${2:-http://repo.jrevolt.io}"
groupid="io.jrevolt.launcher"
artifactid="io.jrevolt.launcher"

urljar="${repo}/service/local/artifact/maven/redirect?r=${reponame}&g=${groupid}&a=${artifactid}&v=${mversion}&e=jar"
urlsh="https://raw.githubusercontent.com/${grepo}/${gversion}/bin/jrevolt.sh"
fjar="io.jrevolt.launcher.jar"

echo "Updating JRevolt Launcher..."
cd $(dirname $0)

url=$(curl -sk --head "$urljar" | grep "Location:" | sed "s/Location: //" | tr -d '\r')

wget -qN $url

[ -L $fjar ] && rm $fjar
ln -s $(basename $url) $fjar
chmod 640 $fjar

wget -qN $urlsh
chmod a+x $(basename $urlsh)

ls -l $fjar $(basename $urlsh)


