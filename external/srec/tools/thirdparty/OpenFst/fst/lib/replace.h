// replace.h
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
//
// \file
// Functions and classes for the recursive replacement of Fsts.
//

#ifndef FST_LIB_REPLACE_H__
#define FST_LIB_REPLACE_H__

#include <ext/hash_map>
using __gnu_cxx::hash_map;

#include "fst/lib/fst.h"
#include "fst/lib/cache.h"
#include "fst/lib/test-properties.h"

namespace fst {

// By default ReplaceFst will copy the input label of the 'replace arc'.
// For acceptors we do not want this behaviour. Instead we need to
// create an epsilon arc when recursing into the appropriate Fst.
// The epsilon_on_replace option can be used to toggle this behaviour.
struct ReplaceFstOptions : CacheOptions {
  int64 root;    // root rule for expansion
  bool  epsilon_on_replace;

  ReplaceFstOptions(const CacheOptions &opts, int64 r)
      : CacheOptions(opts), root(r), epsilon_on_replace(false) {}
  explicit ReplaceFstOptions(int64 r)
      : root(r), epsilon_on_replace(false) {}
  ReplaceFstOptions(int64 r, bool epsilon_replace_arc)
      : root(r), epsilon_on_replace(epsilon_replace_arc) {}
  ReplaceFstOptions()
      : root(kNoLabel), epsilon_on_replace(false) {}
};

//
// \class ReplaceFstImpl
// \brief Implementation class for replace class Fst
//
// The replace implementation class supports a dynamic
// expansion of a recursive transition network represented as Fst
// with dynamic replacable arcs.
//
template <class A>
class ReplaceFstImpl : public CacheImpl<A> {
 public:
  using FstImpl<A>::SetType;
  using FstImpl<A>::SetProperties;
  using FstImpl<A>::Properties;
  using FstImpl<A>::SetInputSymbols;
  using FstImpl<A>::SetOutputSymbols;
  using FstImpl<A>::InputSymbols;
  using FstImpl<A>::OutputSymbols;

  using CacheImpl<A>::HasStart;
  using CacheImpl<A>::HasArcs;
  using CacheImpl<A>::SetStart;

  typedef typename A::Label   Label;
  typedef typename A::Weight  Weight;
  typedef typename A::StateId StateId;
  typedef CacheState<A> State;
  typedef A Arc;
  typedef hash_map<Label, Label> NonTerminalHash;


  // \struct StateTuple
  // \brief Tuple of information that uniquely defines a state
  struct StateTuple {
    typedef int PrefixId;

    StateTuple() {}
    StateTuple(PrefixId p, StateId f, StateId s) :
        prefix_id(p), fst_id(f), fst_state(s) {}

    PrefixId prefix_id;  // index in prefix table
    StateId fst_id;      // current fst being walked
    StateId fst_state;   // current state in fst being walked, not to be
                         // confused with the state_id of the combined fst
  };

  // constructor for replace class implementation.
  // \param fst_tuples array of label/fst tuples, one for each non-terminal
  ReplaceFstImpl(const vector< pair<Label, const Fst<A>* > >& fst_tuples,
                 const ReplaceFstOptions &opts)
      : CacheImpl<A>(opts), opts_(opts) {
    SetType("replace");
    if (fst_tuples.size() > 0) {
      SetInputSymbols(fst_tuples[0].second->InputSymbols());
      SetOutputSymbols(fst_tuples[0].second->OutputSymbols());
    }

    fst_array_.push_back(0);
    for (size_t i = 0; i < fst_tuples.size(); ++i)
      AddFst(fst_tuples[i].first, fst_tuples[i].second);

    SetRoot(opts.root);
  }

  explicit ReplaceFstImpl(const ReplaceFstOptions &opts)
      : CacheImpl<A>(opts), opts_(opts), root_(kNoLabel) {
    fst_array_.push_back(0);
  }

