<?xml version="1.0" encoding="UTF-8"?>
<!--
  Issue #275: render a Requel project as the structural half of a ticket, in Markdown.

  The section contract (which sections are structural, which are narrative, and what each one
  holds) is doc/guides/emit-ticket-format.md. This template is the structural half; the narrative
  sections (problem statement, blocking check, intentional behaviour, open-questions prose) are
  not generated here.

  Rules that keep the output byte-identical for an unchanged project:
  - every list is sorted by the numeric part of its id (creation order), never by export order;
  - no dates, no generate-id();
  - names and text go through normalize-space, so the export's indentation never leaks in.

  Every IDREF is resolved through the by-id key and the "require" template: a reference that
  resolves to nothing stops the transform with a message naming it, rather than emitting a gap.
-->
<xsl:stylesheet version="1.0" xmlns:rp="http://www.rreganjr.com/requel"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" exclude-result-prefixes="rp">

	<xsl:output method="text" encoding="UTF-8" media-type="text/markdown" />

	<!-- The project version (#275 content fingerprint), passed in by the run path. -->
	<xsl:param name="projectVersion" select="''" />

	<xsl:variable name="nl" select="'&#10;'" />

	<xsl:key name="by-id" match="*[@id]" use="@id" />
	<!-- Entities carrying an annotation, by the annotation's id. -->
	<xsl:key name="annotated" match="*[rp:annotations/rp:annotationRef]"
		use="rp:annotations/rp:annotationRef" />

	<xsl:variable name="project" select="/rp:project" />
	<xsl:variable name="issues" select="$project/rp:annotations/rp:issue | $project/rp:annotations/rp:lexicalIssue" />
	<xsl:variable name="unresolved" select="$issues[string-length(@resolvedByPosition) = 0]" />
	<xsl:variable name="open" select="$unresolved[@mustBeResolved = 'true']" />
	<xsl:variable name="advisory" select="$unresolved[not(@mustBeResolved = 'true')]" />

	<xsl:template match="/rp:project">
		<xsl:text># </xsl:text><xsl:value-of select="normalize-space(rp:name)" /><xsl:value-of select="$nl" />
		<xsl:value-of select="$nl" />
		<xsl:text>Project version: </xsl:text>
		<xsl:choose>
			<xsl:when test="string-length($projectVersion) > 0"><xsl:value-of select="$projectVersion" /></xsl:when>
			<xsl:otherwise>unknown</xsl:otherwise>
		</xsl:choose>
		<xsl:value-of select="$nl" />
		<!-- Always say what the sources are, including that there are none: a reader cannot tell
		     "no sources recorded" from "the generator left them out" otherwise. -->
		<xsl:text>Sources: </xsl:text>
		<xsl:choose>
			<xsl:when test="rp:externalSources/rp:externalSource">
				<xsl:for-each select="rp:externalSources/rp:externalSource">
					<xsl:sort select="@system" />
					<xsl:sort select="@externalId" />
					<xsl:if test="position() > 1"><xsl:text>, </xsl:text></xsl:if>
					<xsl:call-template name="source-label" />
				</xsl:for-each>
			</xsl:when>
			<xsl:otherwise>none recorded</xsl:otherwise>
		</xsl:choose>
		<xsl:value-of select="$nl" />

		<xsl:call-template name="goals" />
		<xsl:call-template name="actors" />
		<xsl:call-template name="usecases" />
		<xsl:call-template name="other-scenarios" />
		<xsl:call-template name="glossary" />
		<xsl:call-template name="open-issues" />
		<xsl:call-template name="resources" />
		<xsl:call-template name="traceability" />
	</xsl:template>

	<!-- Goals: numbered, name and text. -->
	<xsl:template name="goals">
		<xsl:if test="rp:goals/rp:goal">
			<xsl:value-of select="$nl" /><xsl:text>## Goals</xsl:text><xsl:value-of select="$nl" /><xsl:value-of select="$nl" />
			<xsl:for-each select="rp:goals/rp:goal">
				<xsl:sort select="substring-after(@id, '_')" data-type="number" />
				<xsl:value-of select="position()" /><xsl:text>. **</xsl:text>
				<xsl:value-of select="normalize-space(rp:name)" /><xsl:text>**</xsl:text>
				<xsl:call-template name="dash-text" />
				<xsl:value-of select="$nl" />
			</xsl:for-each>
		</xsl:if>
	</xsl:template>

	<!-- Actors -->
	<xsl:template name="actors">
		<xsl:if test="rp:actors/rp:actor">
			<xsl:value-of select="$nl" /><xsl:text>## Actors</xsl:text><xsl:value-of select="$nl" /><xsl:value-of select="$nl" />
			<xsl:for-each select="rp:actors/rp:actor">
				<xsl:sort select="substring-after(@id, '_')" data-type="number" />
				<xsl:text>- **</xsl:text><xsl:value-of select="normalize-space(rp:name)" /><xsl:text>**</xsl:text>
				<xsl:call-template name="dash-text" />
				<xsl:value-of select="$nl" />
			</xsl:for-each>
		</xsl:if>
	</xsl:template>

	<!-- Use cases, each with its primary and additional scenarios and their ordered steps. -->
	<xsl:template name="usecases">
		<xsl:if test="rp:usecases/rp:usecase">
			<xsl:value-of select="$nl" /><xsl:text>## Use Cases</xsl:text><xsl:value-of select="$nl" />
			<xsl:for-each select="rp:usecases/rp:usecase">
				<xsl:sort select="substring-after(@id, '_')" data-type="number" />
				<xsl:value-of select="$nl" />
				<xsl:text>### </xsl:text><xsl:value-of select="normalize-space(rp:name)" /><xsl:value-of select="$nl" />
				<xsl:if test="string-length(normalize-space(rp:text)) > 0">
					<xsl:value-of select="$nl" /><xsl:value-of select="normalize-space(rp:text)" /><xsl:value-of select="$nl" />
				</xsl:if>
				<xsl:value-of select="$nl" />
				<xsl:if test="rp:primaryActorRef">
					<xsl:text>Primary actor: </xsl:text>
					<xsl:call-template name="ref-name"><xsl:with-param name="ref" select="rp:primaryActorRef" /></xsl:call-template>
					<xsl:value-of select="$nl" />
				</xsl:if>
				<xsl:if test="rp:actors/rp:actorRef">
					<xsl:text>Actors: </xsl:text>
					<xsl:apply-templates select="rp:actors/rp:actorRef" mode="ref-list">
						<xsl:sort select="substring-after(., '_')" data-type="number" />
					</xsl:apply-templates>
					<xsl:value-of select="$nl" />
				</xsl:if>
				<xsl:if test="rp:goals/rp:goalRef">
					<xsl:text>Goals: </xsl:text>
					<xsl:apply-templates select="rp:goals/rp:goalRef" mode="ref-list">
						<xsl:sort select="substring-after(., '_')" data-type="number" />
					</xsl:apply-templates>
					<xsl:value-of select="$nl" />
				</xsl:if>
				<xsl:for-each select="rp:scenarioRef">
					<xsl:call-template name="scenario-block">
						<xsl:with-param name="ref" select="." />
						<xsl:with-param name="label" select="'Primary scenario'" />
					</xsl:call-template>
				</xsl:for-each>
				<xsl:for-each select="rp:additionalScenarios/rp:scenarioRef">
					<xsl:sort select="substring-after(., '_')" data-type="number" />
					<xsl:call-template name="scenario-block">
						<xsl:with-param name="ref" select="." />
						<xsl:with-param name="label" select="'Scenario'" />
					</xsl:call-template>
				</xsl:for-each>
			</xsl:for-each>
		</xsl:if>
	</xsl:template>

	<!-- Scenarios no use case and no other scenario uses, so every scenario appears somewhere. -->
	<xsl:template name="other-scenarios">
		<xsl:variable name="others" select="rp:scenarios/rp:scenario[
				not(@id = //rp:usecase/rp:scenarioRef)
				and not(@id = //rp:usecase/rp:additionalScenarios/rp:scenarioRef)
				and not(@id = //rp:scenario/rp:steps/rp:stepRef)]" />
		<xsl:if test="$others">
			<xsl:value-of select="$nl" /><xsl:text>## Other Scenarios</xsl:text><xsl:value-of select="$nl" />
			<xsl:for-each select="$others">
				<xsl:sort select="substring-after(@id, '_')" data-type="number" />
				<xsl:call-template name="scenario-block">
					<xsl:with-param name="ref" select="@id" />
					<xsl:with-param name="label" select="'Scenario'" />
				</xsl:call-template>
			</xsl:for-each>
		</xsl:if>
	</xsl:template>

	<xsl:template name="scenario-block">
		<xsl:param name="ref" />
		<xsl:param name="label" />
		<xsl:variable name="scenario" select="key('by-id', string($ref))" />
		<xsl:call-template name="require">
			<xsl:with-param name="nodes" select="$scenario" />
			<xsl:with-param name="what" select="concat('scenario ', $ref)" />
		</xsl:call-template>
		<xsl:value-of select="$nl" />
		<xsl:text>**</xsl:text><xsl:value-of select="$label" /><xsl:text>: </xsl:text>
		<xsl:value-of select="normalize-space($scenario/rp:name)" /><xsl:text>**</xsl:text>
		<xsl:value-of select="$nl" />
		<xsl:value-of select="$nl" />
		<xsl:choose>
			<xsl:when test="$scenario/rp:steps/rp:stepRef">
				<xsl:call-template name="steps">
					<xsl:with-param name="scenario" select="$scenario" />
					<xsl:with-param name="indent" select="''" />
				</xsl:call-template>
			</xsl:when>
			<!-- Say so, rather than leave a heading with nothing under it. -->
			<xsl:otherwise><xsl:text>_No steps recorded._</xsl:text><xsl:value-of select="$nl" /></xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- A scenario's steps in order; a sub-scenario is named and its steps nested under it. -->
	<xsl:template name="steps">
		<xsl:param name="scenario" />
		<xsl:param name="indent" />
		<xsl:for-each select="$scenario/rp:steps/rp:stepRef">
			<xsl:variable name="step" select="key('by-id', string(.))" />
			<xsl:call-template name="require">
				<xsl:with-param name="nodes" select="$step" />
				<xsl:with-param name="what" select="concat('step ', ., ' of scenario ', $scenario/@id)" />
			</xsl:call-template>
			<xsl:value-of select="$indent" /><xsl:value-of select="position()" /><xsl:text>. </xsl:text>
			<xsl:value-of select="normalize-space($step/rp:name)" />
			<xsl:value-of select="$nl" />
			<xsl:if test="local-name($step) = 'scenario'">
				<xsl:call-template name="steps">
					<xsl:with-param name="scenario" select="$step" />
					<xsl:with-param name="indent" select="concat($indent, '   ')" />
				</xsl:call-template>
			</xsl:if>
		</xsl:for-each>
	</xsl:template>

	<!-- Glossary: term, definition, and the canonical term an alternate stands for. -->
	<xsl:template name="glossary">
		<xsl:if test="rp:glossary/rp:term">
			<xsl:value-of select="$nl" /><xsl:text>## Glossary</xsl:text><xsl:value-of select="$nl" /><xsl:value-of select="$nl" />
			<xsl:for-each select="rp:glossary/rp:term">
				<xsl:sort select="substring-after(@id, '_')" data-type="number" />
				<xsl:text>- **</xsl:text><xsl:value-of select="normalize-space(rp:name)" /><xsl:text>**</xsl:text>
				<xsl:call-template name="dash-text" />
				<xsl:if test="string-length(@canonicalTerm) > 0">
					<xsl:text> (see **</xsl:text>
					<xsl:call-template name="ref-name"><xsl:with-param name="ref" select="@canonicalTerm" /></xsl:call-template>
					<xsl:text>**)</xsl:text>
				</xsl:if>
				<xsl:value-of select="$nl" />
			</xsl:for-each>
		</xsl:if>
	</xsl:template>

	<!-- Open issues: severity, then fresh before stale, then id. Advisory issues are counted in
	     Traceability only. -->
	<xsl:template name="open-issues">
		<xsl:if test="$open">
			<xsl:value-of select="$nl" /><xsl:text>## Open Issues</xsl:text><xsl:value-of select="$nl" /><xsl:value-of select="$nl" />
			<xsl:call-template name="issue-group"><xsl:with-param name="severity" select="'HIGH'" /><xsl:with-param name="stale" select="false()" /></xsl:call-template>
			<xsl:call-template name="issue-group"><xsl:with-param name="severity" select="'HIGH'" /><xsl:with-param name="stale" select="true()" /></xsl:call-template>
			<xsl:call-template name="issue-group"><xsl:with-param name="severity" select="'MEDIUM'" /><xsl:with-param name="stale" select="false()" /></xsl:call-template>
			<xsl:call-template name="issue-group"><xsl:with-param name="severity" select="'MEDIUM'" /><xsl:with-param name="stale" select="true()" /></xsl:call-template>
			<xsl:call-template name="issue-group"><xsl:with-param name="severity" select="'LOW'" /><xsl:with-param name="stale" select="false()" /></xsl:call-template>
			<xsl:call-template name="issue-group"><xsl:with-param name="severity" select="'LOW'" /><xsl:with-param name="stale" select="true()" /></xsl:call-template>
		</xsl:if>
	</xsl:template>

	<xsl:template name="issue-group">
		<xsl:param name="severity" />
		<xsl:param name="stale" />
		<!-- A missing severity reads as MEDIUM (#271). -->
		<xsl:for-each select="$open[(@severity = $severity or ($severity = 'MEDIUM' and string-length(@severity) = 0))
				and (string-length(normalize-space(@staleOn)) > 0) = $stale]">
			<xsl:sort select="substring-after(@id, '_')" data-type="number" />
			<xsl:variable name="issue" select="." />
			<xsl:text>- **</xsl:text><xsl:value-of select="$severity" /><xsl:text>** </xsl:text>
			<xsl:value-of select="normalize-space(rp:text)" />
			<xsl:variable name="on" select="key('annotated', @id)" />
			<xsl:if test="$on">
				<xsl:text> — on </xsl:text>
				<xsl:for-each select="$on">
					<xsl:sort select="local-name()" />
					<xsl:sort select="substring-after(@id, '_')" data-type="number" />
					<xsl:if test="position() > 1"><xsl:text>, </xsl:text></xsl:if>
					<xsl:call-template name="type-label" />
					<xsl:text> "</xsl:text><xsl:value-of select="normalize-space(rp:name)" /><xsl:text>"</xsl:text>
					<xsl:if test="contains(concat(' ', normalize-space($issue/@staleOn), ' '), concat(' ', @id, ' '))">
						<xsl:text> (stale: the entity changed since this was raised)</xsl:text>
					</xsl:if>
				</xsl:for-each>
			</xsl:if>
			<xsl:value-of select="$nl" />
		</xsl:for-each>
	</xsl:template>

	<!-- Resources (#273): every source with its kind, locator, note, precedence and the entities
	     derived from or citing it. -->
	<xsl:template name="resources">
		<xsl:if test="rp:externalSources/rp:externalSource">
			<xsl:value-of select="$nl" /><xsl:text>## Resources</xsl:text><xsl:value-of select="$nl" /><xsl:value-of select="$nl" />
			<xsl:for-each select="rp:externalSources/rp:externalSource">
				<xsl:sort select="@system" />
				<xsl:sort select="@externalId" />
				<xsl:text>- **</xsl:text><xsl:call-template name="source-label" /><xsl:text>**</xsl:text>
				<xsl:if test="string-length(@kind) > 0">
					<xsl:text> (</xsl:text><xsl:value-of select="@kind" /><xsl:text>)</xsl:text>
				</xsl:if>
				<xsl:text> — </xsl:text><xsl:value-of select="@system" /><xsl:text> </xsl:text><xsl:value-of select="@externalId" />
				<xsl:if test="string-length(@locator) > 0">
					<xsl:text>, </xsl:text><xsl:value-of select="@locator" />
				</xsl:if>
				<xsl:value-of select="$nl" />
				<xsl:if test="string-length(normalize-space(@note)) > 0">
					<xsl:text>  - Note: </xsl:text><xsl:value-of select="normalize-space(@note)" /><xsl:value-of select="$nl" />
				</xsl:if>
				<xsl:if test="rp:defersTo">
					<xsl:text>  - Defers to: </xsl:text>
					<xsl:for-each select="rp:defersTo">
						<xsl:sort select="@system" />
						<xsl:sort select="@externalId" />
						<xsl:variable name="system" select="@system" />
						<xsl:variable name="externalId" select="@externalId" />
						<xsl:variable name="superior" select="$project/rp:externalSources/rp:externalSource[@system = $system and @externalId = $externalId]" />
						<xsl:call-template name="require">
							<xsl:with-param name="nodes" select="$superior" />
							<xsl:with-param name="what" select="concat('source ', $system, ' ', $externalId)" />
						</xsl:call-template>
						<xsl:if test="position() > 1"><xsl:text>; </xsl:text></xsl:if>
						<xsl:for-each select="$superior"><xsl:call-template name="source-label" /></xsl:for-each>
						<xsl:if test="string-length(normalize-space(@note)) > 0">
							<xsl:text> (</xsl:text><xsl:value-of select="normalize-space(@note)" /><xsl:text>)</xsl:text>
						</xsl:if>
					</xsl:for-each>
					<xsl:value-of select="$nl" />
				</xsl:if>
				<xsl:call-template name="source-links"><xsl:with-param name="relation" select="'DERIVED_FROM'" /><xsl:with-param name="label" select="'Derived'" /></xsl:call-template>
				<xsl:call-template name="source-links"><xsl:with-param name="relation" select="'CITES'" /><xsl:with-param name="label" select="'Cited by'" /></xsl:call-template>
			</xsl:for-each>
		</xsl:if>
	</xsl:template>

	<xsl:template name="source-links">
		<xsl:param name="relation" />
		<xsl:param name="label" />
		<xsl:if test="rp:sourceLink[@relation = $relation]">
			<xsl:text>  - </xsl:text><xsl:value-of select="$label" /><xsl:text>: </xsl:text>
			<xsl:for-each select="rp:sourceLink[@relation = $relation]">
				<xsl:sort select="substring-after(@entityRef, '_')" data-type="number" />
				<xsl:sort select="@fragment" />
				<xsl:variable name="entity" select="key('by-id', string(@entityRef))" />
				<xsl:call-template name="require">
					<xsl:with-param name="nodes" select="$entity" />
					<xsl:with-param name="what" select="concat('entity ', @entityRef, ' linked from a source')" />
				</xsl:call-template>
				<xsl:if test="position() > 1"><xsl:text>, </xsl:text></xsl:if>
				<xsl:value-of select="normalize-space($entity/rp:name)" />
				<xsl:if test="string-length(@fragment) > 0">
					<xsl:text> (</xsl:text><xsl:value-of select="@fragment" /><xsl:text>)</xsl:text>
				</xsl:if>
			</xsl:for-each>
			<xsl:value-of select="$nl" />
		</xsl:if>
	</xsl:template>

	<!-- Traceability: counts per type and the source list. -->
	<xsl:template name="traceability">
		<xsl:value-of select="$nl" /><xsl:text>## Traceability</xsl:text><xsl:value-of select="$nl" /><xsl:value-of select="$nl" />
		<xsl:text>Requel project "</xsl:text><xsl:value-of select="normalize-space(rp:name)" /><xsl:text>": </xsl:text>
		<xsl:value-of select="count(rp:goals/rp:goal)" /><xsl:text> goals, </xsl:text>
		<xsl:value-of select="count(rp:actors/rp:actor)" /><xsl:text> actors, </xsl:text>
		<xsl:value-of select="count(rp:usecases/rp:usecase)" /><xsl:text> use cases, </xsl:text>
		<xsl:value-of select="count(rp:scenarios/rp:scenario)" /><xsl:text> scenarios, </xsl:text>
		<xsl:value-of select="count(rp:scenarios/rp:step)" /><xsl:text> steps, </xsl:text>
		<xsl:value-of select="count(rp:stories/rp:story)" /><xsl:text> stories, </xsl:text>
		<xsl:value-of select="count(rp:glossary/rp:term)" /><xsl:text> glossary terms, </xsl:text>
		<xsl:value-of select="count($open)" /><xsl:text> open issues (</xsl:text>
		<xsl:value-of select="count($open[string-length(normalize-space(@staleOn)) > 0])" /><xsl:text> stale), </xsl:text>
		<xsl:value-of select="count($advisory)" /><xsl:text> advisory issues.</xsl:text>
		<xsl:value-of select="$nl" />
		<xsl:if test="rp:externalSources/rp:externalSource">
			<xsl:text>Sources: </xsl:text>
			<xsl:for-each select="rp:externalSources/rp:externalSource">
				<xsl:sort select="@system" />
				<xsl:sort select="@externalId" />
				<xsl:if test="position() > 1"><xsl:text>, </xsl:text></xsl:if>
				<xsl:call-template name="source-label" />
			</xsl:for-each>
			<xsl:text>.</xsl:text>
			<xsl:value-of select="$nl" />
		</xsl:if>
	</xsl:template>

	<!-- Helpers -->

	<!-- " — text" when the entity has text. -->
	<xsl:template name="dash-text">
		<xsl:if test="string-length(normalize-space(rp:text)) > 0">
			<xsl:text> — </xsl:text><xsl:value-of select="normalize-space(rp:text)" />
		</xsl:if>
	</xsl:template>

	<xsl:template name="source-label">
		<xsl:choose>
			<xsl:when test="string-length(normalize-space(@title)) > 0"><xsl:value-of select="normalize-space(@title)" /></xsl:when>
			<xsl:otherwise><xsl:value-of select="@externalId" /></xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="type-label">
		<xsl:choose>
			<xsl:when test="local-name() = 'goal'">Goal</xsl:when>
			<xsl:when test="local-name() = 'actor'">Actor</xsl:when>
			<xsl:when test="local-name() = 'usecase'">Use case</xsl:when>
			<xsl:when test="local-name() = 'scenario'">Scenario</xsl:when>
			<xsl:when test="local-name() = 'step'">Step</xsl:when>
			<xsl:when test="local-name() = 'story'">Story</xsl:when>
			<xsl:when test="local-name() = 'term'">Term</xsl:when>
			<xsl:when test="local-name() = 'project'">Project</xsl:when>
			<xsl:otherwise>Stakeholder</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- The name of the entity a reference points to; fails when there is none. -->
	<xsl:template name="ref-name">
		<xsl:param name="ref" />
		<xsl:variable name="entity" select="key('by-id', string($ref))" />
		<xsl:call-template name="require">
			<xsl:with-param name="nodes" select="$entity" />
			<xsl:with-param name="what" select="concat('entity ', $ref)" />
		</xsl:call-template>
		<xsl:value-of select="normalize-space($entity/rp:name)" />
	</xsl:template>

<!-- A comma-separated list of referenced names. Applied with its xsl:sort at the call site:
	     XSLTC drops every node when a for-each sorts a node-set passed in with xsl:with-param. -->
	<xsl:template match="*" mode="ref-list">
		<xsl:if test="position() > 1"><xsl:text>, </xsl:text></xsl:if>
		<xsl:call-template name="ref-name"><xsl:with-param name="ref" select="." /></xsl:call-template>
	</xsl:template>

	<!--
	  Stop the transform when a lookup found nothing. Use it for every reference and every entity a
	  template asks for by name:
	    <xsl:call-template name="require">
	      <xsl:with-param name="nodes" select="rp:goals/rp:goal[rp:name = 'Blocking check']"/>
	      <xsl:with-param name="what" select="'goal named Blocking check'"/>
	    </xsl:call-template>
	  The run path reports the message as a 422 REPORT_FAILED.
	-->
	<xsl:template name="require">
		<xsl:param name="nodes" />
		<xsl:param name="what" />
		<xsl:if test="count($nodes) = 0">
			<xsl:message terminate="yes">
				<xsl:text>The report references </xsl:text><xsl:value-of select="$what" />
				<xsl:text>, which is not in the project.</xsl:text>
			</xsl:message>
		</xsl:if>
	</xsl:template>
</xsl:stylesheet>
