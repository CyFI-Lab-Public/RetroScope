// determinize.h

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
// Functions and classes to determinize an FST.

#ifndef FST_LIB_DETERMINIZE_H__
#define FST_LIB_DETERMINIZE_H__

#include <algorithm>
#include <map>

#include <ext/hash_map>
using __gnu_cxx::hash_map;
#include <ext/slist>
using __gnu_cxx::slist;

#include "fst/lib/cache.h"
#include "fst/lib/factor-weight.h"
#include "fst/lib/map.h"
#include "fst/lib/test-properties.h"

namespace fst {

//
// COMMON DIVISORS - these are used in determinization to compute
// the transition weights. In the simplest case, it is just the same
// as the semiring Plus(). However, other choices permit more efficient
// determinization when the output contains strings.
//

// The default common divisor uses the semiring Plus.
template <class W>
class DefaultCommonDivisor {
 public:
  typedef W Weight;

  W operator()(const W &w1, const W &w2) const { return Plus(w1, w2); }
};


// The label common divisor for a (left) string semiring selects a
// single letter common prefix or the empty string. This is used in
// the determinization of output strings so that at most a single
// letter will appear in the output of a transtion.
template <typename L, StringType S>
class LabelCommonDivisor {
 public:
  typedef StringWeight<L, S> Weight;

  Weight operator()(const Weight &w1, const Weight &w2) const {
    StringWeightIterator<L, S> iter1(w1);
    StringWeightIterator<L, S> iter2(w2);

    if (!(StringWeight<L, S>::Properties() & kLeftSemiring))
      LOG(FATAL) << "LabelCommonDivisor: Weight needs to be left semiring";

    if (w1.Size() == 0 || w2.Size() == 0)
      return Weight::One();
    else if (w1 == Weight::Zero())
      return Weight(iter2.Value());
    else if (w2 == Weight::Zero())
      return Weight(iter1.Value());
    else if (iter1.Value() == iter2.Value())
      return Weight(iter1.Value());
    else
      return Weight::One();
  }
};


// The gallic common divisor uses the label common divisor on the
// string component and the template argument D common divisor on the
// weight component, which defaults to the default common divisor.
template <class L, class W, StringType S, class D = DefaultCommonDivisor<W> >
class GallicCommonDivisor {
 public:
  typedef GallicWeight<L, W, S> Weight;

  Weight operator()(const Weight &w1, const Weight &w2) const {
    return Weight(label_common_divisor_(w1.Value1(), w2.Value1()),
                  weight_common_divisor_(w1.Value2(), w2.Value2()));
  }

 private:
  LabelCommonDivisor<L, S> label_common_divisor_;
  D weight_common_divisor_;
};

// Options for finite-state transducer determinization.
struct DeterminizeFstOptions : CacheOptions {
  float delta;  // Quantization delta for subset weights

  explicit DeterminizeFstOptions(const CacheOptions &opts, float del = kDelta)
      : CacheOptions(opts), delta(del) {}

  explicit DeterminizeFstOptions(float del = kDelta) : delta(del) {}
};


// Implementation of delayed DeterminizeFst. This base class is
// common to the variants that implement acceptor and transducer
// determinization.
template <class A>
class DeterminizeFstImplBase : public CacheImpl<A> {
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

  DeterminizeFstImplBase(const Fst<A> &fst, const CacheOptions &opts)
      : CacheImpl<A>(opts), fst_(fst.Copy()) {
    SetType("determinize");
    uint64 props = fst.Properties(kFstProperties, false);
    SetProperties(DeterminizeProperties(props), kCopyProperties);

    SetInputSymbols(fst.InputSymbols());
    SetOutputSymbols(fst.OutputSymbols());
  }

  virtual ~DeterminizeFstImplBase() { delete fst_; }

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

  virtual StateId ComputeStart() = 0;

  virtual Weight ComputeFinal(StateId s) = 0;

 protected:
  const Fst<A> *fst_;            // Input Fst

  DISALLOW_EVIL_CONSTRUCTORS(DeterminizeFstImplBase);
};


// Implementation of delayed determinization for weighted acceptors.
// It is templated on the arc type A and the common divisor C.
template <class A, class C>
class DeterminizeFsaImpl : public DeterminizeFstImplBase<A> {
 public:
  using DeterminizeFstImplBase<A>::fst_;

