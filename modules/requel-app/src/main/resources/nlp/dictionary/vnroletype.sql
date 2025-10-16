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

/*Data for the table `vnroletype` */

LOCK TABLES `vnroletype` WRITE;

insert into `vnroletype` (`roletypeid`,`type`,`semrole`) values ('1','Agent','AGENT');
insert into `vnroletype` (`roletypeid`,`type`,`semrole`) values ('2','Theme','THEME');
insert into `vnroletype` (`roletypeid`,`type`,`semrole`) values ('3','Destination','DESTINATION');
insert into `vnroletype` (`roletypeid`,`type`,`semrole`) values ('4','Experiencer','EXPERIENCER');
insert into `vnroletype` (`roletypeid`,`type`,`semrole`) values ('5','Predicate','PREDICATE');
insert into `vnroletype` (`roletypeid`,`type`,`semrole`) values ('6','Location','LOCATION');
insert into `vnroletype` (`roletypeid`,`type`,`semrole`) values ('7','Topic','TOPIC');
insert into `vnroletype` (`roletypeid`,`type`,`semrole`) values ('8','Recipient','RECIPIENT');
insert into `vnroletype` (`roletypeid`,`type`,`semrole`) values ('9','Patient','PATIENT');
insert into `vnroletype` (`roletypeid`,`type`,`semrole`) values ('10','Patient1','PATIENT1');
insert into `vnroletype` (`roletypeid`,`type`,`semrole`) values ('11','Patient2','PATIENT2');
insert into `vnroletype` (`roletypeid`,`type`,`semrole`) values ('12','Cause','CAUSE');
insert into `vnroletype` (`roletypeid`,`type`,`semrole`) values ('13','Proposition','PROPOSITION');
insert into `vnroletype` (`roletypeid`,`type`,`semrole`) values ('14','Attribute','ATTRIBUTE');
insert into `vnroletype` (`roletypeid`,`type`,`semrole`) values ('15','Source','SOURCE');
insert into `vnroletype` (`roletypeid`,`type`,`semrole`) values ('16','Actor','ACTOR');
insert into `vnroletype` (`roletypeid`,`type`,`semrole`) values ('17','Actor1','ACTOR1');
insert into `vnroletype` (`roletypeid`,`type`,`semrole`) values ('18','Actor2','ACTOR2');
insert into `vnroletype` (`roletypeid`,`type`,`semrole`) values ('19','Instrument','INSTRUMENT');
insert into `vnroletype` (`roletypeid`,`type`,`semrole`) values ('20','Asset','ASSET');
insert into `vnroletype` (`roletypeid`,`type`,`semrole`) values ('21','Material','MATERIAL');
insert into `vnroletype` (`roletypeid`,`type`,`semrole`) values ('22','Product','PRODUCT');
insert into `vnroletype` (`roletypeid`,`type`,`semrole`) values ('23','Beneficiary','BENEFICIARY');
insert into `vnroletype` (`roletypeid`,`type`,`semrole`) values ('24','Extent','EXTENT');
insert into `vnroletype` (`roletypeid`,`type`,`semrole`) values ('25','Theme1','THEME1');
insert into `vnroletype` (`roletypeid`,`type`,`semrole`) values ('26','Theme2','THEME2');
insert into `vnroletype` (`roletypeid`,`type`,`semrole`) values ('27','Time','TIME');
insert into `vnroletype` (`roletypeid`,`type`,`semrole`) values ('28','Value','VALUE');
insert into `vnroletype` (`roletypeid`,`type`,`semrole`) values ('29','Stimulus','STIMULUS');

UNLOCK TABLES;

SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
