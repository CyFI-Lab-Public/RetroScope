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
import java.util.Iterator;
import java.util.Stack;

/**
 * Parser for HDF based on the following grammar by Brandon Long.
 * 
 * COMMAND := (INCLUDE | COMMENT | HDF_SET | HDF_DESCEND | HDF_ASCEND ) INCLUDE := #include
 * "FILENAME" EOL COMMENT := # .* EOL HDF_DESCEND := HDF_NAME_ATTRS { EOL HDF_ASCEND := } EOL
 * HDF_SET := (HDF_ASSIGN | HDF_MULTILINE_ASSIGN | HDF_COPY | HDF_LINK) HDF_ASSIGN := HDF_NAME_ATTRS
 * = .* EOL HDF_MULTILINE_ASSIGN := HDF_NAME_ATTRS << EOM_MARKER EOL (.* EOL)* EOM_MARKER EOL
 * HDF_COPY := HDF_NAME_ATTRS := HDF_NAME EOL HDF_LINK := HDF_NAME_ATTRS : HDF_NAME EOL
 * HDF_NAME_ATTRS := (HDF_NAME | HDF_NAME [HDF_ATTRS]) HDF_ATTRS := (HDF_ATTR | HDF_ATTR, HDF_ATTRS)
 * HDF_ATTR := (HDF_ATTR_KEY | HDF_ATTR_KEY = [^\s,\]]+ | HDF_ATTR_KEY = DQUOTED_STRING)
 * HDF_ATTR_KEY := [0-9a-zA-Z]+ DQUOTED_STRING := "([^\\"]|\\[ntr]|\\.)*" HDF_NAME := (HDF_SUB_NAME
 * | HDF_SUB_NAME\.HDF_NAME) HDF_SUB_NAME := [0-9a-zA-Z_]+ EOM_MARKER := \S.*\S EOL := \n
 */
public class NewHdfParser implements Parser {

  private final StringInternStrategy internStrategy;

  /**
   * Special exception used to detect when we unexpectedly run out of characters on the line.
   */
  private static class OutOfCharsException extends Exception {}

  /**
   * Object used to hold the name and attributes of an HDF node before we are ready to commit it to
   * the Data object.
   */
  private static class HdfNameAttrs {
    String name;
    ArrayList<String> attrs = null;
    int endOfSequence;

    void reset(String newname) {
      // TODO: think about moving interning here instead of parser code
      this.name = newname;
      if (attrs != null) {
        attrs.clear();
      }
      endOfSequence = 0;
    }

    void addAttribute(String key, String value) {
      if (attrs == null) {
        attrs = new ArrayList<String>(10);
      }
      attrs.ensureCapacity(attrs.size() + 2);
      // TODO: think about moving interning here instead of parser code
      attrs.add(key);
      attrs.add(value);
    }

    Data toData(Data data) {
      Data child = data.createChild(name);
      if (attrs != null) {
        Iterator<String> it = attrs.iterator();
        while (it.hasNext()) {
          String key = it.next();
          String value = it.next();
          child.setAttribute(key, value);
        }
      }
      return child;
    }
  }

  static final String UNNAMED_INPUT = "[UNNAMED_INPUT]";

  /**
   * State information that we pass through the parse methods. Allows parser to be reentrant as all
   * the state is passed through method calls.
   */
  static class ParseState {
    final Stack<Data> context = new Stack<Data>();
    final Data output;
    final LineNumberReader lineReader;
    final ErrorHandler errorHandler;
    final ResourceLoader resourceLoader;
    final NewHdfParser hdfParser;
    final boolean ignoreAttributes;
    final HdfNameAttrs hdfNameAttrs;
    final UniqueStack<String> includeStack;
    final String parsedFileName;

    String line;
    Data currentNode;

    private ParseState(Data output, LineNumberReader lineReader, ErrorHandler errorHandler,
        ResourceLoader resourceLoader, NewHdfParser hdfParser, String parsedFileName,
        boolean ignoreAttributes, HdfNameAttrs hdfNameAttrs, UniqueStack<String> includeStack) {
      this.lineReader = lineReader;
      this.errorHandler = errorHandler;
      this.output = output;
      currentNode = output;
      this.resourceLoader = resourceLoader;
      this.hdfParser = hdfParser;
      this.parsedFileName = parsedFileName;
      this.ignoreAttributes = ignoreAttributes;
      this.hdfNameAttrs = hdfNameAttrs;
      this.includeStack = includeStack;
    }

