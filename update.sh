#!/bin/bash

set -eu
error() { echo "ERROR: $*"; exit 1; }
trap 'error ${LINENO}' ERR

realpath() {
	p="$(cd $(dirname $1) && pwd -L)/$(basename $1)"
	which cygpath >/dev/null && cygpath -w $p || echo $p
}

basedir="$HOME/.jrevolt"
defaultVersion="release/0.1.0"

update1() {
	[ -d $basedir ] || mkdir -p $basedir
	cd $basedir
	
	if [[ ! -d .git ]]; then
		echo "Initializing..."
		git init .
		git checkout -b dist
		git remote add -t dist origin https://github.com/jrevolt/io.jrevolt.launcher.git
	fi
	
	echo "Updating JRevolt Launcher scripts..."
	git pull
	
	./update.sh update2
}

update2() {
	cd $basedir
	
	dflt=$defaultVersion
	[ -f .version ] && dflt="$(cat .version)"
	
	# github
	grepo="jrevolt/io.jrevolt.launcher"
	gversion="${1:-$dflt}"
	
	[ "$gversion" = "$dflt" ] || echo "$gversion" > .version

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
	
	./jrevolt.sh version
}

${@:-update1}

