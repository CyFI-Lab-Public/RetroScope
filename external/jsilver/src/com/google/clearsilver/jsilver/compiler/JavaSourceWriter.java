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

import java.io.Closeable;
import java.io.Flushable;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Simple API for generating Java source code. Easier than lots of string manipulation.
 * 
 * <h3>Example</h3>
 * 
 * <pre>
 * java = new JavaSourceWriter(out);
 *
 * java.writeComment("// Auto generated file");
 * java.writePackage("com.something.mypackage");
 * java.writeImports(SomeClassToImport.class, Another.class);
 *
 * java.startClass("SomeClass", "InterfaceA");
 * java.startMethod(Object.class.getMethod("toString"));
 * java.writeStatement(call("System.out.println", string("hello")));
 * java.endClass();
 * </pre>
 * 
 * Note: For writing statements/expressions, staticly import the methods on {@link JavaExpression}.
 */
public class JavaSourceWriter implements Closeable, Flushable {

  private final PrintWriter out;
  private int indent;

  public JavaSourceWriter(Writer out) {
    this.out = new PrintWriter(out);
  }

  public void writePackage(String packageName) {
    // TODO: Verify packageName is valid.
    if (packageName != null) {
      startLine();
      out.append("package ").append(packageName).append(';');
      endLine();
      emptyLine();
    }
  }

  public void writeImports(Class... javaClasses) {
    for (Class javaClass : javaClasses) {
      startLine();
      out.append("import ").append(javaClass.getName()).append(';');
      endLine();
    }
    if (javaClasses.length > 0) {
      emptyLine();
    }
  }

  public void writeComment(String comment) {
    // TODO: Handle line breaks in comments.
    startLine();
    out.append("// ").append(comment);
    endLine();
  }

  public void startClass(String className, String baseClassName, String... interfaceNames) {
    startLine();
    out.append("public class ");
    writeJavaSymbol(out, className);

    if (baseClassName != null) {
      out.append(" extends ");
      writeJavaSymbol(out, baseClassName);
    }

    boolean seenAnyInterfaces = false;
    for (String interfaceName : interfaceNames) {
      if (!seenAnyInterfaces) {
        seenAnyInterfaces = true;
        out.append(" implements ");
      } else {
        out.append(", ");
      }
      writeJavaSymbol(out, interfaceName);
    }

    out.append(' ');
    startBlock();
    emptyLine();
  }

  public void startAnonymousClass(String baseClass, JavaExpression... constructorArgs) {
    out.append("new ");
    writeJavaSymbol(out, baseClass);
    out.append('(');

    boolean seenAnyArgs = false;
    for (JavaExpression constructorArg : constructorArgs) {
      if (seenAnyArgs) {
        out.append(", ");
      }
      seenAnyArgs = true;
      constructorArg.write(out);
    }

    out.append(") ");
    startBlock();
    emptyLine();
  }

  public void endAnonymousClass() {
    endBlock();
  }

  /**
   * Start a method. The signature is based on that of an existing method.
   */
  public void startMethod(Method method, String... paramNames) {
    // This currently does not support generics, varargs or arrays.
    // If you need it - add the support. Don't want to overcomplicate it
    // until necessary.

    if (paramNames.length != method.getParameterTypes().length) {
      throw new IllegalArgumentException("Did not specifiy correct "
          + "number of parameter names for method signature " + method);
    }

    startLine();

    // @Override abstract methods.
    int modifiers = method.getModifiers();
    if (Modifier.isAbstract(modifiers)) {
      out.append("@Override");
      endLine();
      startLine();
    }

    // Modifiers: (public, protected, static)
    if (modifiers != 0) {
      // Modifiers we care about. Ditch the rest. Specifically NOT ABSTRACT.
      modifiers &= Modifier.PUBLIC | Modifier.PROTECTED | Modifier.STATIC;
      out.append(Modifier.toString(modifiers)).append(' ');
    }

    // Return type and name: (e.g. "void doStuff(")
    out.append(method.getReturnType().getSimpleName()).append(' ').append(method.getName()).append(
        '(');

    // Parameters.
    int paramIndex = 0;
    for (Class<?> paramType : method.getParameterTypes()) {
      if (paramIndex > 0) {
        out.append(", ");
      }
      writeJavaSymbol(out, paramType.getSimpleName());
      out.append(' ');
      writeJavaSymbol(out, paramNames[paramIndex]);
      paramIndex++;
    }

    out.append(')');

    // Exceptions thrown.
    boolean seenAnyExceptions = false;
    for (Class exception : method.getExceptionTypes()) {
      if (!seenAnyExceptions) {
        seenAnyExceptions = true;
        endLine();
        startLine();
        out.append("    throws ");
      } else {
        out.append(", ");
      }
      writeJavaSymbol(out, exception.getSimpleName());
    }

    out.append(' ');
    startBlock();
  }

  public void startIfBlock(JavaExpression expression) {
    startLine();
    out.append("if (");
    writeExpression(expression);
    out.append(") ");
    startBlock();
  }

  public void endIfStartElseBlock() {
    endBlock();
    out.append(" else ");
    startBlock();
  }

  public void endIfBlock() {
    endBlock();
    endLine();
  }

  public void startScopedBlock() {
    startLine();
    startBlock();
  }

  public void endScopedBlock() {
    endBlock();
    endLine();
  }

  public void startIterableForLoop(String type, String name, JavaExpression expression) {
    startLine();
    out.append("for (");
    writeJavaSymbol(out, type);
    out.append(' ');
    writeJavaSymbol(out, name);
    out.append(" : ");
    writeExpression(expression);
    out.append(") ");
    startBlock();
  }

  public void startForLoop(JavaExpression start, JavaExpression end, JavaExpression increment) {
    startLine();
    out.append("for (");
    writeExpression(start);
    out.append("; ");
    writeExpression(end);
    out.append("; ");
    writeExpression(increment);
    out.append(") ");
    startBlock();
  }

  public void endLoop() {
    endBlock();
    endLine();
  }

  public void writeStatement(JavaExpression expression) {
    startLine();
    writeExpression(expression);
    out.append(';');
    endLine();
  }

  public void writeExpression(JavaExpression expression) {
    expression.write(out);
  }

  public void endMethod() {
    endBlock();
    endLine();
    emptyLine();
  }

  public void endClass() {
    endBlock();
    endLine();
    emptyLine();
  }

  @Override
  public void flush() {
    out.flush();
  }

  @Override
  public void close() {
    out.close();
  }

  private void startBlock() {
    out.append('{');
    endLine();
    indent++;
  }

  private void endBlock() {
    indent--;
    startLine();
    out.append('}');
  }

  private void startLine() {
    for (int i = 0; i < indent; i++) {
      out.append("  ");
    }
  }

  private void endLine() {
    out.append('\n');
  }

  private void emptyLine() {
    out.append('\n');
  }

  public static void writeJavaSymbol(PrintWriter out, String symbol) {
    out.append(symbol); // TODO Make safe and validate.
  }

  public void startField(String type, JavaExpression name) {
    startLine();
    out.append("private final ");
    writeJavaSymbol(out, type);
    out.append(' ');
    name.write(out);
    out.append(" = ");
  }

  public void endField() {
    out.append(';');
    endLine();
    emptyLine();
  }

}
