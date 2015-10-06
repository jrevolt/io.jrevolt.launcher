#!/bin/bash

set -eu
error() { echo "ERROR: $*"; exit 1; }
trap 'error ${LINENO}' ERR

realpath() {
	echo "$(cd $(dirname $1) && pwd -L)/$(basename $1)"
}

update1() {
	echo "Updating JRevolt Launcher scripts..."
	git pull
	$0 update2
}

update2() {
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
	fjar="io.jrevolt.launcher.jar"
	
	echo "Updating JRevolt Launcher library..."
	cd $(dirname $0)
	
	url=$(curl -sk --head "$urljar" | grep "Location:" | sed "s/Location: //" | tr -d '\r')
	
	wget -qN $url
	
	[ -L $fjar ] && rm $fjar
	ln -s $(basename $url) $fjar
	chmod 640 $fjar
	echo "$(basename $url)"
}

${@:-update1}
