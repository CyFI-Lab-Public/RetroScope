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

import com.google.clearsilver.jsilver.autoescape.EscapeMode;
import static com.google.clearsilver.jsilver.compiler.JavaExpression.BooleanLiteralExpression;
import static com.google.clearsilver.jsilver.compiler.JavaExpression.Type;
import static com.google.clearsilver.jsilver.compiler.JavaExpression.call;
import static com.google.clearsilver.jsilver.compiler.JavaExpression.callFindVariable;
import static com.google.clearsilver.jsilver.compiler.JavaExpression.callOn;
import static com.google.clearsilver.jsilver.compiler.JavaExpression.declare;
import static com.google.clearsilver.jsilver.compiler.JavaExpression.increment;
import static com.google.clearsilver.jsilver.compiler.JavaExpression.infix;
import static com.google.clearsilver.jsilver.compiler.JavaExpression.inlineIf;
import static com.google.clearsilver.jsilver.compiler.JavaExpression.integer;
import static com.google.clearsilver.jsilver.compiler.JavaExpression.literal;
import static com.google.clearsilver.jsilver.compiler.JavaExpression.macro;
import static com.google.clearsilver.jsilver.compiler.JavaExpression.string;
import static com.google.clearsilver.jsilver.compiler.JavaExpression.symbol;
import com.google.clearsilver.jsilver.data.Data;
import com.google.clearsilver.jsilver.data.DataContext;
import com.google.clearsilver.jsilver.functions.Function;
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
import com.google.clearsilver.jsilver.syntax.node.ANoopCommand;
import com.google.clearsilver.jsilver.syntax.node.ASetCommand;
import com.google.clearsilver.jsilver.syntax.node.AUvarCommand;
import com.google.clearsilver.jsilver.syntax.node.AVarCommand;
import com.google.clearsilver.jsilver.syntax.node.AWithCommand;
import com.google.clearsilver.jsilver.syntax.node.PCommand;
import com.google.clearsilver.jsilver.syntax.node.PExpression;
import com.google.clearsilver.jsilver.syntax.node.PPosition;
import com.google.clearsilver.jsilver.syntax.node.PVariable;
import com.google.clearsilver.jsilver.syntax.node.Start;
import com.google.clearsilver.jsilver.syntax.node.TCsOpen;
import com.google.clearsilver.jsilver.syntax.node.TWord;
import com.google.clearsilver.jsilver.template.Macro;
import com.google.clearsilver.jsilver.template.RenderingContext;
import com.google.clearsilver.jsilver.template.Template;
import com.google.clearsilver.jsilver.values.Value;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Translates a JSilver AST into compilable Java code. This executes much faster than the
 * interpreter.
 * 
 * @see TemplateCompiler
 */
public class TemplateTranslator extends DepthFirstAdapter {

  // Root data
  public static final JavaExpression DATA = symbol(Type.DATA, "data");
  // RenderingContext
  public static final JavaExpression CONTEXT = symbol("context");
  // DataContext
  public static final JavaExpression DATA_CONTEXT = symbol(Type.DATA_CONTEXT, "dataContext");
  public static final JavaExpression NULL = symbol("null");
  // Accessed from macros as well.
  public static final JavaExpression RESOURCE_LOADER = callOn(CONTEXT, "getResourceLoader");
  public static final JavaExpression TEMPLATE_LOADER = symbol("getTemplateLoader()");
  public static final JavaExpression THIS_TEMPLATE = symbol("this");

  private final JavaSourceWriter java;

  private final String packageName;
  private final String className;

  private final ExpressionTranslator expressionTranslator = new ExpressionTranslator();
  private final VariableTranslator variableTranslator =
      new VariableTranslator(expressionTranslator);
  private final EscapingEvaluator escapingEvaluator = new EscapingEvaluator(variableTranslator);

  private static final Method RENDER_METHOD;