  ReplaceFstImpl(const ReplaceFstImpl& impl)
      : opts_(impl.opts_), state_tuples_(impl.state_tuples_),
        state_hash_(impl.state_hash_),
        prefix_hash_(impl.prefix_hash_),
        stackprefix_array_(impl.stackprefix_array_),
        nonterminal_hash_(impl.nonterminal_hash_),
        root_(impl.root_) {
    SetType("replace");
    SetProperties(impl.Properties(), kCopyProperties);
    SetInputSymbols(InputSymbols());
    SetOutputSymbols(OutputSymbols());
    fst_array_.reserve(impl.fst_array_.size());
    fst_array_.push_back(0);
    for (size_t i = 1; i < impl.fst_array_.size(); ++i)
      fst_array_.push_back(impl.fst_array_[i]->Copy());
  }

  ~ReplaceFstImpl() {
    for (size_t i = 1; i < fst_array_.size(); ++i) {
      delete fst_array_[i];
    }
  }

  // Add to Fst array
  void AddFst(Label label, const Fst<A>* fst) {
    nonterminal_hash_[label] = fst_array_.size();
    fst_array_.push_back(fst->Copy());
    if (fst_array_.size() > 1) {
      vector<uint64> inprops(fst_array_.size());

      for (size_t i = 1; i < fst_array_.size(); ++i) {
        inprops[i] = fst_array_[i]->Properties(kCopyProperties, false);
      }
      SetProperties(ReplaceProperties(inprops));

      const SymbolTable* isymbols = fst_array_[1]->InputSymbols();
      const SymbolTable* osymbols = fst_array_[1]->OutputSymbols();
      for (size_t i = 2; i < fst_array_.size(); ++i) {
        if (!CompatSymbols(isymbols, fst_array_[i]->InputSymbols())) {
          LOG(FATAL) << "ReplaceFst::AddFst input symbols of Fst " << i-1
                     << " does not match input symbols of base Fst (0'th fst)";
        }
        if (!CompatSymbols(osymbols, fst_array_[i]->OutputSymbols())) {
          LOG(FATAL) << "ReplaceFst::AddFst output symbols of Fst " << i-1
                     << " does not match output symbols of base Fst "
                     << "(0'th fst)";
        }
      }
    }
  }

  // Computes the dependency graph of the replace class and returns
  // true if the dependencies are cyclic. Cyclic dependencies will result
  // in an un-expandable replace fst.
  bool CyclicDependencies() const {
    StdVectorFst depfst;

    // one state for each fst
    for (size_t i = 1; i < fst_array_.size(); ++i)
      depfst.AddState();

    // an arc from each state (representing the fst) to the
    // state representing the fst being replaced
    for (size_t i = 1; i < fst_array_.size(); ++i) {
      for (StateIterator<Fst<A> > siter(*(fst_array_[i]));
           !siter.Done(); siter.Next()) {
        for (ArcIterator<Fst<A> > aiter(*(fst_array_[i]), siter.Value());
             !aiter.Done(); aiter.Next()) {
          const A& arc = aiter.Value();

          typename NonTerminalHash::const_iterator it =
              nonterminal_hash_.find(arc.olabel);
          if (it != nonterminal_hash_.end()) {
            Label j = it->second - 1;
            depfst.AddArc(i - 1, A(arc.olabel, arc.olabel, Weight::One(), j));
          }
        }
      }
    }

    depfst.SetStart(root_ - 1);
    depfst.SetFinal(root_ - 1, Weight::One());
    return depfst.Properties(kCyclic, true);
  }

  // set root rule for expansion
  void SetRoot(Label root) {
    Label nonterminal = nonterminal_hash_[root];
    root_ = (nonterminal > 0) ? nonterminal : 1;
  }

  // Change Fst array
  void SetFst(Label label, const Fst<A>* fst) {
    Label nonterminal = nonterminal_hash_[label];
    delete fst_array_[nonterminal];
    fst_array_[nonterminal] = fst->Copy();
  }

