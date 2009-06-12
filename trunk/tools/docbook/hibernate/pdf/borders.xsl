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
  ~ Red Hat Author(s): Steve Ebersole
  -->

<xsl:stylesheet version='1.0'
                xmlns="http://www.w3.org/TR/xhtml1/transitional"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                exclude-result-prefixes="#default">

    <xsl:include href="../common/basic.xsl"/>

    <xsl:variable name="border.text.limit">37</xsl:variable>

    <xsl:param name="double.sided">1</xsl:param>
    <xsl:param name="header.rule">1</xsl:param>
    <xsl:param name="headers.on.blank.pages">1</xsl:param>
    <xsl:param name="footers.on.blank.pages">1</xsl:param>
    <xsl:param name="header.column.widths" select="'1 0 1'"/>
    <xsl:param name="footer.column.widths" select="'1 0 1'"/>

    <xsl:template name="component.title.nomarkup">
        <xsl:param name="node" select="."/>

        <xsl:variable name="id">
            <xsl:call-template name="object.id">
                <xsl:with-param name="object" select="$node"/>
            </xsl:call-template>
        </xsl:variable>

        <xsl:variable name="title">
            <xsl:apply-templates select="$node" mode="object.title.markup">
                <xsl:with-param name="allow-anchors" select="1"/>
            </xsl:apply-templates>
        </xsl:variable>
        <xsl:copy-of select="$title"/>
    </xsl:template>


    <!--###################################################-->
    <!--##                   Header                      ##-->
    <!--###################################################-->
    <xsl:template name="header.content">
        <xsl:param name="pageclass" select="''"/>
        <xsl:param name="sequence" select="''"/>
        <xsl:param name="position" select="''"/>
        <xsl:param name="gentext-key" select="''"/>
        <!-- sequence can be odd, even, first, blank -->
        <!-- position can be left, center, right -->
<!--
        <fo:block>
            <xsl:text>(</xsl:text>
            <xsl:value-of select="$pageclass"/>
            <xsl:text>, </xsl:text>
            <xsl:value-of select="$sequence"/>
            <xsl:text>, </xsl:text>
            <xsl:value-of select="$position"/>
            <xsl:text>, </xsl:text>
            <xsl:value-of select="$gentext-key"/>
            <xsl:text>)</xsl:text>
        </fo:block>
-->
        <fo:block>
            <xsl:choose>
                <xsl:when test="($sequence = 'first' or $pageclass != 'body')">
                    <!-- skip rendering header -->
                </xsl:when>
                <xsl:when test="($sequence='even' and $position='left')">
                    <xsl:variable name="text">
                        <xsl:call-template name="component.title.nomarkup"/>
                    </xsl:variable>
                    <fo:inline keep-together.within-line="always" font-weight="bold">
                        <xsl:choose>
                            <xsl:when test="string-length($text) &gt; ($border.text.limit + 3)">
                                <xsl:value-of select="concat(substring($text, 0, $border.text.limit), '...')"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="$text"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </fo:inline>
                </xsl:when>
                <xsl:when test="($sequence='odd' and $position='right')">
                    <fo:inline keep-together.within-line="always">
                        <!-- todo : can this ever be larger than $border.text.limit??? -->
                        <fo:retrieve-marker retrieve-class-name="section.head.marker"
                                            retrieve-position="first-including-carryover"
                                            retrieve-boundary="page-sequence"/>
                    </fo:inline>
                </xsl:when>
                <xsl:when test="$position='left'">
                    <xsl:call-template name="draft.text"/>
                </xsl:when>
                <xsl:when test="$position='center'">
                </xsl:when>
                <xsl:when test="$position='right'">
                    <xsl:call-template name="draft.text"/>
                </xsl:when>
                <xsl:when test="$sequence = 'first'">
                </xsl:when>
                <xsl:when test="$sequence = 'blank'">
                </xsl:when>
            </xsl:choose>
        </fo:block>
    </xsl:template>

    <xsl:template name="head.sep.rule">
        <xsl:param name="pageclass"/>
        <xsl:param name="sequence"/>
        <xsl:param name="gentext-key"/>
        <xsl:if test="$header.rule != 0">
            <xsl:attribute name="border-bottom-width">0.5pt</xsl:attribute>
            <xsl:attribute name="border-bottom-style">solid</xsl:attribute>
            <xsl:attribute name="border-bottom-color">
                <xsl:value-of select="$logo.color.brown"/>
            </xsl:attribute>
        </xsl:if>
    </xsl:template>


    <!--###################################################-->
    <!--##                   Footer                      ##-->
    <!--###################################################-->
    <xsl:template name="footer.content">
        <xsl:param name="pageclass" select="''"/>
        <xsl:param name="sequence" select="''"/>
        <xsl:param name="position" select="''"/>
        <xsl:param name="gentext-key" select="''"/>

        <!-- pageclass can be front, body, back -->
        <!-- sequence can be odd, even, first, blank -->
        <!-- position can be left, center, right -->