  private int tempVariable = 0;
  /**
   * Used to determine the escaping to apply before displaying a variable. If propagateEscapeStatus
   * is enabled, string and numeric literals are not escaped, nor is the output of an escaping
   * function. If not, any expression that contains an escaping function is not escaped. This
   * maintains compatibility with the way ClearSilver works.
   */
  private boolean propagateEscapeStatus;

  /**
   * Holds Macro information used while generating code.
   */
  private static class MacroInfo {
    /**
     * JavaExpression used for outputting the static Macro variable name.
     */
    JavaExpression symbol;

    /**
     * Parser node for the definition. Stored for evaluation after main render method is output.
     */
    ADefCommand defNode;
  }

  /**
   * Map of macro names to definition nodes and java expressions used to refer to them.
   */
  private final Map<String, MacroInfo> macroMap = new HashMap<String, MacroInfo>();

  /**
   * Used to iterate through list of macros. We can't rely on Map's iterator because we may be
   * adding to the map as we iterate through the values() list and that would throw a
   * ConcurrentModificationException.
   */
  private final Queue<MacroInfo> macroQueue = new LinkedList<MacroInfo>();

  /**
   * Creates a MacroInfo object and adds it to the data structures. Also outputs statement to
   * register the macro.
   * 
   * @param name name of the macro as defined in the template.
   * @param symbol static variable name of the macro definition.
   * @param defNode parser node holding the macro definition to be evaluated later.
   */
  private void addMacro(String name, JavaExpression symbol, ADefCommand defNode) {
    if (macroMap.get(name) != null) {
      // TODO: This macro is already defined. Should throw an error.
    }
    MacroInfo info = new MacroInfo();
    info.symbol = symbol;
    info.defNode = defNode;
    macroMap.put(name, info);
    macroQueue.add(info);

    // Register the macro.
    java.writeStatement(callOn(CONTEXT, "registerMacro", string(name), symbol));
  }

  static {
    try {
      RENDER_METHOD = Template.class.getMethod("render", RenderingContext.class);
    } catch (NoSuchMethodException e) {
      throw new Error("Cannot find CompiledTemplate.render() method! " + "Has signature changed?",
          e);
    }
  }

  public TemplateTranslator(String packageName, String className, Writer output,
      boolean propagateEscapeStatus) {
    this.packageName = packageName;
    this.className = className;
    java = new JavaSourceWriter(output);
    this.propagateEscapeStatus = propagateEscapeStatus;
  }

  @Override
  public void caseStart(Start node) {
    java.writeComment("This class is autogenerated by JSilver. Do not edit.");
    java.writePackage(packageName);
    java.writeImports(BaseCompiledTemplate.class, Template.class, Macro.class,
        RenderingContext.class, Data.class, DataContext.class, Function.class,
        FunctionExecutor.class, Value.class, EscapeMode.class, IOException.class);
    java.startClass(className, BaseCompiledTemplate.class.getSimpleName());

    // Implement render() method.
    java.startMethod(RENDER_METHOD, "context");
    java
        .writeStatement(declare(Type.DATA_CONTEXT, "dataContext", callOn(CONTEXT, "getDataContext")));
    java.writeStatement(callOn(CONTEXT, "pushExecutionContext", THIS_TEMPLATE));
    super.caseStart(node); // Walk template AST.
    java.writeStatement(callOn(CONTEXT, "popExecutionContext"));
    java.endMethod();

    // The macros have to be defined outside of the render method.
    // (Well actually they *could* be defined inline as anon classes, but it
    // would make the generated code quite hard to understand).
    MacroTransformer macroTransformer = new MacroTransformer();

    while (!macroQueue.isEmpty()) {
      MacroInfo curr = macroQueue.remove();
      macroTransformer.parseDefNode(curr.symbol, curr.defNode);
    }

    java.endClass();
  }

