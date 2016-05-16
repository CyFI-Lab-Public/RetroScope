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

package com.google.clearsilver.jsilver.functions.bundles;

import com.google.clearsilver.jsilver.functions.FunctionRegistry;
import com.google.clearsilver.jsilver.functions.operators.AddFunction;
import com.google.clearsilver.jsilver.functions.operators.AndFunction;
import com.google.clearsilver.jsilver.functions.operators.DivideFunction;
import com.google.clearsilver.jsilver.functions.operators.EqualFunction;
import com.google.clearsilver.jsilver.functions.operators.ExistsFunction;
import com.google.clearsilver.jsilver.functions.operators.GreaterFunction;
import com.google.clearsilver.jsilver.functions.operators.GreaterOrEqualFunction;
import com.google.clearsilver.jsilver.functions.operators.LessFunction;
import com.google.clearsilver.jsilver.functions.operators.LessOrEqualFunction;
import com.google.clearsilver.jsilver.functions.operators.ModuloFunction;
import com.google.clearsilver.jsilver.functions.operators.MultiplyFunction;
import com.google.clearsilver.jsilver.functions.operators.NotEqualFunction;
import com.google.clearsilver.jsilver.functions.operators.NotFunction;
import com.google.clearsilver.jsilver.functions.operators.NumericAddFunction;
import com.google.clearsilver.jsilver.functions.operators.NumericEqualFunction;
import com.google.clearsilver.jsilver.functions.operators.NumericFunction;
import com.google.clearsilver.jsilver.functions.operators.NumericNotEqualFunction;
import com.google.clearsilver.jsilver.functions.operators.OrFunction;
import com.google.clearsilver.jsilver.functions.operators.SubtractFunction;
import com.google.clearsilver.jsilver.functions.structure.NameFunction;

/**
 * Function registry containing core operators used in expressions.
 * 
 * These are: + - * / % ? ! && || == != &lt; &gt; &lt= &gt;=, name.
 * 
 * @see FunctionRegistry
 */
public class CoreOperators extends FunctionRegistry {

  @Override
  protected void setupDefaultFunctions() {
    super.setupDefaultFunctions();
    registerFunction("+", new AddFunction());
    registerFunction("#+", new NumericAddFunction());
    registerFunction("-", new SubtractFunction());
    registerFunction("*", new MultiplyFunction());
    registerFunction("/", new DivideFunction());
    registerFunction("%", new ModuloFunction());
    registerFunction("?", new ExistsFunction());
    registerFunction("!", new NotFunction());
    registerFunction("&&", new AndFunction());
    registerFunction("||", new OrFunction());
    registerFunction("==", new EqualFunction());
    registerFunction("#==", new NumericEqualFunction());
    registerFunction("!=", new NotEqualFunction());
    registerFunction("#!=", new NumericNotEqualFunction());
    registerFunction("<", new LessFunction());
    registerFunction(">", new GreaterFunction());
    registerFunction("<=", new LessOrEqualFunction());
    registerFunction(">=", new GreaterOrEqualFunction());
    registerFunction("#", new NumericFunction());

    // Not an operator, but JSilver cannot function without as it's used by
    // the <?cs name ?> command.
    registerFunction("name", new NameFunction());
  }

}
