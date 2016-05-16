// compose.h
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
// Class to compute the composition of two FSTs

#ifndef FST_LIB_COMPOSE_H__
#define FST_LIB_COMPOSE_H__

#include <algorithm>

#include <ext/hash_map>
using __gnu_cxx::hash_map;

#include "fst/lib/cache.h"
#include "fst/lib/test-properties.h"

namespace fst {

// Enumeration of uint64 bits used to represent the user-defined
// properties of FST composition (in the template parameter to
// ComposeFstOptions<T>). The bits stand for extensions of generic FST
// composition. ComposeFstOptions<> (all the bits unset) is the "plain"
// compose without any extra extensions.
enum ComposeTypes {
  // RHO: flags dealing with a special "rest" symbol in the FSTs.
  // NB: at most one of the bits COMPOSE_FST1_RHO, COMPOSE_FST2_RHO
  // may be set.
  COMPOSE_FST1_RHO    = 1ULL<<0,  // "Rest" symbol on the output side of fst1.
  COMPOSE_FST2_RHO    = 1ULL<<1,  // "Rest" symbol on the input side of fst2.
  COMPOSE_FST1_PHI    = 1ULL<<2,  // "Failure" symbol on the output
                                  // side of fst1.
  COMPOSE_FST2_PHI    = 1ULL<<3,  // "Failure" symbol on the input side
                                  // of fst2.
  COMPOSE_FST1_SIGMA  = 1ULL<<4,  // "Any" symbol on the output side of
                                  // fst1.
  COMPOSE_FST2_SIGMA  = 1ULL<<5,  // "Any" symbol on the input side of
                                  // fst2.
  // Optimization related bits.
  COMPOSE_GENERIC     = 1ULL<<32,  // Disables optimizations, applies
                                   // the generic version of the
                                   // composition algorithm. This flag
                                   // is used for internal testing
                                   // only.

  // -----------------------------------------------------------------
  // Auxiliary enum values denoting specific combinations of
  // bits. Internal use only.
  COMPOSE_RHO         = COMPOSE_FST1_RHO | COMPOSE_FST2_RHO,
  COMPOSE_PHI         = COMPOSE_FST1_PHI | COMPOSE_FST2_PHI,
  COMPOSE_SIGMA       = COMPOSE_FST1_SIGMA | COMPOSE_FST2_SIGMA,
  COMPOSE_SPECIAL_SYMBOLS = COMPOSE_RHO | COMPOSE_PHI | COMPOSE_SIGMA,

  // -----------------------------------------------------------------
  // The following bits, denoting specific optimizations, are
  // typically set *internally* by the composition algorithm.
  COMPOSE_FST1_STRING = 1ULL<<33,  // fst1 is a string
  COMPOSE_FST2_STRING = 1ULL<<34,  // fst2 is a string
  COMPOSE_FST1_DET    = 1ULL<<35,  // fst1 is deterministic
  COMPOSE_FST2_DET    = 1ULL<<36,  // fst2 is deterministic
  COMPOSE_INTERNAL_MASK    = 0xffffffff00000000ULL
};


template <uint64 T = 0ULL>
struct ComposeFstOptions : public CacheOptions {
  explicit ComposeFstOptions(const CacheOptions &opts) : CacheOptions(opts) {}
  ComposeFstOptions() { }
};


// Abstract base for the implementation of delayed ComposeFst. The
// concrete specializations are templated on the (uint64-valued)
// properties of the FSTs being composed.
template <class A>
class ComposeFstImplBase : public CacheImpl<A> {
 public:
  using FstImpl<A>::SetType;
  using FstImpl<A>::SetProperties;
  using FstImpl<A>::Properties;
  using FstImpl<A>::SetInputSymbols;
  using FstImpl<A>::SetOutputSymbols;

  using CacheBaseImpl< CacheState<A> >::HasStart;
  using CacheBaseImpl< CacheState<A> >::HasFinal;
  using CacheBaseImpl< CacheState<A> >::HasArcs;

  typedef typename A::Label Label;
  typedef typename A::Weight Weight;
  typedef typename A::StateId StateId;
  typedef CacheState<A> State;

