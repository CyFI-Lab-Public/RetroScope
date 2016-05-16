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
import com.google.clearsilver.jsilver.syntax.node.AEscapeCommand;
import com.google.clearsilver.jsilver.syntax.node.AFunctionExpression;
import com.google.clearsilver.jsilver.syntax.node.AMultipleCommand;
import com.google.clearsilver.jsilver.syntax.node.ANameVariable;
import com.google.clearsilver.jsilver.syntax.node.AStringExpression;
import com.google.clearsilver.jsilver.syntax.node.AVarCommand;
import com.google.clearsilver.jsilver.syntax.node.Node;
import com.google.clearsilver.jsilver.syntax.node.PCommand;
import com.google.clearsilver.jsilver.syntax.node.PExpression;
import com.google.clearsilver.jsilver.syntax.node.PPosition;
import com.google.clearsilver.jsilver.syntax.node.PVariable;
import com.google.clearsilver.jsilver.syntax.node.TString;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Recursively optimizes the syntax tree with a set of simple operations. This class currently
 * optimizes:
 * <ul>
 * <li>String concatenation in var commands
 * <li>Function calls to escaping functions
 * </ul>
 * <p>
 * String add expressions in var commands are optimized by replacing something like:
 * 
 * <pre>
 * &lt;cs? var:a + b ?&gt;
 * </pre>
 * with:
 * 
 * <pre>
 * &lt;cs? var:a ?&gt;&lt;cs? var:b ?&gt;
 * </pre>
 * 
 * This avoids having to construct the intermediate result {@code a + b} at runtime and reduces
 * runtime heap allocations.
 * <p>
 * Functions call to escaping functions are optimized by replacing them with the equivalent escaping
 * construct. This is faster because escapers are called with the strings themselves whereas general
 * function calls require value objects to be created.
 * <p>
 * Expressions such as:
 * 
 * <pre>
 * &lt;cs? var:html_escape(foo) ?&gt;
 * </pre>
 * are turned into:
 * 
 * <pre>
 * &lt;cs? escape:&quot;html&quot; ?&gt;
 * &lt;cs? var:foo ?&gt;
 * &lt;?cs /escape ?&gt;
 * </pre>
 * 
 * It also optimizes sequences of escaped expressions into a single escaped sequence.
 * <p>
 * It is important to note that these optimizations cannot be done in isolation if we want to
 * optimize compound expressions such as:
 * 
 * <pre>
 * &lt;cs? html_escape(foo + bar) + baz ?&gt;
 * </pre>
 * which is turned into:
 * 
 * <pre>
 * &lt;cs? escape:&quot;html&quot; ?&gt;
 * &lt;cs? var:foo ?&gt;
 * &lt;cs? var:bar ?&gt;
 * &lt;?cs /escape ?&gt;
 * &lt;?cs var:baz ?&gt;
 * </pre>
 * 
 * WARNING: This class isn't strictly just an optimization and its modification of the syntax tree
 * actually improves JSilver's behavior, bringing it more in line with ClearSilver. Consider the
 * sequence:
 * 
 * <pre>
 * &lt;cs? escape:&quot;html&quot; ?&gt;
 * &lt;cs? var:url_escape(foo) ?&gt;
 * &lt;?cs /escape ?&gt;
 * </pre>
 * 
 * In JSilver (without this optimizer being run) this would result in {@code foo} being escaped by
 * both the html escaper and the url escaping function. However ClearSilver treats top-level escaper
 * functions specially and {@code foo} is only escaped once by the url escaping function.
 * 
 * The good news is that this optimization rewrites the above example to:
 * 
 * <pre>
 * &lt;cs? escape:&quot;html&quot; ?&gt;
 * &lt;cs? escape:&quot;url&quot; ?&gt;
 * &lt;cs? var:foo ?&gt;
 * &lt;?cs /escape ?&gt;
 * &lt;?cs /escape ?&gt;
 * </pre>
 * which fixes the problem because the new url escaper replaces the existing html escaper (rather
 * than combining with it).
 * 
 * The only fly in the ointment here is the {@code url_validate} function which is treated like an
 * escaper by ClearSilver but which does not (currently) have an escaper associated with it. This
 * means that:
 * 
 * <pre>
 * &lt;cs? escape:&quot;html&quot; ?&gt;
 * &lt;cs? var:url_validate(foo) ?&gt;
 * &lt;?cs /escape ?&gt;
 * </pre>
 * will not be rewritten by this class and will result in {@code foo} being escaped twice.
 * 
 */
