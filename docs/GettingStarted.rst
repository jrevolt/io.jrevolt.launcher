===============
Getting Started
===============

Step 1: :ref:`Installation`

# Install

Follow [Installation](Installation).

# First run

```
$ ~/.jrevolt/jrevolt.sh
```

A help screen like this should be printed:

```
Usage:
  java -jar jrevolt.jar [launcher-options] <artifact> [application-arguments]
  java -jar jrevolt.jar [launcher-options] <tool> [tool-options]
Artifact:
  <groupId>:<artifactId>:<version>[:<packaging>[:<classifier>[:<mainclass>]]]
  <file-path>
Tool:
  repository --id=<alias> --url=<URL> --username=<username>
  encrypt    --key --value
  decrypt    --key
  config             : Show configuration
  version            : Show version
Launcher Options:
  apphome            : Application home directory
  appname            : Application name
  defaults           : Comma-separated list of URLs specifying hierarchy of MvnLauncher
                       configuration files (*.properties)
  debug              : Enables debug output
  quiet              : Suppresses any output but errors
  statusLine         : Enables status line feedback. Use if the autodetection fails.
  cache              : Directory where all cached files are stored.
  showClasspath      : Dump actual classpath information when constructed.
  offline            : Switches to offline mode. No repository operations are performed
                       and the process relies on cache only.
  updateInterval     : Remote repostitory update interval [seconds]
  verify             : Set this to false to disable downloaded artifact SHA1 verification.
  ignoreCache        : When enabled, cache content is ignored and
                       all artifacts are downloaded again.
  failOnError        : Disable this if you want to try execution despite the errors.
  cacheFileProtocol  : Disable this to use the file-based repository directly
                       (instead of caching them)
  updateReleases     : Enable this to check for updates to released
                       (usually one-time download) artifacts.
  updateSnapshots    : Disable this to ignore snapshot updates.
  execute            : Disable this to only update the application and skip its execution.
  update             : Enable this to ignore cache expiration flags
                       and force updates checking.
  repository         : Name of the repository to use. Such repository must have
                       corresponding entry in your vault (created previously using
                       --MvnLauncher.save=true)
  url                : Maven repository URL
  username           : Maven repository authentication: username
  password           : Maven repository authentication: password
  save               : true
  artifact           : Artifact URI (group:artifact:version)
See:
  https://github.com/jrevolt/io.jrevolt.launcher/wiki/About
  https://github.com/jrevolt/io.jrevolt.launcher/wiki/Getting-Started
  https://github.com/jrevolt/io.jrevolt.launcher/wiki/Reference
Generate Certificate and Private Key for sensitive data encryption:
  $ mkdir ~/.jrevolt
  $ openssl genrsa | openssl pkcs8 -topk8 -nocrypt -out ~/.jrevolt/vault.key
  $ subject="/CN=$(whoami)@$(hostname)/OU=MyDepartment/O=MyOrganization/L=MyLocation/C=US"
  $ openssl req -new -x509 -days 1095 -subj "$subject" -key ~/.jrevolt/vault.key -out ~/.jrevolt/vault.crt
```

# Checking Version

```
$ ~/.jrevolt/jrevolt.sh version
```

Output:

```
JRevolt Launcher (0.1.0.RELEASE, Fri Oct 30 10:49:54 CET 2015)
```

# Updating

```
$ ~/.jrevolt/update.sh
```

Output:

```
Updating scripts  ... OK
Updating binaries ... OK
Current version   ... JRevolt Launcher (0.1.0.RELEASE, Fri Oct 30 10:49:54 CET 2015)
```

# Checking Configuration

```
$ ~/.jrevolt/jrevolt.sh config
```

Output:

