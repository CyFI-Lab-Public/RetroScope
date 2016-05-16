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

import com.google.clearsilver.jsilver.data.TypeConverter;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Represents a node of a Java expression.
 * 
 * This class contains static helper methods for common types of expressions, or you can just create
 * your own subclass.
 */
public abstract class JavaExpression {

  /**
   * Simple type enumeration to allow us to compare the return types of expressions easily and cast
   * expressions nicely.
   */
  public enum Type {
    STRING("String") {
      @Override
      protected JavaExpression cast(JavaExpression expression) {
        if (expression.getType() == VAR_NAME) {
          expression = expression.cast(DATA);
        }
        return call(Type.STRING, "asString", expression);
      }
    },
    INT("int") {
      @Override
      protected JavaExpression cast(JavaExpression expression) {
        if (expression.getType() == VAR_NAME) {
          expression = expression.cast(DATA);
        }
        return call(Type.INT, "asInt", expression);
      }
    },
    BOOLEAN("boolean") {
      @Override
      protected JavaExpression cast(JavaExpression expression) {
        if (expression.getType() == VAR_NAME) {
          expression = expression.cast(DATA);
        }
        return call(Type.BOOLEAN, "asBoolean", expression);
      }
    },
    VALUE("Value") {
      @Override
      protected JavaExpression cast(JavaExpression expression) {
        if (expression.getType() == VAR_NAME) {
          return call(Type.VALUE, "asVariableValue", expression, TemplateTranslator.DATA_CONTEXT);
        } else {
          return call(Type.VALUE, "asValue", expression);
        }
      }
    },
    DATA("Data") {
      @Override
      protected JavaExpression cast(JavaExpression expression) {
        if (expression.getType() == VAR_NAME) {
          return callFindVariable(expression, false);
        } else {
          throw new JSilverCompilationException("Cannot cast to 'Data' for expression:\n"
              + expression.toString());
        }
      }
    },
    // This is a string that represents the name of a Data path.
    VAR_NAME("String") {
      @Override
      protected JavaExpression cast(JavaExpression expression) {
        final JavaExpression stringExpr = expression.cast(Type.STRING);
        return new JavaExpression(Type.VAR_NAME) {
          public void write(PrintWriter out) {
            stringExpr.write(out);
          }
        };
      }
    },
    // This is a special type because we only cast from DataContext, never to it.
    DATA_CONTEXT("DataContext") {
      @Override
      protected JavaExpression cast(JavaExpression expression) {
        throw new JSilverCompilationException("Cannot cast to 'DataContext' for expression:\n"
            + expression.toString());
      }
    },
    // This is a special type because we only cast from Data, never to it.
    MACRO("Macro") {
      @Override
      protected JavaExpression cast(JavaExpression expression) {
        throw new JSilverCompilationException("Cannot cast to 'Macro' for expression:\n"
            + expression.toString());
      }
    },
    // Use this type for JavaExpressions that have no type (such as method
    // calls with no return value). Wraps the input expression with a
    // JavaExpression of Type VOID.
    VOID("Void") {
      @Override
      protected JavaExpression cast(final JavaExpression expression) {
        return new JavaExpression(Type.VOID) {
          @Override
          public void write(PrintWriter out) {
            expression.write(out);
          }
        };
      }
    };

    /** Useful constant for unknown types */
    public static final Type UNKNOWN = null;

    /**
     * The Java literal representing the type (e.g. "int", "boolean", "Value")
     */
    public final String symbol;

    /**
     * Unconditionally casts the given expression to the type. This should only be called after it
     * has been determined that the destination type is not the same as the expression type.
     */
    protected abstract JavaExpression cast(JavaExpression expression);

    private Type(String symbol) {
      this.symbol = symbol;
    }
  }

  private final Type type;

  /**
   * Creates a typed expression. Typed expressions allow for greater optimization by avoiding
   * unnecessary casting operations.
   * 
   * @param type the Type of the expression. Must be from the enum above and represent a primitive
   *        or a Class name or void.
   */
  public JavaExpression(Type type) {
    this.type = type;
  }

