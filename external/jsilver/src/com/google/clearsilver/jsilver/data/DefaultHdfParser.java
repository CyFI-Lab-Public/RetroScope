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

package com.google.clearsilver.jsilver.data;

import com.google.clearsilver.jsilver.resourceloader.ResourceLoader;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses data in HierachicalDataFormat (HDF), generating callbacks for data encountered in the
 * stream.
 */
public class DefaultHdfParser implements Parser {

  private int initialContextSize = 10;

  public void parse(Reader reader, Data output, ErrorHandler errorHandler,
      ResourceLoader resourceLoader, String dataFileName, boolean ignoreAttributes)
      throws IOException {
    LineNumberReader lineReader = new LineNumberReader(reader);
    // Although a linked list could be used here, we iterate a lot and the
    // size will rarely get > 10 deep. In this case ArrayList is faster than
    // LinkedList.
    List<String> context = new ArrayList<String>(initialContextSize);
    String line;
    while ((line = lineReader.readLine()) != null) {
      parseLine(line, output, context, lineReader, dataFileName, errorHandler);
    }
  }

  private void parseLine(String line, Data output, List<String> context,
      LineNumberReader lineReader, String dataFileName, ErrorHandler errorHandler)
      throws IOException {
    line = stripComment(line);

    Split split;
    if ((split = split(line, "=")) != null) {
      // some.thing = Hello
      output.setValue(createFullPath(context, split.left), split.right);
    } else if ((split = split(line, "<<")) != null) {
      // some.thing << EOM
      // Blah blah
      // Blah blah
      // EOM
      output.setValue(createFullPath(context, split.left), readToToken(lineReader, split.right));
    } else if ((split = split(line, "{")) != null) {
      // some.thing {
      // ...
      context.add(split.left);
    } else if (split(line, "}") != null) {
      // ...
      // }
      context.remove(context.size() - 1);
    } else if ((split = split(line, ":")) != null) {
      // some.tree : another.tree
      output.setSymlink(createFullPath(context, split.left), split.right);
    } else if (line.trim().length() != 0) {
      // Anything else
      if (errorHandler != null) {
        errorHandler.error(lineReader.getLineNumber(), line, dataFileName, "Bad HDF syntax");
      }
    }
  }

  private String stripComment(String line) {
    int commentPosition = line.indexOf('#');
    int equalsPosition = line.indexOf('=');
    if (commentPosition > -1 && (equalsPosition == -1 || commentPosition < equalsPosition)) {
      return line.substring(0, commentPosition);
    } else {
      return line;
    }
  }

  /**
   * Reads lines from a reader until a line is encountered that matches token (or end of stream).
   */
  private String readToToken(LineNumberReader reader, String token) throws IOException {
    StringBuilder result = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null && !line.trim().equals(token)) {
      result.append(line).append('\n');
    }
    return result.toString();
  }

  /**
   * Creates the full path, based on the current context.
   */
  private String createFullPath(List<String> context, String subPath) {
    StringBuilder result = new StringBuilder();
    for (String contextItem : context) {
      result.append(contextItem).append('.');
    }
    result.append(subPath);
    return result.toString();
  }

  /**
   * Split a line in two, based on a delimiter. If the delimiter is not found, null is returned.
   */
  private Split split(String line, String delimiter) {
    int position = line.indexOf(delimiter);
    if (position > -1) {
      Split result = new Split();
      result.left = line.substring(0, position).trim();
      result.right = line.substring(position + delimiter.length()).trim();
      return result;
    } else {
      return null;
    }
  }

  private static class Split {
    String left;
    String right;
  }

  /**
   * Returns a factory object that constructs DefaultHdfParser objects.
   */
  public static ParserFactory newFactory() {
    return new ParserFactory() {
      public Parser newInstance() {
        return new DefaultHdfParser();
      }
    };
  }

}