  // Return or compute start state of replace fst
  StateId Start() {
    if (!HasStart()) {
      if (fst_array_.size() == 1) {      // no fsts defined for replace
        SetStart(kNoStateId);
        return kNoStateId;
      } else {
        const Fst<A>* fst = fst_array_[root_];
        StateId fst_start = fst->Start();
        if (fst_start == kNoStateId)  // root Fst is empty
          return kNoStateId;

        int prefix = PrefixId(StackPrefix());
        StateId start = FindState(StateTuple(prefix, root_, fst_start));
        SetStart(start);
        return start;
      }
    } else {
      return CacheImpl<A>::Start();
    }
  }

  // return final weight of state (kInfWeight means state is not final)
  Weight Final(StateId s) {
    if (!HasFinal(s)) {
      const StateTuple& tuple  = state_tuples_[s];
      const StackPrefix& stack = stackprefix_array_[tuple.prefix_id];
      const Fst<A>* fst = fst_array_[tuple.fst_id];
      StateId fst_state = tuple.fst_state;

      if (fst->Final(fst_state) != Weight::Zero() && stack.Depth() == 0)
        SetFinal(s, fst->Final(fst_state));
      else
        SetFinal(s, Weight::Zero());
    }
    return CacheImpl<A>::Final(s);
  }

  size_t NumArcs(StateId s) {
    if (!HasArcs(s))
      Expand(s);
    return CacheImpl<A>::NumArcs(s);
  }

  size_t NumInputEpsilons(StateId s) {
    if (!HasArcs(s))
      Expand(s);
    return CacheImpl<A>::NumInputEpsilons(s);
  }

  size_t NumOutputEpsilons(StateId s) {
    if (!HasArcs(s))
      Expand(s);
    return CacheImpl<A>::NumOutputEpsilons(s);
  }

  // return the base arc iterator, if arcs have not been computed yet,
  // extend/recurse for new arcs.
  void InitArcIterator(StateId s, ArcIteratorData<A> *data) {
    if (!HasArcs(s))
      Expand(s);
    CacheImpl<A>::InitArcIterator(s, data);
  }

  // Find/create an Fst state given a StateTuple.  Only create a new
  // state if StateTuple is not found in the state hash.
  StateId FindState(const StateTuple& tuple) {
    typename StateTupleHash::iterator it = state_hash_.find(tuple);
    if (it == state_hash_.end()) {
      StateId new_state_id = state_tuples_.size();
      state_tuples_.push_back(tuple);
      state_hash_[tuple] = new_state_id;
      return new_state_id;
    } else {
      return it->second;
    }
  }

  // extend current state (walk arcs one level deep)
  void Expand(StateId s) {
    StateTuple tuple  = state_tuples_[s];
    const Fst<A>* fst = fst_array_[tuple.fst_id];
    StateId fst_state = tuple.fst_state;
    if (fst_state == kNoStateId) {
      SetArcs(s);
      return;
    }

    // if state is final, pop up stack
    const StackPrefix& stack = stackprefix_array_[tuple.prefix_id];
    if (fst->Final(fst_state) != Weight::Zero() && stack.Depth()) {
      int prefix_id = PopPrefix(stack);
      const PrefixTuple& top = stack.Top();

      StateId nextstate =
        FindState(StateTuple(prefix_id, top.fst_id, top.nextstate));
      AddArc(s, A(0, 0, fst->Final(fst_state), nextstate));
    }

    // extend arcs leaving the state
    for (ArcIterator< Fst<A> > aiter(*fst, fst_state);
         !aiter.Done(); aiter.Next()) {
      const Arc& arc = aiter.Value();
      if (arc.olabel == 0) {  // expand local fst
        StateId nextstate =
          FindState(StateTuple(tuple.prefix_id, tuple.fst_id, arc.nextstate));
        AddArc(s, A(arc.ilabel, arc.olabel, arc.weight, nextstate));
      } else {
        // check for non terminal
        typename NonTerminalHash::const_iterator it =
            nonterminal_hash_.find(arc.olabel);
        if (it != nonterminal_hash_.end()) {  // recurse into non terminal
          Label nonterminal = it->second;
          const Fst<A>* nt_fst = fst_array_[nonterminal];
          int nt_prefix = PushPrefix(stackprefix_array_[tuple.prefix_id],
                                     tuple.fst_id, arc.nextstate);

          // if start state is valid replace, else arc is implicitly
          // deleted
          StateId nt_start = nt_fst->Start();
          if (nt_start != kNoStateId) {
            StateId nt_nextstate = FindState(
                StateTuple(nt_prefix, nonterminal, nt_start));
            Label ilabel = (opts_.epsilon_on_replace) ? 0 : arc.ilabel;
            AddArc(s, A(ilabel, 0, arc.weight, nt_nextstate));
          }
        } else {
          StateId nextstate =
            FindState(
                StateTuple(tuple.prefix_id, tuple.fst_id, arc.nextstate));
          AddArc(s, A(arc.ilabel, arc.olabel, arc.weight, nextstate));
        }
      }
    }

    SetArcs(s);
  }


