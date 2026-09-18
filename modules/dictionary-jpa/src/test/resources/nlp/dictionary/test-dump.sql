/* Shaped like the real nlp/dictionary/*.sql.gz dumps, two rows instead of 45. The real dumps live
   in nlp-jpa, which dictionary-jpa does not depend on, so this fixture keeps the unit test on its
   own classpath.

   Comment style matters: DictionarySQLInitializer.readSQL strips block comments and has no
   handling for `--` line comments, so a `--` line would be accumulated into the statement that
   follows it and defeat the `startsWith("lock tables")` filter. The real dumps use block comments
   throughout. See doc/architecture/DICTIONARY_LOADING.md. */

SET NAMES utf8;

SET SQL_MODE='';

SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO';

/*Data for the table `categorydef` */

LOCK TABLES `categorydef` WRITE;

insert into `categorydef` (`categoryid`,`name`,`pos`) values (0,'adj.all','a');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (1,'adj.pert','a');

UNLOCK TABLES;

SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
