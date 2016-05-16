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
import com.google.clearsilver.jsilver.syntax.node.ASequenceExpression;
import com.google.clearsilver.jsilver.syntax.node.PExpression;

import java.util.LinkedList;

/**
 * Simple optimizer to simplify expression sequences which only have a single element. This
 * optimization should be run as early as possible because it simplifies the syntax tree (and some
 * later optimizations may rely on the simplified structure).
 */
public class SequenceOptimizer extends DepthFirstAdapter {

  /**
   * Removes sequence expressions with only one element.
   */
  @Override
  public void caseASequenceExpression(ASequenceExpression originalNode) {
    super.caseASequenceExpression(originalNode);
    LinkedList<PExpression> args = originalNode.getArgs();
    if (args.size() == 1) {
      originalNode.replaceBy(args.getFirst());
    }
  }
}
