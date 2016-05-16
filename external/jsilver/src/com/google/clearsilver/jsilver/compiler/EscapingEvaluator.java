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

import static com.google.clearsilver.jsilver.compiler.JavaExpression.BooleanLiteralExpression;
import static com.google.clearsilver.jsilver.compiler.JavaExpression.callOn;
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
 * Generates a JavaExpression to determine whether a given CS expression should be escaped before
 * displaying. If propagateEscapeStatus is enabled, string and numeric literals are not escaped, nor
 * is the output of an escaping function. If not, any expression that contains an escaping function
 * is not escaped. This maintains compatibility with the way ClearSilver works.
 */
public class EscapingEvaluator extends DepthFirstAdapter {

  private JavaExpression currentEscapingExpression;
  private boolean propagateEscapeStatus;
  private final VariableTranslator variableTranslator;

  public EscapingEvaluator(VariableTranslator variableTranslator) {
    super();
    this.variableTranslator = variableTranslator;
  }

  /**
   * Returns a JavaExpression that can be used to decide whether a given variable should be escaped.
   * 
   * @param expression variable expression to be evaluated.
   * @param propagateEscapeStatus Whether to propagate the variable's escape status.
   * 
   * @return Returns a {@code JavaExpression} representing a boolean expression that evaluates to
   *         {@code true} if {@code expression} should be exempted from escaping and {@code false}
   *         otherwise.
   */
  public JavaExpression computeIfExemptFromEscaping(PExpression expression,
      boolean propagateEscapeStatus) {
    if (propagateEscapeStatus) {
      return computeForPropagateStatus(expression);
    }
    return computeEscaping(expression, propagateEscapeStatus);
  }

  private JavaExpression computeForPropagateStatus(PExpression expression) {
    // This function generates a boolean expression that evaluates to true
    // if the input should be exempt from escaping. As this should only be
    // called when PropagateStatus is enabled we must check EscapeMode as
    // well as isPartiallyEscaped.
    // The interpreter mode equivalent of this boolean expression would be :
    // ((value.getEscapeMode() != EscapeMode.ESCAPE_NONE) || value.isPartiallyEscaped())

    JavaExpression escapeMode = computeEscaping(expression, true);
    JavaExpression partiallyEscaped = computeEscaping(expression, false);

    JavaExpression escapeModeCheck =
        JavaExpression.infix(JavaExpression.Type.BOOLEAN, "!=", escapeMode, JavaExpression
            .symbol("EscapeMode.ESCAPE_NONE"));

    return JavaExpression.infix(JavaExpression.Type.BOOLEAN, "||", escapeModeCheck,
        partiallyEscaped);
  }

  /**
   * Compute the escaping applied to the given expression. Uses {@code propagateEscapeStatus} to
   * determine how to treat constants, and whether escaping is required on a part of the expression
   * or the whole expression.
   */
  public JavaExpression computeEscaping(PExpression expression, boolean propagateEscapeStatus) {
    try {
      assert currentEscapingExpression == null : "Not reentrant";
      this.propagateEscapeStatus = propagateEscapeStatus;
      expression.apply(this);
      assert currentEscapingExpression != null : "No escaping calculated";
      return currentEscapingExpression;
    } finally {
      currentEscapingExpression = null;
    }
  }

  private void setEscaping(JavaExpression escaping) {
    currentEscapingExpression = escaping;
  }

  /**
   * String concatenation. Do not escape the combined string, if either of the halves has been
   * escaped.
   */
  @Override
  public void caseAAddExpression(AAddExpression node) {
    node.getLeft().apply(this);
    JavaExpression left = currentEscapingExpression;
    node.getRight().apply(this);
    JavaExpression right = currentEscapingExpression;

    setEscaping(or(left, right));
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

    setEscaping(function(fullFunctionName.toString(), args));
  }

  /**
   * Do not escape the output of a function if either the function is an escaping function, or any
   * of its parameters have been escaped.
   */
  private JavaExpression function(String name, PExpression... csExpressions) {
    if (propagateEscapeStatus) {
      // context.isEscapingFunction("name") ? EscapeMode.ESCAPE_IS_CONSTANT : EscapeMode.ESCAPE_NONE
      return JavaExpression.inlineIf(JavaExpression.Type.UNKNOWN, callOn(
          JavaExpression.Type.BOOLEAN, TemplateTranslator.CONTEXT, "isEscapingFunction",
          string(name)), JavaExpression.symbol("EscapeMode.ESCAPE_IS_CONSTANT"), JavaExpression
          .symbol("EscapeMode.ESCAPE_NONE"));
    }
    JavaExpression finalExpression = BooleanLiteralExpression.FALSE;
    for (int i = 0; i < csExpressions.length; i++) {
      // Will put result in currentEscapingExpression.
      csExpressions[i].apply(this);
      finalExpression = or(finalExpression, currentEscapingExpression);
    }
    JavaExpression funcExpr =
        callOn(JavaExpression.Type.BOOLEAN, TemplateTranslator.CONTEXT, "isEscapingFunction",
            string(name));
    return or(finalExpression, funcExpr);
  }

