#!/bin/bash
set -eu

grepo="jrevolt/io.jrevot.launcher"
gbranch="develop"

repo="${1:-http://repo.jrevolt.io}"
groupid="io.jrevolt.launcher"
artifactid="io.jrevolt.launcher"
version="${2:-develop-SNAPSHOT}"

url="${repo}/service/local/artifact/maven/redirect?r=snapshots&g=${groupid}&a=${artifactid}&v=${version}&e=jar"

echo "Updating JRevolt Launcher..."
cd $(dirname $0)

url=$(curl -sk --head "$url" \
	| perl -n -e '/^Location: (.*)$/ && print "$1\n"' \
	| sed 's/\r//'
)
curl -sk --remote-name $url
[ -L springboot.jar ] && rm springboot.jar
ln -s $(ls -1 spring-boot-launcher-*.jar | tail -n1) springboot.jar
ls -l springboot.jar
chmod 640 *.jar

wget -q -N https://raw.githubusercontent.com/${grepo}/${gbranch}/bin/springboot.sh
chmod a+x springboot.sh