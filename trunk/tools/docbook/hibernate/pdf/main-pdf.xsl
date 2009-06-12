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

    <xsl:import href="http://docbook.sourceforge.net/release/xsl/current/fo/docbook.xsl" />
    <xsl:import href="../common/basic.xsl" />
    <xsl:import href="coverpages.xsl" />
    <xsl:import href="borders.xsl" />


    <!--###################################################-->
    <!--##                Hypenation                     ##-->
    <!--###################################################-->
    <xsl:param name="hyphenate">false</xsl:param>
    <xsl:param name="hyphenation-character">-</xsl:param>


    <!--###################################################-->
    <!--##             Paper & Page Size                 ##-->
    <!--###################################################-->
    <xsl:param name="alignment">left</xsl:param>
    <xsl:param name="line-height">1.4</xsl:param>

    <xsl:param name="paper.type" select="'A4'"/>

    <xsl:param name="page.margin.top">15mm</xsl:param>
    <xsl:param name="region.before.extent">10mm</xsl:param>
    <xsl:param name="body.margin.top">15mm</xsl:param>

    <xsl:param name="body.margin.bottom">15mm</xsl:param>
    <xsl:param name="region.after.extent">10mm</xsl:param>
    <xsl:param name="page.margin.bottom">15mm</xsl:param>

    <xsl:param name="page.margin.outer">30mm</xsl:param>
    <xsl:param name="page.margin.inner">30mm</xsl:param>


    <!--###################################################-->
    <!--##              Custom Toc Line                  ##-->
    <!--###################################################-->
    <xsl:template name="toc.line">
        <xsl:variable name="id">
            <xsl:call-template name="object.id"/>
        </xsl:variable>

        <xsl:variable name="label">
            <xsl:apply-templates select="." mode="label.markup"/>
        </xsl:variable>

        <fo:block text-align-last="justify" end-indent="{$toc.indent.width}pt" last-line-end-indent="-{$toc.indent.width}pt">
            <fo:inline keep-with-next.within-line="always">
                <fo:basic-link internal-destination="{$id}">
                    <!-- Chapter titles should be bold. -->
                    <xsl:choose>
                        <xsl:when test="local-name(.) = 'chapter'">
                            <xsl:attribute name="font-weight">bold</xsl:attribute>
                        </xsl:when>
                    </xsl:choose>

                    <xsl:if test="$label != ''">
                        <xsl:copy-of select="$label"/>
                        <xsl:value-of select="$autotoc.label.separator"/>
                    </xsl:if>
                    <xsl:apply-templates select="." mode="titleabbrev.markup"/>
                </fo:basic-link>
            </fo:inline>
            <fo:inline keep-together.within-line="always">
                <xsl:text> </xsl:text>
                <fo:leader leader-pattern="dots"
                    leader-pattern-width="3pt"
                    leader-alignment="reference-area"
                    keep-with-next.within-line="always"/>
                <xsl:text> </xsl:text>
                <fo:basic-link internal-destination="{$id}">
                    <fo:page-number-citation ref-id="{$id}"/>
                </fo:basic-link>
            </fo:inline>
        </fo:block>
    </xsl:template>


    <!--###################################################-->
    <!--##                  Titles                       ##-->
    <!--###################################################-->
    <xsl:param name="title.color">#59666c</xsl:param>

    <xsl:attribute-set name="book.titlepage.recto.style">
        <xsl:attribute name="font-family">
            <xsl:value-of select="$title.fontset"/>
        </xsl:attribute>
        <xsl:attribute name="color">
            <xsl:value-of select="$title.color"/>
        </xsl:attribute>
        <xsl:attribute name="font-weight">bold</xsl:attribute>
        <xsl:attribute name="font-size">12pt</xsl:attribute>
        <xsl:attribute name="text-align">center</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="chapter.titlepage.recto.style">
        <xsl:attribute name="color">
            <xsl:value-of select="$title.color"/>
        </xsl:attribute>
        <xsl:attribute name="background-color">white</xsl:attribute>
        <xsl:attribute name="font-size">
            <xsl:choose>
                <xsl:when test="$l10n.gentext.language = 'ja-JP'">
                    <xsl:value-of select="$body.font.master * 1.7"/>
                    <xsl:text>pt</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:text>24pt</xsl:text>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:attribute>
        <xsl:attribute name="font-weight">bold</xsl:attribute>
        <xsl:attribute name="text-align">left</xsl:attribute>
        <xsl:attribute name="padding-left">1em</xsl:attribute>
        <xsl:attribute name="padding-right">1em</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="preface.titlepage.recto.style">
        <xsl:attribute name="font-family">
            <xsl:value-of select="$title.fontset"/>
        </xsl:attribute>
        <xsl:attribute name="color">
            <xsl:value-of select="$title.color"/>
        </xsl:attribute>
        <xsl:attribute name="font-size">12pt</xsl:attribute>
        <xsl:attribute name="font-weight">bold</xsl:attribute>
    </xsl:attribute-set>

    <xsl:template match="title" mode="chapter.titlepage.recto.auto.mode">
        <fo:block xsl:use-attribute-sets="chapter.titlepage.recto.style">
            <xsl:call-template name="component.title">
                <xsl:with-param name="node" select="ancestor-or-self::chapter[1]"/>
            </xsl:call-template>
        </fo:block>
    </xsl:template>

    <xsl:attribute-set name="section.title.properties">
        <xsl:attribute name="font-family">
            <xsl:value-of select="$title.font.family"/>
        </xsl:attribute>
        <xsl:attribute name="font-weight">bold</xsl:attribute>
        <xsl:attribute name="keep-with-next.within-column">always</xsl:attribute>
        <xsl:attribute name="space-before.minimum">0.8em</xsl:attribute>
        <xsl:attribute name="space-before.optimum">1.0em</xsl:attribute>
        <xsl:attribute name="space-before.maximum">1.2em</xsl:attribute>
        <xsl:attribute name="text-align">left</xsl:attribute>
        <xsl:attribute name="start-indent">
            <xsl:value-of select="$title.margin.left"/>
        </xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="section.title.level1.properties">
        <xsl:attribute name="color">
            <xsl:value-of select="$title.color"/>
        </xsl:attribute>
        <xsl:attribute name="font-size">
            <xsl:value-of select="$body.font.master * 1.6"/>
            <xsl:text>pt</xsl:text>
        </xsl:attribute>
    </xsl:attribute-set>
    <xsl:attribute-set name="section.title.level2.properties">
        <xsl:attribute name="color">
            <xsl:value-of select="$title.color"/>
        </xsl:attribute>
        <xsl:attribute name="font-size">
            <xsl:value-of select="$body.font.master * 1.4"/>
            <xsl:text>pt</xsl:text>
        </xsl:attribute>
    </xsl:attribute-set>
    <xsl:attribute-set name="section.title.level3.properties">
        <xsl:attribute name="color">
            <xsl:value-of select="$title.color"/>
        </xsl:attribute>
        <xsl:attribute name="font-size">
            <xsl:value-of select="$body.font.master * 1.3"/>
            <xsl:text>pt</xsl:text>
        </xsl:attribute>
    </xsl:attribute-set>
    <xsl:attribute-set name="section.title.level4.properties">
        <xsl:attribute name="color">
            <xsl:value-of select="$title.color"/>
        </xsl:attribute>
        <xsl:attribute name="font-size">
            <xsl:value-of select="$body.font.master * 1.2"/>
            <xsl:text>pt</xsl:text>
        </xsl:attribute>
    </xsl:attribute-set>
    <xsl:attribute-set name="section.title.level5.properties">
        <xsl:attribute name="color">
            <xsl:value-of select="$title.color"/>
        </xsl:attribute>
        <xsl:attribute name="font-size">
            <xsl:value-of select="$body.font.master * 1.1"/>
            <xsl:text>pt</xsl:text>
        </xsl:attribute>
    </xsl:attribute-set>
    <xsl:attribute-set name="section.title.level6.properties">
        <xsl:attribute name="color">
            <xsl:value-of select="$title.color"/>
        </xsl:attribute>
        <xsl:attribute name="font-size">
            <xsl:value-of select="$body.font.master"/>
            <xsl:text>pt</xsl:text>
        </xsl:attribute>
    </xsl:attribute-set>


    <!--###################################################-->
    <!--##                    Misc                       ##-->
    <!--###################################################-->

    <!-- Format Variable Lists as Blocks (prevents horizontal overflow). -->
    <xsl:param name="variablelist.as.blocks">1</xsl:param>


    <!--###################################################-->
    <!--##               Fonts & Styles                  ##-->
    <!--###################################################-->
    <xsl:param name="body.font.master">11</xsl:param>

    <xsl:attribute-set name="monospace.properties">
        <xsl:attribute name="font-family">
            <xsl:value-of select="$monospace.font.family"/>
        </xsl:attribute>
        <xsl:attribute name="font-size">0.8em</xsl:attribute>
    </xsl:attribute-set>

    <xsl:template name="pickfont-sans">
        <xsl:variable name="font">
            <xsl:choose>
                <xsl:when test="$l10n.gentext.language = 'ja-JP'">
                    <xsl:text>KochiMincho,</xsl:text>
                </xsl:when>
                <xsl:when test="$l10n.gentext.language = 'ko-KR'">
                    <xsl:text>BaekmukBatang,</xsl:text>
                </xsl:when>
                <xsl:when test="$l10n.gentext.language = 'zh-CN'">
                    <xsl:text>ARPLKaitiMGB,</xsl:text>
                </xsl:when>
                <xsl:when test="$l10n.gentext.language = 'bn-IN'">
                    <xsl:text>LohitBengali,</xsl:text>
                </xsl:when>
                <xsl:when test="$l10n.gentext.language = 'ta-IN'">
                    <xsl:text>LohitTamil,</xsl:text>
                </xsl:when>
                <xsl:when test="$l10n.gentext.language = 'pa-IN'">
                    <xsl:text>LohitPunjabi,</xsl:text>
                </xsl:when>
                <xsl:when test="$l10n.gentext.language = 'hi-IN'">
                    <xsl:text>LohitHindi,</xsl:text>
                </xsl:when>
                <xsl:when test="$l10n.gentext.language = 'gu-IN'">
                    <xsl:text>LohitGujarati,</xsl:text>
                </xsl:when>
                <xsl:when test="$l10n.gentext.language = 'zh-TW'">
                    <xsl:text>ARPLMingti2LBig5,</xsl:text>
                </xsl:when>
            </xsl:choose>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$fop1.extensions != 0">
                <xsl:copy-of select="$font"/>
                <xsl:text>LiberationSans,sans-serif</xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy-of select="$font"/>
                <xsl:text>sans-serif</xsl:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="pickfont-serif">
        <xsl:variable name="font">
            <xsl:choose>
                <xsl:when test="$l10n.gentext.language = 'ja-JP'">
                    <xsl:text>KochiMincho,</xsl:text>
                </xsl:when>
                <xsl:when test="$l10n.gentext.language = 'ko-KR'">
                    <xsl:text>BaekmukBatang,</xsl:text>
                </xsl:when>
                <xsl:when test="$l10n.gentext.language = 'zh-CN'">
                    <xsl:text>ARPLKaitiMGB,</xsl:text>
                </xsl:when>
                <xsl:when test="$l10n.gentext.language = 'bn-IN'">
                    <xsl:text>LohitBengali,</xsl:text>
                </xsl:when>
                <xsl:when test="$l10n.gentext.language = 'ta-IN'">
                    <xsl:text>LohitTamil,</xsl:text>
                </xsl:when>
                <xsl:when test="$l10n.gentext.language = 'pa-IN'">
                    <xsl:text>LohitPunjabi,</xsl:text>
                </xsl:when>
                <xsl:when test="$l10n.gentext.language = 'hi-IN'">
                    <xsl:text>LohitHindi,</xsl:text>
                </xsl:when>
                <xsl:when test="$l10n.gentext.language = 'gu-IN'">
                    <xsl:text>LohitGujarati,</xsl:text>
                </xsl:when>
                <xsl:when test="$l10n.gentext.language = 'zh-TW'">
                    <xsl:text>ARPLMingti2LBig5,</xsl:text>
                </xsl:when>
            </xsl:choose>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$fop1.extensions != 0">
                <xsl:copy-of select="$font"/>
                <xsl:text>LiberationSerif,serif</xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy-of select="$font"/>
                <xsl:text>serif</xsl:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="pickfont-mono">
        <xsl:variable name="font">
            <xsl:choose>
                <xsl:when test="$l10n.gentext.language = 'ja-JP'">
                    <xsl:text>KochiMincho,</xsl:text>
                </xsl:when>
                <xsl:when test="$l10n.gentext.language = 'ko-KR'">
                    <xsl:text>BaekmukBatang,</xsl:text>
                </xsl:when>
                <xsl:when test="$l10n.gentext.language = 'zh-CN'">
                    <xsl:text>ARPLKaitiMGB,</xsl:text>
                </xsl:when>
                <xsl:when test="$l10n.gentext.language = 'bn-IN'">
                    <xsl:text>LohitBengali,</xsl:text>
                </xsl:when>
                <xsl:when test="$l10n.gentext.language = 'ta-IN'">
                    <xsl:text>LohitTamil,</xsl:text>
                </xsl:when>
                <xsl:when test="$l10n.gentext.language = 'pa-IN'">
                    <xsl:text>LohitPunjabi,</xsl:text>
                </xsl:when>
                <xsl:when test="$l10n.gentext.language = 'hi-IN'">
                    <xsl:text>LohitHindi,</xsl:text>
                </xsl:when>
                <xsl:when test="$l10n.gentext.language = 'gu-IN'">
                    <xsl:text>LohitGujarati,</xsl:text>
                </xsl:when>
                <xsl:when test="$l10n.gentext.language = 'zh-TW'">
                    <xsl:text>ARPLMingti2LBig5,</xsl:text>
                </xsl:when>
            </xsl:choose>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$fop1.extensions != 0">
                <xsl:copy-of select="$font"/>
                <xsl:text>LiberationMono,monospace</xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy-of select="$font"/>
                <xsl:text>monospace</xsl:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:param name="title.font.family">
        <xsl:call-template name="pickfont-sans"/>
    </xsl:param>

    <xsl:param name="body.font.family">
        <xsl:call-template name="pickfont-sans"/>
    </xsl:param>

    <xsl:param name="monospace.font.family">
        <xsl:call-template name="pickfont-mono"/>
    </xsl:param>

    <xsl:param name="sans.font.family">
        <xsl:call-template name="pickfont-sans"/>
    </xsl:param>

</xsl:stylesheet>