    public static ParseState createNewParseState(Data output, Reader reader,
        ErrorHandler errorHandler, ResourceLoader resourceLoader, NewHdfParser hdfParser,
        String parsedFileName, boolean ignoreAttributes) {

      if (parsedFileName == null) {
        parsedFileName = UNNAMED_INPUT;
      }
      UniqueStack<String> includeStack = new UniqueStack<String>();
      includeStack.push(parsedFileName);

      return new ParseState(output, new LineNumberReader(reader), errorHandler, resourceLoader,
          hdfParser, parsedFileName, ignoreAttributes, new HdfNameAttrs(), includeStack);
    }

    public static ParseState createParseStateForIncludedFile(ParseState originalState,
        String includeFileName, Reader includeFileReader) {
      return new ParseState(originalState.output, new LineNumberReader(includeFileReader),
          originalState.errorHandler, originalState.resourceLoader, originalState.hdfParser,
          originalState.parsedFileName, originalState.ignoreAttributes, new HdfNameAttrs(),
          originalState.includeStack);
    }
  }


  /**
   * Constructor for {@link NewHdfParser}.
   * 
   * @param internPool - {@link StringInternStrategy} instance used to optimize the HDF parsing.
   */
  public NewHdfParser(StringInternStrategy internPool) {
    this.internStrategy = internPool;
  }

  private static class NewHdfParserFactory implements ParserFactory {
    private final StringInternStrategy stringInternStrategy;

    public NewHdfParserFactory(StringInternStrategy stringInternStrategy) {
      this.stringInternStrategy = stringInternStrategy;
    }

    @Override
    public Parser newInstance() {
      return new NewHdfParser(stringInternStrategy);
    }
  }

  /**
   * Creates a {@link ParserFactory} instance.
   * 
   * <p>
   * Provided {@code stringInternStrategy} instance will be used by shared all {@link Parser}
   * objects created by the factory and used to optimize the HDF parsing process by reusing the
   * String for keys and values.
   * 
   * @param stringInternStrategy - {@link StringInternStrategy} instance used to optimize the HDF
   *        parsing.
   * @return an instance of {@link ParserFactory} implementation.
   */
  public static ParserFactory newFactory(StringInternStrategy stringInternStrategy) {
    return new NewHdfParserFactory(stringInternStrategy);
  }

  public void parse(Reader reader, Data output, Parser.ErrorHandler errorHandler,
      ResourceLoader resourceLoader, String dataFileName, boolean ignoreAttributes)
      throws IOException {

    parse(ParseState.createNewParseState(output, reader, errorHandler, resourceLoader, this,
        dataFileName, ignoreAttributes));
  }

  private void parse(ParseState state) throws IOException {
    while ((state.line = state.lineReader.readLine()) != null) {
      String seq = stripWhitespace(state.line);
      try {
        parseCommand(seq, state);
      } catch (OutOfCharsException e) {
        reportError(state, "End of line was prematurely reached. Parse error.");
      }
    }
  }

  private static final String INCLUDE_WS = "#include ";

  private void parseCommand(String seq, ParseState state) throws IOException, OutOfCharsException {
    if (seq.length() == 0) {
      // Empty line.
      return;
    }
    if (charAt(seq, 0) == '#') {
      // If there isn't a match on include then this is a comment and we do nothing.
      if (matches(seq, 0, INCLUDE_WS)) {
        // This is an include command
        int start = skipLeadingWhitespace(seq, INCLUDE_WS.length());
        parseInclude(seq, start, state);
      }
      return;
    } else if (charAt(seq, 0) == '}') {
      if (skipLeadingWhitespace(seq, 1) != seq.length()) {
        reportError(state, "Extra chars after '}'");
        return;
      }
      handleAscend(state);
    } else {
      parseHdfElement(seq, state);
    }
  }

  private void parseInclude(String seq, int start, ParseState state) throws IOException,
      OutOfCharsException {
    int end = seq.length();
    if (charAt(seq, start) == '"') {
      if (charAt(seq, end - 1) == '"') {
        start++;
        end--;
      } else {
        reportError(state, "Missing '\"' at end of include");
        return;
      }
    }
    handleInclude(seq.substring(start, end), state);
  }

  private static final int NO_MATCH = -1;