  typedef typename A::Label Label;
  typedef typename A::Weight Weight;
  typedef typename A::StateId StateId;

  struct Element {
    Element() {}

    Element(StateId s, Weight w) : state_id(s), weight(w) {}

    StateId state_id;  // Input state Id
    Weight weight;     // Residual weight
  };
  typedef slist<Element> Subset;
  typedef map<Label, Subset*> LabelMap;

  DeterminizeFsaImpl(const Fst<A> &fst, C common_divisor,
                     const DeterminizeFstOptions &opts)
      : DeterminizeFstImplBase<A>(fst, opts),
        delta_(opts.delta), common_divisor_(common_divisor),
        subset_hash_(0, SubsetKey(), SubsetEqual(&elements_)) {
    if (!fst.Properties(kAcceptor, true))
     LOG(FATAL)  << "DeterminizeFst: argument not an acceptor";
  if (!(Weight::Properties() & kLeftSemiring))
    LOG(FATAL) << "DeterminizeFst: Weight needs to be left distributive: "
               << Weight::Type();
  }

  virtual ~DeterminizeFsaImpl() {
    for (unsigned int i = 0; i < subsets_.size(); ++i) 
      delete subsets_[i];
  }

  virtual StateId ComputeStart() {
    StateId s = fst_->Start();
    if (s == kNoStateId)
      return kNoStateId;
    Element element(s, Weight::One());
    Subset *subset = new Subset;
    subset->push_front(element);
    return FindState(subset);
  }

  virtual Weight ComputeFinal(StateId s) {
    Subset *subset = subsets_[s];
    Weight final = Weight::Zero();
    for (typename Subset::iterator siter = subset->begin();
         siter != subset->end();
         ++siter) {
      Element &element = *siter;
      final = Plus(final, Times(element.weight,
                                fst_->Final(element.state_id)));
      }
    return final;
  }

  // Finds the state corresponding to a subset. Only creates a new state
  // if the subset is not found in the subset hash. FindState takes
  // ownership of the subset argument (so that it doesn't have to copy it
  // if it creates a new state).
  //
  // The method exploits the following device: all pairs stored in the
  // associative container subset_hash_ are of the form (subset,
  // id(subset) + 1), i.e. subset_hash_[subset] > 0 if subset has been
  // stored previously. For unassigned subsets, the call to
  // subset_hash_[subset] creates a new pair (subset, 0). As a result,
  // subset_hash_[subset] == 0 iff subset is new.
  StateId FindState(Subset *subset) {
    StateId &assoc_value = subset_hash_[subset];
    if (assoc_value == 0) {  // subset wasn't present; assign it a new ID
      subsets_.push_back(subset);
      assoc_value = subsets_.size();
    } else {
      delete subset;
    }
    return assoc_value - 1;  // NB: assoc_value = ID + 1
  }

  // Computes the outgoing transitions from a state, creating new destination
  // states as needed.
  virtual void Expand(StateId s) {

    LabelMap label_map;
    LabelSubsets(s, &label_map);

    for (typename LabelMap::iterator liter = label_map.begin();
         liter != label_map.end();
         ++liter)
      AddArc(s, liter->first, liter->second);
    this->SetArcs(s);
  }

 private:
  // Constructs destination subsets per label. At return, subset
  // element weights include the input automaton label weights and the
  // subsets may contain duplicate states.
  void LabelSubsets(StateId s, LabelMap *label_map) {
    Subset *src_subset = subsets_[s];

    for (typename Subset::iterator siter = src_subset->begin();
         siter != src_subset->end();
         ++siter) {
      Element &src_element = *siter;
      for (ArcIterator< Fst<A> > aiter(*fst_, src_element.state_id);
           !aiter.Done();
           aiter.Next()) {
        const A &arc = aiter.Value();
        Element dest_element(arc.nextstate,
                             Times(src_element.weight, arc.weight));
        Subset* &dest_subset = (*label_map)[arc.ilabel];
        if (dest_subset == 0)
          dest_subset = new Subset;
        dest_subset->push_front(dest_element);
      }
    }
  }

