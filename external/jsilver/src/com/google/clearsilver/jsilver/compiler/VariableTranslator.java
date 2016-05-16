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

package com.google.clearsilver.jsilver.compiler;

import static com.google.clearsilver.jsilver.compiler.JavaExpression.StringExpression;
import static com.google.clearsilver.jsilver.compiler.JavaExpression.Type;
import static com.google.clearsilver.jsilver.compiler.JavaExpression.literal;
import com.google.clearsilver.jsilver.syntax.analysis.DepthFirstAdapter;
import com.google.clearsilver.jsilver.syntax.node.ADecNumberVariable;
import com.google.clearsilver.jsilver.syntax.node.ADescendVariable;
import com.google.clearsilver.jsilver.syntax.node.AExpandVariable;
import com.google.clearsilver.jsilver.syntax.node.AHexNumberVariable;
import com.google.clearsilver.jsilver.syntax.node.ANameVariable;
import com.google.clearsilver.jsilver.syntax.node.PVariable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Translates a variable name (e.g. search.results.3.title) into the Java code for use as a key in
 * looking up a variable (e.g. "search.results.3.title").
 * 
 * While it is possible to reuse an instance of this class repeatedly, it is not thread safe or
 * reentrant. Evaluating an expression such as: <code>a.b[c.d]</code> would require two instances.
 */
public class VariableTranslator extends DepthFirstAdapter {

  private List<JavaExpression> components;

  private final ExpressionTranslator expressionTranslator;

  public VariableTranslator(ExpressionTranslator expressionTranslator) {
    this.expressionTranslator = expressionTranslator;
  }

  /**
   * See class description.
   * 
   * @param csVariable Variable node in template's AST.
   * @return Appropriate code (as JavaExpression).
   */
  public JavaExpression translate(PVariable csVariable) {
    try {
      assert components == null;
      components = new ArrayList<JavaExpression>();
      csVariable.apply(this);
      components = joinComponentsWithDots(components);
      components = combineAdjacentStrings(components);
      return concatenate(components);
    } finally {
      components = null;
    }
  }

  @Override
  public void caseANameVariable(ANameVariable node) {
    components.add(new StringExpression(node.getWord().getText()));
  }

  @Override
  public void caseADecNumberVariable(ADecNumberVariable node) {
    components.add(new StringExpression(node.getDecNumber().getText()));
  }

  @Override
  public void caseAHexNumberVariable(AHexNumberVariable node) {
    components.add(new StringExpression(node.getHexNumber().getText()));
  }

  @Override
  public void caseADescendVariable(ADescendVariable node) {
    node.getParent().apply(this);
    node.getChild().apply(this);
  }

  @Override
  public void caseAExpandVariable(AExpandVariable node) {
    node.getParent().apply(this);
    components.add(expressionTranslator.translateToString(node.getChild()));
  }

  /**
   * Inserts dots between each component in the path.
   * 
   * e.g. from: "a", "b", something, "c" to: "a", ".", "b", ".", something, ".", "c"
   */
  private List<JavaExpression> joinComponentsWithDots(List<JavaExpression> in) {
    List<JavaExpression> out = new ArrayList<JavaExpression>(in.size() * 2);
    for (JavaExpression component : in) {
      if (!out.isEmpty()) {
        out.add(DOT);
      }
      out.add(component);
    }
    return out;
  }

  private static final JavaExpression DOT = new StringExpression(".");

  /**
   * Combines adjacent strings.
   * 
   * e.g. from: "a", ".", "b", ".", something, ".", "c" to : "a.b.", something, ".c"
   */
  private List<JavaExpression> combineAdjacentStrings(List<JavaExpression> in) {
    assert !in.isEmpty();
    List<JavaExpression> out = new ArrayList<JavaExpression>(in.size());
    JavaExpression last = null;
    for (JavaExpression current : in) {
      if (last == null) {
        last = current;
        continue;
      }
      if (current instanceof StringExpression && last instanceof StringExpression) {
        // Last and current are both strings - combine them.
        StringExpression currentString = (StringExpression) current;
        StringExpression lastString = (StringExpression) last;
        last = new StringExpression(lastString.getValue() + currentString.getValue());
      } else {
        out.add(last);
        last = current;
      }
    }
    out.add(last);
    return out;
  }

  /**
   * Concatenate a list of JavaExpressions into a single string.
   * 
   * e.g. from: "a", "b", stuff to : "a" + "b" + stuff
   */
  private JavaExpression concatenate(List<JavaExpression> expressions) {
    StringWriter buffer = new StringWriter();
    PrintWriter out = new PrintWriter(buffer);
    boolean seenFirst = false;
    for (JavaExpression expression : expressions) {
      if (seenFirst) {
        out.print(" + ");
      }
      seenFirst = true;
      expression.write(out);
    }
    return literal(Type.VAR_NAME, buffer.toString());
  }

}
