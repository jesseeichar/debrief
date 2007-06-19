<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version='1.0'>

<xsl:output method="text" indent="no"/>

<!-- ********************************************************************
     $Id: docparam2txt.xsl 6294 2006-09-13 08:51:12Z xmldoc $
     ********************************************************************

     This file is part of the XSL DocBook Stylesheet distribution.
     See ../README or http://docbook.sf.net/release/xsl/current for
     copyright and other information.

     ******************************************************************** -->

<xsl:template match="preface|reference|refentry|appendix">
  <xsl:value-of select="concat(@id,'.html','&#x0a;')"/>
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="text()"/>

</xsl:stylesheet>