  /**
   * Chunk of data (i.e. not a CS command).
   */
  @Override
  public void caseADataCommand(ADataCommand node) {
    String content = node.getData().getText();
    java.writeStatement(callOn(CONTEXT, "writeUnescaped", string(content)));
  }

  /**
   * &lt;?cs var:blah &gt; expression. Evaluate as string and write output, using default escaping.
   */
  @Override
  public void caseAVarCommand(AVarCommand node) {
    capturePosition(node.getPosition());

    String tempVariableName = generateTempVariable("result");
    JavaExpression result = symbol(Type.STRING, tempVariableName);
    java.writeStatement(declare(Type.STRING, tempVariableName, expressionTranslator
        .translateToString(node.getExpression())));

    JavaExpression escaping =
        escapingEvaluator.computeIfExemptFromEscaping(node.getExpression(), propagateEscapeStatus);
    writeVariable(result, escaping);
  }

  /**
   * &lt;?cs uvar:blah &gt; expression. Evaluate as string and write output, but don't escape.
   */
  @Override
  public void caseAUvarCommand(AUvarCommand node) {
    capturePosition(node.getPosition());
    java.writeStatement(callOn(CONTEXT, "writeUnescaped", expressionTranslator
        .translateToString(node.getExpression())));
  }

  /**
   * &lt;?cs set:x='y' &gt; command.
   */
  @Override
  public void caseASetCommand(ASetCommand node) {
    capturePosition(node.getPosition());
    String tempVariableName = generateTempVariable("setNode");

    // Data setNode1 = dataContext.findVariable("x", true);
    JavaExpression setNode = symbol(Type.DATA, tempVariableName);
    java.writeStatement(declare(Type.DATA, tempVariableName, callFindVariable(variableTranslator
        .translate(node.getVariable()), true)));
    // setNode1.setValue("hello");
    java.writeStatement(callOn(setNode, "setValue", expressionTranslator.translateToString(node
        .getExpression())));

    if (propagateEscapeStatus) {
      // setNode1.setEscapeMode(EscapeMode.ESCAPE_IS_CONSTANT);
      java.writeStatement(callOn(setNode, "setEscapeMode", escapingEvaluator.computeEscaping(node
          .getExpression(), propagateEscapeStatus)));
    }
  }

  /**
   * &lt;?cs name:blah &gt; command. Writes out the name of the original variable referred to by a
   * given node.
   */
  @Override
  public void caseANameCommand(ANameCommand node) {
    capturePosition(node.getPosition());
    JavaExpression readNode =
        callFindVariable(variableTranslator.translate(node.getVariable()), false);
    java.writeStatement(callOn(CONTEXT, "writeEscaped", call("getNodeName", readNode)));
  }

  /**
   * &lt;?cs if:blah &gt; ... &lt;?cs else &gt; ... &lt;?cs /if &gt; command.
   */
  @Override
  public void caseAIfCommand(AIfCommand node) {
    capturePosition(node.getPosition());

    java.startIfBlock(expressionTranslator.translateToBoolean(node.getExpression()));
    node.getBlock().apply(this);
    if (!(node.getOtherwise() instanceof ANoopCommand)) {
      java.endIfStartElseBlock();
      node.getOtherwise().apply(this);
    }
    java.endIfBlock();
  }

  /**
   * &lt;?cs each:x=Stuff &gt; ... &lt;?cs /each &gt; command. Loops over child items of a data
   * node.
   */
  @Override
  public void caseAEachCommand(AEachCommand node) {
    capturePosition(node.getPosition());

    JavaExpression parent = expressionTranslator.translateToData(node.getExpression());
    writeEach(node.getVariable(), parent, node.getCommand());
  }

