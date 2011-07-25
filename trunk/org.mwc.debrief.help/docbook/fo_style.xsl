<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version='1.0'
  xmlns="http://www.w3.org/TR/xhtml1/transitional"
  exclude-result-prefixes="#default">

  <xsl:import href="file:../../contribs/docbook-xsl-1.76.1/fo/docbook.xsl"/>

  <!-- Add other variable definitions here -->

  <xsl:param name="paper.type" select="'A4'"/>
  <xsl:param name="alignment">left</xsl:param>
  <xsl:param name="body.font.master">10</xsl:param>
  <xsl:param name="body.font.family">sans-serif</xsl:param>
  <xsl:param name="double-sided">1</xsl:param>


  <!-- get the extensions to work -->
  <xsl:param name="fop1.extensions" select="1"/>
  <xsl:param name="tablecolumns.extension" select="0"/>
  <xsl:param name="use.extensions" select="'1'"/>

  <!-- insert the correct indexes -->

  <xsl:param name="generate.index" select="1"/>
  <xsl:param name="generate.toc">
    book      toc,title
    part      title
  </xsl:param>
  <xsl:param name="toc.section.depth" select="1"/>
  <xsl:param name="insert.xref.page.number" select="yes"/>
  <xsl:param name="column.count.index" select="2"/>
  <xsl:param name="column.gap.index" select="'2pt'"/>
  
  <xsl:param name="label.from.part" select="1"/>
  <xsl:param name="component.label.includes.part.label" select="1"/>
  <xsl:param name="section.label.includes.part.label" select="1"/>

  <!-- sort out the admonitions (tip icons) -->

  <xsl:param name="admon.graphics.path">images/</xsl:param>
  <xsl:param name="admon.graphics" select="1"/>
  <xsl:param name="admon.graphics.extension" select="'.gif'"/>  
  
  <!-- numbering -->
  <xsl:param name="appendix.autolabel" select="'I'"/>
  <xsl:param name="chapter.autolabel" select="1"/>
  <xsl:param name="part.autolabel" select="'A'"/>
  <xsl:param name="section.autolabel" select="1"/>
  <xsl:param name="preface.autolabel" select="1"/>

  <!-- other, general modifications -->
  	<!-- don't indent the child paragraphs: this helps our preformatted text 
		fix on the page -->
	<xsl:param name="body.start.indent" select="0" />
  
  <xsl:attribute-set name="monospace.verbatim.properties">
    <xsl:attribute name="wrap-option">wrap</xsl:attribute>
</xsl:attribute-set>

  <xsl:attribute-set name="section.title.level1.properties">
    <xsl:attribute name="font-size">
      <xsl:value-of select="$body.font.master * 1.66"/>
      <xsl:text>pt</xsl:text>
    </xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="section.title.level2.properties">
    <xsl:attribute name="font-size">
      <xsl:value-of select="$body.font.master * 1.25"/>
      <xsl:text>pt</xsl:text>
    </xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="section.title.level3.properties">
    <xsl:attribute name="font-size">
      <xsl:value-of select="$body.font.master * 1.1"/>
      <xsl:text>pt</xsl:text>
    </xsl:attribute>
  </xsl:attribute-set>


  <xsl:attribute-set name="section.title.level4.properties">
    <xsl:attribute name="font-size">
      <xsl:value-of select="$body.font.master * 1.05"/>
      <xsl:text>pt</xsl:text>
    </xsl:attribute>
  </xsl:attribute-set>
</xsl:stylesheet>
