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

import com.google.clearsilver.jsilver.autoescape.EscapeMode;
import com.google.clearsilver.jsilver.data.DataContext;
import com.google.clearsilver.jsilver.functions.FunctionExecutor;
import com.google.clearsilver.jsilver.syntax.analysis.DepthFirstAdapter;
import com.google.clearsilver.jsilver.syntax.node.AAddExpression;
import com.google.clearsilver.jsilver.syntax.node.AAndExpression;
import com.google.clearsilver.jsilver.syntax.node.ADecimalExpression;
import com.google.clearsilver.jsilver.syntax.node.ADescendVariable;
import com.google.clearsilver.jsilver.syntax.node.ADivideExpression;
import com.google.clearsilver.jsilver.syntax.node.AEqExpression;
import com.google.clearsilver.jsilver.syntax.node.AExistsExpression;
import com.google.clearsilver.jsilver.syntax.node.AFunctionExpression;
import com.google.clearsilver.jsilver.syntax.node.AGtExpression;
import com.google.clearsilver.jsilver.syntax.node.AGteExpression;
import com.google.clearsilver.jsilver.syntax.node.AHexExpression;
import com.google.clearsilver.jsilver.syntax.node.ALtExpression;
import com.google.clearsilver.jsilver.syntax.node.ALteExpression;
import com.google.clearsilver.jsilver.syntax.node.AModuloExpression;
import com.google.clearsilver.jsilver.syntax.node.AMultiplyExpression;
import com.google.clearsilver.jsilver.syntax.node.ANameVariable;
import com.google.clearsilver.jsilver.syntax.node.ANeExpression;
import com.google.clearsilver.jsilver.syntax.node.ANegativeExpression;
import com.google.clearsilver.jsilver.syntax.node.ANotExpression;
import com.google.clearsilver.jsilver.syntax.node.ANumericAddExpression;
import com.google.clearsilver.jsilver.syntax.node.ANumericEqExpression;
import com.google.clearsilver.jsilver.syntax.node.ANumericExpression;
import com.google.clearsilver.jsilver.syntax.node.ANumericNeExpression;
import com.google.clearsilver.jsilver.syntax.node.AOrExpression;
import com.google.clearsilver.jsilver.syntax.node.AStringExpression;
import com.google.clearsilver.jsilver.syntax.node.ASubtractExpression;
import com.google.clearsilver.jsilver.syntax.node.AVariableExpression;
import com.google.clearsilver.jsilver.syntax.node.PExpression;
import com.google.clearsilver.jsilver.values.Value;
import static com.google.clearsilver.jsilver.values.Value.literalValue;

import java.util.LinkedList;

/**
 * Walks the tree of a PExpression node and evaluates the expression.
 * @see #evaluate(PExpression)
 */
public class ExpressionEvaluator extends DepthFirstAdapter {

  private Value currentValue;

  private final DataContext context;

  private final FunctionExecutor functionExecutor;

  /**
   * @param context
   * @param functionExecutor Used for executing functions in expressions. As well as looking up
   *        named functions (e.g. html_escape), it also uses
   */
  public ExpressionEvaluator(DataContext context, FunctionExecutor functionExecutor) {
    this.context = context;
    this.functionExecutor = functionExecutor;
  }

  /**
   * Evaluate an expression into a single value.
   */
  public Value evaluate(PExpression expression) {
    assert currentValue == null;

    expression.apply(this);
    Value result = currentValue;
    currentValue = null;

    assert result != null : "No result set from " + expression.getClass();
    return result;
  }

  @Override
  public void caseAVariableExpression(AVariableExpression node) {
    VariableLocator variableLocator = new VariableLocator(this);
    String variableName = variableLocator.getVariableName(node.getVariable());
    setResult(Value.variableValue(variableName, context));
  }

  @Override
  public void caseAStringExpression(AStringExpression node) {
    String value = node.getValue().getText();
    value = value.substring(1, value.length() - 1); // Remove enclosing quotes.
    // The expression was a constant string literal. Does not
    // need to be autoescaped, as it was created by the template developer.
    Value result = literalValue(value, EscapeMode.ESCAPE_IS_CONSTANT, false);
    setResult(result);
  }

  @Override
  public void caseADecimalExpression(ADecimalExpression node) {
    String value = node.getValue().getText();
    setResult(literalValue(Integer.parseInt(value), EscapeMode.ESCAPE_IS_CONSTANT, false));
  }

