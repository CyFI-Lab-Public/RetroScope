/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.clearsilver.jsilver.precompiler;

import com.google.clearsilver.jsilver.autoescape.EscapeMode;
import com.google.clearsilver.jsilver.exceptions.JSilverAutoEscapingException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Utility class that reads in the file output by BatchCompiler that is a list of template names and
 * corresponding class names and returns a Map of template filenames to class names which can be fed
 * to {@see com.google.clearsilver.jsilver.JSilverOptions#setPrecompiledTemplateMap}
 */
public class PrecompiledTemplateMapFileReader {

  private final String mapFileName;
  private final String dirPattern;
  private final String rootDir;

  private Map<Object, String> templateMap = null;

  /**
   * Helper object that reads in the specified resource file and generates a mapping of template
   * filenames to corresponding BaseCompiledTemplate class names.
   * 
   * @param filename name of the resource file to read the map from.
   * @param dirPattern prefix to remove from read in template names. Used in conjunction with
   *        rootDir to update template file paths.
   * @param rootDir optional string to prepend to all non-absolute template filenames. Should be set
   *        to the location of the templates in production via a flag.
   */
  public PrecompiledTemplateMapFileReader(String filename, String dirPattern, String rootDir) {
    this.mapFileName = filename;
    this.dirPattern = dirPattern;
    this.rootDir = rootDir;
  }

  public Map<Object, String> getTemplateMap() throws IOException {
    if (templateMap == null) {
      templateMap = makeTemplateMap(mapFileName, rootDir);
    }
    return templateMap;
  }

  private Map<Object, String> makeTemplateMap(String templateMapFile, String rootDir)
      throws IOException {
    Map<Object, String> templateMap = new HashMap<Object, String>();
    LineNumberReader reader = null;
    try {
      reader = new LineNumberReader(getMapFileReader(templateMapFile));
      for (String line = reader.readLine(); line != null; line = reader.readLine()) {
        // Process single line from the templateMapFile
        // and put found templates into templateMap.
        processTemplateMapFileLine(line, reader.getLineNumber(), templateMap, templateMapFile,
            rootDir);
      }
    } finally {
      if (reader != null) {
        reader.close();
      }
    }
    return ImmutableMap.copyOf(templateMap);
  }

  private void processTemplateMapFileLine(String line, int lineNumber,
      Map<Object, String> templateMap, String templateMapFile, String rootDir) {

    line = line.trim();
    if (line.isEmpty() || line.startsWith("#")) {
      // Ignore blank lines and comment lines.
      return;
    }
    StringTokenizer st = new StringTokenizer(line);
    if (!st.hasMoreTokens()) {
      throw new IllegalArgumentException("No template file name found in " + templateMapFile
          + " on line " + lineNumber + ": " + line);
    }
    String templateName = st.nextToken();
    if (dirPattern != null && templateName.startsWith(dirPattern)) {
      templateName = templateName.substring(dirPattern.length());
    }
    if (rootDir != null) {
      // If it is not an absolute path and we were given a root directory,
      // prepend it.
      templateName = rootDir + templateName;
    }
    if (!st.hasMoreTokens()) {
      throw new IllegalArgumentException("No class name found in " + templateMapFile + " on line "
          + lineNumber + ": " + line);
    }
    String className = st.nextToken();
    EscapeMode escapeMode;
    if (!st.hasMoreTokens()) {
      escapeMode = EscapeMode.ESCAPE_NONE;
    } else {
      String escapeCmd = st.nextToken();
      try {
        escapeMode = EscapeMode.computeEscapeMode(escapeCmd);
      } catch (JSilverAutoEscapingException e) {
        throw new IllegalArgumentException("Invalid escape mode found in " + templateMapFile
            + " on line " + lineNumber + ": " + escapeCmd);
      }
    }
    PrecompiledTemplateMapKey key = new PrecompiledTemplateMapKey(templateName, escapeMode);
    templateMap.put(key, className);
  }

  @VisibleForTesting
  protected Reader getMapFileReader(String templateMapFile) throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    InputStream in = classLoader.getResourceAsStream(templateMapFile);
    if (in == null) {
      throw new FileNotFoundException("Unable to locate resource: " + templateMapFile);
    }
    return new InputStreamReader(in, "UTF-8");
  }

}
