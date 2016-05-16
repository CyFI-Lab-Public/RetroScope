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
import com.google.clearsilver.jsilver.data.Data;
import com.google.clearsilver.jsilver.data.DataContext;
import com.google.clearsilver.jsilver.exceptions.ExceptionUtil;
import com.google.clearsilver.jsilver.exceptions.JSilverIOException;
import com.google.clearsilver.jsilver.exceptions.JSilverInterpreterException;
import com.google.clearsilver.jsilver.functions.FunctionExecutor;
import com.google.clearsilver.jsilver.syntax.analysis.DepthFirstAdapter;
import com.google.clearsilver.jsilver.syntax.node.AAltCommand;
import com.google.clearsilver.jsilver.syntax.node.AAutoescapeCommand;
import com.google.clearsilver.jsilver.syntax.node.ACallCommand;
import com.google.clearsilver.jsilver.syntax.node.ADataCommand;
import com.google.clearsilver.jsilver.syntax.node.ADefCommand;
import com.google.clearsilver.jsilver.syntax.node.AEachCommand;
import com.google.clearsilver.jsilver.syntax.node.AEscapeCommand;
import com.google.clearsilver.jsilver.syntax.node.AEvarCommand;
import com.google.clearsilver.jsilver.syntax.node.AHardIncludeCommand;
import com.google.clearsilver.jsilver.syntax.node.AHardLincludeCommand;
import com.google.clearsilver.jsilver.syntax.node.AIfCommand;
import com.google.clearsilver.jsilver.syntax.node.AIncludeCommand;
import com.google.clearsilver.jsilver.syntax.node.ALincludeCommand;
import com.google.clearsilver.jsilver.syntax.node.ALoopCommand;
import com.google.clearsilver.jsilver.syntax.node.ALoopIncCommand;
import com.google.clearsilver.jsilver.syntax.node.ALoopToCommand;
import com.google.clearsilver.jsilver.syntax.node.ALvarCommand;
import com.google.clearsilver.jsilver.syntax.node.ANameCommand;
import com.google.clearsilver.jsilver.syntax.node.ANameVariable;
import com.google.clearsilver.jsilver.syntax.node.ASetCommand;
import com.google.clearsilver.jsilver.syntax.node.AUvarCommand;
import com.google.clearsilver.jsilver.syntax.node.AVarCommand;
import com.google.clearsilver.jsilver.syntax.node.AWithCommand;
import com.google.clearsilver.jsilver.syntax.node.PCommand;
import com.google.clearsilver.jsilver.syntax.node.PExpression;
import com.google.clearsilver.jsilver.syntax.node.PPosition;
import com.google.clearsilver.jsilver.syntax.node.PVariable;
import com.google.clearsilver.jsilver.syntax.node.TCsOpen;
import com.google.clearsilver.jsilver.syntax.node.TWord;
import com.google.clearsilver.jsilver.template.Macro;
import com.google.clearsilver.jsilver.template.RenderingContext;
import com.google.clearsilver.jsilver.template.Template;
import com.google.clearsilver.jsilver.template.TemplateLoader;
import com.google.clearsilver.jsilver.values.Value;
import com.google.clearsilver.jsilver.values.VariableValue;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Main JSilver interpreter. This walks a template's AST and renders the result out.
 */
public class TemplateInterpreter extends DepthFirstAdapter {

  private final Template template;

  private final ExpressionEvaluator expressionEvaluator;
  private final VariableLocator variableLocator;
  private final TemplateLoader templateLoader;
  private final RenderingContext context;
  private final DataContext dataContext;

  public TemplateInterpreter(Template template, TemplateLoader templateLoader,
      RenderingContext context, FunctionExecutor functionExecutor) {
    this.template = template;
    this.templateLoader = templateLoader;
    this.context = context;
    this.dataContext = context.getDataContext();

    expressionEvaluator = new ExpressionEvaluator(dataContext, functionExecutor);
    variableLocator = new VariableLocator(expressionEvaluator);
  }

