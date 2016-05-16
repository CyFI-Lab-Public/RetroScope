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

package com.google.clearsilver.jsilver.functions;

import com.google.clearsilver.jsilver.autoescape.EscapeMode;

import com.google.clearsilver.jsilver.exceptions.JSilverInterpreterException;
import com.google.clearsilver.jsilver.values.Value;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple implementation of FunctionFinder that you can register your own functions with.
 *
 * @see FunctionExecutor
 */
public class FunctionRegistry implements FunctionExecutor {

  protected Map<String, Function> functions = new HashMap<String, Function>();
  protected Map<String, TextFilter> escapers = new HashMap<String, TextFilter>();

  public FunctionRegistry() {
    setupDefaultFunctions();
  }

  @Override
  public Value executeFunction(String name, Value... args) {
    Function function = functions.get(name);
    if (function == null) {
      throw new JSilverInterpreterException("Function not found " + name);
    }
    Value result = function.execute(args);
    if (result == null) {
      throw new JSilverInterpreterException("Function " + name + " did not return value");
    }
    return result;
  }

  @Override
  public void escape(String name, String input, Appendable output) throws IOException {
    if (name == null || name.isEmpty() || name.equals("none")) {
      output.append(input);
    } else {
      TextFilter escaper = escapers.get(name);
      if (escaper == null) {
        throw new JSilverInterpreterException("Unknown escaper: " + name);
      }
      escaper.filter(input, output);
    }
  }

  @Override
  public boolean isEscapingFunction(String name) {
    Function function = functions.get(name);
    if (function == null) {
      throw new JSilverInterpreterException("Function not found " + name);
    }
    return function.isEscapingFunction();
  }

  /**
   * Subclasses can override this to register their own functions.
   */
  protected void setupDefaultFunctions() {}

  /**
   * Register a Function with a given name.
   */
  public void registerFunction(String name, Function function) {
    functions.put(name, function);
  }

  /**
   * Register a TextFilter as a Function that takes a single String argument and returns the
   * filtered value.
   */
  public void registerFunction(String name, final TextFilter textFilter) {
    registerFunction(name, textFilter, false);
  }

  public void registerFunction(String name, final TextFilter textFilter, final boolean isEscaper) {

    // Adapt a TextFilter to the Function interface.
    registerFunction(name, new Function() {
      @Override
      public Value execute(Value... args) {
        if (args.length != 1) {
          throw new IllegalArgumentException("Expected 1 argument");
        }
        String in = args[0].asString();
        StringBuilder out = new StringBuilder(in.length());
        try {
          textFilter.filter(in, out);
        } catch (IOException e) {
          throw new JSilverInterpreterException(e.getMessage());
        }

        EscapeMode mode;
        boolean isPartiallyEscaped;
        if (isEscaper) {
          // This function escapes its input. Hence the output is
          // partiallyEscaped.
          mode = EscapeMode.ESCAPE_IS_CONSTANT;
          isPartiallyEscaped = true;
        } else {
          mode = EscapeMode.ESCAPE_NONE;
          isPartiallyEscaped = false;
          for (Value arg : args) {
            if (arg.isPartiallyEscaped()) {
              isPartiallyEscaped = true;
              break;
            }
          }
        }
        return Value.literalValue(out.toString(), mode, isPartiallyEscaped);
      }

      public boolean isEscapingFunction() {
        return isEscaper;
      }
    });
  }

  /**
   * Registers an escaper, that is called when executing a &lt;?cs escape ?&gt; command.
   * 
   * @param name The name with which &lt;?cs escape ?&gt; will invoke this escaper.
   * @param escaper A TextFilter that implements the escaping functionality.
   */
  public void registerEscapeMode(String name, TextFilter escaper) {

    escapers.put(name, escaper);
  }

}
