<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0">

<xsl:include href="xsl-params.xsl"/>

<xsl:param name="html-baseuri"
           select="'http://docbook.sourceforge.net/release/xsl/current/doc/html/'"/>

<xsl:param name="fo-baseuri"
           select="'http://docbook.sourceforge.net/release/xsl/current/doc/fo/'"/>

<xsl:param name="manpages-baseuri"
           select="'http://docbook.sourceforge.net/release/xsl/current/doc/manpages/'"/>

<xsl:param name="roundtrip-baseuri"
           select="'http://docbook.sourceforge.net/release/xsl/current/doc/roundtrip/'"/>

<xsl:template match="parameter">
  <xsl:variable name="markup">
    <xsl:apply-imports/>
  </xsl:variable>

  <xsl:variable name="ishtml">
    <xsl:call-template name="is-html-parameter">
      <xsl:with-param name="param" select="normalize-space(.)"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="isfo">
    <xsl:call-template name="is-fo-parameter">
      <xsl:with-param name="param" select="normalize-space(.)"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="ismanpages">
    <xsl:call-template name="is-manpages-parameter">
      <xsl:with-param name="param" select="normalize-space(.)"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="isroundtrip">
    <xsl:call-template name="is-roundtrip-parameter">
      <xsl:with-param name="param" select="normalize-space(.)"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:choose>
    <xsl:when test="$ishtml != 0">
      <a href="{concat($html-baseuri, normalize-space(.))}.html">
        <xsl:copy-of select="$markup"/>
      </a>
    </xsl:when>
    <xsl:when test="$isfo != 0">
      <a href="{concat($fo-baseuri, normalize-space(.))}.html">
        <xsl:copy-of select="$markup"/>
      </a>
    </xsl:when>
    <xsl:when test="$ismanpages != 0">
      <a href="{concat($manpages-baseuri, normalize-space(.))}.html">
        <xsl:copy-of select="$markup"/>
      </a>
    </xsl:when>
    <xsl:when test="$isroundtrip != 0">
      <a href="{concat($roundtrip-baseuri, normalize-space(.))}.html">
        <xsl:copy-of select="$markup"/>
      </a>
    </xsl:when>
    <xsl:otherwise>
      <xsl:copy-of select="$markup"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

</xsl:stylesheet>
