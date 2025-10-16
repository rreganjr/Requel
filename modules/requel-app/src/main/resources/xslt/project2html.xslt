<?xml version="1.0" encoding="UTF-8"?>
<!-- $Id: project2html.xslt,v 1.9 2009/03/06 02:06:45 rregan Exp $
 
 An XSLT for rendering a Requel Project xml file as an html document.
 
 -->
<xsl:stylesheet version="1.0" xmlns="http://www.w3.org/1999/xhtml"
	xmlns:rp="http://www.rreganjr.com/requel" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xs="http://www.w3.org/2001/XMLSchema"
	xmlns:xi="http://www.w3.org/2001/XInclude" exclude-result-prefixes="xs xsi rp xsl xi">

	<xsl:output method="xml" encoding="UTF-8" indent="yes" />

	<xsl:template match="/rp:project">
		<html>
			<head>
				<title>
					<xsl:value-of select="rp:name" />
				</title>
				<xsl:call-template name="css"/>
			</head>
			<body>
				<div class="project">
					<h1>Requirements for &quot;<xsl:value-of select="rp:name" />&quot;</h1>
					<h5>Client: <xsl:value-of select="rp:organization/@name" /></h5>
					<h5>Revision: <xsl:value-of select="@revision" /></h5>
					<h4>Table of Contents</h4>
					<div class="toc">
						<ul>
							<xsl:if test="count(rp:stakeholders/rp:stakeholder) > 0">
								<li><a href="#stakeholders">Stakeholders</a>
								<ol>
									<xsl:apply-templates select="rp:stakeholders/rp:stakeholder" mode="summary"/>
								</ol>								
								</li>
							</xsl:if>
							<xsl:if test="count(rp:teams/rp:team) > 0">
								<li><a href="#teams">Teams</a>
								<ol>
									<xsl:apply-templates select="rp:teams/rp:team" mode="summary"/>
								</ol>								
								</li>
							</xsl:if>
							<xsl:if test="count(rp:actors/rp:actor) > 0">
								<li><a href="#actors">Actors</a>
								<ol>
									<xsl:apply-templates select="rp:actors/rp:actor" mode="summary"/>
								</ol>								
								</li>
							</xsl:if>
							<xsl:if test="count(rp:goals/rp:goal) > 0">
								<li><a href="#goals">Goals</a>
								<ol>
									<xsl:apply-templates select="rp:goals/rp:goal" mode="summary"/>
								</ol>								
								</li>
							</xsl:if>
							<xsl:if test="count(rp:stories/rp:story) > 0">
								<li><a href="#stories">Stories</a>
								<ol>
									<xsl:apply-templates select="rp:stories/rp:story" mode="summary"/>
								</ol>								
								</li>
							</xsl:if>
							<xsl:if test="count(rp:usecases/rp:usecase) > 0">
								<li><a href="#usecases">Use Cases</a>
								<ol>
									<xsl:apply-templates select="rp:usecases/rp:usecase" mode="summary"/>
								</ol>								
								</li>
							</xsl:if>
							<xsl:if test="count(rp:scenarios/rp:scenario) > 0">
								<li><a href="#scenarios">Scenarios</a>
								<ol>
									<xsl:apply-templates select="rp:scenarios/rp:scenario" mode="summary"/>
								</ol>								
								</li>
							</xsl:if>
							<xsl:if test="count(rp:glossary/rp:term) > 0">
								<li><a href="#glossary">Glossary</a>
								<ol>
									<xsl:apply-templates select="rp:glossary/rp:term" mode="summary"/>
								</ol>								
								</li>
							</xsl:if>
						</ul>
					</div>
					<div class="body">
						<xsl:apply-templates select="rp:stakeholders" />
						<xsl:apply-templates select="rp:teams" />
						<xsl:apply-templates select="rp:actors" />
						<xsl:apply-templates select="rp:goals" />
						<xsl:apply-templates select="rp:stories" />
						<xsl:apply-templates select="rp:usecases" />
						<xsl:apply-templates select="rp:scenarios" />
						<xsl:apply-templates select="rp:glossary" />
					</div>
				</div>
			</body>
		</html>
	</xsl:template>

	<xsl:template match="rp:stakeholder[count(rp:user/rp:username) = 0]|rp:team|rp:actor|rp:goal|rp:story|rp:usecase|rp:scenario|rp:term" mode="summary">
		<li>
			<a href="#{generate-id(.)}">
				<xsl:value-of select="rp:name" />
			</a>
		</li>
	</xsl:template>
	<xsl:template match="rp:stakeholder[count(rp:user/rp:username) > 0]" mode="summary">
		<li>
			<a href="#{generate-id(.)}">
				<xsl:value-of select="rp:user/rp:name" /> [<xsl:value-of select="rp:user/rp:username" />]
			</a>
		</li>
	</xsl:template>
	
	<!-- Stakeholders -->
	<xsl:template match="rp:stakeholders">
		<xsl:if test="count(rp:stakeholder) > 0">
			<div class="stakeholders">
				<a name="stakeholders"></a>
				<h2>Stakeholders</h2>
				<table class="stakeholders">
					<thead>
						<tr>
							<th>User?</th>
							<th>Name</th>
							<th>Organization</th>
							<th>Email</th>
							<th>Phone</th>
						</tr>
					</thead>
					<tbody>
						<xsl:apply-templates select="rp:stakeholder" />
					</tbody>
				</table>
			</div>
		</xsl:if>
	</xsl:template>

	<xsl:template match="rp:stakeholder[count(rp:user/rp:username) > 0]">
		<tr>
			<td>
				Yes
			</td>
			<td>
				<a name="{generate-id(.)}">
					<xsl:value-of select="rp:user/rp:name" />
					(<xsl:value-of select="rp:user/rp:username" />)
				</a>
			</td>
			<td>
				<xsl:apply-templates select="rp:user/rp:organization/@name" />
			</td>
			<td>
				<xsl:apply-templates select="rp:user/rp:emailAddress" />
			</td>
			<td>
				<xsl:apply-templates select="rp:user/rp:phoneNumber" />
			</td>
		</tr>
		<xsl:if test="count(rp:goals/*) > 0">
			<tr>
				<td></td>
				<td colspan="4">
					<xsl:apply-templates select="rp:goals" />
				</td>
			</tr>
		</xsl:if>
		<xsl:if test="count(rp:annotations/*) > 0">
			<tr>
				<td></td>
				<td colspan="4">
					<xsl:apply-templates select="rp:annotations" />
				</td>
			</tr>
		</xsl:if>
	</xsl:template>

	<xsl:template match="rp:stakeholder">
		<tr>
			<td>
				No
			</td>
			<td>
				<a name="{generate-id(.)}">
					<xsl:value-of select="rp:name" />
				</a>
			</td>
			<td>
				<xsl:apply-templates select="rp:organization/@name" />
			</td>
			<td>
			</td>
			<td>
			</td>
		</tr>
		<xsl:if test="count(rp:goals/*) > 0">
			<tr>
				<td></td>
				<td colspan="4">
					<xsl:apply-templates select="rp:goals" />
				</td>
			</tr>
		</xsl:if>
		<xsl:if test="count(rp:annotations/*) > 0">
			<tr>
				<td></td>
				<td colspan="4">
					<xsl:apply-templates select="rp:annotations" />
				</td>
			</tr>
		</xsl:if>
	</xsl:template>

	<xsl:template match="rp:stakeholderRef">
		<xsl:variable name="ref-id" select="." />
		<li class="stakeholder">
			<span class="body">
				<a href="#{generate-id(//rp:stakeholder[@id = $ref-id])}">
					<xsl:apply-templates select="//rp:stakeholder[@id = $ref-id]/rp:name" />
				</a>
			</span>
		</li>
	</xsl:template>

	<!-- Teams -->
	<xsl:template match="rp:teams">
		<xsl:if test="count(rp:team) > 0">
			<a name="teams"></a>
			<h2>Teams</h2>
			<div class="body">
				<xsl:apply-templates select="*"/>
			</div>
		</xsl:if>
	</xsl:template>

	<xsl:template match="rp:team">
		<h2><xsl:value-of select="rp:name"/></h2>
		<div class="body">
			<xsl:apply-templates select="rp:members"/>
			<xsl:apply-templates select="rp:annotations" />
		</div>
	</xsl:template>

	<!-- Actors -->
	<xsl:template match="rp:project/rp:actors">
		<xsl:if test="count(rp:actor) > 0">
			<a name="actors"></a>
			<h2>Actors</h2>
			<div class="body">
				<xsl:apply-templates select="*" />
			</div>
		</xsl:if>
	</xsl:template>

	<xsl:template match="rp:actors">
		<xsl:if test="count(*) > 0">
			<h4>Actors</h4>
			<div class="body">
				<xsl:apply-templates select="*" />
			</div>
		</xsl:if>
	</xsl:template>

	<xsl:template match="rp:actor">
		<div class="actor">
			<h4>
				<a name="{generate-id(.)}">
					<xsl:value-of select="rp:name" />
				</a>
			</h4>
			<div class="body">
				<xsl:apply-templates select="rp:text" />
				<xsl:apply-templates select="rp:goals" />
				<xsl:apply-templates select="rp:annotations" />
			</div>
		</div>
	</xsl:template>

	<xsl:template match="rp:actorRef">
		<xsl:variable name="ref-id" select="." />
		<li class="actor">
			<span class="body">
				<a href="#{generate-id(//rp:actor[@id = $ref-id])}">
					<xsl:apply-templates select="//rp:actor[@id = $ref-id]/rp:name" />
				</a>
			</span>
		</li>
	</xsl:template>

	<xsl:template match="rp:primaryActorRef">
		<xsl:variable name="ref-id" select="." />
		<a href="#{generate-id(//rp:actor[@id = $ref-id])}">
			<xsl:apply-templates select="//rp:actor[@id = $ref-id]/rp:name" />
		</a>
	</xsl:template>

	<!-- Goals -->
	<xsl:template match="rp:project/rp:goals">
		<xsl:if test="count(rp:goal) > 0">
			<div class="goals">
				<a name="goals"></a>
				<h2>Goals</h2>
				<ul class="goals">
					<xsl:apply-templates select="*" />
				</ul>
			</div>
		</xsl:if>
	</xsl:template>

	<xsl:template match="rp:goals">
		<xsl:if test="count(*) > 0">
			<h4>Goals</h4>
			<ul class="goals">
				<xsl:apply-templates select="*" />
			</ul>
		</xsl:if>
	</xsl:template>

	<xsl:template match="rp:goal">
		<li class="goal">
			<div class="goal">
				<h4>
					<a name="{generate-id(.)}">
						<xsl:value-of select="rp:name" />
					</a>
				</h4>
				<div class="body">
					<div>
						<xsl:value-of select="rp:text" />
					</div>
					<xsl:apply-templates select="rp:goalRelations" />
					<xsl:apply-templates select="rp:annotations" />
				</div>
			</div>
		</li>
	</xsl:template>

	<xsl:template match="rp:goalRef">
		<xsl:variable name="ref-id" select="." />
		<li class="goal">
			<span class="body">
				<a href="#{generate-id(//rp:goal[@id = $ref-id])}">
					<xsl:apply-templates select="//rp:goal[@id = $ref-id]/rp:name" />
				</a>
			</span>
		</li>
	</xsl:template>

	<xsl:template match="rp:goalRelations">
		<xsl:if test="count(rp:goalRelation) > 0">
			<h4>Goal Relations</h4>
			<ul>
				<xsl:apply-templates select="rp:goalRelation" />
			</ul>
		</xsl:if>
	</xsl:template>

	<xsl:template match="rp:goalRelation">
		<xsl:variable name="ref-id" select="@toGoal" />
		<li class="goal">
			<div>
				<xsl:value-of select="@relationType" /> -
				<a href="#{generate-id(//rp:goal[@id = $ref-id])}">
					<xsl:apply-templates select="//rp:goal[@id = $ref-id]/rp:name" />
				</a>
			</div>
			<xsl:apply-templates select="rp:annotations" />
		</li>
	</xsl:template>

	<!-- Stories -->
	<xsl:template match="rp:project/rp:stories">
		<xsl:if test="count(rp:story) > 0">
			<a name="stories"></a>
			<h2>Stories</h2>
			<div class="body">
				<xsl:apply-templates select="*" />
			</div>
		</xsl:if>
	</xsl:template>

	<xsl:template match="rp:stories">
		<xsl:if test="count(*) > 0">
			<h4>Stories</h4>
			<xsl:choose>
				<xsl:when test="count(rp:story) > 0">
					<div class="body">
						<xsl:apply-templates select="rp:story" />
					</div>
				</xsl:when>
				<xsl:when test="count(rp:storyRef) > 0">
					<div class="body">
						<ul>
							<xsl:for-each select="rp:storyRef">
								<li><xsl:apply-templates select="." /></li>
							</xsl:for-each>
						</ul>
					</div>
				</xsl:when>
			</xsl:choose>
		</xsl:if>
	</xsl:template>

	<xsl:template match="rp:story">
		<div class="story">
			<h4>
				<a name="{generate-id(.)}">
					<xsl:value-of select="@storyType" />
					Story -
					<xsl:value-of select="rp:name" />
				</a>
			</h4>
			<div class="body">
				<xsl:apply-templates select="rp:text" />
			</div>
			<xsl:apply-templates select="rp:actors" />
			<xsl:apply-templates select="rp:goals" />
			<xsl:apply-templates select="rp:annotations" />
		</div>
	</xsl:template>

	<xsl:template match="rp:storyRef">
		<xsl:variable name="ref-id" select="." />
		<a href="#{generate-id(//rp:story[@id = $ref-id])}">
			<xsl:apply-templates select="//rp:story[@id = $ref-id]/rp:name" />
		</a>
	</xsl:template>

	<!-- Use Cases -->
	<xsl:template match="rp:project/rp:usecases">
		<xsl:if test="count(rp:usecase) > 0">
			<a name="usecases"></a>
			<h2>Use Cases</h2>
			<div class="body">
				<xsl:apply-templates select="*" />
			</div>
		</xsl:if>
	</xsl:template>

	<xsl:template match="rp:usecases">
		<xsl:if test="count(*) > 0">
			<h4>Use Cases</h4>
			<div class="body">
				<xsl:apply-templates select="rp:usecase" />
			</div>
		</xsl:if>
	</xsl:template>

	<xsl:template match="rp:usecaseRef">
		<xsl:variable name="ref-id" select="." />
		<a href="#{generate-id(//rp:usecase[@id = $ref-id])}">
			<xsl:apply-templates select="//rp:usecase[@id = $ref-id]/rp:name" />
		</a>
	</xsl:template>

	<xsl:template match="rp:usecase">
		<div class="usecase">
			<h4>
				<a name="{generate-id(.)}">
					<xsl:value-of select="rp:name" />
				</a>
			</h4>
			<div class="body">
				<xsl:if test="string-length(rp:text) > 0">
					<h4>Description</h4>
					<xsl:apply-templates select="rp:text" />
				</xsl:if>
				<xsl:apply-templates select="rp:goals" />
				<xsl:apply-templates select="rp:stories" />
				<h4>Primary Actor: <xsl:apply-templates select="rp:primaryActorRef" /></h4>
				<xsl:apply-templates select="rp:actors" />
				<h4>Scenario</h4>
				<xsl:variable name="scenarioRef" select="string(rp:scenarioRef)" />
				<xsl:apply-templates select="//rp:scenario[@id = $scenarioRef]" mode="usecase" />
				<xsl:apply-templates select="rp:annotations" />
			</div>
		</div>
	</xsl:template>

	<!-- Scenarios - only include scenarios that aren't part of another scenario or use case -->
	<xsl:template match="rp:project/rp:scenarios">
		<xsl:variable name="rootScenariosExist">
			<xsl:for-each select="rp:scenario">
				<xsl:variable name="scenarioId" select="@id"/>
				<xsl:choose>
					<xsl:when test="count(//rp:scenarioRef[string(text()) = $scenarioId]) = 0 and count(//rp:stepRef[string(text()) = $scenarioId]) = 0"><xsl:value-of select="concat(./@id,' ')"/></xsl:when>
				</xsl:choose>
			</xsl:for-each>
		</xsl:variable>
		<xsl:comment> <xsl:value-of select="$rootScenariosExist"/> </xsl:comment>
		<xsl:if test="string-length($rootScenariosExist) > 0">
			<a name="scenarios"></a>
			<h2>Scenarios (not used else where)</h2>
			<div class="body">
				<xsl:for-each select="rp:scenario">
					<xsl:variable name="scenarioId" select="@id"/>
					<xsl:if test="count(//rp:scenarioRef[string(text()) = $scenarioId]) = 0 and count(//rp:stepRef[string(text()) = $scenarioId]) = 0">
						<xsl:apply-templates select="." />
					</xsl:if>
				</xsl:for-each>
			</div>
		</xsl:if>
	</xsl:template>

	<xsl:template match="rp:scenarios">
		<xsl:if test="count(*) > 0">
			<h4>Scenarios</h4>
			<div class="body">
				<xsl:apply-templates select="rp:scenario" />
			</div>
		</xsl:if>
	</xsl:template>

	<xsl:template match="rp:scenario" mode="usecase">
		<div class="scenario">
			<xsl:apply-templates select="rp:steps" />
		</div>
	</xsl:template>

	<xsl:template match="rp:scenario">
		<div class="scenario">
			<h4>
				<a name="{generate-id(.)}">
				<xsl:value-of select="@scenarioType" />
				-
				<xsl:value-of select="rp:name" />
				</a>
			</h4>
			<xsl:if test="string-length(rp:text) > 0">
				<h4>Description</h4>
				<xsl:apply-templates select="rp:text" />
			</xsl:if>
			<xsl:apply-templates select="rp:steps" />
			<xsl:apply-templates select="rp:annotations" />			
		</div>
	</xsl:template>

	<xsl:template match="rp:steps">
		<ol class="scenario">
			<xsl:for-each select="rp:stepRef">
				<li class="step"><xsl:apply-templates select="."/></li>
			</xsl:for-each>
		</ol>
	</xsl:template>

	<xsl:template match="rp:stepRef">
		<xsl:variable name="stepRefId" select="text()" />
		<xsl:apply-templates select="//rp:scenario[@id = $stepRefId]|//rp:step[@id = $stepRefId]" mode="steps"/>
	</xsl:template>
	
	<!-- Steps and embedded scenarios -->
	<xsl:template match="rp:scenario" mode="steps">
		<xsl:value-of select="@scenarioType" /> - <xsl:value-of select="rp:name" />
		<xsl:apply-templates select="rp:steps" />
	</xsl:template>
	<xsl:template match="rp:step" mode="steps">
		<xsl:value-of select="@scenarioType" /> - <xsl:value-of select="rp:name" />
	</xsl:template>

	<!-- Glossary -->
	<xsl:template match="rp:project/rp:glossary">
		<xsl:if test="count(rp:term) > 0">
			<a name="glossary"></a>
			<h2>Glossary</h2>
			<div class="body">
				<table class="stakeholders">
					<thead>
						<tr>
							<th>Term</th>
							<th>Definition</th>
						</tr>
					</thead>
					<tbody>
						<xsl:apply-templates select="rp:term" />
					</tbody>
				</table>
			</div>
		</xsl:if>
	</xsl:template>

	<xsl:template match="rp:term">
		<tr>
			<td><xsl:value-of select="rp:name" /></td>
			<td><xsl:value-of select="rp:text" /></td>
		</tr>
	</xsl:template>	
	
	<!-- Annotations -->
	<xsl:template match="rp:annotations">
