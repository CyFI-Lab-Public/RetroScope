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

package com.google.clearsilver.jsilver.interpreter;

import com.google.clearsilver.jsilver.syntax.analysis.DepthFirstAdapter;
import com.google.clearsilver.jsilver.syntax.node.ADecNumberVariable;
import com.google.clearsilver.jsilver.syntax.node.ADescendVariable;
import com.google.clearsilver.jsilver.syntax.node.AExpandVariable;
import com.google.clearsilver.jsilver.syntax.node.AHexNumberVariable;
import com.google.clearsilver.jsilver.syntax.node.ANameVariable;
import com.google.clearsilver.jsilver.syntax.node.PVariable;
import com.google.clearsilver.jsilver.values.Value;

/**
 * Walks a PVariable node from the parse tree and returns a Data path name.
 *
 * @see #getVariableName(com.google.clearsilver.jsilver.syntax.node.PVariable)
 */
public class VariableLocator extends DepthFirstAdapter {

  private StringBuilder currentName;

  private final ExpressionEvaluator expressionEvaluator;

  public VariableLocator(ExpressionEvaluator expressionEvaluator) {
    this.expressionEvaluator = expressionEvaluator;
  }

  /**
   * If the root PVariable we are evaluating is a simple one then skip creating the StringBuilder
   * and descending the tree.
   * 
   * @param variable the variable node to evaluate.
   * @return a String representing the Variable name, or {@code null} if it is a compound variable
   *         node.
   */
  private String quickEval(PVariable variable) {
    if (variable instanceof ANameVariable) {
      return ((ANameVariable) variable).getWord().getText();
    } else if (variable instanceof ADecNumberVariable) {
      return ((ADecNumberVariable) variable).getDecNumber().getText();
    } else if (variable instanceof AHexNumberVariable) {
      return ((AHexNumberVariable) variable).getHexNumber().getText();
    } else {
      // This is a compound variable. Evaluate the slow way.
      return null;
    }
  }

  /**
   * Returns a Data variable name extracted during evaluation.
   * 
   * @param variable the parsed variable name node to traverse
   */
  public String getVariableName(PVariable variable) {
    String result = quickEval(variable);
    if (result != null) {
      return result;
    }
    StringBuilder lastName = currentName;
    currentName = new StringBuilder(10);
    variable.apply(this);
    result = currentName.toString();
    currentName = lastName;
    return result;
  }

  @Override
  public void caseANameVariable(ANameVariable node) {
    descendVariable(node.getWord().getText());
  }

  @Override
  public void caseADecNumberVariable(ADecNumberVariable node) {
    descendVariable(node.getDecNumber().getText());
  }

  @Override
  public void caseAHexNumberVariable(AHexNumberVariable node) {
    descendVariable(node.getHexNumber().getText());
  }

  @Override
  public void caseADescendVariable(ADescendVariable node) {
    node.getParent().apply(this);
    node.getChild().apply(this);
  }

  @Override
  public void caseAExpandVariable(AExpandVariable node) {
    node.getParent().apply(this);
    Value value = expressionEvaluator.evaluate(node.getChild());
    descendVariable(value.asString());
  }

  private void descendVariable(String name) {
    if (currentName.length() != 0) {
      currentName.append('.');
    }
    currentName.append(name);
  }
}
