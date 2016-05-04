=========
Reference
=========

----------
Invocation
----------

Options:
 - java archive
 - shell script wrapper
 - docker image

Java Archive
============

java <JVM options> <system properties> launcher.jar <launcher options> <artifact> <app args>

Script Wrapper
==============

Allows you to mix all the options:

jrevolt.sh <artifact> <options>


Docker Image
============

All in one environment for running applications as services

docker run jrevolt/launcher <launcher arguments>

Volumes:
 - /etc/jrevolt
 - /etc/${appname}
 - /var/cache/jrevolt
 - /var/log/${appname}