  ComposeFstImplBase(const Fst<A> &fst1,
                     const Fst<A> &fst2,
                     const CacheOptions &opts)
      :CacheImpl<A>(opts), fst1_(fst1.Copy()), fst2_(fst2.Copy()) {
    SetType("compose");
    uint64 props1 = fst1.Properties(kFstProperties, false);
    uint64 props2 = fst2.Properties(kFstProperties, false);
    SetProperties(ComposeProperties(props1, props2), kCopyProperties);

    if (!CompatSymbols(fst2.InputSymbols(), fst1.OutputSymbols()))
      LOG(FATAL) << "ComposeFst: output symbol table of 1st argument "
                 << "does not match input symbol table of 2nd argument";

    SetInputSymbols(fst1.InputSymbols());
    SetOutputSymbols(fst2.OutputSymbols());
  }

  virtual ~ComposeFstImplBase() {
    delete fst1_;
    delete fst2_;
  }

  StateId Start() {
    if (!HasStart()) {
      StateId start = ComputeStart();
      if (start != kNoStateId) {
        this->SetStart(start);
      }
    }
    return CacheImpl<A>::Start();
  }

  Weight Final(StateId s) {
    if (!HasFinal(s)) {
      Weight final = ComputeFinal(s);
      this->SetFinal(s, final);
    }
    return CacheImpl<A>::Final(s);
  }

  virtual void Expand(StateId s) = 0;

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

  void InitArcIterator(StateId s, ArcIteratorData<A> *data) {
    if (!HasArcs(s))
      Expand(s);
    CacheImpl<A>::InitArcIterator(s, data);
  }

  // Access to flags encoding compose options/optimizations etc.  (for
  // debugging).
  virtual uint64 ComposeFlags() const = 0;

 protected:
  virtual StateId ComputeStart() = 0;
  virtual Weight ComputeFinal(StateId s) = 0;

  const Fst<A> *fst1_;            // first input Fst
  const Fst<A> *fst2_;            // second input Fst
};


// The following class encapsulates implementation-dependent details
// of state tuple lookup, i.e. a bijective mapping from triples of two
// FST states and an epsilon filter state to the corresponding state
// IDs of the fst resulting from composition. The mapping must
// implement the [] operator in the style of STL associative
// containers (map, hash_map), i.e. table[x] must return a reference
// to the value associated with x. If x is an unassigned tuple, the
// operator must automatically associate x with value 0.
//
// NB: "table[x] == 0" for unassigned tuples x is required by the
// following off-by-one device used in the implementation of
// ComposeFstImpl. The value stored in the table is equal to tuple ID
// plus one, i.e. it is always a strictly positive number. Therefore,
// table[x] is equal to 0 if and only if x is an unassigned tuple (in
// which the algorithm assigns a new ID to x, and sets table[x] -
// stored in a reference - to "new ID + 1"). This form of lookup is
// more efficient than calling "find(x)" and "insert(make_pair(x, new
// ID))" if x is an unassigned tuple.
//
// The generic implementation is a wrapper around a hash_map.
template <class A, uint64 T>
class ComposeStateTable {
 public:
  typedef typename A::StateId StateId;

  struct StateTuple {
    StateTuple() {}
    StateTuple(StateId s1, StateId s2, int f)
        : state_id1(s1), state_id2(s2), filt(f) {}
    StateId state_id1;  // state Id on fst1
    StateId state_id2;  // state Id on fst2
    int filt;           // epsilon filter state
  };

  ComposeStateTable() {
    StateTuple empty_tuple(kNoStateId, kNoStateId, 0);
  }

  // NB: if 'tuple' is not in 'table_', the pair (tuple, StateId()) is
  // inserted into 'table_' (standard STL container semantics). Since
  // StateId is a built-in type, the explicit default constructor call
  // StateId() returns 0.
  StateId &operator[](const StateTuple &tuple) {
    return table_[tuple];
  }

 private:
  // Comparison object for hashing StateTuple(s).
  class StateTupleEqual {
   public:
    bool operator()(const StateTuple& x, const StateTuple& y) const {
      return x.state_id1 == y.state_id1 &&
             x.state_id2 == y.state_id2 &&
             x.filt == y.filt;
    }
  };

  static const int kPrime0 = 7853;
  static const int kPrime1 = 7867;