public class VarOptimizer extends DepthFirstAdapter {

  /**
   * A list of escaper names that are also exposed as escaping functions (eg, if the "foo" escaper
   * is also exposed as "foo_escape" function then this collection should contain the string "foo").
   */
  private final Collection<String> escaperNames;

  public VarOptimizer(Collection<String> escaperNames) {
    this.escaperNames = escaperNames;
  }

  @Override
  public void caseAMultipleCommand(AMultipleCommand multiCommand) {
    super.caseAMultipleCommand(multiCommand);
    multiCommand.replaceBy(optimizeEscapeSequences(multiCommand));
  }

  @Override
  public void caseAVarCommand(AVarCommand varCommand) {
    super.caseAVarCommand(varCommand);
    varCommand.replaceBy(optimizeVarCommands(varCommand));
  }

  /**
   * Optimizes a complex var command by recursively expanding its expression into a sequence of
   * simpler var commands. Currently two expressions are targetted for expansion: string
   * concatenation and escaping functions.
   */
  private PCommand optimizeVarCommands(AVarCommand varCommand) {
    PExpression expression = varCommand.getExpression();
    PPosition position = varCommand.getPosition();

    // This test relies on the type optimizer having replaced add commands
    // with numeric add commands.
    if (expression instanceof AAddExpression) {
      // Replace: <?cs var:a + b ?>
      // with: <?cs var:a ?><?cs var:b ?>
      AAddExpression addExpression = (AAddExpression) expression;
      AMultipleCommand multiCommand = new AMultipleCommand();
      addToContents(multiCommand, optimizedVarCommandOf(position, addExpression.getLeft()));
      addToContents(multiCommand, optimizedVarCommandOf(position, addExpression.getRight()));
      return optimizeEscapeSequences(multiCommand);
    }

    // This test relies on the sequence optimizer removing single element
    // sequence commands.
    if (expression instanceof AFunctionExpression) {
      // Replace: <?cs var:foo_escape(x) ?>
      // with: <?cs escape:"foo" ?><?cs var:x ?><?cs /escape ?>
      AFunctionExpression functionExpression = (AFunctionExpression) expression;
      String name = escapeNameOf(functionExpression);
      if (escaperNames.contains(name)) {
        LinkedList<PExpression> args = functionExpression.getArgs();
        if (args.size() == 1) {
          return new AEscapeCommand(position, quotedStringExpressionOf(name),
              optimizedVarCommandOf(position, args.getFirst()));
        }
      }
    }
    return varCommand;
  }

  /**
   * Create a var command from the given expression and recursively optimize it, returning the
   * result.
   */
  private PCommand optimizedVarCommandOf(PPosition position, PExpression expression) {
    return optimizeVarCommands(new AVarCommand(cloneOf(position), cloneOf(expression)));
  }

  /** Simple helper to clone nodes in a typesafe way */
  @SuppressWarnings("unchecked")
  private static <T extends Node> T cloneOf(T t) {
    return (T) t.clone();
  }

  /**
   * Helper to efficiently add commands to a multiple command (if the command to be added is a
   * multiple command, we add its contents). This is used to implement a tail recursion optimization
   * to flatten multiple commands.
   */
  private static void addToContents(AMultipleCommand multi, PCommand command) {
    if (command instanceof AMultipleCommand) {
      multi.getCommand().addAll(((AMultipleCommand) command).getCommand());
    } else {
      multi.getCommand().add(command);
    }
  }

  /** When used as functions, escapers have the name 'foo_escape' */
  private static final String ESCAPE_SUFFIX = "_escape";

