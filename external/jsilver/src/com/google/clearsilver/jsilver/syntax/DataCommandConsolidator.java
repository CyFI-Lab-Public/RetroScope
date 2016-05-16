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
import com.google.clearsilver.jsilver.syntax.node.AAltCommand;
import com.google.clearsilver.jsilver.syntax.node.ACallCommand;
import com.google.clearsilver.jsilver.syntax.node.ADataCommand;
import com.google.clearsilver.jsilver.syntax.node.ADefCommand;
import com.google.clearsilver.jsilver.syntax.node.AEachCommand;
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
import com.google.clearsilver.jsilver.syntax.node.AUvarCommand;
import com.google.clearsilver.jsilver.syntax.node.AVarCommand;
import com.google.clearsilver.jsilver.syntax.node.AWithCommand;
import com.google.clearsilver.jsilver.syntax.node.EOF;
import com.google.clearsilver.jsilver.syntax.node.TData;

import java.util.ArrayList;
import java.util.List;

/**
 * Consolidates runs of (unescaped literal output) data commands, deferring output until another
 * output command (var, call, etc) is encountered.
 */
public class DataCommandConsolidator extends DepthFirstAdapter {
  /**
   * The current block nesting level. This is incremented whenever a conditional command is
   * encountered.
   */
  private int currentBlockNestingLevel = 0;
  /**
   * A list of the data commands we're currently considering for consolidation.
   */
  private final List<ADataCommand> datas = new ArrayList<ADataCommand>();
  /** The block nesting level of the data commands above. */
  private int datasBlockNestingLevel = -1;

  /**
   * Data consolidation barrier: consolidates all data contents into the last data command in the
   * datas list, replacing all but the last node with no-ops.
   */
  private void barrier() {
    if (datas.size() > 1) {
      // Put aside the last data command for later, then remove all the other
      // data commands, coalescing their contents into the last command.
      ADataCommand last = datas.remove(datas.size() - 1);

      StringBuilder sb = new StringBuilder();
      for (ADataCommand data : datas) {
        sb.append(data.getData().getText());
        data.replaceBy(null); // removes the node
      }

      sb.append(last.getData().getText());
      last.replaceBy(new ADataCommand(new TData(sb.toString())));
    }
    datas.clear();
    datasBlockNestingLevel = -1;
  }

  /** Block entry: just increments the current block nesting level. */
  private void blockEntry() {
    assert datasBlockNestingLevel <= currentBlockNestingLevel;
    ++currentBlockNestingLevel;
  }

  /**
   * Block exit: acts as a conditional barrier only to data contained within the block.
   */
  private void blockExit() {
    assert datasBlockNestingLevel <= currentBlockNestingLevel;
    if (datasBlockNestingLevel == currentBlockNestingLevel) {
      barrier();
    }
    --currentBlockNestingLevel;
  }

  // data commands: continue to accumulate as long as the block nesting level
  // is unchanged.

  @Override
  public void caseADataCommand(ADataCommand node) {
    assert datasBlockNestingLevel <= currentBlockNestingLevel;
    if (currentBlockNestingLevel != datasBlockNestingLevel) {
      barrier();
    }
    datas.add(node);
    datasBlockNestingLevel = currentBlockNestingLevel;
  }

  // var, lvar, evar, uvar, name: all unconditional barriers.

  @Override
  public void inAVarCommand(AVarCommand node) {
    barrier();
  }

  @Override
  public void inALvarCommand(ALvarCommand node) {
    barrier();
  }

  @Override
  public void inAUvarCommand(AUvarCommand node) {
    barrier();
  }

  @Override
  public void inAEvarCommand(AEvarCommand node) {
    barrier();
  }

  @Override
  public void inANameCommand(ANameCommand node) {
    barrier();
  }

  // loop, each: block barriers.

  @Override
  public void inALoopCommand(ALoopCommand node) {
    blockEntry();
  }

  @Override
  public void inALoopIncCommand(ALoopIncCommand node) {
    blockEntry();
  }

  @Override
  public void inALoopToCommand(ALoopToCommand node) {
    blockEntry();
  }

  @Override
  public void inAEachCommand(AEachCommand node) {
    blockEntry();
  }

  @Override
  public void inAWithCommand(AWithCommand node) {
    blockEntry();
  }

  @Override
  public void outALoopCommand(ALoopCommand node) {
    blockExit();
  }

  @Override
  public void outALoopIncCommand(ALoopIncCommand node) {
    blockExit();
  }

  @Override
  public void outALoopToCommand(ALoopToCommand node) {
    blockExit();
  }

  @Override
  public void outAEachCommand(AEachCommand node) {
    blockExit();
  }

  @Override
  public void outAWithCommand(AWithCommand node) {
    blockExit();
  }

  // def: special case: run another instance of this optimizer on the contained
  // commands. def produces no output, so it should not act as a barrier to
  // any accumulated data nodes; however, it contains data nodes, so we can
  // (and should) consolidate them.

  @Override
  public void caseADefCommand(ADefCommand node) {
    DataCommandConsolidator consolidator = new DataCommandConsolidator();
    node.getCommand().apply(consolidator);
    consolidator.barrier(); // Force final consolidation, just like EOF would.
  }

  // call: unconditional barrier.

  @Override
  public void inACallCommand(ACallCommand node) {
    barrier();
  }

  // if: special case: each branch is a block barrier.

  @Override
  public void caseAIfCommand(AIfCommand node) {
    if (node.getBlock() != null) {
      blockEntry();
      node.getBlock().apply(this);
      blockExit();
    }
    if (node.getOtherwise() != null) {
      blockEntry();
      node.getOtherwise().apply(this);
      blockExit();
    }
  }

  // alt: block barrier.

  @Override
  public void inAAltCommand(AAltCommand node) {
    blockEntry();
  }

  @Override
  public void outAAltCommand(AAltCommand node) {
    blockExit();
  }

  // include, hard include, linclude, hard linclude unconditional barriers.

  @Override
  public void caseAIncludeCommand(AIncludeCommand node) {
    barrier();
  }

  @Override
  public void caseAHardIncludeCommand(AHardIncludeCommand node) {
    barrier();
  }

  @Override
  public void caseALincludeCommand(ALincludeCommand node) {
    barrier();
  }

  @Override
  public void caseAHardLincludeCommand(AHardLincludeCommand node) {
    barrier();
  }

  // EOF: unconditional barrier.

  @Override
  public void caseEOF(EOF node) {
    barrier();
  }
}