  // Hash function for StateTuple to Fst states.
  class StateTupleKey {
   public:
    size_t operator()(const StateTuple& x) const {
      return static_cast<size_t>(x.state_id1 +
                                 x.state_id2 * kPrime0 +
                                 x.filt * kPrime1);
    }
  };

  // Lookup table mapping state tuples to state IDs.
  typedef hash_map<StateTuple,
                         StateId,
                         StateTupleKey,
                         StateTupleEqual> StateTable;
 // Actual table data.
  StateTable table_;

  DISALLOW_EVIL_CONSTRUCTORS(ComposeStateTable);
};


// State tuple lookup table for the composition of a string FST with a
// deterministic FST.  The class maps state tuples to their unique IDs
// (i.e. states of the ComposeFst). Main optimization: due to the
// 1-to-1 correspondence between the states of the input string FST
// and those of the resulting (string) FST, a state tuple (s1, s2) is
// simply mapped to StateId s1. Hence, we use an STL vector as a
// lookup table. Template argument Fst1IsString specifies which FST is
// a string (this determines whether or not we index the lookup table
// by the first or by the second state).
template <class A, bool Fst1IsString>
class StringDetComposeStateTable {
 public:
  typedef typename A::StateId StateId;

  struct StateTuple {
    typedef typename A::StateId StateId;
    StateTuple() {}
    StateTuple(StateId s1, StateId s2, int /* f */)
        : state_id1(s1), state_id2(s2) {}
    StateId state_id1;  // state Id on fst1
    StateId state_id2;  // state Id on fst2
    static const int filt = 0;  // 'fake' epsilon filter - only needed
                                // for API compatibility
  };

  StringDetComposeStateTable() {}

  // Subscript operator. Behaves in a way similar to its map/hash_map
  // counterpart, i.e. returns a reference to the value associated
  // with 'tuple', inserting a 0 value if 'tuple' is unassigned.
  StateId &operator[](const StateTuple &tuple) {
    StateId index = Fst1IsString ? tuple.state_id1 : tuple.state_id2;
    if (index >= (StateId)data_.size()) { 
      // NB: all values in [old_size; index] are initialized to 0.
      data_.resize(index + 1);
    }
    return data_[index];
  }

 private:
  vector<StateId> data_;

  DISALLOW_EVIL_CONSTRUCTORS(StringDetComposeStateTable);
};


// Specializations of ComposeStateTable for the string/det case.
// Both inherit from StringDetComposeStateTable.
template <class A>
class ComposeStateTable<A, COMPOSE_FST1_STRING | COMPOSE_FST2_DET>
    : public StringDetComposeStateTable<A, true> { };

template <class A>
class ComposeStateTable<A, COMPOSE_FST2_STRING | COMPOSE_FST1_DET>
    : public StringDetComposeStateTable<A, false> { };


// Parameterized implementation of FST composition for a pair of FSTs
// matching the property bit vector T. If possible,
// instantiation-specific switches in the code are based on the values
// of the bits in T, which are known at compile time, so unused code
// should be optimized away by the compiler.
template <class A, uint64 T>
class ComposeFstImpl : public ComposeFstImplBase<A> {
  typedef typename A::StateId StateId;
  typedef typename A::Label   Label;
  typedef typename A::Weight  Weight;
  using FstImpl<A>::SetType;
  using FstImpl<A>::SetProperties;

  enum FindType { FIND_INPUT  = 1,          // find input label on fst2
                  FIND_OUTPUT = 2,          // find output label on fst1
                  FIND_BOTH   = 3 };        // find choice state dependent

  typedef ComposeStateTable<A, T & COMPOSE_INTERNAL_MASK> StateTupleTable;
  typedef typename StateTupleTable::StateTuple StateTuple;

