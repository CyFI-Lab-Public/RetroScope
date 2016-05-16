//===- FGNode.h -----------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_FGNODE_H
#define MCLD_FGNODE_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

#include <llvm/Support/DataTypes.h>

#include <vector>

namespace mcld
{

class Relocation;
class ResolveInfo;
class Fragment;

/** \class FGNode
 *  \brief FGNode is a node for FragmentGraph
 */
class FGNode
{
public:
  typedef ResolveInfo* Slot;
  typedef Relocation*  Signal;

  typedef std::vector<Fragment*> FragmentListType;
  typedef FragmentListType::iterator frag_iterator;
  typedef FragmentListType::const_iterator const_frag_iterator;

  typedef std::vector<Slot> SlotListType;
  typedef SlotListType::iterator slot_iterator;
  typedef SlotListType::const_iterator const_slot_iterator;

  typedef std::vector<Signal> SignalListType;
  typedef SignalListType::iterator signal_iterator;
  typedef SignalListType::const_iterator const_signal_iterator;

public:
  FGNode();
  explicit FGNode(uint32_t pIndex);

  void addFragment(Fragment* pFrag);
  void addSignal(Signal pSignal);
  void addSlot(Slot pSlot);

  /// ----- observers ----- ///
  uint32_t getIndex() const
  {  return m_Index; }

  slot_iterator         slot_begin   ()       { return m_Slots.begin();     }
  const_slot_iterator   slot_begin   () const { return m_Slots.begin();     }
  slot_iterator         slot_end     ()       { return m_Slots.end();       }
  const_slot_iterator   slot_end     () const { return m_Slots.end();       }

  signal_iterator       signal_begin ()       { return m_Signals.begin();   }
  const_signal_iterator signal_begin () const { return m_Signals.begin();   }
  signal_iterator       signal_end   ()       { return m_Signals.end();     }
  const_signal_iterator signal_end   () const { return m_Signals.end();     }

  frag_iterator         frag_begin   ()       { return m_Fragments.begin(); }
  const_frag_iterator   frag_begin   () const { return m_Fragments.begin(); }
  frag_iterator         frag_end     ()       { return m_Fragments.end();   }
  const_frag_iterator   frag_end     () const { return m_Fragments.end();   }

private:
  FragmentListType m_Fragments;

  /// m_Signals - a list of relocations describes the possible fan-out of this
  /// node
  SignalListType m_Signals;

  /// m_Slots - a list of symbols describes the possible fan-in of this node
  SlotListType m_Slots;

  /// m_Index - the index in the reachability matrix
  uint32_t m_Index;
};

} // namespace of mcld

#endif

