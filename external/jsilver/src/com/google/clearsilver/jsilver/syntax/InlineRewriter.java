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

import com.google.clearsilver.jsilver.exceptions.JSilverBadSyntaxException;
import com.google.clearsilver.jsilver.syntax.analysis.AnalysisAdapter;
import com.google.clearsilver.jsilver.syntax.analysis.DepthFirstAdapter;
import com.google.clearsilver.jsilver.syntax.node.ADataCommand;
import com.google.clearsilver.jsilver.syntax.node.AInlineCommand;
import com.google.clearsilver.jsilver.syntax.node.ANoopCommand;
import com.google.clearsilver.jsilver.syntax.node.PCommand;
import com.google.clearsilver.jsilver.syntax.node.TData;

/**
 * Rewrites the AST to replace all 'inline' commands with their associated inner
 * command sub-tree, where all whitespace data commands have been removed.
 *
 * <p>The following template:
 * <pre>
 * <?cs inline?>
 * <?cs if:x.flag?>
 *   <?cs var:">> " + x.foo + " <<"?>
 * <?cs /if?>
 * <?cs /inline?>
 * </pre>
 *
 * <p>will render as if it had been written:
 * <pre>
 * <?cs if:x.flag?><?cs var:">> " + x.foo + " <<"?><?cs /if?>
 * </pre>
 *
 * <p>The inline command is intended only to allow neater template authoring.
 * As such there is a restriction that any data commands (ie, bare literal text)
 * inside an inline command can consist only of whitespace characters. This
 * limits the risk of accidentally modifying the template's output in an
 * unexpected way when using the inline command. Literal text may still be
 * rendered in an inlined section if it is part of a var command.
 *
 * <p>Data commands containing only whitespace are effectively removed by
 * replacing them with noop commands. These can be removed (if needed) by a
 * later optimization step but shouldn't cause any issues.
 */
public class InlineRewriter extends DepthFirstAdapter {

  /**
   * Inner visitor class to recursively replace data commands with noops.
   */
  private static AnalysisAdapter WHITESPACE_STRIPPER = new DepthFirstAdapter() {
    @Override
    public void caseADataCommand(ADataCommand node) {
      TData data = node.getData();
      if (isAllWhitespace(data.getText())) {
        node.replaceBy(new ANoopCommand());
        return;
      }
      // TODO: Add more information here (line numbers etc...)
      throw new JSilverBadSyntaxException(
          "literal text in an inline block may only contain whitespace", data.getText(), null, data
              .getLine(), data.getPos(), null);
    }

    @Override
    public void caseAInlineCommand(AInlineCommand node) {
      // Once in an inline block, just remove any more we encounter.
      PCommand command = node.getCommand();
      node.replaceBy(command);
      command.apply(this);
    }
  };

  private static boolean isAllWhitespace(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (!Character.isWhitespace(s.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Removes data commands within an inline command.
   * 
   * @throws JSilverBadSyntaxException if any data commands within the inline block contain
   *         non-whitespace text.
   */
  @Override
  public void caseAInlineCommand(AInlineCommand node) {
    node.getCommand().apply(WHITESPACE_STRIPPER);
    node.replaceBy(node.getCommand());
  }
}
