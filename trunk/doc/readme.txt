
Using the  sun.reflect.Reflection getCallerClass(index) method won't work in Eclipse

Fix by going into Project Properties > Java Compiler > Errors/Warning 
Click on the link to configure workspace settings
Under deprecated and Restricted API
Change: Forbidden References (access rules): to Warning from Error



Upgrade jdk16 to jdk16 04 or later - for jaxb 2.1

Set JRE_HOME=jdk16 before starting tomcat
Drop the database before starting tomcat
Pass in memory parameters to tomcat startup.bat -Xms1000M -Xmx1000M



NOTE: jdk 1.6.4 or later is required for JAXB 2.1. Using an earlier version of 1.6 results in an error:

java.lang.LinkageError: JAXB 2.0 API is being loaded from the bootstrap
classloader, but this RI (from
jar:file:/C:/Documents%20and%20Settings/ron/My%20Documents/workspace33/Reque
l/lib/jaxb/jaxb-impl.jar!/com/sun/xml/bind/v2/model/impl/ModelBuilder.class)
needs 2.1 API. Use the endorsed directory mechanism to place jaxb-api.jar in
the bootstrap classloader. (See
http://java.sun.com/j2se/1.5.0/docs/guide/standards/)