  /**
   * Cast this expression to the destination type (possibly a no-op)
   */
  public JavaExpression cast(Type destType) {
    return (type != destType) ? destType.cast(this) : this;
  }

  /**
   * Gets the type of this expression (or {@code null} if unknown).
   */
  public Type getType() {
    return type;
  }

  /**
   * Implementations use this to output the expression as Java code.
   */
  public abstract void write(PrintWriter out);

  @Override
  public String toString() {
    StringWriter out = new StringWriter();
    write(new PrintWriter(out));
    return out.toString();
  }

  /**
   * An untyped method call (e.g. doStuff(x, "y")).
   */
  public static JavaExpression call(final String method, final JavaExpression... params) {
    return call(null, method, params);
  }

  /**
   * A typed method call (e.g. doStuff(x, "y")).
   */
  public static JavaExpression call(Type type, final String method, final JavaExpression... params) {
    return new JavaExpression(type) {
      @Override
      public void write(PrintWriter out) {
        JavaSourceWriter.writeJavaSymbol(out, method);
        out.append('(');
        boolean seenAnyParams = false;
        for (JavaExpression param : params) {
          if (seenAnyParams) {
            out.append(", ");
          } else {
            seenAnyParams = true;
          }
          param.write(out);
        }
        out.append(')');
      }
    };
  }

  /**
   * An untyped method call on an instance (e.g. thingy.doStuff(x, "y")). We assume it returns VOID
   * and thus there is no return value.
   */
  public static JavaExpression callOn(final JavaExpression instance, final String method,
      final JavaExpression... params) {
    return callOn(Type.VOID, instance, method, params);
  }

  /**
   * A typed method call on an instance (e.g. thingy.doStuff(x, "y")).
   */
  public static JavaExpression callOn(Type type, final JavaExpression instance,
      final String method, final JavaExpression... params) {
    return new JavaExpression(type) {
      @Override
      public void write(PrintWriter out) {
        instance.write(out);
        out.append('.');
        call(method, params).write(out);
      }
    };
  }

  /**
   * A Java string (e.g. "hello\nworld").
   */
  public static JavaExpression string(String value) {
    return new StringExpression(value);
  }

  public static class StringExpression extends JavaExpression {

    private final String value;

    public StringExpression(String value) {
      super(Type.STRING);
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public void write(PrintWriter out) {
      // TODO: This is not production ready yet - needs more
      // thorough escaping mechanism.
      out.append('"');
      char[] chars = value.toCharArray();
      for (char c : chars) {
        switch (c) {
          // Single quote (') does not need to be escaped as it's in a
          // double-quoted (") string.
          case '\n':
            out.append("\\n");
            break;
          case '\r':
            out.append("\\r");
            break;
          case '\t':
            out.append("\\t");
            break;
          case '\\':
            out.append("\\\\");
            break;
          case '"':
            out.append("\\\"");
            break;
          case '\b':
            out.append("\\b");
            break;
          case '\f':
            out.append("\\f");
            break;
          default:
            out.append(c);
        }
      }
      out.append('"');
    }
  }

  /**
   * A JavaExpression to represent boolean literal values ('true' or 'false').
   */
  public static class BooleanLiteralExpression extends JavaExpression {

    private final boolean value;

    public static final BooleanLiteralExpression FALSE = new BooleanLiteralExpression(false);
    public static final BooleanLiteralExpression TRUE = new BooleanLiteralExpression(true);

    private BooleanLiteralExpression(boolean value) {
      super(Type.BOOLEAN);
      this.value = value;
    }

    public boolean getValue() {
      return value;
    }

    @Override
    public void write(PrintWriter out) {
      out.append(String.valueOf(value));
    }
  }

  /**
   * An integer.
   */
  public static JavaExpression integer(String value) {
    // Just parse it to to check that it is valid
    TypeConverter.parseNumber(value);
    return literal(Type.INT, value);
  }

  /**
   * An integer.
   */
  public static JavaExpression integer(int value) {
    return literal(Type.INT, String.valueOf(value));
  }

  /**
   * A boolean
   */
  public static JavaExpression bool(boolean value) {
    return literal(Type.BOOLEAN, value ? "true" : "false");
  }