```
[INF] JRevolt Launcher (0.1.0.RELEASE, Fri Oct 30 10:49:54 CET 2015)
[DBG] JRevolt Launcher configuration:
[DBG] - jrevolt.launcher.apphome         : /usr/java/jdk1.8.0_66/jre
[DBG] - jrevolt.launcher.appname         : JavaApp
[DBG] - jrevolt.launcher.defaults        : file:jrevolt.properties,file:///${jrevolt.launcher.apphome}/${jrevolt.launcher.appname}.properties,file:///${jrevolt.launcher.apphome}/jrevolt.properties,file:////home/patrik/.jrevolt/defaults.properties,file:///etc/jrevolt/defaults.properties,classpath:META-INF/jrevolt/defaults.properties,classpath:META-INF/io.jrevolt.launcher/defaults.properties
[DBG] - jrevolt.launcher.repositories    : central,jrevolt
[DBG] - jrevolt.launcher.cache           : /home/patrik/.jrevolt/cache
[DBG] - jrevolt.launcher.delegate        : false
[DBG] - jrevolt.launcher.debug           : true
[DBG] - jrevolt.launcher.quiet           : false
[DBG] - jrevolt.launcher.ansi            : true
[DBG] - jrevolt.launcher.offline         : false
[DBG] - jrevolt.launcher.verify          : false
[DBG] - jrevolt.launcher.ignoreCache     : false
[DBG] - jrevolt.launcher.failOnError     : true
[DBG] - jrevolt.launcher.updateReleases  : false
[DBG] - jrevolt.launcher.updateSnapshots : true
[DBG] - jrevolt.launcher.execute         : true
[DBG] - jrevolt.launcher.update          : true
[DBG] - jrevolt.launcher.updateInterval  : P1D
[DBG] - jrevolt.launcher.skipDownload    : false
[DBG] - jrevolt.launcher.resolvers       : 4
[DBG] - jrevolt.launcher.downloaders     : 3
[DBG] - jrevolt.launcher.retries         : 3
```

# Running Example Application

```
$ jrevolt.sh io.jrevolt.example:io.jrevolt.example.hello:develop-SNAPSHOT
```

Output:

```
[INF] - Downloaded  : aopalliance:aopalliance:1.0:jar                                                  (   4KB @central)
[INF] - Downloaded  : ch.qos.logback:logback-classic:1.1.3:jar                                         ( 274KB @central)
[INF] - Downloaded  : ch.qos.logback:logback-core:1.1.3:jar                                            ( 444KB @central)
[INF] - Downloaded  : commons-logging:commons-logging:1.2:jar                                          (  60KB @central)
[INF] - Downloaded  : io.jrevolt.example:io.jrevolt.example.hello:develop-20151124.204914-14:jar       (   2KB @central)
[INF] - Downloaded  : io.jrevolt.launcher:io.jrevolt.launcher:0.1.0-20151030.093615-60:jar             ( 260KB @central)
[INF] - Downloaded  : org.slf4j:jcl-over-slf4j:1.7.12:jar                                              (  16KB @central)
[INF] - Downloaded  : org.slf4j:jul-to-slf4j:1.7.12:jar                                                (   4KB @central)
[INF] - Downloaded  : org.slf4j:log4j-over-slf4j:1.7.12:jar                                            (  23KB @central)
[INF] - Downloaded  : org.slf4j:slf4j-api:1.7.12:jar                                                   (  31KB @central)
[INF] - Downloaded  : org.springframework.boot:spring-boot-autoconfigure:1.3.0.RC1:jar                 ( 710KB @central)
[INF] - Downloaded  : org.springframework.boot:spring-boot-loader:1.3.0.RC1:jar                        (  85KB @central)
[INF] - Downloaded  : org.springframework.boot:spring-boot-starter-logging:1.3.0.RC1:jar               (   2KB @central)
[INF] - Downloaded  : org.springframework.boot:spring-boot-starter:1.3.0.RC1:jar                       (   2KB @central)
[INF] - Downloaded  : org.springframework.boot:spring-boot:1.3.0.RC1:jar                               ( 516KB @central)
[INF] - Downloaded  : org.springframework:spring-aop:4.2.2.RELEASE:jar                                 ( 357KB @central)
[INF] - Downloaded  : org.springframework:spring-beans:4.2.2.RELEASE:jar                               ( 712KB @central)
[INF] - Downloaded  : org.springframework:spring-context:4.2.2.RELEASE:jar                             (1062KB @central)
[INF] - Downloaded  : org.springframework:spring-core:4.2.2.RELEASE:jar                                (1043KB @central)
[INF] - Downloaded  : org.springframework:spring-expression:4.2.2.RELEASE:jar                          ( 256KB @central)
[INF] - Downloaded  : org.yaml:snakeyaml:1.16:jar                                                      ( 263KB @central)
[INF] Summary: 21 archives, 6135 KB total (resolved in 3249 msec, downloaded 6136 KB in 33 requests, 1888 KBps). Warnings/Errors: 0/0.
Hello!
```