  // Adds an arc from state S to the destination state associated
  // with subset DEST_SUBSET (as created by LabelSubsets).
  void AddArc(StateId s, Label label, Subset *dest_subset) {
    A arc;
    arc.ilabel = label;
    arc.olabel = label;
    arc.weight = Weight::Zero();

    typename Subset::iterator oiter;
    for (typename Subset::iterator diter = dest_subset->begin();
         diter != dest_subset->end();) {
      Element &dest_element = *diter;
      // Computes label weight.
      arc.weight = common_divisor_(arc.weight, dest_element.weight);

      while ((StateId)elements_.size() <= dest_element.state_id) 
        elements_.push_back(0);
      Element *matching_element = elements_[dest_element.state_id];
      if (matching_element) {
        // Found duplicate state: sums state weight and deletes dup.
        matching_element->weight = Plus(matching_element->weight,
                                        dest_element.weight);
        ++diter;
        dest_subset->erase_after(oiter);
      } else {
        // Saves element so we can check for duplicate for this state.
        elements_[dest_element.state_id] = &dest_element;
        oiter = diter;
        ++diter;
      }
    }

    // Divides out label weight from destination subset elements.
    // Quantizes to ensure comparisons are effective.
    // Clears element vector.
    for (typename Subset::iterator diter = dest_subset->begin();
         diter != dest_subset->end();
         ++diter) {
      Element &dest_element = *diter;
      dest_element.weight = Divide(dest_element.weight, arc.weight,
                                   DIVIDE_LEFT);
      dest_element.weight = dest_element.weight.Quantize(delta_);
      elements_[dest_element.state_id] = 0;
    }

    arc.nextstate = FindState(dest_subset);
    CacheImpl<A>::AddArc(s, arc);
  }

  // Comparison object for hashing Subset(s). Subsets are not sorted in this
  // implementation, so ordering must not be assumed in the equivalence
  // test.
  class SubsetEqual {
   public:
    // Constructor takes vector needed to check equality. See immediately
    // below for constraints on it.
    explicit SubsetEqual(vector<Element *> *elements)
        : elements_(elements) {}

    // At each call to operator(), elements_[state] must be defined and
    // NULL for each state in the subset arguments. When this operator
    // returns, elements_ will preserve that property. We keep it
    // full of NULLs so that it is ready for the next call.
    bool operator()(Subset* subset1, Subset* subset2) const {
        if (subset1->size() != subset2->size())
          return false;

      // Loads first subset elements in element vector.
      for (typename Subset::iterator iter1 = subset1->begin();
           iter1 != subset1->end();
           ++iter1) {
        Element &element1 = *iter1;
        (*elements_)[element1.state_id] = &element1;
      }

      // Checks second subset matches first via element vector.
      for (typename Subset::iterator iter2 = subset2->begin();
           iter2 != subset2->end();
           ++iter2) {
        Element &element2 = *iter2;
        Element *element1 = (*elements_)[element2.state_id];
        if (!element1 || element1->weight != element2.weight) {
          // Mismatch found. Resets element vector before returning false.
          for (typename Subset::iterator iter1 = subset1->begin();
               iter1 != subset1->end();
               ++iter1)
            (*elements_)[iter1->state_id] = 0;
          return false;
        } else {
          (*elements_)[element2.state_id] = 0;  // Clears entry
        }
      }
      return true;
    }
   private:
    vector<Element *> *elements_;
  };

  // Hash function for Subset to Fst states. Subset elements are not
  // sorted in this implementation, so the hash must be invariant
  // under subset reordering.
  class SubsetKey {
   public:
    size_t operator()(const Subset* subset) const {
      size_t hash = 0;
      for (typename Subset::const_iterator iter = subset->begin();
           iter != subset->end();
           ++iter) {
        const Element &element = *iter;
        int lshift = element.state_id % kPrime;
        int rshift = sizeof(size_t) - lshift;
        hash ^= element.state_id << lshift ^
                element.state_id >> rshift ^
                element.weight.Hash();
      }
      return hash;
    }

   private:
    static const int kPrime = sizeof(size_t) == 8 ? 23 : 13;
  };

  float delta_;                  // Quantization delta for subset weights
  C common_divisor_;

