// Copyright 2008 Google Inc. All Rights Reserved.

package com.google.clearsilver.jsilver.syntax.node;

import java.util.LinkedList;

/**
 * Replacement for SableCC generated AMultipleCommand. Iterates much faster. Important because this
 * iteration is called a lot.
 * 
 * NOTE: Because the SableCC generated code contains methods of package visibility that need to be
 * overriden, this class needs to reside in the same package.
 * 
 * @see com.google.clearsilver.jsilver.syntax.SyntaxTreeOptimizer
 */
public class AOptimizedMultipleCommand extends PCommand {

  private final PCommand[] commands;

  public AOptimizedMultipleCommand(AMultipleCommand originalNode) {
    LinkedList<PCommand> originalChildCommands = originalNode.getCommand();
    commands = new PCommand[originalChildCommands.size()];
    originalChildCommands.toArray(commands);
    for (int i = 0; i < commands.length; i++) {
      commands[i].parent(this); // set parent.
    }
  }

  @Override
  public Object clone() {
    return this; // Immutable object. Clone not necessary.
  }

  @Override
  void removeChild(Node child) {
    throw new UnsupportedOperationException();
  }

  @Override
  void replaceChild(Node oldChild, Node newChild) {
    if (newChild == null) {
      throw new IllegalArgumentException("newChild cannot be null.");
    }
    // Replace child
    for (int i = 0; i < commands.length; i++) {
      if (commands[i] == oldChild) {
        commands[i] = (PCommand) newChild;
        newChild.parent(this);
        oldChild.parent(null);
        return;
      }
    }
    throw new RuntimeException("Not a child.");
  }

  @Override
  public void apply(Switch sw) {
    for (int i = 0; i < commands.length; i++) {
      commands[i].apply(sw);
    }
  }
}
