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

import com.google.clearsilver.jsilver.autoescape.AutoEscapeContext;
import com.google.clearsilver.jsilver.autoescape.EscapeMode;
import com.google.clearsilver.jsilver.exceptions.JSilverAutoEscapingException;
import com.google.clearsilver.jsilver.syntax.analysis.DepthFirstAdapter;
import com.google.clearsilver.jsilver.syntax.node.AAltCommand;
import com.google.clearsilver.jsilver.syntax.node.AAutoescapeCommand;
import com.google.clearsilver.jsilver.syntax.node.ACallCommand;
import com.google.clearsilver.jsilver.syntax.node.AContentTypeCommand;
import com.google.clearsilver.jsilver.syntax.node.ACsOpenPosition;
import com.google.clearsilver.jsilver.syntax.node.ADataCommand;
import com.google.clearsilver.jsilver.syntax.node.ADefCommand;
import com.google.clearsilver.jsilver.syntax.node.AEscapeCommand;
import com.google.clearsilver.jsilver.syntax.node.AEvarCommand;
import com.google.clearsilver.jsilver.syntax.node.AHardIncludeCommand;
import com.google.clearsilver.jsilver.syntax.node.AHardLincludeCommand;
import com.google.clearsilver.jsilver.syntax.node.AIfCommand;
import com.google.clearsilver.jsilver.syntax.node.AIncludeCommand;
import com.google.clearsilver.jsilver.syntax.node.ALincludeCommand;
import com.google.clearsilver.jsilver.syntax.node.ALvarCommand;
import com.google.clearsilver.jsilver.syntax.node.ANameCommand;
import com.google.clearsilver.jsilver.syntax.node.AStringExpression;
import com.google.clearsilver.jsilver.syntax.node.AUvarCommand;
import com.google.clearsilver.jsilver.syntax.node.AVarCommand;
import com.google.clearsilver.jsilver.syntax.node.Node;
import com.google.clearsilver.jsilver.syntax.node.PCommand;
import com.google.clearsilver.jsilver.syntax.node.PPosition;
import com.google.clearsilver.jsilver.syntax.node.Start;
import com.google.clearsilver.jsilver.syntax.node.TCsOpen;
import com.google.clearsilver.jsilver.syntax.node.TString;
import com.google.clearsilver.jsilver.syntax.node.Token;

/**
 * Run a context parser (currently only HTML parser) over the AST, determine nodes that need
 * escaping, and apply the appropriate escaping command to those nodes. The parser is fed literal
 * data (from DataCommands), which it uses to track the context. When variables (e.g. VarCommand)
 * are encountered, we query the parser for its current context, and apply the appropriate escaping
 * command.
 */
public class AutoEscaper extends DepthFirstAdapter {

  private AutoEscapeContext autoEscapeContext;
  private boolean skipAutoEscape;
  private final EscapeMode escapeMode;
  private final String templateName;
  private boolean contentTypeCalled;

  /**
   * Create an AutoEscaper, which will apply the specified escaping mode. If templateName is
   * non-null, it will be used while displaying error messages.
   * 
   * @param mode
   * @param templateName
   */
  public AutoEscaper(EscapeMode mode, String templateName) {
    this.templateName = templateName;
    if (mode.equals(EscapeMode.ESCAPE_NONE)) {
      throw new JSilverAutoEscapingException("AutoEscaper called when no escaping is required",
          templateName);
    }
    escapeMode = mode;
    if (mode.isAutoEscapingMode()) {
      autoEscapeContext = new AutoEscapeContext(mode, templateName);
      skipAutoEscape = false;
    } else {
      autoEscapeContext = null;
    }
  }

  /**
   * Create an AutoEscaper, which will apply the specified escaping mode. When possible, use
   * #AutoEscaper(EscapeMode, String) instead. It specifies the template being auto escaped, which
   * is useful when displaying error messages.
   * 
   * @param mode
   */
  public AutoEscaper(EscapeMode mode) {
    this(mode, null);
  }