  /**
   * &lt;?cs with:x=Something &gt; ... &lt;?cs /with &gt; command. Aliases a value within a specific
   * scope.
   */
  @Override
  public void caseAWithCommand(AWithCommand node) {
    capturePosition(node.getPosition());

    java.startScopedBlock();
    java.writeComment("with:");

    // Extract the value first in case the temp variable has the same name.
    JavaExpression value = expressionTranslator.translateUntyped(node.getExpression());
    String methodName = null;
    if (value.getType() == Type.VAR_NAME) {
      String withValueName = generateTempVariable("withValue");
      java.writeStatement(declare(Type.STRING, withValueName, value));
      value = symbol(Type.VAR_NAME, withValueName);
      methodName = "createLocalVariableByPath";

      // We need to check if the variable exists. If not, we skip the with
      // call.
      java.startIfBlock(JavaExpression.infix(Type.BOOLEAN, "!=", value.cast(Type.DATA), literal(
          Type.DATA, "null")));
    } else {
      // Cast to string so we support numeric or boolean values as well.
      value = value.cast(Type.STRING);
      methodName = "createLocalVariableByValue";
    }

    JavaExpression itemKey = variableTranslator.translate(node.getVariable());

    // Push a new local variable scope for the with local variable
    java.writeStatement(callOn(DATA_CONTEXT, "pushVariableScope"));

    java.writeStatement(callOn(DATA_CONTEXT, methodName, itemKey, value));
    node.getCommand().apply(this);

    // Release the variable scope used by the with statement
    java.writeStatement(callOn(DATA_CONTEXT, "popVariableScope"));

    if (value.getType() == Type.VAR_NAME) {
      // End of if block that checks that the Data node exists.
      java.endIfBlock();
    }

    java.endScopedBlock();
  }

  /**
   * &lt;?cs loop:10 &gt; ... &lt;?cs /loop &gt; command. Loops over a range of numbers, starting at
   * zero.
   */
  @Override
  public void caseALoopToCommand(ALoopToCommand node) {
    capturePosition(node.getPosition());

    JavaExpression start = integer(0);
    JavaExpression end = expressionTranslator.translateToNumber(node.getExpression());
    JavaExpression incr = integer(1);
    writeLoop(node.getVariable(), start, end, incr, node.getCommand());
  }

  /**
   * &lt;?cs loop:0,10 &gt; ... &lt;?cs /loop &gt; command. Loops over a range of numbers.
   */
  @Override
  public void caseALoopCommand(ALoopCommand node) {
    capturePosition(node.getPosition());

    JavaExpression start = expressionTranslator.translateToNumber(node.getStart());
    JavaExpression end = expressionTranslator.translateToNumber(node.getEnd());
    JavaExpression incr = integer(1);
    writeLoop(node.getVariable(), start, end, incr, node.getCommand());
  }

  /**
   * &lt;?cs loop:0,10,2 &gt; ... &lt;?cs /loop &gt; command. Loops over a range of numbers, with a
   * specific increment.
   */
  @Override
  public void caseALoopIncCommand(ALoopIncCommand node) {
    capturePosition(node.getPosition());

    JavaExpression start = expressionTranslator.translateToNumber(node.getStart());
    JavaExpression end = expressionTranslator.translateToNumber(node.getEnd());
    JavaExpression incr = expressionTranslator.translateToNumber(node.getIncrement());
    writeLoop(node.getVariable(), start, end, incr, node.getCommand());
  }

