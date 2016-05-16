<?xml version="1.0" encoding="utf-8"?>
<!--
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 -->

<!DOCTYPE xsl:stylesheet [ <!ENTITY nbsp "&#160;"> ]>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="html" version="1.0" encoding="UTF-8" indent="yes" />
    <xsl:template match="/">
        <html>
            <head>
                <script type="text/javascript">
                    function toggleVisibility(id) {
                        element = document.getElementById(id); 
                        if (element.style.display == "none") {
                            element.style.display = ""; 
                        } else { 
                            element.style.display = "none";
                        } 
                    }
                </script>
                <style type="text/css">
                    body {
                        background-color: #CCCCCC;
                        font-family: sans-serif;
                        margin: 10px;
                    }

                    .info {
                        margin-bottom: 10px;
                    }

                    .apks, .package, .class {
                        cursor: pointer;
                        text-decoration: underline;
                    }

                    .packageDetails {
                        padding-left: 20px;
                    }

                    .classDetails {
                        padding-left: 40px;
                    }

                    .method {
                        font-family: courier;
                        white-space: nowrap;
                    }

                    .red {
                        background-color: #FF6666;
                    }

                    .yellow {
                        background-color: #FFFF66;
                    }

                    .green {
                        background-color: #66FF66;
                    }

                    .deprecated {
                        text-decoration: line-through;
                    }
                </style>
            </head>
            <body>
                <h1><xsl:value-of select="api-coverage/@title" /></h1>
                <div class="info">
                    Generated: <xsl:value-of select="api-coverage/@generatedTime" />
                </div>
                <div class="total">
                    Total:&nbsp;<xsl:value-of select="api-coverage/total/@coveragePercentage" />%
                &nbsp;(<xsl:value-of select="api-coverage/total/@numCovered" />/<xsl:value-of select="api-coverage/total/@numTotal" />)
                </div>
                <div class="apks" onclick="toggleVisibility('sourceApks')">
                    Source APKs (<xsl:value-of select="count(api-coverage/debug/sources/apk)" />)
                </div>
                <div id="sourceApks" style="display: none">
                    <ul>
                        <xsl:for-each select="api-coverage/debug/sources/apk">
                            <li><xsl:value-of select="@path" /></li>
                        </xsl:for-each>
                    </ul>
                </div>
                <ul>
                    <xsl:for-each select="api-coverage/api/package">
                        <xsl:call-template name="packageOrClassListItem">
                            <xsl:with-param name="bulletClass" select="'package'" />
                        </xsl:call-template>
                        <div class="packageDetails" id="{@name}" style="display: none">
                            <ul>
                                <xsl:for-each select="class">
                                    <xsl:call-template name="packageOrClassListItem">
                                        <xsl:with-param name="bulletClass" select="'class'" />
                                    </xsl:call-template>
                                    <div class="classDetails" id="{@name}" style="display: none">
                                        <xsl:for-each select="constructor">
                                            <xsl:call-template name="methodListItem" />
                                        </xsl:for-each>
                                        <xsl:for-each select="method">
                                            <xsl:call-template name="methodListItem" />
                                        </xsl:for-each>
                                    </div>
                                </xsl:for-each>
                            </ul>
                        </div>
                    </xsl:for-each>
                </ul>
            </body>
        </html>
    </xsl:template>
    
    <xsl:template name="packageOrClassListItem">
        <xsl:param name="bulletClass" />

        <xsl:variable name="colorClass">
            <xsl:choose>
                <xsl:when test="@coveragePercentage &lt;= 50">red</xsl:when>
                <xsl:when test="@coveragePercentage &lt;= 80">yellow</xsl:when>
                <xsl:otherwise>green</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        
        <xsl:variable name="deprecatedClass">
            <xsl:choose>
                <xsl:when test="@deprecated = 'true'">deprecated</xsl:when>
                <xsl:otherwise></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <li class="{$bulletClass}" onclick="toggleVisibility('{@name}')">
            <span class="{$colorClass} {$deprecatedClass}">
                <b><xsl:value-of select="@name" /></b>
                &nbsp;<xsl:value-of select="@coveragePercentage" />%
                &nbsp;(<xsl:value-of select="@numCovered" />/<xsl:value-of select="@numTotal" />)
            </span>
        </li>   
    </xsl:template>
  
  <xsl:template name="methodListItem">

    <xsl:variable name="deprecatedClass">
        <xsl:choose>
            <xsl:when test="@deprecated = 'true'">deprecated</xsl:when>
            <xsl:otherwise></xsl:otherwise>
        </xsl:choose>
    </xsl:variable>

    <span class="method {$deprecatedClass}">
      <xsl:choose>
        <xsl:when test="@covered = 'true'">[X]</xsl:when>
        <xsl:otherwise>[ ]</xsl:otherwise>
      </xsl:choose>
      <xsl:if test="@returnType != ''">&nbsp;<xsl:value-of select="@returnType" /></xsl:if>
      <b>&nbsp;<xsl:value-of select="@name" /></b><xsl:call-template name="formatParameters" />
    </span>
    <br />
  </xsl:template>

  <xsl:template name="formatParameters">(<xsl:for-each select="parameter">
      <xsl:value-of select="@type" />
      <xsl:if test="not(position() = last())">,&nbsp;</xsl:if>
    </xsl:for-each>)
  </xsl:template>
  
</xsl:stylesheet>