  @Override
  public void caseStart(Start start) {
    if (!escapeMode.isAutoEscapingMode()) {
      // For an explicit EscapeMode like {@code EscapeMode.ESCAPE_HTML}, we
      // do not need to parse the rest of the tree. Instead, we just wrap the
      // entire tree in a <?cs escape ?> node.
      handleExplicitEscapeMode(start);
    } else {
      AutoEscapeContext.AutoEscapeState startState = autoEscapeContext.getCurrentState();
      // call super.caseStart, which will make us visit the rest of the tree,
      // so we can determine the appropriate escaping to apply for each
      // variable.
      super.caseStart(start);
      AutoEscapeContext.AutoEscapeState endState = autoEscapeContext.getCurrentState();
      if (!autoEscapeContext.isPermittedStateChangeForIncludes(startState, endState)) {
        // If template contains a content-type command, the escaping context
        // was intentionally changed. Such a change in context is fine as long
        // as the current template is not included inside another. There is no
        // way to verify that the template is not an include template however,
        // so ignore the error and depend on developers doing the right thing.
        if (contentTypeCalled) {
          return;
        }
        // We do not permit templates to end in a different context than they start in.
        // This is so that an included template does not modify the context of
        // the template that includes it.
        throw new JSilverAutoEscapingException("Template starts in context " + startState
            + " but ends in different context " + endState, templateName);
      }
    }
  }

  private void handleExplicitEscapeMode(Start start) {
    AStringExpression escapeExpr =
        new AStringExpression(new TString("\"" + escapeMode.getEscapeCommand() + "\""));

    PCommand node = start.getPCommand();
    AEscapeCommand escape =
        new AEscapeCommand(new ACsOpenPosition(new TCsOpen("<?cs ", 0, 0)), escapeExpr,
            (PCommand) node.clone());

    node.replaceBy(escape);
  }

  @Override
  public void caseADataCommand(ADataCommand node) {
    String data = node.getData().getText();
    autoEscapeContext.setCurrentPosition(node.getData().getLine(), node.getData().getPos());
    autoEscapeContext.parseData(data);
  }

  @Override
  public void caseADefCommand(ADefCommand node) {
  // Ignore the entire defcommand subtree, don't even parse it.
  }

  @Override
  public void caseAIfCommand(AIfCommand node) {
    setCurrentPosition(node.getPosition());

    /*
     * Since AutoEscaper is being applied while building the AST, and not during rendering, the html
     * context of variables is sometimes ambiguous. For instance: <?cs if: X ?><script><?cs /if ?>
     * <?cs var: MyVar ?>
     * 
     * Here MyVar may require js escaping or html escaping depending on whether the "if" condition
     * is true or false.
     * 
     * To avoid such ambiguity, we require all branches of a conditional statement to end in the
     * same context. So, <?cs if: X ?><script>X <?cs else ?><script>Y<?cs /if ?> is fine but,
     * 
     * <?cs if: X ?><script>X <?cs elif: Y ?><script>Y<?cs /if ?> is not.
     */
    AutoEscapeContext originalEscapedContext = autoEscapeContext.cloneCurrentEscapeContext();
    // Save position of the start of if statement.
    int line = autoEscapeContext.getLineNumber();
    int column = autoEscapeContext.getColumnNumber();

    if (node.getBlock() != null) {
      node.getBlock().apply(this);
    }
    AutoEscapeContext.AutoEscapeState ifEndState = autoEscapeContext.getCurrentState();
    // restore original context before executing else block
    autoEscapeContext = originalEscapedContext;

    // Interestingly, getOtherwise() is not null even when the if command
    // has no else branch. In such cases, getOtherwise() contains a
    // Noop command.
    // In practice this does not matter for the checks being run here.
    if (node.getOtherwise() != null) {
      node.getOtherwise().apply(this);
    }
    AutoEscapeContext.AutoEscapeState elseEndState = autoEscapeContext.getCurrentState();

    if (!ifEndState.equals(elseEndState)) {
      throw new JSilverAutoEscapingException("'if/else' branches have different ending contexts "
          + ifEndState + " and " + elseEndState, templateName, line, column);
    }
  }

  @Override
  public void caseAEscapeCommand(AEscapeCommand node) {
    boolean saved_skip = skipAutoEscape;
    skipAutoEscape = true;
    node.getCommand().apply(this);
    skipAutoEscape = saved_skip;
  }