  private void parseHdfElement(String seq, ParseState state) throws IOException,
      OutOfCharsException {
    // Re-use a single element to avoid repeated allocations/trashing (serious
    // performance impact, 5% of real service performance)
    HdfNameAttrs element = state.hdfNameAttrs;
    if (!parseHdfNameAttrs(element, seq, 0, state)) {
      return;
    }
    int index = skipLeadingWhitespace(seq, element.endOfSequence);
    switch (charAt(seq, index)) {
      case '{':
        // Descend
        if (index + 1 != seq.length()) {
          reportError(state, "No characters expected after '{'");
          return;
        }
        handleDescend(state, element);
        return;
      case '=':
        // Assignment
        index = skipLeadingWhitespace(seq, index + 1);
        String value = internStrategy.intern(seq.substring(index, seq.length()));
        handleAssign(state, element, value);
        return;
      case ':':
        if (charAt(seq, index + 1) == '=') {
          // Copy
          index = skipLeadingWhitespace(seq, index + 2);
          String src = parseHdfName(seq, index);
          if (src == null) {
            reportError(state, "Invalid HDF name");
            return;
          }
          if (index + src.length() != seq.length()) {
            reportError(state, "No characters expected after '{'");
            return;
          }
          handleCopy(state, element, src);
        } else {
          // Link
          index = skipLeadingWhitespace(seq, index + 1);
          String src = parseHdfName(seq, index);
          if (src == null) {
            reportError(state, "Invalid HDF name");
            return;
          }
          if (index + src.length() != seq.length()) {
            reportError(state, "No characters expected after '{'");
            return;
          }
          handleLink(state, element, src);
        }
        return;
      case '<':
        if (charAt(seq, index + 1) != '<') {
          reportError(state, "Expected '<<'");
        }
        index = skipLeadingWhitespace(seq, index + 2);
        String eomMarker = seq.substring(index, seq.length());
        // TODO: think about moving interning to handleAssign()
        String multilineValue = internStrategy.intern(parseMultilineValue(state, eomMarker));
        if (multilineValue == null) {
          return;
        }
        handleAssign(state, element, multilineValue);
        return;
      default:
        reportError(state, "No valid operator");
        return;
    }
  }

  /**
   * This method parses out an HDF element name and any optional attributes into a caller-supplied
   * HdfNameAttrs object. It returns a {@code boolean} with whether it succeeded to parse.
   */
  private boolean parseHdfNameAttrs(HdfNameAttrs destination, String seq, int index,
      ParseState state) throws OutOfCharsException {
    String hdfName = parseHdfName(seq, index);
    if (hdfName == null) {
      reportError(state, "Invalid HDF name");
      return false;
    }
    destination.reset(hdfName);
    index = skipLeadingWhitespace(seq, index + hdfName.length());
    int end = parseAttributes(seq, index, state, destination);
    if (end == NO_MATCH) {
      // Error already reported below.
      return false;
    } else {
      destination.endOfSequence = end;
      return true;
    }
  }

  /**
   * Parses a valid hdf path name.
   */
  private String parseHdfName(String seq, int index) throws OutOfCharsException {
    int end = index;
    while (end < seq.length() && isHdfNameChar(charAt(seq, end))) {
      end++;
    }
    if (end == index) {
      return null;
    }
    return internStrategy.intern(seq.substring(index, end));
  }

  /**
   * Looks for optional attributes and adds them to the HdfNameAttrs object passed into the method.
   */
  private int parseAttributes(String seq, int index, ParseState state, HdfNameAttrs element)
      throws OutOfCharsException {
    if (charAt(seq, index) != '[') {
      // No attributes to parse
      return index;
    }
    index = skipLeadingWhitespace(seq, index + 1);

    // If we don't care about attributes, just skip over them.
    if (state.ignoreAttributes) {
      while (charAt(seq, index) != ']') {
        index++;
      }
      return index + 1;
    }

    boolean first = true;
    do {
      if (first) {
        first = false;
      } else if (charAt(seq, index) == ',') {
        index = skipLeadingWhitespace(seq, index + 1);
      } else {
        reportError(state, "Error parsing attribute list");
      }
      index = parseAttribute(seq, index, state, element);
      if (index == NO_MATCH) {
        // reportError called by parseAttribute already.
        return NO_MATCH;
      }
      index = skipLeadingWhitespace(seq, index);
    } while (charAt(seq, index) != ']');
    return index + 1;
  }

  private static final String DEFAULT_ATTR_VALUE = "1";

