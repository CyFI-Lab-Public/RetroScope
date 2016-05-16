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
import com.google.clearsilver.jsilver.exceptions.JSilverBadSyntaxException;
import com.google.clearsilver.jsilver.exceptions.JSilverIOException;
import com.google.clearsilver.jsilver.syntax.lexer.Lexer;
import com.google.clearsilver.jsilver.syntax.lexer.LexerException;
import com.google.clearsilver.jsilver.syntax.node.Start;
import com.google.clearsilver.jsilver.syntax.node.Switch;
import com.google.clearsilver.jsilver.syntax.parser.Parser;
import com.google.clearsilver.jsilver.syntax.parser.ParserException;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.Arrays;

/**
 * Parses a JSilver text template into an abstract syntax tree (AST).
 * <p/>
 * Acts as a facade around SableCC generated code. The simplest way to process the resulting tree is
 * to use a visitor by extending
 * {@link com.google.clearsilver.jsilver.syntax.analysis.DepthFirstAdapter} and passing it to
 * {@link Start#apply(com.google.clearsilver.jsilver.syntax.node.Switch)}.
 * <p/>
 * <h3>Example:</h3>
 * 
 * <pre>
 * SyntaxTreeBuilder builder = new SyntaxTreeBuilder();
 * Start tree = builder.parse(myTemplate, "some-template.cs");
 * // Dump out the tree
 * tree.apply(new SyntaxTreeDumper(System.out));
 * </pre>
 * 
 */
public class SyntaxTreeBuilder {

  public SyntaxTreeBuilder() {}

  /**
   * Size of buffer in PushbackReader... needs to be large enough to parse CS opening tag and push
   * back if it is not valid. e.g. "&lt;?csX" : not a tag, so pushback.
   */
  private static final int PUSHBACK_SIZE = "<?cs ".length();

  /**
   * Syntax tree optimizers, declared in the order they must be applied:
   * <ol>
   * <li>Type resultion makes the abstract tree concrete and must come first.
   * <li>Sequence optimization simplifies the tree and should come before most other optimizations.
   * <li>Inline rewriting to remove data nodes from 'inline' sections. This should come before any
   * optimization of variables.
   * <li>Var optimization simplifies complex var expressions and must come after both type
   * resolution and sequence optimization.
   * </ol>
   */
  protected final Switch typeResolver = new TypeResolver();
  protected final Switch sequenceOptimizer = new SequenceOptimizer();
  protected final Switch inlineRewriter = new InlineRewriter();
  protected final Switch varOptimizer = new VarOptimizer(Arrays.asList("html", "js", "url"));

  /**
   * Perform any additional processing on the tree. EscapeMode and templateName are required by
   * AutoEscaper.
   * 
   * @param root The AST to post process.
   * @param escapeMode The escaping mode to apply to the given AST. If this is not
   *        EscapeMode.ESCAPE_NONE, AutoEscaper will be called on the AST.
   * @param templateName The name of template being processed. Passed to AutoEscaper, which uses it
   *        when displaying error messages.
   */
  protected void process(Start root, EscapeMode escapeMode, String templateName) {
    root.apply(typeResolver);
    root.apply(sequenceOptimizer);
    root.apply(inlineRewriter);
    // Temporarily disabled ('cos it doesn't quite work)
    // root.apply(varOptimizer);

    if (!escapeMode.equals(EscapeMode.ESCAPE_NONE)) {
      // AutoEscaper contains per-AST context like HTML parser object.
      // Therefore, instantiating a new AutoEscaper each time.
      root.apply(new AutoEscaper(escapeMode, templateName));
    }
  }

  /**
   * @param templateName Used for meaningful error messages.
   * @param escapeMode Run {@link AutoEscaper} on the abstract syntax tree created from template.
   */
  public TemplateSyntaxTree parse(Reader input, String templateName, EscapeMode escapeMode)
      throws JSilverIOException, JSilverBadSyntaxException {
    try {
      PushbackReader pushbackReader = new PushbackReader(input, PUSHBACK_SIZE);
      Lexer lexer = new Lexer(pushbackReader);
      Parser parser = new Parser(lexer);
      Start root = parser.parse();
      process(root, escapeMode, templateName);
      return new TemplateSyntaxTree(root);
    } catch (IOException exception) {
      throw new JSilverIOException(exception);
    } catch (ParserException exception) {
      throw new JSilverBadSyntaxException(exception.getMessage(), exception.getToken().getText(),
          templateName, exception.getToken().getLine(), exception.getToken().getPos(), exception);
    } catch (LexerException exception) {
      throw new JSilverBadSyntaxException(exception.getMessage(), null, templateName,
          JSilverBadSyntaxException.UNKNOWN_POSITION, JSilverBadSyntaxException.UNKNOWN_POSITION,
          exception);
    }
  }
}