  // ------------------------------------------------------------------------
  // COMMAND PROCESSING

  /**
   * Chunk of data (i.e. not a CS command).
   */
  @Override
  public void caseADataCommand(ADataCommand node) {
    context.writeUnescaped(node.getData().getText());
  }

  /**
   * &lt;?cs var:blah &gt; expression. Evaluate as string and write output, using default escaping.
   */
  @Override
  public void caseAVarCommand(AVarCommand node) {
    setLastPosition(node.getPosition());

    // Evaluate expression.
    Value value = expressionEvaluator.evaluate(node.getExpression());
    writeVariable(value);
  }

  /**
   * &lt;?cs uvar:blah &gt; expression. Evaluate as string and write output, but don't escape.
   */
  @Override
  public void caseAUvarCommand(AUvarCommand node) {
    setLastPosition(node.getPosition());

    // Evaluate expression.
    Value value = expressionEvaluator.evaluate(node.getExpression());
    context.writeUnescaped(value.asString());
  }

  /**
   * &lt;?cs lvar:blah &gt; command. Evaluate expression and execute commands within.
   */
  @Override
  public void caseALvarCommand(ALvarCommand node) {
    setLastPosition(node.getPosition());
    evaluateVariable(node.getExpression(), "[lvar expression]");
  }

  /**
   * &lt;?cs evar:blah &gt; command. Evaluate expression and execute commands within.
   */
  @Override
  public void caseAEvarCommand(AEvarCommand node) {
    setLastPosition(node.getPosition());
    evaluateVariable(node.getExpression(), "[evar expression]");
  }

  private void evaluateVariable(PExpression expression, String stackTraceDescription) {
    // Evaluate expression.
    Value value = expressionEvaluator.evaluate(expression);

    // Now parse result, into new mini template.
    Template template =
        templateLoader.createTemp(stackTraceDescription, value.asString(), context
            .getAutoEscapeMode());

    // Intepret new template.
    try {
      template.render(context);
    } catch (IOException e) {
      throw new JSilverInterpreterException(e.getMessage());
    }
  }

  /**
   * &lt;?cs linclude!'somefile.cs' &gt; command. Lazily includes another template (at render time).
   * Throw an error if file does not exist.
   */
  @Override
  public void caseAHardLincludeCommand(AHardLincludeCommand node) {
    setLastPosition(node.getPosition());
    include(node.getExpression(), false);
  }

  /**
   * &lt;?cs linclude:'somefile.cs' &gt; command. Lazily includes another template (at render time).
   * Silently ignore if the included file does not exist.
   */
  @Override
  public void caseALincludeCommand(ALincludeCommand node) {
    setLastPosition(node.getPosition());
    include(node.getExpression(), true);
  }

  /**
   * &lt;?cs include!'somefile.cs' &gt; command. Throw an error if file does not exist.
   */
  @Override
  public void caseAHardIncludeCommand(AHardIncludeCommand node) {
    setLastPosition(node.getPosition());
    include(node.getExpression(), false);
  }

  /**
   * &lt;?cs include:'somefile.cs' &gt; command. Silently ignore if the included file does not
   * exist.
   */
  @Override
  public void caseAIncludeCommand(AIncludeCommand node) {
    setLastPosition(node.getPosition());
    include(node.getExpression(), true);
  }

  /**
   * &lt;?cs set:x='y' &gt; command.
   */
  @Override
  public void caseASetCommand(ASetCommand node) {
    setLastPosition(node.getPosition());
    String variableName = variableLocator.getVariableName(node.getVariable());

    try {
      Data variable = dataContext.findVariable(variableName, true);
      Value value = expressionEvaluator.evaluate(node.getExpression());
      variable.setValue(value.asString());
      // TODO: what about nested structures?
      // "set" was used to set a variable to a constant or escaped value like
      // <?cs set: x = "<b>X</b>" ?> or <?cs set: y = html_escape(x) ?>
      // Keep track of this so autoescaping code can take it into account.
      variable.setEscapeMode(value.getEscapeMode());
    } catch (UnsupportedOperationException e) {
      // An error occurred - probably due to trying to modify an UnmodifiableData
      throw new UnsupportedOperationException(createUnsupportedOperationMessage(node, context
          .getIncludedTemplateNames()), e);
    }
  }