  /**
   * An untyped symbol (e.g. myVariable).
   */
  public static JavaExpression symbol(final String value) {
    return new JavaExpression(Type.UNKNOWN) {
      @Override
      public void write(PrintWriter out) {
        JavaSourceWriter.writeJavaSymbol(out, value);
      }
    };
  }

  /**
   * A typed symbol (e.g. myVariable).
   */
  public static JavaExpression symbol(Type type, final String value) {
    return new JavaExpression(type) {
      @Override
      public void write(PrintWriter out) {
        JavaSourceWriter.writeJavaSymbol(out, value);
      }
    };
  }

  public static JavaExpression macro(final String value) {
    return symbol(Type.MACRO, value);
  }

  /**
   * A typed assignment (e.g. stuff = doSomething()).
   */
  public static JavaExpression assign(Type type, final String name, final JavaExpression value) {
    return new JavaExpression(type) {
      @Override
      public void write(PrintWriter out) {
        JavaSourceWriter.writeJavaSymbol(out, name);
        out.append(" = ");
        value.write(out);
      }
    };
  }

  /**
   * A typed assignment with declaration (e.g. String stuff = doSomething()). Use this in preference
   * when declaring variables from typed expressions.
   */
  public static JavaExpression declare(final Type type, final String name,
      final JavaExpression value) {
    return new JavaExpression(type) {
      @Override
      public void write(PrintWriter out) {
        JavaSourceWriter.writeJavaSymbol(out, type.symbol);
        out.append(' ');
        assign(type, name, value).write(out);
      }
    };
  }

  /**
   * An infix expression (e.g. (a + b) ).
   */
  public static JavaExpression infix(Type type, final String operator, final JavaExpression left,
      final JavaExpression right) {
    return new JavaExpression(type) {
      @Override
      public void write(PrintWriter out) {
        out.append("(");
        left.write(out);
        out.append(" ").append(operator).append(" ");
        right.write(out);
        out.append(")");
      }
    };
  }

  /**
   * An prefix expression (e.g. (-a) ).
   */
  public static JavaExpression prefix(Type type, final String operator,
      final JavaExpression expression) {
    return new JavaExpression(type) {
      @Override
      public void write(PrintWriter out) {
        out.append("(").append(operator);
        expression.write(out);
        out.append(")");
      }
    };
  }

  /**
   * A three term inline if expression (e.g. (a ? b : c) ).
   */
  public static JavaExpression inlineIf(Type type, final JavaExpression query,
      final JavaExpression trueExp, final JavaExpression falseExp) {
    if (query.getType() != Type.BOOLEAN) {
      throw new IllegalArgumentException("Expect BOOLEAN expression");
    }
    return new JavaExpression(type) {
      @Override
      public void write(PrintWriter out) {
        out.append("(");
        query.write(out);
        out.append(" ? ");
        trueExp.write(out);
        out.append(" : ");
        falseExp.write(out);
        out.append(")");
      }
    };
  }

  /**
   * An increment statement (e.g. a += b). The difference with infix is that this does not wrap the
   * expression in parentheses as that is not a valid statement.
   */
  public static JavaExpression increment(Type type, final JavaExpression accumulator,
      final JavaExpression incr) {
    return new JavaExpression(type) {
      @Override
      public void write(PrintWriter out) {
        accumulator.write(out);
        out.append(" += ");
        incr.write(out);
      }
    };
  }

  /**
   * A literal expression (e.g. anything!). This method injects whatever string it is given into the
   * Java code - use only in cases where there can be no ambiguity about how the string could be
   * interpreted by the compiler.
   */
  public static JavaExpression literal(Type type, final String value) {
    return new JavaExpression(type) {
      @Override
      public void write(PrintWriter out) {
        out.append(value);
      }
    };
  }

  public static JavaExpression callFindVariable(JavaExpression expression, boolean create) {
    if (expression.getType() != Type.VAR_NAME) {
      throw new IllegalArgumentException("Expect VAR_NAME expression");
    }
    return callOn(Type.DATA, TemplateTranslator.DATA_CONTEXT, "findVariable", expression,
        JavaExpression.bool(create));
  }
}
