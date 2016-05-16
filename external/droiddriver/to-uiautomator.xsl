<?xml version="1.0"?>
<!-- To convert DroidDriver dump (say dd.xml) to UiAutomatorViewer format (say ua.uix), run:
     xsltproc -o ua.uix to-uiautomator.xsl dd.xml -->
<xsl:transform version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:strip-space elements="*" />
  <xsl:template match="/">
    <hierarchy rotation="0">
      <xsl:apply-templates />
    </hierarchy>
  </xsl:template>
  <xsl:template match="*">
    <node>
      <xsl:attribute name="index"><xsl:value-of select="position()-1" /></xsl:attribute>
      <xsl:attribute name="text"><xsl:value-of select="@text" /></xsl:attribute>
      <xsl:attribute name="resource-id"><xsl:value-of select="@resource-id" /></xsl:attribute>
      <xsl:attribute name="class"><xsl:value-of select="@class" /></xsl:attribute>
      <xsl:attribute name="package"><xsl:value-of select="@package" /></xsl:attribute>
      <xsl:attribute name="content-desc"><xsl:value-of select="@content-desc" /></xsl:attribute>
      <xsl:attribute name="checkable"><xsl:value-of select="boolean(@checkable)" /></xsl:attribute>
      <xsl:attribute name="checked"><xsl:value-of select="boolean(@checked)" /></xsl:attribute>
      <xsl:attribute name="clickable"><xsl:value-of select="boolean(@clickable)" /></xsl:attribute>
      <xsl:attribute name="enabled"><xsl:value-of select="boolean(@enabled)" /></xsl:attribute>
      <xsl:attribute name="focusable"><xsl:value-of select="boolean(@focusable)" /></xsl:attribute>
      <xsl:attribute name="focused"><xsl:value-of select="boolean(@focused)" /></xsl:attribute>
      <xsl:attribute name="scrollable"><xsl:value-of select="boolean(@scrollable)" /></xsl:attribute>
      <xsl:attribute name="long-clickable"><xsl:value-of select="boolean(@long-clickable)" /></xsl:attribute>
      <xsl:attribute name="password"><xsl:value-of select="boolean(@password)" /></xsl:attribute>
      <xsl:attribute name="selected"><xsl:value-of select="boolean(@selected)" /></xsl:attribute>
      <xsl:attribute name="bounds"><xsl:value-of select="@bounds" /></xsl:attribute>
      <xsl:apply-templates />
    </node>
  </xsl:template>
</xsl:transform>