  /*
   * This function tries to optimize the output expression where possible: instead of
   * "(false || context.isEscapingFunction())" it returns "context.isEscapingFunction()".
   */
  private JavaExpression or(JavaExpression first, JavaExpression second) {
    if (propagateEscapeStatus) {
      return JavaExpression.callOn(JavaExpression.symbol("EscapeMode"), "combineModes", first,
          second);
    }

    if (first instanceof BooleanLiteralExpression) {
      BooleanLiteralExpression expr = (BooleanLiteralExpression) first;
      if (expr.getValue()) {
        return expr;
      } else {
        return second;
      }
    }
    if (second instanceof BooleanLiteralExpression) {
      BooleanLiteralExpression expr = (BooleanLiteralExpression) second;
      if (expr.getValue()) {
        return expr;
      } else {
        return first;
      }
    }
    return JavaExpression.infix(JavaExpression.Type.BOOLEAN, "||", first, second);
  }

  /*
   * All the following operators have no effect on escaping, so just default to 'false'.
   */

  /**
   * Process AST node for a variable (e.g. a.b.c).
   */
  @Override
  public void caseAVariableExpression(AVariableExpression node) {
    if (propagateEscapeStatus) {
      JavaExpression varName = variableTranslator.translate(node.getVariable());
      setEscaping(callOn(TemplateTranslator.DATA_CONTEXT, "findVariableEscapeMode", varName));
    } else {
      setDefaultEscaping();
    }
  }

  private void setDefaultEscaping() {
    if (propagateEscapeStatus) {
      setEscaping(JavaExpression.symbol("EscapeMode.ESCAPE_IS_CONSTANT"));
    } else {
      setEscaping(BooleanLiteralExpression.FALSE);
    }
  }

  /**
   * Process AST node for a string (e.g. "hello").
   */
  @Override
  public void caseAStringExpression(AStringExpression node) {
    setDefaultEscaping();
  }

  /**
   * Process AST node for a decimal integer (e.g. 123).
   */
  @Override
  public void caseADecimalExpression(ADecimalExpression node) {
    setDefaultEscaping();
  }

  /**
   * Process AST node for a hex integer (e.g. 0x1AB).
   */
  @Override
  public void caseAHexExpression(AHexExpression node) {
    setDefaultEscaping();
  }

  @Override
  public void caseANumericExpression(ANumericExpression node) {
    setDefaultEscaping();
  }

  @Override
  public void caseANotExpression(ANotExpression node) {
    setDefaultEscaping();
  }

  @Override
  public void caseAExistsExpression(AExistsExpression node) {
    setDefaultEscaping();
  }

  @Override
  public void caseAEqExpression(AEqExpression node) {
    setDefaultEscaping();
  }

  @Override
  public void caseANumericEqExpression(ANumericEqExpression node) {
    setDefaultEscaping();
  }

  @Override
  public void caseANeExpression(ANeExpression node) {
    setDefaultEscaping();
  }

  @Override
  public void caseANumericNeExpression(ANumericNeExpression node) {
    setDefaultEscaping();
  }

  @Override
  public void caseALtExpression(ALtExpression node) {
    setDefaultEscaping();
  }

  @Override
  public void caseAGtExpression(AGtExpression node) {
    setDefaultEscaping();
  }

  @Override
  public void caseALteExpression(ALteExpression node) {
    setDefaultEscaping();
  }

  @Override
  public void caseAGteExpression(AGteExpression node) {
    setDefaultEscaping();
  }

  @Override
  public void caseAAndExpression(AAndExpression node) {
    setDefaultEscaping();
  }

  @Override
  public void caseAOrExpression(AOrExpression node) {
    setDefaultEscaping();
  }

  @Override
  public void caseANumericAddExpression(ANumericAddExpression node) {
    setDefaultEscaping();
  }

  @Override
  public void caseASubtractExpression(ASubtractExpression node) {
    setDefaultEscaping();
  }

  @Override
  public void caseAMultiplyExpression(AMultiplyExpression node) {
    setDefaultEscaping();
  }

  @Override
  public void caseADivideExpression(ADivideExpression node) {
    setDefaultEscaping();
  }

  @Override
  public void caseAModuloExpression(AModuloExpression node) {
    setDefaultEscaping();
  }

  @Override
  public void caseANegativeExpression(ANegativeExpression node) {
    setDefaultEscaping();
  }

}
