<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ng="http://docbook.org/docbook-ng"
                xmlns:db="http://docbook.org/ns/docbook"
                xmlns:exsl="http://exslt.org/common"
                exclude-result-prefixes="exsl db ng"
                version='1.0'>

<!-- ********************************************************************
     $Id: html2roff.xsl 6492 2007-01-14 20:21:31Z xmldoc $
     ********************************************************************

     This file is part of the XSL DocBook Stylesheet distribution.
     See ../README or http://docbook.sf.net/release/xsl/current/ for
     copyright and other information.

     ******************************************************************** -->

<!-- * Standalone stylesheet for doing "HTML to roff" transformation of a -->
<!-- * stylesheet; which currently just means that it transforms all -->
<!-- * <br/> instances into a line break, and all <pre></pre> instances -->
<!-- * into roff "no fill region" markup -->

<!-- ==================================================================== -->

  <xsl:output method="xml"
              encoding="UTF-8"
              indent="no"/>

  <xsl:template match="/">
    <xsl:apply-templates/>
    <xsl:text>&#x0a;</xsl:text>
  </xsl:template>

  <xsl:template match="node() | @*">
    <xsl:copy>
      <xsl:apply-templates select="@* | node()"/>
    </xsl:copy>
  </xsl:template>

  <!-- ==================================================================== -->

  <xsl:template match="br">
    <xsl:element name="xsl:text">&#10;.&#10;</xsl:element>
  </xsl:template>

  <xsl:template match="pre">
    <xsl:element name="xsl:text">&#x2302;sp&#10;</xsl:element>
    <xsl:element name="xsl:text">&#x2302;nf&#10;</xsl:element>
    <xsl:copy>
      <xsl:apply-templates select="@* | node()"/>
    </xsl:copy>
    <xsl:element name="xsl:text">&#10;</xsl:element>
    <xsl:element name="xsl:text">&#x2302;fi&#10;</xsl:element>
  </xsl:template>

</xsl:stylesheet>
