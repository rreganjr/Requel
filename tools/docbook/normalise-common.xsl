<xsl:stylesheet version='1.0'
  xmlns:xsl='http://www.w3.org/1999/XSL/Transform'
  xmlns:rnd='http://docbook.org/ns/docbook/roundtrip'
  xmlns:db='http://docbook.org/ns/docbook'
  exclude-result-prefixes='db'>

  <!-- ********************************************************************
       $Id$
       ********************************************************************

       This file is part of the XSL DocBook Stylesheet distribution.
       See ../README or http://nwalsh.com/docbook/xsl/ for copyright
       and other information.

       ******************************************************************** -->

  <!-- rnd:map-paragraph-style and rd:map-character-style
       allows the application to customise
       the style names used by overriding this template.
       The idea is to map custom names back to standard names. -->
  <xsl:template name='rnd:map-paragraph-style'>
    <xsl:param name='style'/>
    <xsl:choose>
      <xsl:when test='starts-with($style, "Normal")'/>

      <!-- Probably should fold all style names to lower-case -->
      <xsl:when test='$style = "Caption"'>caption</xsl:when>
      
      <!-- rreganjr: requel UserGuide.doc custom mapping -->
      <xsl:when test='$style = "BodyText"'></xsl:when>
      <xsl:when test='$style = "Title"'>book-title</xsl:when>
      <xsl:when test='$style = "Subtitle"'>book-subtitle</xsl:when>
      <xsl:when test='$style = "Heading1"'>chapter-title</xsl:when>
      <xsl:when test='$style = "Heading2"'>sect1-title</xsl:when>
      <xsl:when test='$style = "TOC1"'></xsl:when>
      <xsl:when test='$style = "TOC2"'></xsl:when>
      <xsl:when test='$style = "TableofFigures"'>chapter-title</xsl:when>
      <xsl:when test='$style = "Quotation"'>blockquote</xsl:when>
      <xsl:when test='$style = "TableCell"'></xsl:when>
      <xsl:when test='$style = "code-page"'>programlisting</xsl:when>

      <xsl:otherwise>
        <xsl:value-of select='$style'/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template name='rnd:map-character-style'>
    <xsl:param name='style'/>
    <xsl:choose>
      <xsl:when test='$style = "subtitle0"'>emphasis-bold</xsl:when>
      <xsl:otherwise>
        <xsl:value-of select='$style'/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