  /**
   * &lt;?cs name:blah &gt; command. Writes out the name of the original variable referred to by a
   * given node.
   */
  @Override
  public void caseANameCommand(ANameCommand node) {
    setLastPosition(node.getPosition());
    String variableName = variableLocator.getVariableName(node.getVariable());
    Data variable = dataContext.findVariable(variableName, false);
    if (variable != null) {
      context.writeEscaped(variable.getSymlink().getName());
    }
  }

  /**
   * &lt;?cs if:blah &gt; ... &lt;?cs else &gt; ... &lt;?cs /if &gt; command.
   */
  @Override
  public void caseAIfCommand(AIfCommand node) {
    setLastPosition(node.getPosition());
    Value value = expressionEvaluator.evaluate(node.getExpression());
    if (value.asBoolean()) {
      node.getBlock().apply(this);
    } else {
      node.getOtherwise().apply(this);
    }
  }


  /**
   * &lt;?cs escape:'html' &gt; command. Changes default escaping function.
   */
  @Override
  public void caseAEscapeCommand(AEscapeCommand node) {
    setLastPosition(node.getPosition());
    Value value = expressionEvaluator.evaluate(node.getExpression());
    String escapeStrategy = value.asString();

    context.pushEscapingFunction(escapeStrategy);
    node.getCommand().apply(this);
    context.popEscapingFunction();
  }

  /**
   * A fake command injected by AutoEscaper.
   * 
   * AutoEscaper determines the html context in which an include or lvar or evar command is called
   * and stores this context in the AAutoescapeCommand node.
   */
  @Override
  public void caseAAutoescapeCommand(AAutoescapeCommand node) {
    setLastPosition(node.getPosition());
    Value value = expressionEvaluator.evaluate(node.getExpression());
    String escapeStrategy = value.asString();

    EscapeMode mode = EscapeMode.computeEscapeMode(escapeStrategy);

    context.pushAutoEscapeMode(mode);
    node.getCommand().apply(this);
    context.popAutoEscapeMode();
  }

  /**
   * &lt;?cs with:x=Something &gt; ... &lt;?cs /with &gt; command. Aliases a value within a specific
   * scope.
   */
  @Override
  public void caseAWithCommand(AWithCommand node) {
    setLastPosition(node.getPosition());
    VariableLocator variableLocator = new VariableLocator(expressionEvaluator);
    String withVar = variableLocator.getVariableName(node.getVariable());
    Value value = expressionEvaluator.evaluate(node.getExpression());

    if (value instanceof VariableValue) {
      if (((VariableValue) value).getReference() == null) {
        // With refers to a non-existent variable. Do nothing.
        return;
      }
    }

    dataContext.pushVariableScope();
    setTempVariable(withVar, value);
    node.getCommand().apply(this);
    dataContext.popVariableScope();
  }

  /**
   * &lt;?cs loop:10 &gt; ... &lt;?cs /loop &gt; command. Loops over a range of numbers, starting at
   * zero.
   */
  @Override
  public void caseALoopToCommand(ALoopToCommand node) {
    setLastPosition(node.getPosition());
    int end = expressionEvaluator.evaluate(node.getExpression()).asNumber();

    // Start is always zero, increment is always 1, so end < 0 is invalid.
    if (end < 0) {
      return; // Incrementing the wrong way. Avoid infinite loop.
    }

    loop(node.getVariable(), 0, end, 1, node.getCommand());
  }