 public:
  ComposeFstImpl(const Fst<A> &fst1,
                 const Fst<A> &fst2,
                 const CacheOptions &opts)
      :ComposeFstImplBase<A>(fst1, fst2, opts) {

    bool osorted = fst1.Properties(kOLabelSorted, false);
    bool isorted = fst2.Properties(kILabelSorted, false);

    switch (T & COMPOSE_SPECIAL_SYMBOLS) {
      case COMPOSE_FST1_RHO:
      case COMPOSE_FST1_PHI:
      case COMPOSE_FST1_SIGMA:
        if (!osorted || FLAGS_fst_verify_properties)
          osorted = fst1.Properties(kOLabelSorted, true);
        if (!osorted)
          LOG(FATAL) << "ComposeFst: 1st argument not output label "
                     << "sorted (special symbols present)";
        break;
      case COMPOSE_FST2_RHO:
      case COMPOSE_FST2_PHI:
      case COMPOSE_FST2_SIGMA:
        if (!isorted || FLAGS_fst_verify_properties)
          isorted = fst2.Properties(kILabelSorted, true);
        if (!isorted)
          LOG(FATAL) << "ComposeFst: 2nd argument not input label "
                     << "sorted (special symbols present)";
        break;
      case 0:
        if (!isorted && !osorted || FLAGS_fst_verify_properties) {
          osorted = fst1.Properties(kOLabelSorted, true);
          if (!osorted)
            isorted = fst2.Properties(kILabelSorted, true);
        }
        break;
      default:
        LOG(FATAL)
          << "ComposeFst: More than one special symbol used in composition";
    }

    if (isorted && (T & COMPOSE_FST2_SIGMA)) {
      find_type_ = FIND_INPUT;
    } else if (osorted && (T & COMPOSE_FST1_SIGMA)) {
      find_type_ = FIND_OUTPUT;
    } else if (isorted && (T & COMPOSE_FST2_PHI)) {
      find_type_ = FIND_INPUT;
    } else if (osorted && (T & COMPOSE_FST1_PHI)) {
      find_type_ = FIND_OUTPUT;
    } else if (isorted && (T & COMPOSE_FST2_RHO)) {
      find_type_ = FIND_INPUT;
    } else if (osorted && (T & COMPOSE_FST1_RHO)) {
      find_type_ = FIND_OUTPUT;
    } else if (isorted && (T & COMPOSE_FST1_STRING)) {
      find_type_ = FIND_INPUT;
    } else if(osorted && (T & COMPOSE_FST2_STRING)) {
      find_type_ = FIND_OUTPUT;
    } else if (isorted && osorted) {
      find_type_ = FIND_BOTH;
    } else if (isorted) {
      find_type_ = FIND_INPUT;
    } else if (osorted) {
      find_type_ = FIND_OUTPUT;
    } else {
      LOG(FATAL) << "ComposeFst: 1st argument not output label sorted "
                 << "and 2nd argument is not input label sorted";
    }
  }

  // Finds/creates an Fst state given a StateTuple.  Only creates a new
  // state if StateTuple is not found in the state hash.
  //
  // The method exploits the following device: all pairs stored in the
  // associative container state_tuple_table_ are of the form (tuple,
  // id(tuple) + 1), i.e. state_tuple_table_[tuple] > 0 if tuple has
  // been stored previously. For unassigned tuples, the call to
  // state_tuple_table_[tuple] creates a new pair (tuple, 0). As a
  // result, state_tuple_table_[tuple] == 0 iff tuple is new.
  StateId FindState(const StateTuple& tuple) {
    StateId &assoc_value = state_tuple_table_[tuple];
    if (assoc_value == 0) {  // tuple wasn't present in lookup table:
                             // assign it a new ID.
      state_tuples_.push_back(tuple);
      assoc_value = state_tuples_.size();
    }
    return assoc_value - 1;  // NB: assoc_value = ID + 1
  }

  // Generates arc for composition state s from matched input Fst arcs.
  void AddArc(StateId s, const A &arca, const A &arcb, int f,
              bool find_input) {
    A arc;
    if (find_input) {
      arc.ilabel = arcb.ilabel;
      arc.olabel = arca.olabel;
      arc.weight = Times(arcb.weight, arca.weight);
      StateTuple tuple(arcb.nextstate, arca.nextstate, f);
      arc.nextstate = FindState(tuple);
    } else {
      arc.ilabel = arca.ilabel;
      arc.olabel = arcb.olabel;
      arc.weight = Times(arca.weight, arcb.weight);
      StateTuple tuple(arca.nextstate, arcb.nextstate, f);
      arc.nextstate = FindState(tuple);
    }
    CacheImpl<A>::AddArc(s, arc);
  }

