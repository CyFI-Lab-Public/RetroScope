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

import com.google.clearsilver.jsilver.syntax.node.Start;
import com.google.clearsilver.jsilver.syntax.node.Switch;
import com.google.clearsilver.jsilver.syntax.node.Switchable;

/**
 * Simple wrapper class to encapsulate the root node of the AST and allow additional information to
 * be associated with it.
 */
public class TemplateSyntaxTree implements Switchable {
  private final Start root;

  TemplateSyntaxTree(Start root) {
    this.root = root;
  }

  public Start getRoot() {
    return root;
  }

  @Override
  public void apply(Switch sw) {
    root.apply(sw);
  }
}