  // private helper classes
 private:
  static const int kPrime0 = 7853;
  static const int kPrime1 = 7867;

  // \class StateTupleEqual
  // \brief Compare two StateTuples for equality
  class StateTupleEqual {
   public:
    bool operator()(const StateTuple& x, const StateTuple& y) const {
      return ((x.prefix_id == y.prefix_id) && (x.fst_id == y.fst_id) &&
              (x.fst_state == y.fst_state));
    }
  };

  // \class StateTupleKey
  // \brief Hash function for StateTuple to Fst states
  class StateTupleKey {
   public:
    size_t operator()(const StateTuple& x) const {
      return static_cast<size_t>(x.prefix_id +
                                 x.fst_id * kPrime0 +
                                 x.fst_state * kPrime1);
    }
  };

  typedef hash_map<StateTuple, StateId, StateTupleKey, StateTupleEqual>
  StateTupleHash;

  // \class PrefixTuple
  // \brief Tuple of fst_id and destination state (entry in stack prefix)
  struct PrefixTuple {
    PrefixTuple(Label f, StateId s) : fst_id(f), nextstate(s) {}

    Label   fst_id;
    StateId nextstate;
  };

  // \class StackPrefix
  // \brief Container for stack prefix.
  class StackPrefix {
   public:
    StackPrefix() {}

    // copy constructor
    StackPrefix(const StackPrefix& x) :
        prefix_(x.prefix_) {
    }

    void Push(int fst_id, StateId nextstate) {
      prefix_.push_back(PrefixTuple(fst_id, nextstate));
    }

    void Pop() {
      prefix_.pop_back();
    }

    const PrefixTuple& Top() const {
      return prefix_[prefix_.size()-1];
    }

    size_t Depth() const {
      return prefix_.size();
    }

   public:
    vector<PrefixTuple> prefix_;
  };


  // \class StackPrefixEqual
  // \brief Compare two stack prefix classes for equality
  class StackPrefixEqual {
   public:
    bool operator()(const StackPrefix& x, const StackPrefix& y) const {
      if (x.prefix_.size() != y.prefix_.size()) return false;
      for (size_t i = 0; i < x.prefix_.size(); ++i) {
        if (x.prefix_[i].fst_id    != y.prefix_[i].fst_id ||
           x.prefix_[i].nextstate != y.prefix_[i].nextstate) return false;
      }
      return true;
    }
  };

  //
  // \class StackPrefixKey
  // \brief Hash function for stack prefix to prefix id
  class StackPrefixKey {
   public:
    size_t operator()(const StackPrefix& x) const {
      int sum = 0;
      for (size_t i = 0; i < x.prefix_.size(); ++i) {
        sum += x.prefix_[i].fst_id + x.prefix_[i].nextstate*kPrime0;
      }
      return (size_t) sum;
    }
  };