  // Arranges it so that the first arg to OrderedExpand is the Fst
  // that will be passed to FindLabel.
  void Expand(StateId s) {
    StateTuple &tuple = state_tuples_[s];
    StateId s1 = tuple.state_id1;
    StateId s2 = tuple.state_id2;
    int f = tuple.filt;
    if (find_type_ == FIND_INPUT)
      OrderedExpand(s, ComposeFstImplBase<A>::fst2_, s2,
                    ComposeFstImplBase<A>::fst1_, s1, f, true);
    else
      OrderedExpand(s, ComposeFstImplBase<A>::fst1_, s1,
                    ComposeFstImplBase<A>::fst2_, s2, f, false);
  }

  // Access to flags encoding compose options/optimizations etc.  (for
  // debugging).
  virtual uint64 ComposeFlags() const { return T; }

 private:
  // This does that actual matching of labels in the composition. The
  // arguments are ordered so FindLabel is called with state SA of
  // FSTA for each arc leaving state SB of FSTB. The FIND_INPUT arg
  // determines whether the input or output label of arcs at SB is
  // the one to match on.
  void OrderedExpand(StateId s, const Fst<A> *fsta, StateId sa,
                     const Fst<A> *fstb, StateId sb, int f, bool find_input) {

    size_t numarcsa = fsta->NumArcs(sa);
    size_t numepsa = find_input ? fsta->NumInputEpsilons(sa) :
                     fsta->NumOutputEpsilons(sa);
    bool finala = fsta->Final(sa) != Weight::Zero();
    ArcIterator< Fst<A> > aitera(*fsta, sa);
    // First handle special epsilons and sigmas on FSTA
    for (; !aitera.Done(); aitera.Next()) {
      const A &arca = aitera.Value();
      Label match_labela = find_input ? arca.ilabel : arca.olabel;
      if (match_labela > 0) {
        break;
      }
      if ((T & COMPOSE_SIGMA) != 0 &&  match_labela == kSigmaLabel) {
        // Found a sigma? Match it against all (non-special) symbols
        // on side b.
        for (ArcIterator< Fst<A> > aiterb(*fstb, sb);
             !aiterb.Done();
             aiterb.Next()) {
          const A &arcb = aiterb.Value();
          Label labelb = find_input ? arcb.olabel : arcb.ilabel;
          if (labelb <= 0) continue;
          AddArc(s, arca, arcb, 0, find_input);
        }
      } else if (f == 0 && match_labela == 0) {
        A earcb(0, 0, Weight::One(), sb);
        AddArc(s, arca, earcb, 0, find_input);  // move forward on epsilon
      }
    }
    // Next handle non-epsilon matches, rho labels, and epsilons on FSTB
    for (ArcIterator< Fst<A> > aiterb(*fstb, sb);
         !aiterb.Done();
         aiterb.Next()) {
      const A &arcb = aiterb.Value();
      Label match_labelb = find_input ? arcb.olabel : arcb.ilabel;
      if (match_labelb) {  // Consider non-epsilon match
        if (FindLabel(&aitera, numarcsa, match_labelb, find_input)) {
          for (; !aitera.Done(); aitera.Next()) {
            const A &arca = aitera.Value();
            Label match_labela = find_input ? arca.ilabel : arca.olabel;
            if (match_labela != match_labelb)
              break;
            AddArc(s, arca, arcb, 0, find_input);  // move forward on match
          }
        } else if ((T & COMPOSE_SPECIAL_SYMBOLS) != 0) {
          // If there is no transition labelled 'match_labelb' in
          // fsta, try matching 'match_labelb' against special symbols
          // (Phi, Rho,...).
          for (aitera.Reset(); !aitera.Done(); aitera.Next()) {
            A arca = aitera.Value();
            Label labela = find_input ? arca.ilabel : arca.olabel;
            if (labela >= 0) {
              break;
            } else if (((T & COMPOSE_PHI) != 0) && (labela == kPhiLabel)) {
              // Case 1: if a failure transition exists, follow its
              // transitive closure until a) a transition labelled
              // 'match_labelb' is found, or b) the initial state of
              // fsta is reached.

              StateId sf = sa;  // Start of current failure transition.
              while (labela == kPhiLabel && sf != arca.nextstate) {
                sf = arca.nextstate;

                size_t numarcsf = fsta->NumArcs(sf);
                ArcIterator< Fst<A> > aiterf(*fsta, sf);
                if (FindLabel(&aiterf, numarcsf, match_labelb, find_input)) {
                  // Sub-case 1a: there exists a transition starting
                  // in sf and consuming symbol 'match_labelb'.
                  AddArc(s, aiterf.Value(), arcb, 0, find_input);
                  break;
                } else {
                  // No transition labelled 'match_labelb' found: try
                  // next failure transition (starting at 'sf').
                  for (aiterf.Reset(); !aiterf.Done(); aiterf.Next()) {
                    arca = aiterf.Value();
                    labela = find_input ? arca.ilabel : arca.olabel;
                    if (labela >= kPhiLabel) break;
                  }
                }
              }
              if (labela == kPhiLabel && sf == arca.nextstate) {
                // Sub-case 1b: failure transitions lead to start
                // state without finding a matching
                // transition. Therefore, we generate a loop in start
                // state of fsta.
                A loop(match_labelb, match_labelb, Weight::One(), sf);
                AddArc(s, loop, arcb, 0, find_input);
              }
            } else if (((T & COMPOSE_RHO) != 0) && (labela == kRhoLabel)) {
              // Case 2: 'match_labelb' can be matched against a
              // "rest" (rho) label in fsta.
              if (find_input) {
                arca.ilabel = match_labelb;
                if (arca.olabel == kRhoLabel)
                  arca.olabel = match_labelb;
              } else {
                arca.olabel = match_labelb;
                if (arca.ilabel == kRhoLabel)
                  arca.ilabel = match_labelb;
              }
              AddArc(s, arca, arcb, 0, find_input);  // move fwd on match
            }
          }
        }
      } else if (numepsa != numarcsa || finala) {  // Handle FSTB epsilon
        A earca(0, 0, Weight::One(), sa);
        AddArc(s, earca, arcb, numepsa > 0, find_input);  // move on epsilon
      }
    }
    this->SetArcs(s);
   }


