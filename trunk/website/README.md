## New Executable Jar for Easy Running

The new jar release [Requel-1.0.1.jar](https://sourceforge.net/projects/requel/files/Requel-1.0.1.jar/download) is much easier to use. It has an embedded tomcat web server so you only need to have a MySQL database running. Just pass database settings and port if 8080 is not available:

Pass in the database setting parameters like this:

* **jdbc url** --spring.datasource.url=<url>
* **username** --spring.datasource.username=<username>
* **password** --spring.datasource.password=<password>
 
Optionally pass the service port like this:

* **port** --server.port=<portnumber>

### Example command

```java -jar Requel-1.0.1.jar --spring.datasource.url=jdbc:mysql://localhost:3306/requeldb?createDatabaseIfNotExist=true --spring.datasource.username=root --spring.datasource.password=password --server.port=8081```


Then access the app http://localhost:8081/

login to the application as **admin** user with password **admin**.

####Note

There are a couple of issues with the latest version. The file upload for xslt document generation and file import is not working, there appears to be a version compatability issue with the file upload widget/library. The analysis isn't working because of a compatibility issue with the Stanford Parser.