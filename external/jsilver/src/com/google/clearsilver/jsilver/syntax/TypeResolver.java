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

import com.google.clearsilver.jsilver.syntax.analysis.DepthFirstAdapter;
import com.google.clearsilver.jsilver.syntax.node.AAddExpression;
import com.google.clearsilver.jsilver.syntax.node.ADecimalExpression;
import com.google.clearsilver.jsilver.syntax.node.ADivideExpression;
import com.google.clearsilver.jsilver.syntax.node.AEqExpression;
import com.google.clearsilver.jsilver.syntax.node.AFunctionExpression;
import com.google.clearsilver.jsilver.syntax.node.AHexExpression;
import com.google.clearsilver.jsilver.syntax.node.AModuloExpression;
import com.google.clearsilver.jsilver.syntax.node.AMultiplyExpression;
import com.google.clearsilver.jsilver.syntax.node.ANameVariable;
import com.google.clearsilver.jsilver.syntax.node.ANeExpression;
import com.google.clearsilver.jsilver.syntax.node.ANegativeExpression;
import com.google.clearsilver.jsilver.syntax.node.ANumericAddExpression;
import com.google.clearsilver.jsilver.syntax.node.ANumericEqExpression;
import com.google.clearsilver.jsilver.syntax.node.ANumericExpression;
import com.google.clearsilver.jsilver.syntax.node.ANumericNeExpression;
import com.google.clearsilver.jsilver.syntax.node.ASubtractExpression;
import com.google.clearsilver.jsilver.syntax.node.PExpression;
import com.google.clearsilver.jsilver.syntax.node.PVariable;

/**
 * AST visitor to add numeric expressions to the syntax tree.
 * 
 * <p>
 * There are three types of expression we need to process; addition, equality and inequality. By
 * default these are treated as string expressions unless one of the operands is numeric, in which
 * case the original expression is replaced with its numeric equivalent. This behavior seems to
 * exactly match Clearsilver's type inference system.
 * 
 * <p>
 * Note how we preprocess our node before testing to see is it should be replaced. This is very
 * important because it means that type inference is propagated correctly along compound
 * expressions. Consider the expression:
 * 
 * <pre>#a + b + c</pre>
 * 
 * which is parsed (left-to-right) as:
 * 
 * <pre>(#a + b) + c</pre>
 * 
 * When we process the left-hand-side sub-expression {@code #a + b} it is turned into a numeric
 * addition (due to the forced numeric value on the left). Then when we process the main expression
 * we propagate the numeric type into it.
 * 
 * <p>
 * This matches Clearsilver behavior but means that the expressions:
 * 
 * <pre>#a + b + c</pre>
 * 
 * and
 * 
 * <pre>c + b + #a</pre>
 * 
 * produce different results (the {@code c + b} subexpression in the latter is evaluated as string
 * concatenation and not numeric addition).
 */
public class TypeResolver extends DepthFirstAdapter {

  @Override
  public void caseAAddExpression(AAddExpression node) {
    super.caseAAddExpression(node);
    PExpression lhs = node.getLeft();
    PExpression rhs = node.getRight();
    if (isNumeric(lhs) || isNumeric(rhs)) {
      node.replaceBy(new ANumericAddExpression(lhs, rhs));
    }
  }

  @Override
  public void caseAEqExpression(AEqExpression node) {
    super.caseAEqExpression(node);
    PExpression lhs = node.getLeft();
    PExpression rhs = node.getRight();
    if (isNumeric(lhs) || isNumeric(rhs)) {
      node.replaceBy(new ANumericEqExpression(lhs, rhs));
    }
  }

  @Override
  public void caseANeExpression(ANeExpression node) {
    super.caseANeExpression(node);
    PExpression lhs = node.getLeft();
    PExpression rhs = node.getRight();
    if (isNumeric(lhs) || isNumeric(rhs)) {
      node.replaceBy(new ANumericNeExpression(lhs, rhs));
    }
  }

  /**
   * Determines whether the given (sub)expression is numeric, which in turn means that its parent
   * expression should be treated as numeric if possible.
   */
  static boolean isNumeric(PExpression node) {
    return node instanceof ANumericExpression // forced numeric (#a)
        || node instanceof ANumericAddExpression // numeric addition (a + b)
        || node instanceof ASubtractExpression // subtraction (a - b)
        || node instanceof AMultiplyExpression // multiplication (a * b)
        || node instanceof ADivideExpression // division (a / b)
        || node instanceof AModuloExpression // modulu (x % b)
        || node instanceof ADecimalExpression // literal decimal (213)
        || node instanceof AHexExpression // literal hex (0xabc or 0XABC)
        || node instanceof ANegativeExpression // negative expression (-a)
        || isNumericFunction(node); // numeric function (subcount)
  }

  /**
   * Determine if the given expression represents a numeric function.
   */
  static boolean isNumericFunction(PExpression node) {
    if (!(node instanceof AFunctionExpression)) {
      return false;
    }
    PVariable functionName = ((AFunctionExpression) node).getName();
    if (functionName instanceof ANameVariable) {
      String name = ((ANameVariable) functionName).getWord().getText();
      if ("max".equals(name) || "min".equals(name) || "abs".equals(name) || "subcount".equals(name)) {
        return true;
      }
    }
    return false;
  }
}
