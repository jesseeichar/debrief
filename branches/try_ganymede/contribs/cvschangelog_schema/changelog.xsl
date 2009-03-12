<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet
    xmlns:xsl='http://www.w3.org/1999/XSL/Transform'
    version='1.0'>

<!--
 The Apache Software License, Version 1.1

 Copyright (c) 2002 The Apache Software Foundation.  All rights
 reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in
    the documentation and/or other materials provided with the
    distribution.

 3. The end-user documentation included with the redistribution, if
    any, must include the following acknowlegement:
       "This product includes software developed by the
        Apache Software Foundation (http://www.apache.org/)."
    Alternately, this acknowlegement may appear in the software itself,
    if and wherever such third-party acknowlegements normally appear.

 4. The names "The Jakarta Project", "Ant", and "Apache Software
    Foundation" must not be used to endorse or promote products derived
    from this software without prior written permission. For written
    permission, please contact apache@apache.org.

 5. Products derived from this software may not be called "Apache"
    nor may "Apache" appear in their names without prior written
    permission of the Apache Group.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.
 ====================================================================

 This software consists of voluntary contributions made by many
 individuals on behalf of the Apache Software Foundation.  For more
 information on the Apache Software Foundation, please see
 <http://www.apache.org/>.
 -->
  <xsl:param name="title"/>
  <xsl:param name="subtitle"/>
  <xsl:param name="module"/>
  <xsl:param name="cvsweb"/>

  <xsl:output method="html" indent="yes"  encoding="US-ASCII"/>

  <!-- Copy standard document elements.  Elements that
       should be ignored must be filtered by apply-templates
       tags. -->
  <xsl:template match="*">
    <xsl:copy>
      <xsl:copy-of select="attribute::*[. != '']"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="changelog">
    <HTML>
      <HEAD>
        <TITLE><xsl:value-of select="$title"/></TITLE>
      </HEAD>
      <BODY link="#000000" alink="#000000" vlink="#000000" text="#000000">
<style type="text/css">
          body, p {
          font-family: verdana,arial,helvetica;
          font-size: 80%;
          color:#000000;
          }
	  .dateAndAuthor {
          font-family: verdana,arial,helvetica;
          font-size: 80%;
          font-weight: bold;
          text-align:left;
          background:#a6caf0;
	  }
          p.msg{
	border : groove;
	border-width : 1px 1px 1px 1px;
          }
          div.time{
          font-size: 80%;
	display : inline;          
          }
          tr, td{
          font-family: verdana,arial,helvetica;
          font-size: 90%;
          background:#eeeee0;
          }	  
	  </style>        
          <h2>
            <a name="top"><xsl:value-of select="$title"/></a>
          </h2>
          <h3>
            <xsl:value-of select="$subtitle"/>
          </h3>
          <p align="right">Produced in support of MWC's  <a href="http://intranet2/coag/asset/">ASSET</a> project.</p>
          <hr size="2"/>
        <TABLE BORDER="0" WIDTH="100%" CELLPADDING="3" CELLSPACING="1">
          
          <xsl:apply-templates select=".//entry">
            <xsl:sort select="date" data-type="text" order="descending"/>
            <xsl:sort select="time" data-type="text" order="descending"/>
          </xsl:apply-templates>
          
        </TABLE>
        
      </BODY>
    </HTML>
  </xsl:template>
  
  <xsl:template match="entry">
    <TR>
      <TD colspan="2" class="dateAndAuthor">
        <xsl:value-of select="date"/>
        <xsl:text> </xsl:text>
       <xsl:value-of select="time"/>
        <xsl:text>  </xsl:text>
        <xsl:value-of select="author"/>
      </TD>
    </TR>
    <TR>
      <TD width="5">
        <xsl:text>    </xsl:text>
      </TD>
      <TD>
      <xsl:apply-templates select="file"/>
         <p class="msg">
	  <xsl:apply-templates select="msg"/></p>
      </TD>
    </TR>
  </xsl:template>

  <xsl:template match="date">
    <i><xsl:value-of select="."/></i>
  </xsl:template>

  <xsl:template match="time">
    <xsl:value-of select="."/>
  </xsl:template>

  <xsl:template match="author">
    <i>
      <a>
        <xsl:attribute name="href">mailto:<xsl:value-of select="."/></xsl:attribute>
        <xsl:value-of select="."/>
      </a>
    </i>
  </xsl:template>

  <xsl:template match="file">
       
        <xsl:value-of select="name" /> (<xsl:value-of select="revision"/>)
  </xsl:template>

  <!-- Any elements within a msg are processed,
       so that we can preserve HTML tags. -->
  <xsl:template match="msg">
    <xsl:apply-templates/>
  </xsl:template>
  
</xsl:stylesheet>