  typedef hash_map<StackPrefix, int, StackPrefixKey, StackPrefixEqual>
  StackPrefixHash;

  // private methods
 private:
  // hash stack prefix (return unique index into stackprefix array)
  int PrefixId(const StackPrefix& prefix) {
    typename StackPrefixHash::iterator it = prefix_hash_.find(prefix);
    if (it == prefix_hash_.end()) {
      int prefix_id = stackprefix_array_.size();
      stackprefix_array_.push_back(prefix);
      prefix_hash_[prefix] = prefix_id;
      return prefix_id;
    } else {
      return it->second;
    }
  }

  // prefix id after a stack pop
  int PopPrefix(StackPrefix prefix) {
    prefix.Pop();
    return PrefixId(prefix);
  }

  // prefix id after a stack push
  int PushPrefix(StackPrefix prefix, Label fst_id, StateId nextstate) {
    prefix.Push(fst_id, nextstate);
    return PrefixId(prefix);
  }


  // private data
 private:
  // runtime options
  ReplaceFstOptions opts_;

  // maps from StateId to StateTuple
  vector<StateTuple> state_tuples_;

  // hashes from StateTuple to StateId
  StateTupleHash state_hash_;

  // cross index of unique stack prefix
  // could potentially have one copy of prefix array
  StackPrefixHash prefix_hash_;
  vector<StackPrefix> stackprefix_array_;

  NonTerminalHash nonterminal_hash_;
  vector<const Fst<A>*> fst_array_;

  Label root_;

  void operator=(const ReplaceFstImpl<A> &);  // disallow
};


//
// \class ReplaceFst
// \brief Recursivively replaces arcs in the root Fst with other Fsts.
// This version is a delayed Fst.
//
// ReplaceFst supports dynamic replacement of arcs in one Fst with
// another Fst. This replacement is recursive.  ReplaceFst can be used
// to support a variety of delayed constructions such as recursive
// transition networks, union, or closure.  It is constructed with an
// array of Fst(s). One Fst represents the root (or topology)
// machine. The root Fst refers to other Fsts by recursively replacing
// arcs labeled as non-terminals with the matching non-terminal
// Fst. Currently the ReplaceFst uses the output symbols of the arcs
// to determine whether the arc is a non-terminal arc or not. A
// non-terminal can be any label that is not a non-zero terminal label
// in the output alphabet.
//
// Note that the constructor uses a vector of pair<>. These correspond
// to the tuple of non-terminal Label and corresponding Fst. For example
// to implement the closure operation we need 2 Fsts. The first root
// Fst is a single Arc on the start State that self loops, it references
// the particular machine for which we are performing the closure operation.
//
template <class A>
class ReplaceFst : public Fst<A> {
 public:
  friend class ArcIterator< ReplaceFst<A> >;
  friend class CacheStateIterator< ReplaceFst<A> >;
  friend class CacheArcIterator< ReplaceFst<A> >;

  typedef A Arc;
  typedef typename A::Label   Label;
  typedef typename A::Weight  Weight;
  typedef typename A::StateId StateId;
  typedef CacheState<A> State;

  ReplaceFst(const vector<pair<Label, const Fst<A>* > >& fst_array,
             Label root)
      : impl_(new ReplaceFstImpl<A>(fst_array, ReplaceFstOptions(root))) {}

  ReplaceFst(const vector<pair<Label, const Fst<A>* > >& fst_array,
             const ReplaceFstOptions &opts)
      : impl_(new ReplaceFstImpl<A>(fst_array, opts)) {}

  ReplaceFst(const ReplaceFst<A>& fst) :
      impl_(new ReplaceFstImpl<A>(*(fst.impl_))) {}

  virtual ~ReplaceFst() {
    delete impl_;
  }