  @Override
  public void caseACallCommand(ACallCommand node) {
    saveAutoEscapingContext(node, node.getPosition());
  }

  @Override
  public void caseALvarCommand(ALvarCommand node) {
    saveAutoEscapingContext(node, node.getPosition());
  }

  @Override
  public void caseAEvarCommand(AEvarCommand node) {
    saveAutoEscapingContext(node, node.getPosition());
  }

  @Override
  public void caseALincludeCommand(ALincludeCommand node) {
    saveAutoEscapingContext(node, node.getPosition());
  }

  @Override
  public void caseAIncludeCommand(AIncludeCommand node) {
    saveAutoEscapingContext(node, node.getPosition());
  }

  @Override
  public void caseAHardLincludeCommand(AHardLincludeCommand node) {
    saveAutoEscapingContext(node, node.getPosition());
  }

  @Override
  public void caseAHardIncludeCommand(AHardIncludeCommand node) {
    saveAutoEscapingContext(node, node.getPosition());
  }

  @Override
  public void caseAVarCommand(AVarCommand node) {
    applyAutoEscaping(node, node.getPosition());
  }

  @Override
  public void caseAAltCommand(AAltCommand node) {
    applyAutoEscaping(node, node.getPosition());
  }

  @Override
  public void caseANameCommand(ANameCommand node) {
    applyAutoEscaping(node, node.getPosition());
  }

  @Override
  public void caseAUvarCommand(AUvarCommand node) {
    // Let parser know that was some text that it has not seen
    setCurrentPosition(node.getPosition());
    autoEscapeContext.insertText();
  }

  /**
   * Handles a &lt;?cs content-type: "content type" ?&gt; command.
   * 
   * This command is used when the auto escaping context of a template cannot be determined from its
   * contents - for example, a CSS stylesheet or a javascript source file. Note that &lt;?cs
   * content-type: ?&gt; command is not required for all javascript and css templates. If the
   * template contains a &lt;script&gt; or &lt;style&gt; tag (or is included from another template
   * within the right tag), auto escaping will recognize the tag and switch context accordingly. On
   * the other hand, if the template serves a resource that is loaded via a &lt;script src= &gt; or
   * &lt;link rel &gt; command, the explicit &lt;?cs content-type: ?&gt; command would be required.
   */
  @Override
  public void caseAContentTypeCommand(AContentTypeCommand node) {
    setCurrentPosition(node.getPosition());
    String contentType = node.getString().getText();
    // Strip out quotes around the string
    contentType = contentType.substring(1, contentType.length() - 1);
    autoEscapeContext.setContentType(contentType);
    contentTypeCalled = true;
  }

  private void applyAutoEscaping(PCommand node, PPosition position) {
    setCurrentPosition(position);
    if (skipAutoEscape) {
      return;
    }

    AStringExpression escapeExpr = new AStringExpression(new TString("\"" + getEscaping() + "\""));
    AEscapeCommand escape = new AEscapeCommand(position, escapeExpr, (PCommand) node.clone());

    node.replaceBy(escape);
    // Now that we have determined the correct escaping for this variable,
    // let parser know that there was some text that it has not seen. The
    // parser may choose to update its state based on this.
    autoEscapeContext.insertText();

  }

  private void setCurrentPosition(PPosition position) {
    // Will eventually call caseACsOpenPosition
    position.apply(this);
  }

  @Override
  public void caseACsOpenPosition(ACsOpenPosition node) {
    Token token = node.getCsOpen();
    autoEscapeContext.setCurrentPosition(token.getLine(), token.getPos());
  }

  private void saveAutoEscapingContext(Node node, PPosition position) {
    setCurrentPosition(position);
    if (skipAutoEscape) {
      return;
    }
    EscapeMode mode = autoEscapeContext.getEscapeModeForCurrentState();
    AStringExpression escapeStrategy =
        new AStringExpression(new TString("\"" + mode.getEscapeCommand() + "\""));
    AAutoescapeCommand command =
        new AAutoescapeCommand(position, escapeStrategy, (PCommand) node.clone());
    node.replaceBy(command);
    autoEscapeContext.insertText();
  }

  private String getEscaping() {
    return autoEscapeContext.getEscapingFunctionForCurrentState();
  }
}
