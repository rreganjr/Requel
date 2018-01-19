#!/bin/bash
#
# usage: install-lib-mvn.sh
#
# this installs jar files from the lib folder that aren't in a maven repository, into the local maven repo
#
#
mvn install:install-file -Dfile=./lib/echo/echopm.jar -DgroupId=com.rreganjr.echopm -DartifactId=echopm -Dversion=1.0.0 -Dpackaging=jar
mvn install:install-file -Dfile=./lib/echo/echopointng-2.2.0rc2.jar -DgroupId=echopointng -DartifactId=echopointng -Dversion=1.0.0 -Dpackaging=jar
mvn install:install-file -Dfile=./lib/echo/Echo2_FileTransfer_WebContainer.jar -DgroupId=com.nextapp -DartifactId=echo2-filetransfer-webcontainer -Dversion=2.1.1 -Dpackaging=jar
mvn install:install-file -Dfile=./lib/echo/Echo2_FileTransfer_App.jar -DgroupId=com.nextapp -DartifactId=echo2-filetransfer-app -Dversion=2.1.1 -Dpackaging=jar
# There is a bug in the 2.1.0 relating to drag and drop that I fixed in the local lib, see src/main/resources/echo/DND.js and readme.txt
mvn install:install-file -Dfile=./lib/echo/Echo2_Extras_App.jar -DgroupId=com.nextapp -DartifactId=echo2extras-app -Dversion=2.1.0a -Dpackaging=jar
mvn install:install-file -Dfile=./lib/echo/Echo2_Extras_WebContainer.jar -DgroupId=com.nextapp -DartifactId=echo2extras-webcontainer -Dversion=2.1.0a -Dpackaging=jar
# in the echo2 web renderer it returns javascript type as plain/text - I changed it to application/javascript
mvn install:install-file -Dfile=./lib/echo/Echo2_App.jar -DgroupId=com.nextapp -DartifactId=echo2-app -Dversion=2.1.1a -Dpackaging=jar
mvn install:install-file -Dfile=./lib/echo/Echo2_WebContainer.jar -DgroupId=com.nextapp -DartifactId=echo2-webcontainer -Dversion=2.1.1a -Dpackaging=jar
mvn install:install-file -Dfile=./lib/echo/Echo2_WebRender.jar -DgroupId=com.nextapp -DartifactId=echo2-webrender -Dversion=2.1.1a -Dpackaging=jar