  // Used to test equivalence of subsets.
  vector<Element *> elements_;

  // Maps from StateId to Subset.
  vector<Subset *> subsets_;

  // Hashes from Subset to its StateId in the output automaton.
  typedef hash_map<Subset *, StateId, SubsetKey, SubsetEqual>
  SubsetHash;

  // Hashes from Label to Subsets corr. to destination states of current state.
  SubsetHash subset_hash_;

  DISALLOW_EVIL_CONSTRUCTORS(DeterminizeFsaImpl);
};


// Implementation of delayed determinization for transducers.
// Transducer determinization is implemented by mapping the input to
// the Gallic semiring as an acceptor whose weights contain the output
// strings and using acceptor determinization above to determinize
// that acceptor.
template <class A, StringType S>
class DeterminizeFstImpl : public DeterminizeFstImplBase<A> {
 public:
  typedef typename A::Label Label;
  typedef typename A::Weight Weight;
  typedef typename A::StateId StateId;

  typedef ToGallicMapper<A, S> ToMapper;
  typedef FromGallicMapper<A, S> FromMapper;

  typedef typename ToMapper::ToArc ToArc;
  typedef MapFst<A, ToArc, ToMapper> ToFst;
  typedef MapFst<ToArc, A, FromMapper> FromFst;

  typedef GallicCommonDivisor<Label, Weight, S> CommonDivisor;
  typedef GallicFactor<Label, Weight, S> FactorIterator;

  // Defined after DeterminizeFst since it calls it.
  DeterminizeFstImpl(const Fst<A> &fst, const DeterminizeFstOptions &opts);

  ~DeterminizeFstImpl() { delete from_fst_; }

  virtual StateId ComputeStart() { return from_fst_->Start(); }

  virtual Weight ComputeFinal(StateId s) { return from_fst_->Final(s); }

  virtual void Expand(StateId s) {
    for (ArcIterator<FromFst> aiter(*from_fst_, s);
         !aiter.Done();
         aiter.Next())
      CacheImpl<A>::AddArc(s, aiter.Value());
    CacheImpl<A>::SetArcs(s);
  }

 private:
  FromFst *from_fst_;

  DISALLOW_EVIL_CONSTRUCTORS(DeterminizeFstImpl);
};


// Determinizes a weighted transducer. This version is a delayed
// Fst. The result will be an equivalent FST that has the property
// that no state has two transitions with the same input label.
// For this algorithm, epsilon transitions are treated as regular
// symbols (cf. RmEpsilon).
//
// The transducer must be functional. The weights must be (weakly)
// left divisible (valid for TropicalWeight and LogWeight).
//
// Complexity:
// - Determinizable: exponential (polynomial in the size of the output)
// - Non-determinizable) does not terminate
//
// The determinizable automata include all unweighted and all acyclic input.
//
// References:
// - Mehryar Mohri, "Finite-State Transducers in Language and Speech
//   Processing". Computational Linguistics, 23:2, 1997.
template <class A>
class DeterminizeFst : public Fst<A> {
 public:
  friend class ArcIterator< DeterminizeFst<A> >;
  friend class CacheStateIterator< DeterminizeFst<A> >;
  friend class CacheArcIterator< DeterminizeFst<A> >;
  template <class B, StringType S> friend class DeterminizeFstImpl;

  typedef A Arc;
  typedef typename A::Weight Weight;
  typedef typename A::StateId StateId;
  typedef typename A::Label Label;
  typedef CacheState<A> State;

  explicit DeterminizeFst(const Fst<A> &fst,
                 const DeterminizeFstOptions &opts = DeterminizeFstOptions()) {
    if (fst.Properties(kAcceptor, true)) {
      // Calls implementation for acceptors.
      typedef DefaultCommonDivisor<Weight> D;
      impl_ = new DeterminizeFsaImpl<A, D>(fst, D(), opts);
    } else {
      // Calls implementation for transducers.
      impl_ = new DeterminizeFstImpl<A, STRING_LEFT_RESTRICT>(fst, opts);
    }
  }

  DeterminizeFst(const DeterminizeFst<A> &fst) : Fst<A>(fst), impl_(fst.impl_) {
    impl_->IncrRefCount();
  }