  /**
   * &lt;?cs loop:0,10 &gt; ... &lt;?cs /loop &gt; command. Loops over a range of numbers.
   */
  @Override
  public void caseALoopCommand(ALoopCommand node) {
    setLastPosition(node.getPosition());
    int start = expressionEvaluator.evaluate(node.getStart()).asNumber();
    int end = expressionEvaluator.evaluate(node.getEnd()).asNumber();

    // Start is always zero, increment is always 1, so end < 0 is invalid.
    if (end < start) {
      return; // Incrementing the wrong way. Avoid infinite loop.
    }

    loop(node.getVariable(), start, end, 1, node.getCommand());
  }

  /**
   * &lt;?cs loop:0,10,2 &gt; ... &lt;?cs /loop &gt; command. Loops over a range of numbers, with a
   * specific increment.
   */
  @Override
  public void caseALoopIncCommand(ALoopIncCommand node) {
    setLastPosition(node.getPosition());
    int start = expressionEvaluator.evaluate(node.getStart()).asNumber();
    int end = expressionEvaluator.evaluate(node.getEnd()).asNumber();
    int incr = expressionEvaluator.evaluate(node.getIncrement()).asNumber();

    if (incr == 0) {
      return; // No increment. Avoid infinite loop.
    }
    if (incr > 0 && start > end) {
      return; // Incrementing the wrong way. Avoid infinite loop.
    }
    if (incr < 0 && start < end) {
      return; // Incrementing the wrong way. Avoid infinite loop.
    }

    loop(node.getVariable(), start, end, incr, node.getCommand());
  }

  /**
   * &lt;?cs each:x=Stuff &gt; ... &lt;?cs /each &gt; command. Loops over child items of a data
   * node.
   */
  @Override
  public void caseAEachCommand(AEachCommand node) {
    setLastPosition(node.getPosition());
    Value expression = expressionEvaluator.evaluate(node.getExpression());

    if (expression instanceof VariableValue) {
      VariableValue variableValue = (VariableValue) expression;
      Data parent = variableValue.getReference();
      if (parent != null) {
        each(node.getVariable(), variableValue.getName(), parent, node.getCommand());
      }
    }
  }

  /**
   * &lt;?cs alt:someValue &gt; ... &lt;?cs /alt &gt; command. If value exists, write it, otherwise
   * write the body of the command.
   */
  @Override
  public void caseAAltCommand(AAltCommand node) {
    setLastPosition(node.getPosition());
    Value value = expressionEvaluator.evaluate(node.getExpression());
    if (value.asBoolean()) {
      writeVariable(value);
    } else {
      node.getCommand().apply(this);
    }
  }

  private void writeVariable(Value value) {
    if (template.getEscapeMode().isAutoEscapingMode()) {
      autoEscapeAndWriteVariable(value);
    } else if (value.isPartiallyEscaped()) {
      context.writeUnescaped(value.asString());
    } else {
      context.writeEscaped(value.asString());
    }
  }

  private void autoEscapeAndWriteVariable(Value value) {
    if (isTrustedValue(value) || value.isPartiallyEscaped()) {
      context.writeUnescaped(value.asString());
    } else {
      context.writeEscaped(value.asString());
    }
  }

  private boolean isTrustedValue(Value value) {
    // True if PropagateEscapeStatus is enabled and value has either been
    // escaped or contains a constant string.
    return context.getAutoEscapeOptions().getPropagateEscapeStatus()
        && !value.getEscapeMode().equals(EscapeMode.ESCAPE_NONE);
  }

  // ------------------------------------------------------------------------
  // MACROS