  // Finds matches to MATCH_LABEL in arcs given by AITER
  // using FIND_INPUT to determine whether to look on input or output.
  bool FindLabel(ArcIterator< Fst<A> > *aiter, size_t numarcs,
                 Label match_label, bool find_input) {
    // binary search for match
    size_t low = 0;
    size_t high = numarcs;
    while (low < high) {
      size_t mid = (low + high) / 2;
      aiter->Seek(mid);
      Label label = find_input ?
                    aiter->Value().ilabel : aiter->Value().olabel;
      if (label > match_label) {
        high = mid;
      } else if (label < match_label) {
        low = mid + 1;
      } else {
        // find first matching label (when non-determinism)
        for (size_t i = mid; i > low; --i) {
          aiter->Seek(i - 1);
          label = find_input ? aiter->Value().ilabel : aiter->Value().olabel;
          if (label != match_label) {
            aiter->Seek(i);
            return true;
          }
        }
        return true;
      }
    }
    return false;
  }

  StateId ComputeStart() {
    StateId s1 = ComposeFstImplBase<A>::fst1_->Start();
    StateId s2 = ComposeFstImplBase<A>::fst2_->Start();
    if (s1 == kNoStateId || s2 == kNoStateId)
      return kNoStateId;
    StateTuple tuple(s1, s2, 0);
    return FindState(tuple);
  }

  Weight ComputeFinal(StateId s) {
    StateTuple &tuple = state_tuples_[s];
    Weight final = Times(ComposeFstImplBase<A>::fst1_->Final(tuple.state_id1),
                         ComposeFstImplBase<A>::fst2_->Final(tuple.state_id2));
    return final;
  }


  FindType find_type_;            // find label on which side?

  // Maps from StateId to StateTuple.
  vector<StateTuple> state_tuples_;

  // Maps from StateTuple to StateId.
  StateTupleTable state_tuple_table_;