  virtual ~DeterminizeFst() { if (!impl_->DecrRefCount()) delete impl_; }

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

  virtual DeterminizeFst<A> *Copy() const {
    return new DeterminizeFst<A>(*this);
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

 protected:
  DeterminizeFstImplBase<A> *Impl() { return impl_; }

 private:
  // This private version is for passing the common divisor to
  // FSA determinization.
  template <class D>
  DeterminizeFst(const Fst<A> &fst, const D &common_divisor,
                 const DeterminizeFstOptions &opts)
      :  impl_(new DeterminizeFsaImpl<A, D>(fst, common_divisor, opts)) {}

  DeterminizeFstImplBase<A> *impl_;

  void operator=(const DeterminizeFst<A> &fst);  // Disallow
};


template <class A, StringType S>
DeterminizeFstImpl<A, S>::DeterminizeFstImpl(
    const Fst<A> &fst, const DeterminizeFstOptions &opts)
    : DeterminizeFstImplBase<A>(fst, opts) {

  // Mapper to an acceptor.
  ToFst to_fst(fst, ToMapper());

  // Determinize acceptor.
  // This recursive call terminates since it passes the common divisor
  // to a private constructor.
  DeterminizeFst<ToArc> det_fsa(to_fst, CommonDivisor(), opts);

  // Mapper back to transducer.
  FactorWeightOptions fopts(CacheOptions(true, 0), opts.delta, true);
  FactorWeightFst<ToArc, FactorIterator> factored_fst(det_fsa, fopts);
  from_fst_ = new FromFst(factored_fst, FromMapper());
}


// Specialization for DeterminizeFst.
template <class A>
class StateIterator< DeterminizeFst<A> >
    : public CacheStateIterator< DeterminizeFst<A> > {
 public:
  explicit StateIterator(const DeterminizeFst<A> &fst)
      : CacheStateIterator< DeterminizeFst<A> >(fst) {}
};


// Specialization for DeterminizeFst.
template <class A>
class ArcIterator< DeterminizeFst<A> >
    : public CacheArcIterator< DeterminizeFst<A> > {
 public:
  typedef typename A::StateId StateId;

  ArcIterator(const DeterminizeFst<A> &fst, StateId s)
      : CacheArcIterator< DeterminizeFst<A> >(fst, s) {
    if (!fst.impl_->HasArcs(s))
      fst.impl_->Expand(s);
  }

 private:
  DISALLOW_EVIL_CONSTRUCTORS(ArcIterator);
};


template <class A> inline
void DeterminizeFst<A>::InitStateIterator(StateIteratorData<A> *data) const
{
  data->base = new StateIterator< DeterminizeFst<A> >(*this);
}


// Useful aliases when using StdArc.
typedef DeterminizeFst<StdArc> StdDeterminizeFst;


struct DeterminizeOptions {
  float delta;                   // Quantization delta for subset weights

  explicit DeterminizeOptions(float d) : delta(d) {}
  DeterminizeOptions() :delta(kDelta) {}
};


// Determinizes a weighted transducer.  This version writes the
// determinized Fst to an output MutableFst.  The result will be an
// equivalent FSt that has the property that no state has two
// transitions with the same input label.  For this algorithm, epsilon
// transitions are treated as regular symbols (cf. RmEpsilon).
//
// The transducer must be functional. The weights must be (weakly)
// left divisible (valid for TropicalWeight and LogWeight).
//
// Complexity:
// - Determinizable: exponential (polynomial in the size of the output)
// - Non-determinizable: does not terminate
//
// The determinizable automata include all unweighted and all acyclic input.
//
// References:
// - Mehryar Mohri, "Finite-State Transducers in Language and Speech
//   Processing". Computational Linguistics, 23:2, 1997.
template <class Arc>
void Determinize(const Fst<Arc> &ifst, MutableFst<Arc> *ofst,
             const DeterminizeOptions &opts = DeterminizeOptions()) {
  DeterminizeFstOptions nopts;
  nopts.delta = opts.delta;
  nopts.gc_limit = 0;  // Cache only the last state for fastest copy.
  *ofst = DeterminizeFst<Arc>(ifst, nopts);
}


}  // namespace fst

#endif  // FST_LIB_DETERMINIZE_H__