  virtual StateId Start() const {
    return impl_->Start();
  }

  virtual Weight Final(StateId s) const {
    return impl_->Final(s);
  }

  virtual size_t NumArcs(StateId s) const {
    return impl_->NumArcs(s);
  }

  virtual size_t NumInputEpsilons(StateId s) const {
    return impl_->NumInputEpsilons(s);
  }

  virtual size_t NumOutputEpsilons(StateId s) const {
    return impl_->NumOutputEpsilons(s);
  }

  virtual uint64 Properties(uint64 mask, bool test) const {
    if (test) {
      uint64 known, test = TestProperties(*this, mask, &known);
      impl_->SetProperties(test, known);
      return test & mask;
    } else {
      return impl_->Properties(mask);
    }
  }

  virtual const string& Type() const {
    return impl_->Type();
  }

  virtual ReplaceFst<A>* Copy() const {
    return new ReplaceFst<A>(*this);
  }

  virtual const SymbolTable* InputSymbols() const {
    return impl_->InputSymbols();
  }

  virtual const SymbolTable* OutputSymbols() const {
    return impl_->OutputSymbols();
  }

  virtual inline void InitStateIterator(StateIteratorData<A> *data) const;

  virtual void InitArcIterator(StateId s, ArcIteratorData<A> *data) const {
    impl_->InitArcIterator(s, data);
  }

  bool CyclicDependencies() const {
    return impl_->CyclicDependencies();
  }

 private:
  ReplaceFstImpl<A>* impl_;
};


// Specialization for ReplaceFst.
template<class A>
class StateIterator< ReplaceFst<A> >
    : public CacheStateIterator< ReplaceFst<A> > {
 public:
  explicit StateIterator(const ReplaceFst<A> &fst)
      : CacheStateIterator< ReplaceFst<A> >(fst) {}

 private:
  DISALLOW_EVIL_CONSTRUCTORS(StateIterator);
};

// Specialization for ReplaceFst.
template <class A>
class ArcIterator< ReplaceFst<A> >
    : public CacheArcIterator< ReplaceFst<A> > {
 public:
  typedef typename A::StateId StateId;

  ArcIterator(const ReplaceFst<A> &fst, StateId s)
      : CacheArcIterator< ReplaceFst<A> >(fst, s) {
    if (!fst.impl_->HasArcs(s))
      fst.impl_->Expand(s);
  }

 private:
  DISALLOW_EVIL_CONSTRUCTORS(ArcIterator);
};

template <class A> inline
void ReplaceFst<A>::InitStateIterator(StateIteratorData<A> *data) const {
  data->base = new StateIterator< ReplaceFst<A> >(*this);
}

typedef ReplaceFst<StdArc> StdReplaceFst;


// // Recursivively replaces arcs in the root Fst with other Fsts.
// This version writes the result of replacement to an output MutableFst.
//
// Replace supports replacement of arcs in one Fst with another
// Fst. This replacement is recursive.  Replace takes an array of
// Fst(s). One Fst represents the root (or topology) machine. The root
// Fst refers to other Fsts by recursively replacing arcs labeled as
// non-terminals with the matching non-terminal Fst. Currently Replace
// uses the output symbols of the arcs to determine whether the arc is
// a non-terminal arc or not. A non-terminal can be any label that is
// not a non-zero terminal label in the output alphabet.  Note that
// input argument is a vector of pair<>. These correspond to the tuple
// of non-terminal Label and corresponding Fst.
template<class Arc>
void Replace(const vector<pair<typename Arc::Label,
             const Fst<Arc>* > >& ifst_array,
             MutableFst<Arc> *ofst, typename Arc::Label root) {
  ReplaceFstOptions opts(root);
  opts.gc_limit = 0;  // Cache only the last state for fastest copy.
  *ofst = ReplaceFst<Arc>(ifst_array, opts);
}

}

#endif  // FST_LIB_REPLACE_H__
