<?xml version="1.0"?>
<!--
  ~ Copyright (c) 2007, Red Hat Middleware, LLC. All rights reserved.
  ~
  ~ This copyrighted material is made available to anyone wishing to use, modify,
  ~ copy, or redistribute it subject to the terms and conditions of the GNU
  ~ Lesser General Public License, v. 2.1. This program is distributed in the
  ~ hope that it will be useful, but WITHOUT A WARRANTY; without even the implied
  ~ warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  ~ Lesser General Public License for more details. You should have received a
  ~ copy of the GNU Lesser General Public License, v.2.1 along with this
  ~ distribution; if not, write to the Free Software Foundation, Inc.,
  ~ 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  ~
  ~ Red Hat Author(s): Christian Bauer, Steve Ebersole
  -->

<xsl:stylesheet version='1.0'
                xmlns="http://www.w3.org/TR/xhtml1/transitional"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                exclude-result-prefixes="#default">

    <!-- Copied from fo/params.xsl -->
    <xsl:param name="l10n.gentext.default.language" select="'en'"/>


    <xsl:variable name="logo.color.gray">#59666c</xsl:variable>
    <xsl:variable name="logo.color.brown">#aea477</xsl:variable>


    <!--###################################################-->
    <!--##                Extensions                     ##-->
    <!--###################################################-->
    <xsl:param name="use.extensions">1</xsl:param>
    <xsl:param name="tablecolumns.extension">0</xsl:param>


    <!--###################################################-->
    <!--##             Table of Contents                 ##-->
    <!--###################################################-->
    <xsl:param name="generate.toc">
        book   toc
    </xsl:param>
    <xsl:param name="toc.section.depth">3</xsl:param>
    <xsl:param name="autotoc.label.separator" select="'.  '"/>


    <!--###################################################-->
    <!--##                  Labels                       ##-->
    <!--###################################################-->
    <!-- Label Chapters and Sections (numbering) -->
    <xsl:param name="chapter.autolabel">1</xsl:param>
    <xsl:param name="section.autolabel">1</xsl:param>
    <xsl:param name="section.label.includes.component.label">1</xsl:param>


    <!--###################################################-->
    <!--##                  Titles                       ##-->
    <!--###################################################-->
    <xsl:param name="title.margin.left">0pc</xsl:param>
    <xsl:param name="formal.title.placement">
        figure after
        example before
        equation before
        table before
        procedure before
    </xsl:param>


    <!--###################################################-->
    <!--##                Programlistings                ##-->
    <!--###################################################-->
    <!-- Verbatim text formatting (programlistings) -->
    <xsl:attribute-set name="verbatim.properties">
        <xsl:attribute name="space-before.minimum">1em</xsl:attribute>
        <xsl:attribute name="space-before.optimum">1em</xsl:attribute>
        <xsl:attribute name="space-before.maximum">1em</xsl:attribute>
        <xsl:attribute name="space-after.minimum">0.1em</xsl:attribute>
        <xsl:attribute name="space-after.optimum">0.1em</xsl:attribute>
        <xsl:attribute name="space-after.maximum">0.1em</xsl:attribute>
        <xsl:attribute name="border-color">#444444</xsl:attribute>
        <xsl:attribute name="border-style">solid</xsl:attribute>
        <xsl:attribute name="border-width">0.1pt</xsl:attribute>
        <xsl:attribute name="padding-top">0.5em</xsl:attribute>
        <xsl:attribute name="padding-left">0.5em</xsl:attribute>
        <xsl:attribute name="padding-right">0.5em</xsl:attribute>
        <xsl:attribute name="padding-bottom">0.5em</xsl:attribute>
        <xsl:attribute name="margin-left">0.5em</xsl:attribute>
        <xsl:attribute name="margin-right">0.5em</xsl:attribute>
    </xsl:attribute-set>


    <!-- Shade (background) programlistings -->
    <xsl:param name="shade.verbatim">1</xsl:param>
    <xsl:attribute-set name="shade.verbatim.style">
        <xsl:attribute name="wrap-option">wrap</xsl:attribute>
        <xsl:attribute name="background-color">#f0f0f0</xsl:attribute>
    </xsl:attribute-set>


    <!--###################################################-->
    <!--##                  Callouts                     ##-->
    <!--###################################################-->
    <xsl:param name="callout.extensions">1</xsl:param>
    <xsl:param name="callout.defaultcolumn">90</xsl:param>
    <xsl:param name="callout.graphics">0</xsl:param>
    <xsl:param name="callout.unicode.number.limit" select="'0'"/>
    <xsl:template name="callout-bug">
        <xsl:param name="conum" select='1'/>
        <fo:inline
            color="black"
            padding-top="0.1em"
            padding-bottom="0.1em"
            padding-start="0.2em"
            padding-end="0.2em"
            baseline-shift="0.1em"
            font-weight="bold"
            font-size="75%">
            <xsl:text>(</xsl:text>
            <xsl:value-of select="$conum"/>
            <xsl:text>)</xsl:text>
        </fo:inline>
    </xsl:template>

</xsl:stylesheet>