<!--
        <fo:block>
            <xsl:text>(</xsl:text>
            <xsl:value-of select="$pageclass"/>
            <xsl:text>, </xsl:text>
            <xsl:value-of select="$sequence"/>
            <xsl:text>, </xsl:text>
            <xsl:value-of select="$position"/>
            <xsl:text>, </xsl:text>
            <xsl:value-of select="$gentext-key"/>
            <xsl:text>)</xsl:text>
        </fo:block>
-->
        <xsl:variable name="version">
            <xsl:choose>
                <xsl:when test="//releaseinfo">
                    <xsl:text>Hibernate </xsl:text>
                    <xsl:value-of select="//releaseinfo"/>
                </xsl:when>
                <xsl:otherwise>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <fo:block>
            <xsl:choose>
                <xsl:when test="$pageclass = 'titlepage'">
                </xsl:when>
                <xsl:when test="$double.sided = 0">
                    <xsl:choose>
                        <xsl:when test="$position = 'left'">
                            <xsl:value-of select="$version"/>
                        </xsl:when>
                        <xsl:when test="$position = 'right'">
                            <fo:page-number/>
                        </xsl:when>
                        <xsl:otherwise>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>
                <xsl:when test="$double.sided != 0">
                    <xsl:choose>
                        <xsl:when test="($sequence = 'odd' or $sequence = 'first') and $position='right'">
                            <fo:page-number/>
                        </xsl:when>
                        <xsl:when test="($sequence = 'odd' or $sequence = 'first') and $position='left'">
                            <xsl:value-of select="$version"/>
                        </xsl:when>
                        <xsl:when test="$sequence = 'even' and $position='left'">
                            <fo:page-number/>
                        </xsl:when>
                        <xsl:when test="$sequence = 'even' and $position='right'">
                            <xsl:value-of select="$version"/>
                        </xsl:when>
                        <xsl:when test="$sequence='blank' and $position='left'">
                            <fo:page-number/>
                        </xsl:when>
                        <xsl:when test="$sequence='blank' and $position='right'">
                            <xsl:value-of select="$version"/>
                        </xsl:when>
                        <xsl:otherwise>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>
                <xsl:otherwise>
                </xsl:otherwise>
            </xsl:choose>
        </fo:block>
    </xsl:template>

    <xsl:template name="foot.sep.rule">
        <xsl:param name="pageclass"/>
        <xsl:param name="sequence"/>
        <xsl:param name="gentext-key"/>

        <xsl:if test="$footer.rule != 0">
            <xsl:attribute name="border-top-width">0.5pt</xsl:attribute>
            <xsl:attribute name="border-top-style">solid</xsl:attribute>
            <xsl:attribute name="border-top-color">
                <xsl:value-of select="$logo.color.brown"/>
            </xsl:attribute>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>