  private void writeLoop(PVariable itemVariable, JavaExpression start, JavaExpression end,
      JavaExpression incr, PCommand command) {

    java.startScopedBlock();

    String startVarName = generateTempVariable("start");
    java.writeStatement(declare(Type.INT, startVarName, start));
    JavaExpression startVar = symbol(Type.INT, startVarName);

    String endVarName = generateTempVariable("end");
    java.writeStatement(declare(Type.INT, endVarName, end));
    JavaExpression endVar = symbol(Type.INT, endVarName);

    String incrVarName = generateTempVariable("incr");
    java.writeStatement(declare(Type.INT, incrVarName, incr));
    JavaExpression incrVar = symbol(Type.INT, incrVarName);

    // TODO: Test correctness of values.
    java.startIfBlock(call(Type.BOOLEAN, "validateLoopArgs", startVar, endVar, incrVar));

    JavaExpression itemKey = variableTranslator.translate(itemVariable);

    // Push a new local variable scope for the loop local variable
    java.writeStatement(callOn(DATA_CONTEXT, "pushVariableScope"));

    String loopVariable = generateTempVariable("loop");
    JavaExpression loopVar = symbol(Type.INT, loopVariable);
    JavaExpression ifStart = declare(Type.INT, loopVariable, startVar);
    JavaExpression ifEnd =
        inlineIf(Type.BOOLEAN, infix(Type.BOOLEAN, ">=", incrVar, integer(0)), infix(Type.BOOLEAN,
            "<=", loopVar, endVar), infix(Type.BOOLEAN, ">=", loopVar, endVar));
    java.startForLoop(ifStart, ifEnd, increment(Type.INT, loopVar, incrVar));

    java.writeStatement(callOn(DATA_CONTEXT, "createLocalVariableByValue", itemKey, symbol(
        loopVariable).cast(Type.STRING), infix(Type.BOOLEAN, "==", symbol(loopVariable), startVar),
        infix(Type.BOOLEAN, "==", symbol(loopVariable), endVar)));
    command.apply(this);

    java.endLoop();

    // Release the variable scope used by the loop statement
    java.writeStatement(callOn(DATA_CONTEXT, "popVariableScope"));

    java.endIfBlock();
    java.endScopedBlock();
  }

  private void writeEach(PVariable itemVariable, JavaExpression parentData, PCommand command) {

    JavaExpression itemKey = variableTranslator.translate(itemVariable);

    // Push a new local variable scope for the each local variable
    java.writeStatement(callOn(DATA_CONTEXT, "pushVariableScope"));

    String childDataVariable = generateTempVariable("child");
    java.startIterableForLoop("Data", childDataVariable, call("getChildren", parentData));

    java.writeStatement(callOn(DATA_CONTEXT, "createLocalVariableByPath", itemKey, callOn(
        Type.STRING, symbol(childDataVariable), "getFullPath")));
    command.apply(this);

    java.endLoop();

    // Release the variable scope used by the each statement
    java.writeStatement(callOn(DATA_CONTEXT, "popVariableScope"));
  }

  /**
   * &lt;?cs alt:someValue &gt; ... &lt;?cs /alt &gt; command. If value exists, write it, otherwise
   * write the body of the command.
   */
  @Override
  public void caseAAltCommand(AAltCommand node) {
    capturePosition(node.getPosition());
    String tempVariableName = generateTempVariable("altVar");

    JavaExpression declaration =
        expressionTranslator.declareAsVariable(tempVariableName, node.getExpression());
    JavaExpression reference = symbol(declaration.getType(), tempVariableName);
    java.writeStatement(declaration);
    java.startIfBlock(reference.cast(Type.BOOLEAN));

    JavaExpression escaping =
        escapingEvaluator.computeIfExemptFromEscaping(node.getExpression(), propagateEscapeStatus);
    writeVariable(reference, escaping);
    java.endIfStartElseBlock();
    node.getCommand().apply(this);
    java.endIfBlock();
  }

  /*
   * Generates a statement that will write out a variable expression, after determining whether the
   * variable expression should be exempted from any global escaping that may currently be in
   * effect. We try to make this determination during translation if possible, and if we cannot, we
   * output an if/else statement to check the escaping status of the expression at run time.
   * 
   * Currently, unless the expression contains a function call, we know at translation tmie that it
   * does not need to be exempted.
   */
  private void writeVariable(JavaExpression result, JavaExpression escapingExpression) {

    if (escapingExpression instanceof BooleanLiteralExpression) {
      BooleanLiteralExpression expr = (BooleanLiteralExpression) escapingExpression;
      if (expr.getValue()) {
        java.writeStatement(callOn(CONTEXT, "writeUnescaped", result.cast(Type.STRING)));
      } else {
        java.writeStatement(callOn(CONTEXT, "writeEscaped", result.cast(Type.STRING)));
      }

    } else {
      java.startIfBlock(escapingExpression);
      java.writeStatement(callOn(CONTEXT, "writeUnescaped", result.cast(Type.STRING)));
      java.endIfStartElseBlock();
      java.writeStatement(callOn(CONTEXT, "writeEscaped", result.cast(Type.STRING)));
      java.endIfBlock();
    }
  }