  /**
   * Parse out a single HDF attribute. If there is no explicit value, use default value of "1" like
   * in C clearsilver. Returns NO_MATCH if it fails to parse an attribute.
   */
  private int parseAttribute(String seq, int index, ParseState state, HdfNameAttrs element)
      throws OutOfCharsException {
    int end = parseAttributeKey(seq, index);
    if (index == end) {
      reportError(state, "No valid attribute key");
      return NO_MATCH;
    }
    String attrKey = internStrategy.intern(seq.substring(index, end));
    index = skipLeadingWhitespace(seq, end);
    if (charAt(seq, index) != '=') {
      // No value for this attribute key. Use default value of "1"
      element.addAttribute(attrKey, DEFAULT_ATTR_VALUE);
      return index;
    }
    // We need to parse out the attribute value.
    index = skipLeadingWhitespace(seq, index + 1);
    if (charAt(seq, index) == '"') {
      index++;
      StringBuilder sb = new StringBuilder();
      end = parseQuotedAttributeValue(seq, index, sb);
      if (end == NO_MATCH) {
        reportError(state, "Unable to parse quoted attribute value");
        return NO_MATCH;
      }
      String attrValue = internStrategy.intern(sb.toString());
      element.addAttribute(attrKey, attrValue);
      end++;
    } else {
      // Simple attribute that has no whitespace.
      String attrValue = parseAttributeValue(seq, index, state);
      if (attrValue == null || attrValue.length() == 0) {
        reportError(state, "No attribute for key " + attrKey);
        return NO_MATCH;
      }

      attrValue = internStrategy.intern(attrValue);
      element.addAttribute(attrKey, attrValue);
      end = index + attrValue.length();
    }
    return end;
  }

  /**
   * Returns the range in the sequence starting at start that corresponds to a valid attribute key.
   */
  private int parseAttributeKey(String seq, int index) throws OutOfCharsException {
    while (isAlphaNumericChar(charAt(seq, index))) {
      index++;
    }
    return index;
  }

  /**
   * Parses a quoted attribute value. Unescapes octal characters and \n, \r, \t, \", etc.
   */
  private int parseQuotedAttributeValue(String seq, int index, StringBuilder sb)
      throws OutOfCharsException {
    char c;
    while ((c = charAt(seq, index)) != '"') {
      if (c == '\\') {
        // Escaped character. Look for 1 to 3 digits in a row as octal or n,t,r.
        index++;
        char next = charAt(seq, index);
        if (isNumericChar(next)) {
          // Parse the next 1 to 3 characters if they are digits. Treat it as an octal code.
          int val = next - '0';
          if (isNumericChar(charAt(seq, index + 1))) {
            index++;
            val = val * 8 + (charAt(seq, index) - '0');
            if (isNumericChar(charAt(seq, index + 1))) {
              index++;
              val = val * 8 + (charAt(seq, index) - '0');
            }
          }
          c = (char) val;
        } else if (next == 'n') {
          c = '\n';
        } else if (next == 't') {
          c = '\t';
        } else if (next == 'r') {
          c = '\r';
        } else {
          // Regular escaped char like " or /
          c = next;
        }
      }
      sb.append(c);
      index++;
    }
    return index;
  }

  /**
   * Parses a simple attribute value that cannot have any whitespace or specific punctuation
   * reserved by the HDF grammar.
   */
  private String parseAttributeValue(String seq, int index, ParseState state)
      throws OutOfCharsException {
    int end = index;
    char c = charAt(seq, end);
    while (c != ',' && c != ']' && c != '"' && !Character.isWhitespace(c)) {
      end++;
      c = charAt(seq, end);
    }
    return seq.substring(index, end);
  }