On first run, you see that JRevolt Launcher donwloads several required dependencies before the application is executed.

Run the same command again.
Output:

```
[INF] Summary: 21 archives, 6135 KB total (resolved in 1170 msec, downloaded 0 KB in 4 requests, 0 KBps). Warnings/Errors: 0/0.
Hello!
```

No new artifacts were downloaded, only a few simple requests were executed to check for snapshot updates.
To see more details, use `--debug` option:

```
$ jrevolt.sh --debug io.jrevolt.example:io.jrevolt.example.hello:develop-SNAPSHOT
```

Output:

```
[DBG] Using repositories:
[DBG] -      central : http://repo1.maven.org/maven2/ (<anonymous>)
[DBG] -      jrevolt : http://repo.jrevolt.io/content/groups/public/ (<anonymous>)
[DBG] Dependencies (alphabetical):
[DBG] - NotModified : aopalliance:aopalliance:1.0:jar                                                  (   4KB @central)
[DBG] - NotModified : ch.qos.logback:logback-classic:1.1.3:jar                                         ( 274KB @central)
[DBG] - NotModified : ch.qos.logback:logback-core:1.1.3:jar                                            ( 444KB @central)
[DBG] - NotModified : commons-logging:commons-logging:1.2:jar                                          (  60KB @central)
[DBG] - NotModified : io.jrevolt.example:io.jrevolt.example.hello:develop-20151124.204914-14:jar       (   2KB @jrevolt)
[DBG] - NotModified : io.jrevolt.launcher:io.jrevolt.launcher:0.1.0-20151030.093615-60:jar             ( 260KB @jrevolt)
[DBG] - NotModified : org.slf4j:jcl-over-slf4j:1.7.12:jar                                              (  16KB @central)
[DBG] - NotModified : org.slf4j:jul-to-slf4j:1.7.12:jar                                                (   4KB @central)
[DBG] - NotModified : org.slf4j:log4j-over-slf4j:1.7.12:jar                                            (  23KB @central)
[DBG] - NotModified : org.slf4j:slf4j-api:1.7.12:jar                                                   (  31KB @central)
[DBG] - NotModified : org.springframework.boot:spring-boot-autoconfigure:1.3.0.RC1:jar                 ( 710KB @jrevolt)
[DBG] - NotModified : org.springframework.boot:spring-boot-loader:1.3.0.RC1:jar                        (  85KB @jrevolt)
[DBG] - NotModified : org.springframework.boot:spring-boot-starter-logging:1.3.0.RC1:jar               (   2KB @jrevolt)
[DBG] - NotModified : org.springframework.boot:spring-boot-starter:1.3.0.RC1:jar                       (   2KB @jrevolt)
[DBG] - NotModified : org.springframework.boot:spring-boot:1.3.0.RC1:jar                               ( 516KB @jrevolt)
[DBG] - NotModified : org.springframework:spring-aop:4.2.2.RELEASE:jar                                 ( 357KB @central)
[DBG] - NotModified : org.springframework:spring-beans:4.2.2.RELEASE:jar                               ( 712KB @central)
[DBG] - NotModified : org.springframework:spring-context:4.2.2.RELEASE:jar                             (1062KB @central)
[DBG] - NotModified : org.springframework:spring-core:4.2.2.RELEASE:jar                                (1043KB @central)
[DBG] - NotModified : org.springframework:spring-expression:4.2.2.RELEASE:jar                          ( 256KB @central)
[DBG] - NotModified : org.yaml:snakeyaml:1.16:jar                                                      ( 263KB @central)
[INF] Summary: 21 archives, 6135 KB total (resolved in 1157 msec, downloaded 0 KB in 4 requests, 0 KBps). Warnings/Errors: 0/0.
[DBG] ## Application Arguments:
[DBG] ##
Hello!
```