  /**
   * &lt;?cs escape:'html' &gt; command. Changes default escaping function.
   */
  @Override
  public void caseAEscapeCommand(AEscapeCommand node) {
    capturePosition(node.getPosition());
    java.writeStatement(callOn(CONTEXT, "pushEscapingFunction", expressionTranslator
        .translateToString(node.getExpression())));
    node.getCommand().apply(this);
    java.writeStatement(callOn(CONTEXT, "popEscapingFunction"));
  }

  /**
   * A fake command injected by AutoEscaper.
   * 
   * AutoEscaper determines the html context in which an include or lvar or evar command is called
   * and stores this context in the AAutoescapeCommand node. This function loads the include or lvar
   * template in this stored context.
   */
  @Override
  public void caseAAutoescapeCommand(AAutoescapeCommand node) {
    capturePosition(node.getPosition());

    java.writeStatement(callOn(CONTEXT, "pushAutoEscapeMode", callOn(symbol("EscapeMode"),
        "computeEscapeMode", expressionTranslator.translateToString(node.getExpression()))));
    node.getCommand().apply(this);
    java.writeStatement(callOn(CONTEXT, "popAutoEscapeMode"));

  }

  /**
   * &lt;?cs linclude:'somefile.cs' &gt; command. Lazily includes another template (at render time).
   * Throw an error if file does not exist.
   */
  @Override
  public void caseAHardLincludeCommand(AHardLincludeCommand node) {
    capturePosition(node.getPosition());
    java.writeStatement(call("include", expressionTranslator
        .translateToString(node.getExpression()), JavaExpression.bool(false), CONTEXT));
  }

  /**
   * &lt;?cs linclude:'somefile.cs' &gt; command. Lazily includes another template (at render time).
   * Silently ignore if the included file does not exist.
   */
  @Override
  public void caseALincludeCommand(ALincludeCommand node) {
    capturePosition(node.getPosition());
    java.writeStatement(call("include", expressionTranslator
        .translateToString(node.getExpression()), JavaExpression.bool(true), CONTEXT));
  }

  /**
   * &lt;?cs include!'somefile.cs' &gt; command. Throw an error if file does not exist.
   */
  @Override
  public void caseAHardIncludeCommand(AHardIncludeCommand node) {
    capturePosition(node.getPosition());
    java.writeStatement(call("include", expressionTranslator
        .translateToString(node.getExpression()), JavaExpression.bool(false), CONTEXT));
  }

  /**
   * &lt;?cs include:'somefile.cs' &gt; command. Silently ignore if the included file does not
   * exist.
   */
  @Override
  public void caseAIncludeCommand(AIncludeCommand node) {
    capturePosition(node.getPosition());
    java.writeStatement(call("include", expressionTranslator
        .translateToString(node.getExpression()), JavaExpression.bool(true), CONTEXT));
  }

  /**
   * &lt;?cs lvar:blah &gt; command. Evaluate expression and execute commands within.
   */
  @Override
  public void caseALvarCommand(ALvarCommand node) {
    capturePosition(node.getPosition());
    evaluateVariable(node.getExpression(), "[lvar expression]");
  }

  /**
   * &lt;?cs evar:blah &gt; command. Evaluate expression and execute commands within.
   */
  @Override
  public void caseAEvarCommand(AEvarCommand node) {
    capturePosition(node.getPosition());
    evaluateVariable(node.getExpression(), "[evar expression]");
  }

