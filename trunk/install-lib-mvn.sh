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