  DISALLOW_EVIL_CONSTRUCTORS(ComposeFstImpl);
};


// Computes the composition of two transducers. This version is a
// delayed Fst. If FST1 transduces string x to y with weight a and FST2
// transduces y to z with weight b, then their composition transduces
// string x to z with weight Times(x, z).
//
// The output labels of the first transducer or the input labels of
// the second transducer must be sorted.  The weights need to form a
// commutative semiring (valid for TropicalWeight and LogWeight).
//
// Complexity:
// Assuming the first FST is unsorted and the second is sorted:
// - Time: O(v1 v2 d1 (log d2 + m2)),
// - Space: O(v1 v2)
// where vi = # of states visited, di = maximum out-degree, and mi the
// maximum multiplicity of the states visited for the ith
// FST. Constant time and space to visit an input state or arc is
// assumed and exclusive of caching.
//
// Caveats:
// - ComposeFst does not trim its output (since it is a delayed operation).
// - The efficiency of composition can be strongly affected by several factors:
//   - the choice of which tnansducer is sorted - prefer sorting the FST
//     that has the greater average out-degree.
//   - the amount of non-determinism
//   - the presence and location of epsilon transitions - avoid epsilon
//     transitions on the output side of the first transducer or
//     the input side of the second transducer or prefer placing
//     them later in a path since they delay matching and can
//     introduce non-coaccessible states and transitions.
template <class A>
class ComposeFst : public Fst<A> {
 public:
  friend class ArcIterator< ComposeFst<A> >;
  friend class CacheStateIterator< ComposeFst<A> >;
  friend class CacheArcIterator< ComposeFst<A> >;

  typedef A Arc;
  typedef typename A::Weight Weight;
  typedef typename A::StateId StateId;
  typedef CacheState<A> State;

  ComposeFst(const Fst<A> &fst1, const Fst<A> &fst2)
      : impl_(Init(fst1, fst2, ComposeFstOptions<>())) { }

  template <uint64 T>
  ComposeFst(const Fst<A> &fst1,
             const Fst<A> &fst2,
             const ComposeFstOptions<T> &opts)
      : impl_(Init(fst1, fst2, opts)) { }

  ComposeFst(const ComposeFst<A> &fst) : Fst<A>(fst), impl_(fst.impl_) {
    impl_->IncrRefCount();
  }

  virtual ~ComposeFst() { if (!impl_->DecrRefCount()) delete impl_;  }

  virtual StateId Start() const { return impl_->Start(); }

  virtual Weight Final(StateId s) const { return impl_->Final(s); }

  virtual size_t NumArcs(StateId s) const { return impl_->NumArcs(s); }

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

  virtual const string& Type() const { return impl_->Type(); }

