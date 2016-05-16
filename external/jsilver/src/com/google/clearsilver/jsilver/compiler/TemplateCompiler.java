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

import com.google.clearsilver.jsilver.autoescape.AutoEscapeOptions;
import com.google.clearsilver.jsilver.autoescape.EscapeMode;
import com.google.clearsilver.jsilver.functions.FunctionExecutor;
import com.google.clearsilver.jsilver.interpreter.TemplateFactory;
import com.google.clearsilver.jsilver.resourceloader.ResourceLoader;
import com.google.clearsilver.jsilver.syntax.TemplateSyntaxTree;
import com.google.clearsilver.jsilver.template.DelegatingTemplateLoader;
import com.google.clearsilver.jsilver.template.Template;
import com.google.clearsilver.jsilver.template.TemplateLoader;

import java.io.StringWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;

/**
 * Takes a template AST and compiles it into a Java class, which executes much faster than the
 * intepreter.
 */
public class TemplateCompiler implements DelegatingTemplateLoader {

  private static final Logger logger = Logger.getLogger(TemplateCompiler.class.getName());

  private static final String PACKAGE_NAME = "com.google.clearsilver.jsilver.compiler";

  // Because each template is isolated in its own ClassLoader, it doesn't
  // matter if there are naming clashes between templates.
  private static final String CLASS_NAME = "$CompiledTemplate";

  private final TemplateFactory templateFactory;

  private final FunctionExecutor globalFunctionExecutor;
  private final AutoEscapeOptions autoEscapeOptions;
  private TemplateLoader templateLoaderDelegate = this;

  public TemplateCompiler(TemplateFactory templateFactory, FunctionExecutor globalFunctionExecutor,
      AutoEscapeOptions autoEscapeOptions) {
    this.templateFactory = templateFactory;
    this.globalFunctionExecutor = globalFunctionExecutor;
    this.autoEscapeOptions = autoEscapeOptions;
  }

  @Override
  public void setTemplateLoaderDelegate(TemplateLoader templateLoaderDelegate) {
    this.templateLoaderDelegate = templateLoaderDelegate;
  }

  @Override
  public Template load(String templateName, ResourceLoader resourceLoader, EscapeMode escapeMode) {
    return compile(templateFactory.find(templateName, resourceLoader, escapeMode), templateName,
        escapeMode);
  }

  @Override
  public Template createTemp(String name, String content, EscapeMode escapeMode) {
    return compile(templateFactory.createTemp(content, escapeMode), name, escapeMode);
  }

  /**
   * Compile AST into Java class.
   * 
   * @param ast A template AST.
   * @param templateName Name of template (e.g. "foo.cs"). Used for error reporting. May be null,
   * @return Template that can be executed (again and again).
   */
  private Template compile(TemplateSyntaxTree ast, String templateName, EscapeMode mode) {
    CharSequence javaSource = translateAstToJavaSource(ast, mode);

    String errorMessage = "Could not compile template: " + templateName;
    Class<?> templateClass = compileAndLoad(javaSource, errorMessage);

    try {
      BaseCompiledTemplate compiledTemplate = (BaseCompiledTemplate) templateClass.newInstance();
      compiledTemplate.setFunctionExecutor(globalFunctionExecutor);
      compiledTemplate.setTemplateName(templateName);
      compiledTemplate.setTemplateLoader(templateLoaderDelegate);
      compiledTemplate.setEscapeMode(mode);
      compiledTemplate.setAutoEscapeOptions(autoEscapeOptions);
      return compiledTemplate;
    } catch (InstantiationException e) {
      throw new Error(e); // Should not be possible. Throw Error if it does.
    } catch (IllegalAccessException e) {
      throw new Error(e); // Should not be possible. Throw Error if it does.
    }
  }

  private CharSequence translateAstToJavaSource(TemplateSyntaxTree ast, EscapeMode mode) {
    StringWriter sourceBuffer = new StringWriter(256);
    boolean propagateStatus =
        autoEscapeOptions.getPropagateEscapeStatus() && mode.isAutoEscapingMode();
    ast.apply(new TemplateTranslator(PACKAGE_NAME, CLASS_NAME, sourceBuffer, propagateStatus));
    StringBuffer javaSource = sourceBuffer.getBuffer();
    logger.log(Level.FINEST, "Compiled template:\n{0}", javaSource);
    return javaSource;
  }

  private Class<?> compileAndLoad(CharSequence javaSource, String errorMessage)
      throws JSilverCompilationException {
    // Need a parent class loader to load dependencies from.
    // This does not use any libraries outside of JSilver (e.g. custom user
    // libraries), so using this class's ClassLoader should be fine.
    ClassLoader parentClassLoader = getClass().getClassLoader();

    // Collect any compiler errors/warnings.
    DiagnosticCollector<JavaFileObject> diagnosticCollector =
        new DiagnosticCollector<JavaFileObject>();

    try {
      // Magical ClassLoader that compiles source code on the fly.
      CompilingClassLoader templateClassLoader =
          new CompilingClassLoader(parentClassLoader, CLASS_NAME, javaSource, diagnosticCollector);
      return templateClassLoader.loadClass(PACKAGE_NAME + "." + CLASS_NAME);
    } catch (Exception e) {
      // Ordinarily, this shouldn't happen as the code is generated. However,
      // in case there's a bug in JSilver, it will be helpful to have as much
      // info as possible in the exception to diagnose the problem.
      throwExceptionWithLotsOfDiagnosticInfo(javaSource, errorMessage, diagnosticCollector
          .getDiagnostics(), e);
      return null; // Keep compiler happy.
    }
  }

  private void throwExceptionWithLotsOfDiagnosticInfo(CharSequence javaSource, String errorMessage,
      List<Diagnostic<? extends JavaFileObject>> diagnostics, Exception cause)
      throws JSilverCompilationException {
    // Create exception with lots of info in it.
    StringBuilder message = new StringBuilder(errorMessage).append('\n');
    message.append("------ Source code ------\n").append(javaSource);
    message.append("------ Compiler messages ------\n");
    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {
      message.append(diagnostic).append('\n');
    }
    message.append("------ ------\n");
    throw new JSilverCompilationException(message.toString(), cause);
  }
}