<!-- 
		<xsl:if test="count(*) > 0">
			<h4>Annotations</h4>
			<ul>
				<xsl:apply-templates select="rp:note|rp:issue|rp:lexicalIssue" />
			</ul>
		</xsl:if>
 -->		
	</xsl:template>

	<xsl:template match="rp:note">
		<li class="note">
			<xsl:apply-templates select="rp:text" />
		</li>
	</xsl:template>

	<xsl:template match="rp:issue|rp:lexicalIssue">
		<li class="issue">
			<xsl:apply-templates select="rp:text" />
			<xsl:apply-templates select="rp:positions" />
		</li>
	</xsl:template>

	<xsl:template match="rp:positions">
		<xsl:if test="count(*) > 0">
			<h4>Solutions</h4>
			<div class="body">
				<ul>
					<xsl:apply-templates select="*" />
				</ul>
			</div>
		</xsl:if>
	</xsl:template>

	<xsl:template match="rp:position|rp:addWordToDictionaryPosition|rp:changeSpellingPosition|rp:addGlossaryTermPosition|rp:addActorPosition">
		<li class="position">
			<xsl:apply-templates select="rp:text" />
			<xsl:apply-templates select="rp:arguments" />
		</li>
	</xsl:template>

	<xsl:template match="rp:arguments">
		<xsl:if test="count(*) > 0">
			<h4>Arguments</h4>
			<div class="body">
				<h5>Strongly For</h5>
				<xsl:apply-templates select="rp:argument[@supportLevel = 'StronglyFor']" />
				<h5>For</h5>
				<xsl:apply-templates select="rp:argument[@supportLevel = 'For']" />
				<h5>Neutral</h5>
				<xsl:apply-templates select="rp:argument[@supportLevel = 'Neutral']" />
				<h5>Against</h5>
				<xsl:apply-templates select="rp:argument[@supportLevel = 'Against']" />
				<h5>Strongly Against</h5>
				<xsl:apply-templates select="rp:argument[@supportLevel = 'StronglyAgainst']" />
			</div>
		</xsl:if>
	</xsl:template>

	<xsl:template match="rp:argument">
		<div class="argument">
			<xsl:apply-templates select="rp:text" />
		</div>
	</xsl:template>


	<!-- CSS style -->
	<xsl:template name="css">
		<style type="text/css">
			html {
				height: 100%;
			}
			
			body {
				height: 100%;
				font-family: Arial;
				font-size: 10pt;
				margin-top: .5in;
				margin-left: .5in;
				margin-right: .5in;
				margin-bottom: .5in;
			}
			
			h1 {
				text-align: center;
				font-family: Arial;
				font-size: 14pt;
			}
			
			h2 {
				text-align: left;
				font-family: Arial;
				font-size: 12pt;
			}
			
			h3, h4, h5, h6 {
				text-align: left;
				font-family: Arial;
				font-size: 11pt;
			}
			
			
			table {
				border: solid 1pt black;
				border-collapse: collapse;
				empty-cells: show;
			}
			
			
			td {
				padding: 5pt;
				border: solid 1pt black;
				border-collapse: collapse;
				font-family: Arial;
				font-size: 10pt;
				vertical-align: top;
				text-align: left;
				font-weight: normal;
			}
			
			div .project .body {
				margin: 0;
			}
			
			div .body {
				margin-top: .25in;
				margin-left: .25in;
				margin-right: .25in;
				margin-bottom: .25in;
			}
			
			.stakeholders table {
				margin-top: .25in;
				margin-left: .25in;
				margin-right: .25in;
				margin-bottom: .25in;
			}
			
			th {
				padding: 4pt;
				border: solid 1pt black;
				border-collapse: collapse;
				font-family: Arial;
				font-size: 10pt;
				vertical-align: bottom;
				text-align: left;
				font-weight: bold;
			}
			
			.issue {
			}
			
			.note {
			}
			
			.p {
				padding-bottom: 10pt;
			}
		</style>	
	</xsl:template>
</xsl:stylesheet>