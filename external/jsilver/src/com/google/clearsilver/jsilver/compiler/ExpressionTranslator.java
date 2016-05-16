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

import com.google.clearsilver.jsilver.compiler.JavaExpression.Type;
import static com.google.clearsilver.jsilver.compiler.JavaExpression.bool;
import static com.google.clearsilver.jsilver.compiler.JavaExpression.call;
import static com.google.clearsilver.jsilver.compiler.JavaExpression.callFindVariable;
import static com.google.clearsilver.jsilver.compiler.JavaExpression.callOn;
import static com.google.clearsilver.jsilver.compiler.JavaExpression.declare;
import static com.google.clearsilver.jsilver.compiler.JavaExpression.integer;
import static com.google.clearsilver.jsilver.compiler.JavaExpression.string;
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

import java.util.LinkedList;

/**
 * Translates a CS expression (from the AST) into an equivalent Java expression.
 * 
 * In order to optimize the expressions nicely this class emits code using a series of wrapper
 * functions for casting to/from various types. Rather than the old style of saying:
 * 
 * <pre>ValueX.asFoo()</pre>
 * 
 * we now write:
 * 
 * <pre>asFoo(ValueX)</pre>
 * 
 * This is actually very important because it means that as we optimize the expressions to return
 * fundamental types, we just have different versions of the {@code asFoo()} methods that take the
 * appropriate types. The user of the expression is responsible for casting it and the producer of
 * the expression is now free to produce optimized expressions.
 */
public class ExpressionTranslator extends DepthFirstAdapter {

  private JavaExpression currentJavaExpression;

  /**
   * Translate a template AST expression into a Java String expression.
   */
  public JavaExpression translateToString(PExpression csExpression) {
    return translateUntyped(csExpression).cast(Type.STRING);
  }

  /**
   * Translate a template AST expression into a Java boolean expression.
   */
  public JavaExpression translateToBoolean(PExpression csExpression) {
    return translateUntyped(csExpression).cast(Type.BOOLEAN);
  }

  /**
   * Translate a template AST expression into a Java integer expression.
   */
  public JavaExpression translateToNumber(PExpression csExpression) {
    return translateUntyped(csExpression).cast(Type.INT);
  }

  /**
   * Translate a template AST expression into a Java Data expression.
   */
  public JavaExpression translateToData(PExpression csExpression) {
    return translateUntyped(csExpression).cast(Type.DATA);
  }

  /**
   * Translate a template AST expression into a Java Data expression.
   */
  public JavaExpression translateToVarName(PExpression csExpression) {
    return translateUntyped(csExpression).cast(Type.VAR_NAME);
  }

  /**
   * Translate a template AST expression into a Java Value expression.
   */
  public JavaExpression translateToValue(PExpression csExpression) {
    return translateUntyped(csExpression).cast(Type.VALUE);
  }

  /**
   * Declares the (typed) expression as a variable with the given name. (e.g. "int foo = 5" or
   * "Data foo = Data.getChild("a.b")"
   */
  public JavaExpression declareAsVariable(String name, PExpression csExpression) {
    JavaExpression expression = translateUntyped(csExpression);
    Type type = expression.getType();
    assert type != null : "all subexpressions should be typed";
    return declare(type, name, expression);
  }

  /**
   * Translate a template AST expression into an untyped expression.
   */
  public JavaExpression translateUntyped(PExpression csExpression) {
    try {
      assert currentJavaExpression == null : "Not reentrant";
      csExpression.apply(this);
      assert currentJavaExpression != null : "No expression created";
      return currentJavaExpression;
    } finally {
      currentJavaExpression = null;
    }
  }

  private void setResult(JavaExpression javaExpression) {
    this.currentJavaExpression = javaExpression;
  }

  /**
   * Process AST node for a variable (e.g. a.b.c).
   */
  @Override
  public void caseAVariableExpression(AVariableExpression node) {
    JavaExpression varName = new VariableTranslator(this).translate(node.getVariable());
    setResult(varName);
  }

  /**
   * Process AST node for a string (e.g. "hello").
   */
  @Override
  public void caseAStringExpression(AStringExpression node) {
    String value = node.getValue().getText();
    value = value.substring(1, value.length() - 1); // Remove enclosing quotes.
    setResult(string(value));
  }

  /**
   * Process AST node for a decimal integer (e.g. 123).
   */
  @Override
  public void caseADecimalExpression(ADecimalExpression node) {
    String value = node.getValue().getText();
    setResult(integer(value));
  }

  /**
   * Process AST node for a hex integer (e.g. 0x1AB).
   */
  @Override
  public void caseAHexExpression(AHexExpression node) {
    String value = node.getValue().getText();
    // Luckily ClearSilver hex representation is a subset of the Java hex
    // representation so we can just use the literal directly.
    // TODO: add well-formedness checks whenever literals are used
    setResult(integer(value));
  }

  /*
   * The next block of functions all convert CS operators into dynamically looked up functions.
   */

  @Override
  public void caseANumericExpression(ANumericExpression node) {
    setResult(cast(Type.INT, node.getExpression()));
  }

  @Override
  public void caseANotExpression(ANotExpression node) {
    setResult(prefix(Type.BOOLEAN, Type.BOOLEAN, "!", node.getExpression()));
  }

  @Override
  public void caseAExistsExpression(AExistsExpression node) {
    // Special case. Exists is only ever an issue for variables, all
    // other expressions unconditionally exist.
    PExpression expression = node.getExpression();
    if (expression instanceof AVariableExpression) {
      expression.apply(this);
      if (currentJavaExpression.getType() == Type.VAR_NAME) {
        currentJavaExpression = callFindVariable(currentJavaExpression, false);
      }
      setResult(call(Type.BOOLEAN, "exists", currentJavaExpression));
    } else {
      // If it's not a variable, it always exists
      // NOTE: It's not clear if we must evaluate the sub-expression
      // here (is there anything that can have side effects??)
      setResult(bool(true));
    }
  }

