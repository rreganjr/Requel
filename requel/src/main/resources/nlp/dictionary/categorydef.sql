SET NAMES utf8;

SET SQL_MODE='';

SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO';

/*Data for the table `categorydef` */

LOCK TABLES `categorydef` WRITE;

insert into `categorydef` (`categoryid`,`name`,`pos`) values (0,'adj.all','a');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (1,'adj.pert','a');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (2,'adv.all','r');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (3,'noun.tops','n');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (4,'noun.act','n');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (5,'noun.animal','n');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (6,'noun.artifact','n');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (7,'noun.attribute','n');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (8,'noun.body','n');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (9,'noun.cognition','n');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (10,'noun.communication','n');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (11,'noun.event','n');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (12,'noun.feeling','n');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (13,'noun.food','n');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (14,'noun.group','n');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (15,'noun.location','n');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (16,'noun.motive','n');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (17,'noun.object','n');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (18,'noun.person','n');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (19,'noun.phenomenon','n');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (20,'noun.plant','n');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (21,'noun.possession','n');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (22,'noun.process','n');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (23,'noun.quantity','n');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (24,'noun.linkdef','n');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (25,'noun.shape','n');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (26,'noun.state','n');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (27,'noun.substance','n');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (28,'noun.time','n');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (29,'verb.body','v');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (30,'verb.change','v');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (31,'verb.cognition','v');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (32,'verb.communication','v');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (33,'verb.competition','v');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (34,'verb.consumption','v');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (35,'verb.contact','v');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (36,'verb.creation','v');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (37,'verb.emotion','v');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (38,'verb.motion','v');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (39,'verb.perception','v');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (40,'verb.possession','v');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (41,'verb.social','v');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (42,'verb.stative','v');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (43,'verb.weather','v');
insert into `categorydef` (`categoryid`,`name`,`pos`) values (44,'adj.ppl','a');

UNLOCK TABLES;

SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