  @Override
  public void caseAHexExpression(AHexExpression node) {
    String value = node.getValue().getText();
    value = value.substring(2); // Remove 0x prefix.
    setResult(literalValue(Integer.parseInt(value, 16), EscapeMode.ESCAPE_IS_CONSTANT, false));
  }

  @Override
  public void caseANumericExpression(ANumericExpression node) {
    executeFunction("#", node.getExpression());
  }

  @Override
  public void caseANotExpression(ANotExpression node) {
    executeFunction("!", node.getExpression());
  }

  @Override
  public void caseAExistsExpression(AExistsExpression node) {
    executeFunction("?", node.getExpression());
  }

  @Override
  public void caseAEqExpression(AEqExpression node) {
    executeFunction("==", node.getLeft(), node.getRight());
  }

  @Override
  public void caseANumericEqExpression(ANumericEqExpression node) {
    executeFunction("#==", node.getLeft(), node.getRight());
  }

  @Override
  public void caseANeExpression(ANeExpression node) {
    executeFunction("!=", node.getLeft(), node.getRight());
  }

  @Override
  public void caseANumericNeExpression(ANumericNeExpression node) {
    executeFunction("#!=", node.getLeft(), node.getRight());
  }

  @Override
  public void caseALtExpression(ALtExpression node) {
    executeFunction("<", node.getLeft(), node.getRight());
  }

  @Override
  public void caseAGtExpression(AGtExpression node) {
    executeFunction(">", node.getLeft(), node.getRight());
  }

  @Override
  public void caseALteExpression(ALteExpression node) {
    executeFunction("<=", node.getLeft(), node.getRight());
  }

  @Override
  public void caseAGteExpression(AGteExpression node) {
    executeFunction(">=", node.getLeft(), node.getRight());
  }

  @Override
  public void caseAAndExpression(AAndExpression node) {
    executeFunction("&&", node.getLeft(), node.getRight());
  }

  @Override
  public void caseAOrExpression(AOrExpression node) {
    executeFunction("||", node.getLeft(), node.getRight());
  }

  @Override
  public void caseAAddExpression(AAddExpression node) {
    executeFunction("+", node.getLeft(), node.getRight());
  }

  @Override
  public void caseANumericAddExpression(ANumericAddExpression node) {
    executeFunction("#+", node.getLeft(), node.getRight());
  }

  @Override
  public void caseASubtractExpression(ASubtractExpression node) {
    executeFunction("-", node.getLeft(), node.getRight());
  }

  @Override
  public void caseAMultiplyExpression(AMultiplyExpression node) {
    executeFunction("*", node.getLeft(), node.getRight());
  }

  @Override
  public void caseADivideExpression(ADivideExpression node) {
    executeFunction("/", node.getLeft(), node.getRight());
  }

  @Override
  public void caseAModuloExpression(AModuloExpression node) {
    executeFunction("%", node.getLeft(), node.getRight());
  }

  @Override
  public void caseANegativeExpression(ANegativeExpression node) {
    executeFunction("-", node.getExpression());
  }

  @Override
  public void caseAFunctionExpression(AFunctionExpression node) {
    LinkedList<PExpression> argsList = node.getArgs();
    PExpression[] args = argsList.toArray(new PExpression[argsList.size()]);

    executeFunction(getFullFunctionName(node), args);
  }

  private void executeFunction(String name, PExpression... expressions) {
    Value[] args = new Value[expressions.length];
    for (int i = 0; i < args.length; i++) {
      args[i] = evaluate(expressions[i]);
    }

    setResult(functionExecutor.executeFunction(name, args));
  }

  /**
   * Sets a result from inside an expression.
   */
  private void setResult(Value value) {
    assert value != null;

    currentValue = value;
  }

  private String getFullFunctionName(AFunctionExpression node) {
    final StringBuilder result = new StringBuilder();
    node.getName().apply(new DepthFirstAdapter() {

      @Override
      public void caseANameVariable(ANameVariable node) {
        result.append(node.getWord().getText());
      }

      @Override
      public void caseADescendVariable(ADescendVariable node) {
        node.getParent().apply(this);
        result.append('.');
        node.getChild().apply(this);
      }
    });
    return result.toString();
  }

}
