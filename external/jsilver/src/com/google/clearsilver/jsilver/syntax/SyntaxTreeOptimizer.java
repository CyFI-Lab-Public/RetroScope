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
import com.google.clearsilver.jsilver.syntax.node.AMultipleCommand;
import com.google.clearsilver.jsilver.syntax.node.AOptimizedMultipleCommand;

/**
 * Visitor that can be applied to the AST to optimize it by replacing nodes with more efficient
 * implementations than the default SableCC generated versions.
 */
public class SyntaxTreeOptimizer extends DepthFirstAdapter {

  /**
   * Replace AMultipleCommand nodes with AOptimizedMultipleCommands, which iterates over children
   * faster.
   */
  @Override
  public void caseAMultipleCommand(AMultipleCommand originalNode) {
    // Recurse through child nodes first. Because the optimised node doesn't
    // handle replacement, go leaves-first.
    super.caseAMultipleCommand(originalNode);
    // Replace this node with the optimized version.
    originalNode.replaceBy(new AOptimizedMultipleCommand(originalNode));
  }

}
