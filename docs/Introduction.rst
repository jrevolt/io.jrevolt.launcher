============
Introduction
============

Tired of classpath, giant web app archives uploads/downloads, complex assemblies and complicated deployment and distribution models? Why not just run the application directly from online Maven repository, with only ~260KB of fast, independent, standalone bootstrap code, without the full-blown Maven infrastructure on client?

Tired of changing single line of code, effectively modifying single 25K module, and having to re-assembly, re-upload, and re-deploy full 150+ MB of enterprise web application archive? Why not just update single 25K module distributable in remote distribution endpoint (maven repository), and simply restarting/reloading the application which downloads and updates this single tiny module, re-using remaining 149+ MB of application from local cache?

Tired of complicated and resource consuming distribution of the new version of client application across hundreds of client machines? Why not performing fast configurable auto-update check on application startup, downloading available updates before launching the app?

Tired of re-inventing plugin architectures, distribution and update models? Why not using embedded maven integration of the repository-enabled launcher to dynamically download plugin dependencies and configure sandboxed plugin classloader with minimal effort?

**Intended Usage**

Deployment site hosts maven repository (e.g. Sonatype Nexus) as a central distribution source.
All artifacts, hosted or third-party, are provided by this service.
CI server (Jenkins, TeamCity, Bamboo) builds and deploys artifacts into this central repository.
JRevolt Launcher is configured to resolve all artifact references against this repository.

This scenario is typical, but not the only option.