  private void evaluateVariable(PExpression expression, String stackTraceDescription) {
    java.writeStatement(callOn(callOn(TEMPLATE_LOADER, "createTemp", string(stackTraceDescription),
        expressionTranslator.translateToString(expression), callOn(CONTEXT, "getAutoEscapeMode")),
        "render", CONTEXT));
  }

  /**
   * &lt;?cs def:someMacro(x,y) &gt; ... &lt;?cs /def &gt; command. Define a macro (available for
   * the remainder of the context).
   */
  @Override
  public void caseADefCommand(ADefCommand node) {
    capturePosition(node.getPosition());

    // This doesn't actually define the macro body yet, it just calls:
    // registerMacro("someMacroName", someReference);
    // where someReference is defined as a field later on (with the body).
    String name = makeWord(node.getMacro());
    if (macroMap.containsKey(name)) {
      // this is a duplicated definition.
      // TODO: Handle duplicates correctly.
    }
    // Keep track of the macro so we can generate the body later.
    // See MacroTransformer.
    addMacro(name, macro("macro" + macroMap.size()), node);
  }

  /**
   * This is a special tree walker that's called after the render() method has been generated to
   * create the macro definitions and their bodies.
   * 
   * It basically generates fields that look like this:
   * 
   * private final Macro macro1 = new CompiledMacro("myMacro", "arg1", "arg2"...) { public void
   * render(Data data, RenderingContext context) { // macro body. } };
   */
  private class MacroTransformer {

