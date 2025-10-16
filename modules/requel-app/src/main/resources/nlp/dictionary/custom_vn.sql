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

/*Data for the table `vnframedef` */

LOCK TABLES `vnclass` WRITE;

insert into vnclass (classid, class, parentid) values (1000, 'enter-data', NULL);
insert into vnclass (classid, class, parentid) values (1001, 'search-data', NULL);

UNLOCK TABLES;

LOCK TABLES `vnframedef` WRITE;

-- The user enters some information.
insert into `vnframedef` (`frameid`,`description1`,`syntax`,`description2`,`number`,`semantics`,`xtag`) 
	values (2000,'NP-V-NP','<NP value=\"Agent\"><SYNRESTRS/></NP><VERB/><NP value=\"Theme\"><SYNRESTRS/></NP>','','','','');
	
-- The user enters some information in the search form.
insert into `vnframedef` (`frameid`,`description1`,`syntax`,`description2`,`number`,`semantics`,`xtag`) 
	values (2001,'NP-V-NP-PP-NP','<NP value=\"Agent\"><SYNRESTRS/></NP><VERB/><NP value=\"Theme\"><SYNRESTRS/></NP><PREP><SELRESTRS><SELRESTR Value=\"+\" type=\"loc\"/></SELRESTRS></PREP><NP value=\"Instrument\"><SYNRESTRS/></NP>','','','','');

-- Some information was entered by the user.
insert into `vnframedef` (`frameid`,`description1`,`syntax`,`description2`,`number`,`semantics`,`xtag`) 
	values (2002,'NP-V-PP-NP','<NP value=\"Theme\"><SYNRESTRS/></NP><VERB/><PREP value=\"by\"><SELRESTRS/></PREP><NP value=\"Agent\"><SYNRESTRS/></NP>','','','','');

-- The user creates a new project.
insert into `vnframedef` (`frameid`,`description1`,`syntax`,`description2`,`number`,`semantics`,`xtag`) 
	values (2003,'NP-V-NP','<NP value=\"Agent\"><SYNRESTRS/></NP><VERB/><NP value=\"Product\"><SYNRESTRS/></NP>','','','','');

-- The phone company billed John for eleven dollars.
insert into `vnframedef` (`frameid`,`description1`,`syntax`,`description2`,`number`,`semantics`,`xtag`) 
	values (2004,'NP-V-NP-PP-NP','<NP value=\"Agent\"><SYNRESTRS/></NP><VERB/><NP value=\"Recipient\"><SYNRESTRS/></NP><PREP value=\"for\"><SELRESTRS/></PREP><NP value=\"Asset\"><SYNRESTRS/></NP>','','','','');

-- A user searches for artists.
insert into `vnframedef` (`frameid`,`description1`,`syntax`,`description2`,`number`,`semantics`,`xtag`) 
	values (2005,'NP-V-PP-NP','<NP value=\"Agent\"><SYNRESTRS/></NP><VERB/><PREP value=\"for\"><SELRESTRS/></PREP><NP value=\"Theme\"><SYNRESTRS/></NP>','','','','');

-- A user searches for artists by name or genre.
insert into `vnframedef` (`frameid`,`description1`,`syntax`,`description2`,`number`,`semantics`,`xtag`) 
	values (2006,'NP-V-PP-NP-PP-NP','<NP value=\"Agent\"><SYNRESTRS/></NP><VERB/><PREP value=\"for\"><SELRESTRS/></PREP><NP value=\"Theme\"><SYNRESTRS/></NP><PREP value=\"by\"><SELRESTRS/></PREP><NP value=\"Attribute\"><SYNRESTRS/></NP>','','','','');

-- A user searches the goals by name or genre.
insert into `vnframedef` (`frameid`,`description1`,`syntax`,`description2`,`number`,`semantics`,`xtag`) 
	values (2007,'NP-V-PP-NP-PP-NP','<NP value=\"Agent\"><SYNRESTRS/></NP><VERB/><NP value=\"Theme\"><SYNRESTRS/></NP><PREP value=\"by\"><SELRESTRS/></PREP><NP value=\"Attribute\"><SYNRESTRS/></NP>','','','','');

-- A user searches the goals for ones concerning performance requirements.
insert into `vnframedef` (`frameid`,`description1`,`syntax`,`description2`,`number`,`semantics`,`xtag`) 
	values (2008,'NP-V-NP-PP-NP-PP-NP','<NP value=\"Agent\"><SYNRESTRS/></NP><VERB/><NP value=\"Theme\"><SYNRESTRS/></NP><PREP value=\"for\"><SELRESTRS/></PREP><NP value=\"Extent\"><SYNRESTRS/></NP><PREP><SELRESTRS/></PREP><NP value=\"Attribute\"><SYNRESTRS/></NP>','','','','');
	
-- A user searches the goals for performance requirements.
insert into `vnframedef` (`frameid`,`description1`,`syntax`,`description2`,`number`,`semantics`,`xtag`) 
	values (2009,'NP-V-PP-NP-PP-NP','<NP value=\"Agent\"><SYNRESTRS/></NP><VERB/><NP value=\"Location\"><SYNRESTRS/></NP><PREP value=\"for\"><SELRESTRS/></PREP><NP value=\"Attribute\"><SYNRESTRS/></NP>','','','','');

UNLOCK TABLES;

/*Data for the table `vnroleref` */

