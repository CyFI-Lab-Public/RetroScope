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

package com.google.clearsilver.jsilver.syntax;

import com.google.clearsilver.jsilver.autoescape.EscapeMode;
import com.google.clearsilver.jsilver.exceptions.JSilverIOException;
import com.google.clearsilver.jsilver.syntax.analysis.DepthFirstAdapter;
import com.google.clearsilver.jsilver.syntax.node.EOF;
import com.google.clearsilver.jsilver.syntax.node.Node;
import com.google.clearsilver.jsilver.syntax.node.Start;
import com.google.clearsilver.jsilver.syntax.node.Token;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Dumps the syntax tree to text. Useful for debugging and understanding how the tree is structured.
 */
public class SyntaxTreeDumper extends DepthFirstAdapter {

  private final Appendable out;

  private final String newLine = System.getProperty("line.separator");

  private int indent;

  public SyntaxTreeDumper(Appendable out) {
    this.out = out;
  }

  /**
   * Dumps to System.out.
   */
  public SyntaxTreeDumper() {
    this(System.out);
  }

  @Override
  public void defaultIn(Node node) {
    write(nodeName(node) + " {");
    indent++;
  }

  @Override
  public void defaultOut(Node node) {
    indent--;
    write("}");
  }

  @Override
  public void defaultCase(Node node) {
    write(nodeName(node));
  }

  private String nodeName(Node node) {
    if (node instanceof Start || node instanceof EOF) {
      return node.getClass().getSimpleName();
    } else if (node instanceof Token) {
      Token token = (Token) node;
      String tokenType = token.getClass().getSimpleName().substring(1);
      return tokenType + " [line:" + token.getLine() + ",pos:" + token.getPos() + "] \""
          + escape(token.getText()) + "\"";
    } else {
      // Turn PSomeProduction, AConcreteSomeProduction
      // Into SomeProduction, Concrete
      String p = node.getClass().getSuperclass().getSimpleName().substring(1);
      String a = node.getClass().getSimpleName().substring(1);
      a = a.substring(0, a.length() - p.length());
      return "<" + a + ">" + p;
    }

  }

  private String escape(String text) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      switch (c) {
        case '\\':
          result.append("\\\\");
          break;
        case '"':
          result.append("\\\"");
          break;
        case '\n':
          result.append("\\n");
          break;
        case '\r':
          result.append("\\r");
          break;
        case '\t':
          result.append("\\t");
          break;
        default:
          result.append(c);
      }
    }
    return result.toString();
  }

  private void write(String text) {
    try {
      // Write to temp string in case output isn't buffered.
      StringBuilder line = new StringBuilder();
      for (int i = 0; i < indent; i++) {
        line.append("  ");
      }
      line.append(text);
      line.append(newLine);
      out.append(line);
    } catch (IOException e) {
      throw new JSilverIOException(e);
    }
  }

  /**
   * Simple command line tool for parsing a template and dumping out the AST.
   */
  public static void main(String[] args) throws IOException {
    if (args.length == 0) {
      System.err.println("Provide filename of template.");
      return;
    }
    String filename = args[0];
    Reader reader = new BufferedReader(new FileReader(filename));
    try {
      SyntaxTreeBuilder builder = new SyntaxTreeBuilder();
      TemplateSyntaxTree tree = builder.parse(reader, filename, EscapeMode.ESCAPE_NONE);
      tree.apply(new SyntaxTreeDumper(System.out));
    } finally {
      reader.close();
    }
  }

}