  /**
   * &lt;?cs def:someMacro(x,y) &gt; ... &lt;?cs /def &gt; command. Define a macro (available for
   * the remainder of the interpreter context.
   */
  @Override
  public void caseADefCommand(ADefCommand node) {
    String macroName = makeWord(node.getMacro());
    LinkedList<PVariable> arguments = node.getArguments();
    String[] argumentNames = new String[arguments.size()];
    int i = 0;
    for (PVariable argument : arguments) {
      if (!(argument instanceof ANameVariable)) {
        throw new JSilverInterpreterException("Invalid name for macro '" + macroName
            + "' argument " + i + " : " + argument);
      }
      argumentNames[i++] = ((ANameVariable) argument).getWord().getText();
    }
    // TODO: Should we enforce that macro args can't repeat the same
    // name?
    context.registerMacro(macroName, new InterpretedMacro(node.getCommand(), template, macroName,
        argumentNames, this, context));
  }

  private String makeWord(LinkedList<TWord> words) {
    if (words.size() == 1) {
      return words.getFirst().getText();
    }
    StringBuilder result = new StringBuilder();
    for (TWord word : words) {
      if (result.length() > 0) {
        result.append('.');
      }
      result.append(word.getText());
    }
    return result.toString();
  }

  /**
   * &lt;?cs call:someMacro(x,y) command. Call a macro. Need to create a new variable scope to hold
   * the local variables defined by the parameters of the macro definition
   */
  @Override
  public void caseACallCommand(ACallCommand node) {
    String macroName = makeWord(node.getMacro());
    Macro macro = context.findMacro(macroName);

    // Make sure that the number of arguments passed to the macro match the
    // number expected.
    if (node.getArguments().size() != macro.getArgumentCount()) {
      throw new JSilverInterpreterException("Number of arguments to macro " + macroName + " ("
          + node.getArguments().size() + ") does not match " + "number of expected arguments ("
          + macro.getArgumentCount() + ")");
    }

    int numArgs = node.getArguments().size();
    if (numArgs > 0) {
      Value[] argValues = new Value[numArgs];

      // We must first evaluate the parameters we are passing or there could be
      // conflicts if new argument names match existing variables.
      Iterator<PExpression> argumentValues = node.getArguments().iterator();
      for (int i = 0; argumentValues.hasNext(); i++) {
        argValues[i] = expressionEvaluator.evaluate(argumentValues.next());
      }

      // No need to bother pushing and popping the variable scope stack
      // if there are no new local variables to declare.
      dataContext.pushVariableScope();

      for (int i = 0; i < argValues.length; i++) {
        setTempVariable(macro.getArgumentName(i), argValues[i]);
      }
    }
    try {
      macro.render(context);
    } catch (IOException e) {
      throw new JSilverIOException(e);
    }
    if (numArgs > 0) {
      // No need to bother pushing and popping the variable scope stack
      // if there are no new local variables to declare.
      dataContext.popVariableScope();
    }
  }

  // ------------------------------------------------------------------------
  // HELPERS
  //
  // Much of the functionality in this section could easily be inlined,
  // however it makes the rest of the interpreter much easier to understand
  // and refactor with them defined here.

  private void each(PVariable variable, String parentName, Data items, PCommand command) {
    // Since HDF variables are now passed to macro parameters by path name
    // we need to create a path for each child when generating the
    // VariableValue object.
    VariableLocator variableLocator = new VariableLocator(expressionEvaluator);
    String eachVar = variableLocator.getVariableName(variable);
    StringBuilder pathBuilder = new StringBuilder(parentName);
    pathBuilder.append('.');
    int length = pathBuilder.length();
    dataContext.pushVariableScope();
    for (Data child : items.getChildren()) {
      pathBuilder.delete(length, pathBuilder.length());
      pathBuilder.append(child.getName());
      setTempVariable(eachVar, Value.variableValue(pathBuilder.toString(), dataContext));
      command.apply(this);
    }
    dataContext.popVariableScope();
  }