  /**
   * Returns the name of the escaper which could replace this function (or null if this function
   * cannot be replaced).
   */
  private static String escapeNameOf(AFunctionExpression function) {
    PVariable nvar = function.getName();
    if (!(nvar instanceof ANameVariable)) {
      // We are not interested in dynamic function calls (such as "a.b(x)")
      return null;
    }
    String name = ((ANameVariable) nvar).getWord().getText();
    if (!name.endsWith(ESCAPE_SUFFIX)) {
      return null;
    }
    return name.substring(0, name.length() - ESCAPE_SUFFIX.length());
  }

  /**
   * Returns a quoted string expression of the given text.
   * <p>
   * This is used because when an escaper is called as a function we need to replace:
   * 
   * <pre>
   * &lt;cs? var:foo_escape(bar) ?&gt;
   * </pre>
   * with:
   * 
   * <pre>
   * &lt;cs? escape:&quot;foo&quot; ?&gt;&lt;cs? var:bar ?&gt;&lt;?cs /escape ?&gt;
   * </pre>
   * Using the quoted escaper name.
   */
  private static AStringExpression quotedStringExpressionOf(String text) {
    assert text.indexOf('"') == -1;
    return new AStringExpression(new TString('"' + text + '"'));
  }

  /**
   * Returns a new command containing the contents of the given multiple command but with with
   * multiple successive (matching) escape commands folded into one.
   */
  private static PCommand optimizeEscapeSequences(AMultipleCommand multiCommand) {
    AEscapeCommand lastEscapeCommand = null;
    LinkedList<PCommand> commands = new LinkedList<PCommand>();
    for (PCommand command : multiCommand.getCommand()) {
      AEscapeCommand escapeCommand = asSimpleEscapeCommand(command);
      if (isSameEscaper(escapeCommand, lastEscapeCommand)) {
        addToContents(contentsOf(lastEscapeCommand), escapeCommand.getCommand());
      } else {
        // Add the original command and set the escaper (possibly null)
        commands.add(command);
        lastEscapeCommand = escapeCommand;
      }
    }
    assert !commands.isEmpty();
    return (commands.size() > 1) ? new AMultipleCommand(commands) : commands.getFirst();
  }

  /**
   * Returns the escaped command associated with the given escape function as a multiple command. If
   * the command was already a multiple command, it is returned, otherwise a new multiple command is
   * created to wrap the original escaped command. This helper facilitates merging multiple
   * sequences of escapers.
   */
  private static AMultipleCommand contentsOf(AEscapeCommand escapeCommand) {
    PCommand escapedCommand = escapeCommand.getCommand();
    if (escapedCommand instanceof AMultipleCommand) {
      return (AMultipleCommand) escapedCommand;
    }
    AMultipleCommand multiCommand = new AMultipleCommand();
    multiCommand.getCommand().add(escapedCommand);
    escapeCommand.setCommand(multiCommand);
    return multiCommand;
  }

  /**
   * Returns the given command only if it is an escape command with a simple, string literal, name;
   * otherwise returns {@code null}.
   */
  private static AEscapeCommand asSimpleEscapeCommand(PCommand command) {
    if (!(command instanceof AEscapeCommand)) {
      return null;
    }
    AEscapeCommand escapeCommand = (AEscapeCommand) command;
    if (!(escapeCommand.getExpression() instanceof AStringExpression)) {
      return null;
    }
    return escapeCommand;
  }

  /**
   * Compares two simple escape commands and returns true if they perform the same escaping
   * function.
   */
  private static boolean isSameEscaper(AEscapeCommand newCommand, AEscapeCommand oldCommand) {
    if (newCommand == null || oldCommand == null) {
      return false;
    }
    return simpleNameOf(newCommand).equals(simpleNameOf(oldCommand));
  }

  /**
   * Returns the name of the given simple escape command (as returned by
   * {@link #asSimpleEscapeCommand(PCommand)}).
   */
  private static String simpleNameOf(AEscapeCommand escapeCommand) {
    return ((AStringExpression) escapeCommand.getExpression()).getValue().getText();
  }
}