  virtual ComposeFst<A> *Copy() const {
    return new ComposeFst<A>(*this);
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

  // Access to flags encoding compose options/optimizations etc.  (for
  // debugging).
  uint64 ComposeFlags() const { return impl_->ComposeFlags(); }

 protected:
  ComposeFstImplBase<A> *Impl() { return impl_; }

 private:
  ComposeFstImplBase<A> *impl_;

  // Auxiliary method encapsulating the creation of a ComposeFst
  // implementation that is appropriate for the properties of fst1 and
  // fst2.
  template <uint64 T>
  static ComposeFstImplBase<A> *Init(
      const Fst<A> &fst1,
      const Fst<A> &fst2,
      const ComposeFstOptions<T> &opts) {

    // Filter for sort properties (forces a property check).
    uint64 sort_props_mask = kILabelSorted | kOLabelSorted;
    // Filter for optimization-related properties (does not force a
    // property-check).
    uint64 opt_props_mask =
      kString | kIDeterministic | kODeterministic | kNoIEpsilons |
      kNoOEpsilons;

    uint64 props1 = fst1.Properties(sort_props_mask, true);
    uint64 props2 = fst2.Properties(sort_props_mask, true);

    props1 |= fst1.Properties(opt_props_mask, false);
    props2 |= fst2.Properties(opt_props_mask, false);

    if (!(Weight::Properties() & kCommutative)) {
      props1 |= fst1.Properties(kUnweighted, true);
      props2 |= fst2.Properties(kUnweighted, true);
      if (!(props1 & kUnweighted) && !(props2 & kUnweighted))
        LOG(FATAL) << "ComposeFst: Weight needs to be a commutative semiring: "
                   << Weight::Type();
    }

    // Case 1: flag COMPOSE_GENERIC disables optimizations.
    if (T & COMPOSE_GENERIC) {
      return new ComposeFstImpl<A, T>(fst1, fst2, opts);
    }

    const uint64 kStringDetOptProps =
      kIDeterministic | kILabelSorted | kNoIEpsilons;
    const uint64 kDetStringOptProps =
      kODeterministic | kOLabelSorted | kNoOEpsilons;

    // Case 2: fst1 is a string, fst2 is deterministic and epsilon-free.
    if ((props1 & kString) &&
        !(T & (COMPOSE_FST1_RHO | COMPOSE_FST1_PHI | COMPOSE_FST1_SIGMA)) &&
        ((props2 & kStringDetOptProps) == kStringDetOptProps)) {
      return new ComposeFstImpl<A, T | COMPOSE_FST1_STRING | COMPOSE_FST2_DET>(
          fst1, fst2, opts);
    }
    // Case 3: fst1 is deterministic and epsilon-free, fst2 is string.
    if ((props2 & kString) &&
        !(T & (COMPOSE_FST1_RHO | COMPOSE_FST1_PHI | COMPOSE_FST1_SIGMA)) &&
        ((props1 & kDetStringOptProps) == kDetStringOptProps)) {
      return new ComposeFstImpl<A, T | COMPOSE_FST2_STRING | COMPOSE_FST1_DET>(
          fst1, fst2, opts);
    }

    // Default case: no optimizations.
    return new ComposeFstImpl<A, T>(fst1, fst2, opts);
  }

  void operator=(const ComposeFst<A> &fst);  // disallow
};


// Specialization for ComposeFst.
template<class A>
class StateIterator< ComposeFst<A> >
    : public CacheStateIterator< ComposeFst<A> > {
 public:
  explicit StateIterator(const ComposeFst<A> &fst)
      : CacheStateIterator< ComposeFst<A> >(fst) {}
};


// Specialization for ComposeFst.
template <class A>
class ArcIterator< ComposeFst<A> >
    : public CacheArcIterator< ComposeFst<A> > {
 public:
  typedef typename A::StateId StateId;

  ArcIterator(const ComposeFst<A> &fst, StateId s)
      : CacheArcIterator< ComposeFst<A> >(fst, s) {
    if (!fst.impl_->HasArcs(s))
      fst.impl_->Expand(s);
  }

 private:
  DISALLOW_EVIL_CONSTRUCTORS(ArcIterator);
};

template <class A> inline
void ComposeFst<A>::InitStateIterator(StateIteratorData<A> *data) const {
  data->base = new StateIterator< ComposeFst<A> >(*this);
}

// Useful alias when using StdArc.
typedef ComposeFst<StdArc> StdComposeFst;


struct ComposeOptions {
  bool connect;  // Connect output

  ComposeOptions(bool c) : connect(c) {}
  ComposeOptions() : connect(true) { }
};


// Computes the composition of two transducers. This version writes
// the composed FST into a MurableFst. If FST1 transduces string x to
// y with weight a and FST2 transduces y to z with weight b, then
// their composition transduces string x to z with weight
// Times(x, z).
//
// The output labels of the first transducer or the input labels of
// the second transducer must be sorted.  The weights need to form a
// commutative semiring (valid for TropicalWeight and LogWeight).
//
// Complexity:
// Assuming the first FST is unsorted and the second is sorted:
// - Time: O(V1 V2 D1 (log D2 + M2)),
// - Space: O(V1 V2 D1 M2)
// where Vi = # of states, Di = maximum out-degree, and Mi is
// the maximum multiplicity for the ith FST.
//
// Caveats:
// - Compose trims its output.
// - The efficiency of composition can be strongly affected by several factors:
//   - the choice of which tnansducer is sorted - prefer sorting the FST
//     that has the greater average out-degree.
//   - the amount of non-determinism
//   - the presence and location of epsilon transitions - avoid epsilon
//     transitions on the output side of the first transducer or
//     the input side of the second transducer or prefer placing
//     them later in a path since they delay matching and can
//     introduce non-coaccessible states and transitions.
template<class Arc>
void Compose(const Fst<Arc> &ifst1, const Fst<Arc> &ifst2,
             MutableFst<Arc> *ofst,
             const ComposeOptions &opts = ComposeOptions()) {
  ComposeFstOptions<> nopts;
  nopts.gc_limit = 0;  // Cache only the last state for fastest copy.
  *ofst = ComposeFst<Arc>(ifst1, ifst2, nopts);
  if (opts.connect)
    Connect(ofst);
}

}  // namespace fst

#endif  // FST_LIB_COMPOSE_H__