  @Override
  public void caseAEqExpression(AEqExpression node) {
    JavaExpression left = cast(Type.STRING, node.getLeft());
    JavaExpression right = cast(Type.STRING, node.getRight());
    setResult(callOn(Type.BOOLEAN, left, "equals", right));
  }

  @Override
  public void caseANumericEqExpression(ANumericEqExpression node) {
    setResult(infix(Type.BOOLEAN, Type.INT, "==", node.getLeft(), node.getRight()));
  }

  @Override
  public void caseANeExpression(ANeExpression node) {
    JavaExpression left = cast(Type.STRING, node.getLeft());
    JavaExpression right = cast(Type.STRING, node.getRight());
    setResult(JavaExpression.prefix(Type.BOOLEAN, "!", callOn(Type.BOOLEAN, left, "equals", right)));
  }

  @Override
  public void caseANumericNeExpression(ANumericNeExpression node) {
    setResult(infix(Type.BOOLEAN, Type.INT, "!=", node.getLeft(), node.getRight()));
  }

  @Override
  public void caseALtExpression(ALtExpression node) {
    setResult(infix(Type.BOOLEAN, Type.INT, "<", node.getLeft(), node.getRight()));
  }

  @Override
  public void caseAGtExpression(AGtExpression node) {
    setResult(infix(Type.BOOLEAN, Type.INT, ">", node.getLeft(), node.getRight()));
  }

  @Override
  public void caseALteExpression(ALteExpression node) {
    setResult(infix(Type.BOOLEAN, Type.INT, "<=", node.getLeft(), node.getRight()));
  }

  @Override
  public void caseAGteExpression(AGteExpression node) {
    setResult(infix(Type.BOOLEAN, Type.INT, ">=", node.getLeft(), node.getRight()));
  }

  @Override
  public void caseAAndExpression(AAndExpression node) {
    setResult(infix(Type.BOOLEAN, Type.BOOLEAN, "&&", node.getLeft(), node.getRight()));
  }

  @Override
  public void caseAOrExpression(AOrExpression node) {
    setResult(infix(Type.BOOLEAN, Type.BOOLEAN, "||", node.getLeft(), node.getRight()));
  }

  @Override
  public void caseAAddExpression(AAddExpression node) {
    setResult(infix(Type.STRING, Type.STRING, "+", node.getLeft(), node.getRight()));
  }

  @Override
  public void caseANumericAddExpression(ANumericAddExpression node) {
    setResult(infix(Type.INT, Type.INT, "+", node.getLeft(), node.getRight()));
  }

  @Override
  public void caseASubtractExpression(ASubtractExpression node) {
    setResult(infix(Type.INT, Type.INT, "-", node.getLeft(), node.getRight()));
  }

  @Override
  public void caseAMultiplyExpression(AMultiplyExpression node) {
    setResult(infix(Type.INT, Type.INT, "*", node.getLeft(), node.getRight()));
  }

  @Override
  public void caseADivideExpression(ADivideExpression node) {
    setResult(infix(Type.INT, Type.INT, "/", node.getLeft(), node.getRight()));
  }

  @Override
  public void caseAModuloExpression(AModuloExpression node) {
    setResult(infix(Type.INT, Type.INT, "%", node.getLeft(), node.getRight()));
  }

  @Override
  public void caseANegativeExpression(ANegativeExpression node) {
    setResult(prefix(Type.INT, Type.INT, "-", node.getExpression()));
  }

  /**
   * Process AST node for a function (e.g. dosomething(...)).
   */
  @Override
  public void caseAFunctionExpression(AFunctionExpression node) {
    LinkedList<PExpression> argsList = node.getArgs();
    PExpression[] args = argsList.toArray(new PExpression[argsList.size()]);

    // Because the function name may have dots in, the parser would have broken
    // it into a little node tree which we need to walk to reconstruct the
    // full name.
    final StringBuilder fullFunctionName = new StringBuilder();
    node.getName().apply(new DepthFirstAdapter() {

      @Override
      public void caseANameVariable(ANameVariable node11) {
        fullFunctionName.append(node11.getWord().getText());
      }

      @Override
      public void caseADescendVariable(ADescendVariable node12) {
        node12.getParent().apply(this);
        fullFunctionName.append('.');
        node12.getChild().apply(this);
      }
    });

    setResult(function(fullFunctionName.toString(), args));
  }

  /**
   * Generate a JavaExpression for calling a function.
   */
  private JavaExpression function(String name, PExpression... csExpressions) {
    // Outputs: context.executeFunction("myfunc", args...);
    JavaExpression[] args = new JavaExpression[1 + csExpressions.length];
    args[0] = string(name);
    for (int i = 0; i < csExpressions.length; i++) {
      args[i + 1] = cast(Type.VALUE, csExpressions[i]);
    }
    return callOn(Type.VALUE, TemplateTranslator.CONTEXT, "executeFunction", args);
  }

  private JavaExpression infix(Type destType, Type srcType, String infix, PExpression leftNode,
      PExpression rightNode) {
    JavaExpression left = cast(srcType, leftNode);
    JavaExpression right = cast(srcType, rightNode);
    return JavaExpression.infix(destType, infix, left, right);
  }

  private JavaExpression prefix(Type destType, Type srcType, String prefix, PExpression node) {
    return JavaExpression.prefix(destType, prefix, cast(srcType, node));
  }

  private JavaExpression cast(Type type, PExpression node) {
    node.apply(this);
    return currentJavaExpression.cast(type);
  }
}