To completely silence the launcher, use `--quiet`:

```
Hello!
```

To go completely offline, use `--offline`:

```
[DBG] Using repositories:
[DBG] -      central : http://repo1.maven.org/maven2/ (<anonymous>)
[DBG] -      jrevolt : http://repo.jrevolt.io/content/groups/public/ (<anonymous>)
[DBG] Dependencies (alphabetical):
[DBG] - Offline     : aopalliance:aopalliance:1.0:jar                                                  (   4KB @cache)
[DBG] - Offline     : ch.qos.logback:logback-classic:1.1.3:jar                                         ( 274KB @cache)
[DBG] - Offline     : ch.qos.logback:logback-core:1.1.3:jar                                            ( 444KB @cache)
[DBG] - Offline     : commons-logging:commons-logging:1.2:jar                                          (  60KB @cache)
[DBG] - Offline     : io.jrevolt.example:io.jrevolt.example.hello:develop-20151124.204914-14:jar       (   2KB @cache)
[DBG] - Offline     : io.jrevolt.launcher:io.jrevolt.launcher:0.1.0-20151030.093615-60:jar             ( 260KB @cache)
[DBG] - Offline     : org.slf4j:jcl-over-slf4j:1.7.12:jar                                              (  16KB @cache)
[DBG] - Offline     : org.slf4j:jul-to-slf4j:1.7.12:jar                                                (   4KB @cache)
[DBG] - Offline     : org.slf4j:log4j-over-slf4j:1.7.12:jar                                            (  23KB @cache)
[DBG] - Offline     : org.slf4j:slf4j-api:1.7.12:jar                                                   (  31KB @cache)
[DBG] - Offline     : org.springframework.boot:spring-boot-autoconfigure:1.3.0.RC1:jar                 ( 710KB @cache)
[DBG] - Offline     : org.springframework.boot:spring-boot-loader:1.3.0.RC1:jar                        (  85KB @cache)
[DBG] - Offline     : org.springframework.boot:spring-boot-starter-logging:1.3.0.RC1:jar               (   2KB @cache)
[DBG] - Offline     : org.springframework.boot:spring-boot-starter:1.3.0.RC1:jar                       (   2KB @cache)
[DBG] - Offline     : org.springframework.boot:spring-boot:1.3.0.RC1:jar                               ( 516KB @cache)
[DBG] - Offline     : org.springframework:spring-aop:4.2.2.RELEASE:jar                                 ( 357KB @cache)
[DBG] - Offline     : org.springframework:spring-beans:4.2.2.RELEASE:jar                               ( 712KB @cache)
[DBG] - Offline     : org.springframework:spring-context:4.2.2.RELEASE:jar                             (1062KB @cache)
[DBG] - Offline     : org.springframework:spring-core:4.2.2.RELEASE:jar                                (1043KB @cache)
[DBG] - Offline     : org.springframework:spring-expression:4.2.2.RELEASE:jar                          ( 256KB @cache)
[DBG] - Offline     : org.yaml:snakeyaml:1.16:jar                                                      ( 263KB @cache)
[INF] Summary: 21 archives, 6135 KB total (resolved in 96 msec, downloaded 0 KB in 0 requests, 0 KBps). Warnings/Errors: 0/0.
[DBG] ## Application Arguments:
[DBG] ##
Hello!
```




# TODO
 - run simple app (springboot+jrevolt)
 - run legacy app (executable springboot app)
 - configuring repositories
 - using vault for repository passwords
 - ...

