/*
SQLyog - Free MySQL GUI v5.12
Host - 5.0.21-community-nt : Database - requel
*********************************************************************
Server version : 5.0.21-community-nt
*/

SET NAMES utf8;

SET SQL_MODE='';

SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO';

/*Data for the table `vnselres` */

LOCK TABLES `vnselres` WRITE;

insert into `vnselres` (`vnselresid`,`name`,`synsetid`,`linkid`) values (1,'abstract',100002137,2);
insert into `vnselres` (`vnselresid`,`name`,`synsetid`,`linkid`) values (2,'animal',100015388,2);
insert into `vnselres` (`vnselresid`,`name`,`synsetid`,`linkid`) values (3,'animate',100004258,2);
insert into `vnselres` (`vnselresid`,`name`,`synsetid`,`linkid`) values (4,'body_part',105220461,2);
insert into `vnselres` (`vnselresid`,`name`,`synsetid`,`linkid`) values (5,'comestible',107556637,2);
insert into `vnselres` (`vnselresid`,`name`,`synsetid`,`linkid`) values (6,'communication',106252138,2);
insert into `vnselres` (`vnselresid`,`name`,`synsetid`,`linkid`) values (7,'concrete',100001930,2);
insert into `vnselres` (`vnselresid`,`name`,`synsetid`,`linkid`) values (8,'currency',113372961,2);
insert into `vnselres` (`vnselresid`,`name`,`synsetid`,`linkid`) values (9,'shape',105064037,2);
insert into `vnselres` (`vnselresid`,`name`,`synsetid`,`linkid`) values (10,'force',111458624,2);
insert into `vnselres` (`vnselresid`,`name`,`synsetid`,`linkid`) values (11,'garment',103419014,2);
insert into `vnselres` (`vnselresid`,`name`,`synsetid`,`linkid`) values (12,'human',102472293,2);
insert into `vnselres` (`vnselresid`,`name`,`synsetid`,`linkid`) values (13,'int_control',105982152,2);
insert into `vnselres` (`vnselresid`,`name`,`synsetid`,`linkid`) values (14,'location',100027167,2);
insert into `vnselres` (`vnselresid`,`name`,`synsetid`,`linkid`) values (15,'machine',103699975,2);
insert into `vnselres` (`vnselresid`,`name`,`synsetid`,`linkid`) values (16,'rigid',105023741,2);
insert into `vnselres` (`vnselresid`,`name`,`synsetid`,`linkid`) values (17,'organization',108008335,2);
insert into `vnselres` (`vnselresid`,`name`,`synsetid`,`linkid`) values (18,'plural',100001740,2);
insert into `vnselres` (`vnselresid`,`name`,`synsetid`,`linkid`) values (19,'pointy',105071556,2);
insert into `vnselres` (`vnselresid`,`name`,`synsetid`,`linkid`) values (20,'refl',106328207,4);
insert into `vnselres` (`vnselresid`,`name`,`synsetid`,`linkid`) values (21,'region',108630985,2);
insert into `vnselres` (`vnselresid`,`name`,`synsetid`,`linkid`) values (22,'scalar',105855125,2);
insert into `vnselres` (`vnselresid`,`name`,`synsetid`,`linkid`) values (23,'solid',115046900,2);
insert into `vnselres` (`vnselresid`,`name`,`synsetid`,`linkid`) values (24,'sound',107371293,2);
insert into `vnselres` (`vnselresid`,`name`,`synsetid`,`linkid`) values (25,'substance',100019613,2);
insert into `vnselres` (`vnselresid`,`name`,`synsetid`,`linkid`) values (26,'time',115113229,2);
insert into `vnselres` (`vnselresid`,`name`,`synsetid`,`linkid`) values (27,'vehicle',104524313,2);
insert into `vnselres` (`vnselresid`,`name`,`synsetid`,`linkid`) values (28,'elongated',100001740,2);
UNLOCK TABLES;

SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
