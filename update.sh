#!/bin/bash

set -u
die() { echo "ERROR: $*"; exit 1; }
trap 'die ${LINENO:-}' ERR

which git >/dev/null 2>&1 || die "Missing: git"

basedir="$HOME/.jrevolt"
defaultVersion="0.1.0.RELEASE"

realpath() {
	p="$(cd $(dirname $1) && pwd -L)/$(basename $1)"
	which cygpath >/dev/null 2>&1 && cygpath -w $p || echo $p
}

install() {
	[ -d $basedir ] || mkdir -p $basedir
	cd $basedir
	[ -f $basedir/.version ] && rm $basedir/.version
	update1 "$@"
}

update1() {
	[ -d $basedir ] || mkdir -p $basedir
	cd $basedir
	
	if [[ ! -d .git ]]; then
		printf "%-17s ... " "Initializing"
		{
		git init .
		git checkout -b dist
		git remote add -t dist origin https://github.com/jrevolt/io.jrevolt.launcher.git
		} >/dev/null 2>&1 && printf "OK\n" || die
	fi

	printf "%-17s ... " "Updating scripts"
	git pull >/dev/null 2>&1 && printf "OK\n" || die
	
	./update.sh update2 "$@"
}

update2() {
	cd $basedir
	
	version="${1:-}"
	dflt=$defaultVersion
	[ -f .version ] && dflt="$(cat .version)"
	
	# github
	grepo="jrevolt/io.jrevolt.launcher"
	gversion="${version:-$dflt}"
	
	[ "$gversion" = "$dflt" ] || echo "$gversion" > .version

	case "$gversion" in
		develop|feature/*|release/*|hotfix/*)
			# assume branch snapshot: strip prefix, if any, and append -SNAPSHOT suffix
			mversion="${gversion//*\/}-SNAPSHOT"
			reponame=snapshots
			;;
		*)
			# assume release
			mversion=$gversion
			reponame=releases
			;;
	esac

	# maven
	repo="${2:-http://repo.jrevolt.io}"
	groupid="io.jrevolt.launcher"
	artifactid="io.jrevolt.launcher"
	
	urljar="${repo}/service/local/artifact/maven/redirect?r=${reponame}&g=${groupid}&a=${artifactid}&v=${mversion}&e=jar"
	fjar="io.jrevolt.launcher.jar"
	
	printf "%-17s ... " "Updating binaries"
	
	{
	cd $(dirname $0)
	
	url=$(curl -sk --head "$urljar" | grep "Location:" | sed "s/Location: //" | tr -d '\r')
	
	wget -qN $url
	
	[ -L $fjar ] && rm $fjar
	ln -s $(basename $url) $fjar
	chmod 640 $fjar
	} >> $basedir/update.log 2>&1 && printf "OK\n" || die
	
	printf "%-17s ... %s\n" "Current version" "$(./jrevolt.sh version)"
}

${@:-update1}