  private String parseMultilineValue(ParseState state, String eomMarker) throws IOException {
    StringBuilder sb = new StringBuilder(256);
    String line;
    while ((line = state.lineReader.readLine()) != null) {
      if (line.startsWith(eomMarker)
          && skipLeadingWhitespace(line, eomMarker.length()) == line.length()) {
        return sb.toString();
      } else {
        sb.append(line).append('\n');
      }
    }
    reportError(state, "EOM " + eomMarker + " never found");
    return null;
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Handlers

  private void handleDescend(ParseState state, HdfNameAttrs element) {
    Data child = handleNodeCreation(state.currentNode, element);
    state.context.push(state.currentNode);
    state.currentNode = child;
  }

  private Data handleNodeCreation(Data node, HdfNameAttrs element) {
    return element.toData(node);
  }

  private void handleAssign(ParseState state, HdfNameAttrs element, String value) {
    // TODO: think about moving interning here
    Data child = handleNodeCreation(state.currentNode, element);
    child.setValue(value);
  }

  private void handleCopy(ParseState state, HdfNameAttrs element, String srcName) {
    Data child = handleNodeCreation(state.currentNode, element);
    Data src = state.output.getChild(srcName);
    if (src != null) {
      child.setValue(src.getValue());
    } else {
      child.setValue("");
    }
  }

  private void handleLink(ParseState state, HdfNameAttrs element, String srcName) {
    Data child = handleNodeCreation(state.currentNode, element);
    child.setSymlink(state.output.createChild(srcName));
  }

  private void handleAscend(ParseState state) {
    if (state.context.isEmpty()) {
      reportError(state, "Too many '}'");
      return;
    }
    state.currentNode = state.context.pop();
  }

  private void handleInclude(String seq, ParseState state) throws IOException {
    String includeFileName = internStrategy.intern(seq);

    // Load the file
    Reader reader = state.resourceLoader.open(includeFileName);
    if (reader == null) {
      reportError(state, "Unable to find file " + includeFileName);
      return;
    }

    // Check whether we are in include loop
    if (!state.includeStack.push(includeFileName)) {
      reportError(state, createIncludeStackTraceMessage(state.includeStack, includeFileName));
      return;
    }

    // Parse the file
    state.hdfParser.parse(ParseState
        .createParseStateForIncludedFile(state, includeFileName, reader));

    if (!includeFileName.equals(state.includeStack.pop())) {
      // Include stack trace is corrupted
      throw new IllegalStateException("Unable to find on include stack: " + includeFileName);
    }
  }

  private String createIncludeStackTraceMessage(UniqueStack<String> includeStack,
      String includeFileName) {
    StringBuilder message = new StringBuilder();
    message.append("File included twice: ");
    message.append(includeFileName);

    message.append(" Include stack: ");
    for (String fileName : includeStack) {
      message.append(fileName);
      message.append(" -> ");
    }
    message.append(includeFileName);
    return message.toString();
  }

  // /////////////////////////////////////////////////////////////////////////
  //
  // Character values

  private static boolean isNumericChar(char c) {
    if ('0' <= c && c <= '9') {
      return true;
    } else {
      return false;
    }
  }

  private static boolean isAlphaNumericChar(char c) {
    if (('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || ('0' <= c && c <= '9')) {
      return true;
    } else {
      return false;
    }
  }

  private static boolean isHdfNameChar(char c) {
    if (isAlphaNumericChar(c) || c == '_' || c == '.') {
      return true;
    } else {
      return false;
    }
  }

  private static String stripWhitespace(String seq) {
    int start = skipLeadingWhitespace(seq, 0);
    int end = seq.length() - 1;
    while (end > start && Character.isWhitespace(seq.charAt(end))) {
      --end;
    }
    if (start == 0 && end == seq.length() - 1) {
      return seq;
    } else {
      return seq.substring(start, end + 1);
    }
  }

  private static int skipLeadingWhitespace(String seq, int index) {
    while (index < seq.length() && Character.isWhitespace(seq.charAt(index))) {
      index++;
    }
    return index;
  }

  /**
   * Determines if a character sequence appears in the given sequence starting at a specified index.
   * 
   * @param seq the sequence that we want to see if it contains the string match.
   * @param start the index into seq where we want to check for match
   * @param match the String we want to look for in the sequence.
   * @return {@code true} if the string match appears in seq starting at the index start, {@code
   *         false} otherwise.
   */
  private static boolean matches(String seq, int start, String match) {
    if (seq.length() - start < match.length()) {
      return false;
    }
    for (int i = 0; i < match.length(); i++) {
      if (match.charAt(i) != seq.charAt(start + i)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Reads the character at the specified index in the given String. Throws an exception to be
   * caught above if the index is out of range.
   */
  private static char charAt(String seq, int index) throws OutOfCharsException {
    if (0 <= index && index < seq.length()) {
      return seq.charAt(index);
    } else {
      throw new OutOfCharsException();
    }
  }


  private static void reportError(ParseState state, String errorMessage) {
    if (state.errorHandler != null) {
      state.errorHandler.error(state.lineReader.getLineNumber(), state.line, state.parsedFileName,
          errorMessage);
    } else {
      throw new RuntimeException("Parse Error on line " + state.lineReader.getLineNumber() + ": "
          + errorMessage + " : " + state.line);
    }
  }
}