LOCK TABLES `vnroleref` WRITE;

-- enter-data for Agent
insert into `vnroleref` (`rolerefid`,`classid`,`roletypeid`,`selrestrs`) 
	values (1002,1000,1,'<SELRESTRS><SELRESTR Value="+" type="animate"/></SELRESTRS>');

-- enter-data for Theme
insert into `vnroleref` (`rolerefid`,`classid`,`roletypeid`,`selrestrs`) 
	values (1003,1000,2,'<SELRESTRS><SELRESTR Value="+" type="abstract"/></SELRESTRS>');
	
-- enter-data for Instrument
insert into `vnroleref` (`rolerefid`,`classid`,`roletypeid`,`selrestrs`) 
	values (1004,1000,19,'<SELRESTRS/>');
	
-- search-data for Agent
insert into `vnroleref` (`rolerefid`,`classid`,`roletypeid`,`selrestrs`) 
	values (1005,1001,1,'<SELRESTRS><SELRESTR Value="+" type="animate"/></SELRESTRS>');

-- search-data for Theme
insert into `vnroleref` (`rolerefid`,`classid`,`roletypeid`,`selrestrs`) 
	values (1006,1001,2,'<SELRESTRS/>');
	
-- search-data for Attribute
insert into `vnroleref` (`rolerefid`,`classid`,`roletypeid`,`selrestrs`) 
	values (1007,1001,14,'<SELRESTRS/>');
	
-- search-data for Extent
insert into `vnroleref` (`rolerefid`,`classid`,`roletypeid`,`selrestrs`) 
	values (1008,1001,24,'<SELRESTRS/>');

update vnroleref set 
	selrestrs = '<SELRESTRS><SELRESTR Value="+" type="location"/></SELRESTRS>'
	where rolerefid = 541;

UNLOCK TABLES;

/*Data for the table `vnframeref` */

LOCK TABLES `vnframeref` WRITE;

-- enter#8
insert into `vnframeref` (`framerefid`,`frameid`,`synsetid`,`wordid`, `classid`) 
	values (40000,2000,201421622,42659, 1000);

-- enter#8
insert into `vnframeref` (`framerefid`,`frameid`,`synsetid`,`wordid`, `classid`) 
	values (40001,2001,201421622,42659, 1000);

-- enter#8
insert into `vnframeref` (`framerefid`,`frameid`,`synsetid`,`wordid`, `classid`) 
	values (40002,2002,201421622,42659, 1000);

-- create#1
insert into `vnframeref` (`framerefid`,`frameid`,`synsetid`,`wordid`, `classid`) 
	values (40003,2003,201617192,31309, 130);

-- bill#1
insert into `vnframeref` (`framerefid`,`frameid`,`synsetid`,`wordid`, `classid`) 
	values (40004,2004,202320374,13899, 38);

-- search#1
insert into `vnframeref` (`framerefid`,`frameid`,`synsetid`,`wordid`, `classid`) 
	values (40005,2005,201315613,116556, 1001);

-- search#2
insert into `vnframeref` (`framerefid`,`frameid`,`synsetid`,`wordid`, `classid`) 
	values (40006,2005,202153709,116556, 1001);

-- search#1
insert into `vnframeref` (`framerefid`,`frameid`,`synsetid`,`wordid`, `classid`) 
	values (40007,2006,201315613,116556, 1001);

-- search#2
insert into `vnframeref` (`framerefid`,`frameid`,`synsetid`,`wordid`, `classid`) 
	values (40008,2006,202153709,116556, 1001);

-- search#1
insert into `vnframeref` (`framerefid`,`frameid`,`synsetid`,`wordid`, `classid`) 
	values (40009,2007,201315613,116556, 1001);

-- search#2
insert into `vnframeref` (`framerefid`,`frameid`,`synsetid`,`wordid`, `classid`) 
	values (40010,2007,202153709,116556, 1001);

-- search#1
insert into `vnframeref` (`framerefid`,`frameid`,`synsetid`,`wordid`, `classid`) 
	values (40011,2008,201315613,116556, 1001);

-- search#2
insert into `vnframeref` (`framerefid`,`frameid`,`synsetid`,`wordid`, `classid`) 
	values (40012,2008,202153709,116556, 1001);
	
-- search#1
insert into `vnframeref` (`framerefid`,`frameid`,`synsetid`,`wordid`, `classid`) 
	values (40013,2009,201315613,116556, 1001);

-- search#2
insert into `vnframeref` (`framerefid`,`frameid`,`synsetid`,`wordid`, `classid`) 
	values (40014,2009,202153709,116556, 1001);

-- add#1
insert into `vnframeref` (`framerefid`,`frameid`,`synsetid`,`wordid`, `classid`) 
	values (40015,2003,200182406,1805, 1000);

-- input#1
insert into `vnframeref` (`framerefid`,`frameid`,`synsetid`,`wordid`, `classid`) 
	values (40016,2000,201422539,69460, 1000);

-- input#1
insert into `vnframeref` (`framerefid`,`frameid`,`synsetid`,`wordid`, `classid`) 
	values (40017,2001,201422539,69460, 1000);

-- input#1
insert into `vnframeref` (`framerefid`,`frameid`,`synsetid`,`wordid`, `classid`) 
	values (40018,2002,201422539,69460, 1000);

UNLOCK TABLES;

SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