  private void loop(PVariable loopVar, int start, int end, int incr, PCommand command) {
    VariableLocator variableLocator = new VariableLocator(expressionEvaluator);
    String varName = variableLocator.getVariableName(loopVar);

    dataContext.pushVariableScope();
    // Loop deals with counting forward or backwards.
    for (int index = start; incr > 0 ? index <= end : index >= end; index += incr) {
      // We reuse the same scope for efficiency and simply overwrite the
      // previous value of the loop variable.
      dataContext.createLocalVariableByValue(varName, String.valueOf(index), index == start,
          index == end);

      command.apply(this);
    }
    dataContext.popVariableScope();
  }

  /**
   * Code common to all three include commands.
   * 
   * @param expression expression representing name of file to include.
   * @param ignoreMissingFile {@code true} if any FileNotFound error generated by the template
   *        loader should be ignored, {@code false} otherwise.
   */
  private void include(PExpression expression, boolean ignoreMissingFile) {
    // Evaluate expression.
    Value path = expressionEvaluator.evaluate(expression);

    String templateName = path.asString();
    if (!context.pushIncludeStackEntry(templateName)) {
      throw new JSilverInterpreterException(createIncludeLoopErrorMessage(templateName, context
          .getIncludedTemplateNames()));
    }

    loadAndRenderIncludedTemplate(templateName, ignoreMissingFile);

    if (!context.popIncludeStackEntry(templateName)) {
      // Include stack trace is corrupted
      throw new IllegalStateException("Unable to find on include stack: " + templateName);
    }
  }

  private String createIncludeLoopErrorMessage(String templateName, Iterable<String> includeStack) {
    StringBuilder message = new StringBuilder();
    message.append("File included twice: ");
    message.append(templateName);

    message.append(" Include stack:");
    for (String fileName : includeStack) {
      message.append("\n -> ");
      message.append(fileName);
    }
    message.append("\n -> ");
    message.append(templateName);
    return message.toString();
  }

  private String createUnsupportedOperationMessage(PCommand node, Iterable<String> includeStack) {
    StringBuilder message = new StringBuilder();

    message.append("exception thrown while parsing node: ");
    message.append(node.toString());
    message.append(" (class ").append(node.getClass().getSimpleName()).append(")");
    message.append("\nTemplate include stack: ");

    for (Iterator<String> iter = includeStack.iterator(); iter.hasNext();) {
      message.append(iter.next());
      if (iter.hasNext()) {
        message.append(" -> ");
      }
    }
    message.append("\n");

    return message.toString();
  }

  // This method should ONLY be called from include()
  private void loadAndRenderIncludedTemplate(String templateName, boolean ignoreMissingFile) {
    // Now load new template with given name.
    Template template = null;
    try {
      template =
          templateLoader.load(templateName, context.getResourceLoader(), context
              .getAutoEscapeMode());
    } catch (RuntimeException e) {
      if (ignoreMissingFile && ExceptionUtil.isFileNotFoundException(e)) {
        return;
      } else {
        throw e;
      }
    }

    // Intepret loaded template.
    try {
      // TODO: Execute lincludes (but not includes) in a separate
      // context.
      template.render(context);
    } catch (IOException e) {
      throw new JSilverInterpreterException(e.getMessage());
    }
  }

  private void setLastPosition(PPosition position) {
    // Walks position node which will eventually result in calling
    // caseTCsOpen().
    position.apply(this);
  }

  /**
   * Every time a &lt;cs token is found, grab the line and position (for helpful error messages).
   */
  @Override
  public void caseTCsOpen(TCsOpen node) {
    int line = node.getLine();
    int column = node.getPos();
    context.setCurrentPosition(line, column);
  }

  private void setTempVariable(String variableName, Value value) {
    if (value instanceof VariableValue) {
      // If the value is a Data variable name, then we store a reference to its
      // name as discovered by the expression evaluator and resolve it each
      // time for correctness.
      dataContext.createLocalVariableByPath(variableName, ((VariableValue) value).getName());
    } else {
      dataContext.createLocalVariableByValue(variableName, value.asString(), value.getEscapeMode());
    }
  }

}