    public void parseDefNode(JavaExpression macroName, ADefCommand node) {
      java.startField("Macro", macroName);

      // Parameters passed to constructor. First is name of macro, the rest
      // are the name of the arguments.
      // e.g. cs def:doStuff(person, cheese)
      // -> new CompiledMacro("doStuff", "person", "cheese") { .. }.
      int i = 0;
      JavaExpression[] args = new JavaExpression[1 + node.getArguments().size()];
      args[i++] = string(makeWord(node.getMacro()));
      for (PVariable argName : node.getArguments()) {
        args[i++] = variableTranslator.translate(argName);
      }
      java.startAnonymousClass("CompiledMacro", args);

      java.startMethod(RENDER_METHOD, "context");
      java.writeStatement(declare(Type.DATA_CONTEXT, "dataContext", callOn(CONTEXT,
          "getDataContext")));
      java.writeStatement(callOn(CONTEXT, "pushExecutionContext", THIS_TEMPLATE));
      // If !context.isRuntimeAutoEscaping(), enable runtime autoescaping for macro call.
      String tempVariableName = generateTempVariable("doRuntimeAutoEscaping");
      JavaExpression value =
          JavaExpression.prefix(Type.BOOLEAN, "!", callOn(CONTEXT, "isRuntimeAutoEscaping"));
      JavaExpression stmt = declare(Type.BOOLEAN, tempVariableName, value);
      java.writeStatement(stmt);

      JavaExpression doRuntimeAutoEscaping = symbol(Type.BOOLEAN, tempVariableName);
      java.startIfBlock(doRuntimeAutoEscaping.cast(Type.BOOLEAN));
      java.writeStatement(callOn(CONTEXT, "startRuntimeAutoEscaping"));
      java.endIfBlock();

      node.getCommand().apply(TemplateTranslator.this);

      java.startIfBlock(doRuntimeAutoEscaping.cast(Type.BOOLEAN));
      java.writeStatement(callOn(CONTEXT, "stopRuntimeAutoEscaping"));
      java.endIfBlock();
      java.writeStatement(callOn(CONTEXT, "popExecutionContext"));
      java.endMethod();
      java.endAnonymousClass();
      java.endField();
    }
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
   * &lt;?cs call:someMacro(x,y) command. Call a macro.
   */
  @Override
  public void caseACallCommand(ACallCommand node) {
    capturePosition(node.getPosition());

    String name = makeWord(node.getMacro());

    java.startScopedBlock();
    java.writeComment("call:" + name);

    // Lookup macro.
    // The expression used for the macro will either be the name of the
    // static Macro object representing the macro (if we can statically
    // determine it), or will be a temporary Macro variable (named
    // 'macroCall###') that gets the result of findMacro at evaluation time.
    JavaExpression macroCalled;
    MacroInfo macroInfo = macroMap.get(name);
    if (macroInfo == null) {
      // We never saw the definition of the macro. Assume it might come in an
      // included file and look it up at render time.
      String macroCall = generateTempVariable("macroCall");
      java
          .writeStatement(declare(Type.MACRO, macroCall, callOn(CONTEXT, "findMacro", string(name))));

      macroCalled = macro(macroCall);
    } else {
      macroCalled = macroInfo.symbol;
    }

    int numArgs = node.getArguments().size();
    if (numArgs > 0) {

      // TODO: Add check that number of arguments passed in equals the
      // number expected by the macro. This should be done at translation
      // time in a future CL.

      JavaExpression[] argValues = new JavaExpression[numArgs];
      JavaExpression[] argStatus = new JavaExpression[numArgs];

      // Read the values first in case the new local variables shares the same
      // name as a variable (or variable expansion) being passed in to the macro.
      int i = 0;
      for (PExpression argNode : node.getArguments()) {
        JavaExpression value = expressionTranslator.translateUntyped(argNode);
        if (value.getType() != Type.VAR_NAME) {
          value = value.cast(Type.STRING);
        }
        String valueName = generateTempVariable("argValue");
        java.writeStatement(declare(Type.STRING, valueName, value));
        argValues[i] = JavaExpression.symbol(value.getType(), valueName);
        if (propagateEscapeStatus) {
          argStatus[i] = escapingEvaluator.computeEscaping(argNode, propagateEscapeStatus);
        } else {
          argStatus[i] = JavaExpression.symbol("EscapeMode.ESCAPE_NONE");
        }

        i++;
      }

      // Push a new local variable scope for this macro execution.
      java.writeStatement(callOn(DATA_CONTEXT, "pushVariableScope"));

      // Create the local variables for each argument.
      for (i = 0; i < argValues.length; i++) {
        JavaExpression value = argValues[i];
        JavaExpression tempVar = callOn(macroCalled, "getArgumentName", integer(i));
        String methodName;
        if (value.getType() == Type.VAR_NAME) {
          methodName = "createLocalVariableByPath";
          java.writeStatement(callOn(DATA_CONTEXT, methodName, tempVar, value));
        } else {
          // Must be String as we cast it above.
          methodName = "createLocalVariableByValue";
          java.writeStatement(callOn(DATA_CONTEXT, methodName, tempVar, value, argStatus[i]));
        }
      }
    }

    // Render macro.
    java.writeStatement(callOn(macroCalled, "render", CONTEXT));

    if (numArgs > 0) {
      // Release the variable scope used by the macro call
      java.writeStatement(callOn(DATA_CONTEXT, "popVariableScope"));
    }

    java.endScopedBlock();
  }

  /**
   * Walks the PPosition tree, which calls {@link #caseTCsOpen(TCsOpen)} below. This is simply to
   * capture the position of the node in the original template file, to help developers diagnose
   * errors.
   */
  private void capturePosition(PPosition position) {
    position.apply(this);
  }

  /**
   * Every time a &lt;cs token is found, grab the line and column and call
   * context.setCurrentPosition() so this is captured for stack traces.
   */
  @Override
  public void caseTCsOpen(TCsOpen node) {
    int line = node.getLine();
    int column = node.getPos();
    java.writeStatement(callOn(CONTEXT, "setCurrentPosition", JavaExpression.integer(line),
        JavaExpression.integer(column)));
  }

  private String generateTempVariable(String prefix) {
    return prefix + tempVariable++;
  }
}
