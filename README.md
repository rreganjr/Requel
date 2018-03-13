## Requel

Requel is a Web-based requirements management system that supports collaboration among all stakeholders and provides (_very limited_) automated assistance to validate requirements and suggest improvements. It supports requirements as goals, stories, and use-cases.

See the [User Guide](https://github.com/rreganjr/Requel/raw/master/doc/UserGuide.pdf) for more details on using Requel. **NOTE** Chapter 5, _Requel Setup_ is no longer relevant with the executable Jar file.

For more information about the motivation for this project see the [Thesis Document](https://github.com/rreganjr/Requel/raw/master/doc/ThesisFinalColor.pdf).

See the example [Requel.xml](https://raw.githubusercontent.com/rreganjr/Requel/v1.0.1-beta/doc/samples/Requel.xml) project file  that can be imported.

### New Executable Jar for Easy Running

This release replicates the functionality of the original release from 2009, but as an executable jar file that is easier to configure and run. It has an embedded tomcat web server so you only need to have a MySQL database running. Just pass database settings and a port to listen on if 8080 is not available:

Pass in the database setting parameters like this:

* **jdbc url** --spring.datasource.url=\<url\>
* **username** --spring.datasource.username=\<username\>
* **password** --spring.datasource.password=\<password\>
 
Optionally pass the service port like this:

* **port** --server.port=\<portnumber\>

### Example command

```
java -jar Requel-1.0.3.jar --spring.datasource.url=jdbc:mysql://localhost:3306/requeldb?createDatabaseIfNotExist=true --spring.datasource.username=root --spring.datasource.password=password --server.port=8081
```

Then access the app http://localhost:8081/

log in to the application as **admin** user with password **admin**.

### If You Use Docker
check out  https://hub.docker.com/r/rreganjr/requel/

```
docker network create requel-net
docker run --name requelDB --net=requel-net -e MYSQL_ROOT_PASSWORD=pa33w0rd -d mysql:5.6.32
docker run --name requel --net=requel-net -p8181:8080 -d rreganjr/requel:1.0.1 --spring.datasource.url=jdbc:mysql://requelDB:3306/requeldb?createDatabaseIfNotExist=true --spring.datasource.password=pa33w0rd
```
Then access the app http://localhost:8181/
