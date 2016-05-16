// rational.h
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
// An Fst implementation and base interface for delayed unions,
// concatenations and closures.

#ifndef FST_LIB_RATIONAL_H__
#define FST_LIB_RATIONAL_H__

#include "fst/lib/map.h"
#include "fst/lib/mutable-fst.h"
#include "fst/lib/replace.h"
#include "fst/lib/test-properties.h"

namespace fst {

typedef CacheOptions RationalFstOptions;

// This specifies whether to add the empty string.
enum ClosureType { CLOSURE_STAR = 0,    // T* -> add the empty string
                   CLOSURE_PLUS = 1 };  // T+ -> don't add the empty string

template <class A> class RationalFst;
template <class A> void Union(RationalFst<A> *fst1, const Fst<A> &fst2);
template <class A> void Concat(RationalFst<A> *fst1, const Fst<A> &fst2);
template <class A> void Closure(RationalFst<A> *fst, ClosureType closure_type);


// Implementation class for delayed unions, concatenations and closures.
template<class A>
class RationalFstImpl : public ReplaceFstImpl<A> {
 public:
  using FstImpl<A>::SetType;
  using FstImpl<A>::SetProperties;
  using FstImpl<A>::Properties;
  using FstImpl<A>::SetInputSymbols;
  using FstImpl<A>::SetOutputSymbols;
  using ReplaceFstImpl<A>::SetRoot;

  typedef typename A::Weight Weight;
  typedef typename A::Label Label;

  explicit RationalFstImpl(const RationalFstOptions &opts)
      : ReplaceFstImpl<A>(ReplaceFstOptions(opts, kNoLabel)),
        nonterminals_(0) {
    SetType("rational");
  }

  // Implementation of UnionFst(fst1,fst2)
  void InitUnion(const Fst<A> &fst1, const Fst<A> &fst2) {
    uint64 props1 = fst1.Properties(kFstProperties, false);
    uint64 props2 = fst2.Properties(kFstProperties, false);
    SetInputSymbols(fst1.InputSymbols());
    SetOutputSymbols(fst1.OutputSymbols());
    rfst_.AddState();
    rfst_.AddState();
    rfst_.SetStart(0);
    rfst_.SetFinal(1, Weight::One());
    rfst_.SetInputSymbols(fst1.InputSymbols());
    rfst_.SetOutputSymbols(fst1.OutputSymbols());
    nonterminals_ = 2;
    rfst_.AddArc(0, A(0, -1, Weight::One(), 1));
    rfst_.AddArc(0, A(0, -2, Weight::One(), 1));
    AddFst(0, &rfst_);
    AddFst(-1, &fst1);
    AddFst(-2, &fst2);
    SetRoot(0);
    SetProperties(UnionProperties(props1, props2, true), kCopyProperties);
  }

  // Implementation of ConcatFst(fst1,fst2)
  void InitConcat(const Fst<A> &fst1, const Fst<A> &fst2) {
    uint64 props1 = fst1.Properties(kFstProperties, false);
    uint64 props2 = fst2.Properties(kFstProperties, false);
    SetInputSymbols(fst1.InputSymbols());
    SetOutputSymbols(fst1.OutputSymbols());
    rfst_.AddState();
    rfst_.AddState();
    rfst_.AddState();
    rfst_.SetStart(0);
    rfst_.SetFinal(2, Weight::One());
    rfst_.SetInputSymbols(fst1.InputSymbols());
    rfst_.SetOutputSymbols(fst1.OutputSymbols());
    nonterminals_ = 2;
    rfst_.AddArc(0, A(0, -1, Weight::One(), 1));
    rfst_.AddArc(1, A(0, -2, Weight::One(), 2));
    AddFst(0, &rfst_);
    AddFst(-1, &fst1);
    AddFst(-2, &fst2);
    SetRoot(0);
    SetProperties(ConcatProperties(props1, props2, true), kCopyProperties);
  }

  // Implementation of ClosureFst(fst, closure_type)
  void InitClosure(const Fst<A> &fst, ClosureType closure_type) {
    uint64 props = fst.Properties(kFstProperties, false);
    SetInputSymbols(fst.InputSymbols());
    SetOutputSymbols(fst.OutputSymbols());
    if (closure_type == CLOSURE_STAR) {
      rfst_.AddState();
      rfst_.SetStart(0);
      rfst_.SetFinal(0, Weight::One());
      rfst_.AddArc(0, A(0, -1, Weight::One(), 0));
    } else {
      rfst_.AddState();
      rfst_.AddState();
      rfst_.SetStart(0);
      rfst_.SetFinal(1, Weight::One());
      rfst_.AddArc(0, A(0, -1, Weight::One(), 1));
      rfst_.AddArc(1, A(0, 0, Weight::One(), 0));
    }
    rfst_.SetInputSymbols(fst.InputSymbols());
    rfst_.SetOutputSymbols(fst.OutputSymbols());
    AddFst(0, &rfst_);
    AddFst(-1, &fst);
    SetRoot(0);
    nonterminals_ = 1;
    SetProperties(ClosureProperties(props, closure_type == CLOSURE_STAR, true),
                  kCopyProperties);
  }

  // Implementation of Union(Fst &, RationalFst *)
  void AddUnion(const Fst<A> &fst) {
    uint64 props1 = Properties();
    uint64 props2 = fst.Properties(kFstProperties, false);
    VectorFst<A> afst;
    afst.AddState();
    afst.AddState();
    afst.SetStart(0);
    afst.SetFinal(1, Weight::One());
    afst.AddArc(0, A(0, -nonterminals_, Weight::One(), 1));
    Union(&rfst_, afst);
    SetFst(0, &rfst_);
    ++nonterminals_;
    SetProperties(UnionProperties(props1, props2, true), kCopyProperties);
  }

  // Implementation of Concat(Fst &, RationalFst *)
  void AddConcat(const Fst<A> &fst) {
    uint64 props1 = Properties();
    uint64 props2 = fst.Properties(kFstProperties, false);
    VectorFst<A> afst;
    afst.AddState();
    afst.AddState();
    afst.SetStart(0);
    afst.SetFinal(1, Weight::One());
    afst.AddArc(0, A(0, -nonterminals_, Weight::One(), 1));
    Concat(&rfst_, afst);
    SetFst(0, &rfst_);
    ++nonterminals_;
    SetProperties(ConcatProperties(props1, props2, true), kCopyProperties);
  }

  // Implementation of Closure(RationalFst *, closure_type)
  void AddClosure(ClosureType closure_type) {
    uint64 props = Properties();
    Closure(&rfst_, closure_type);
    SetFst(0, &rfst_);
    SetProperties(ClosureProperties(props, closure_type == CLOSURE_STAR, true),
                  kCopyProperties);
  }

 private:
  VectorFst<A> rfst_;   // rational topology machine; uses neg. nonterminals
  Label nonterminals_;  // # of nonterminals used

  DISALLOW_EVIL_CONSTRUCTORS(RationalFstImpl);
};

// Parent class for the delayed rational operations - delayed union,
// concatenation, and closure.  This class attaches interface to
// implementation and handles reference counting.
template <class A>
class RationalFst : public Fst<A> {
 public:
  friend class CacheStateIterator< RationalFst<A> >;
  friend class ArcIterator< RationalFst<A> >;
  friend class CacheArcIterator< RationalFst<A> >;
  friend void Union<>(RationalFst<A> *fst1, const Fst<A> &fst2);
  friend void Concat<>(RationalFst<A> *fst1, const Fst<A> &fst2);
  friend void Closure<>(RationalFst<A> *fst, ClosureType closure_type);

  typedef A Arc;
  typedef typename A::Weight Weight;
  typedef typename A::StateId StateId;
  typedef CacheState<A> State;

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
  RationalFst() : impl_(new RationalFstImpl<A>(RationalFstOptions())) {}
  explicit RationalFst(const RationalFstOptions &opts)
      : impl_(new RationalFstImpl<A>(opts)) {}


  RationalFst(const RationalFst<A> &fst) : impl_(fst.impl_) {
    impl_->IncrRefCount();
  }

  virtual ~RationalFst() { if (!impl_->DecrRefCount()) delete impl_; }

  RationalFstImpl<A> *Impl() { return impl_; }

 private:
  RationalFstImpl<A> *impl_;

  void operator=(const RationalFst<A> &fst);  // disallow
};

// Specialization for RationalFst.
template <class A>
class StateIterator< RationalFst<A> >
    : public CacheStateIterator< RationalFst<A> > {
 public:
  explicit StateIterator(const RationalFst<A> &fst)
      : CacheStateIterator< RationalFst<A> >(fst) {}
};

// Specialization for RationalFst.
template <class A>
class ArcIterator< RationalFst<A> >
    : public CacheArcIterator< RationalFst<A> > {
 public:
  typedef typename A::StateId StateId;

  ArcIterator(const RationalFst<A> &fst, StateId s)
      : CacheArcIterator< RationalFst<A> >(fst, s) {
    if (!fst.impl_->HasArcs(s))
      fst.impl_->Expand(s);
  }

 private:
  DISALLOW_EVIL_CONSTRUCTORS(ArcIterator);
};

template <class A> inline
void RationalFst<A>::InitStateIterator(StateIteratorData<A> *data) const {
  data->base = new StateIterator< RationalFst<A> >(*this);
}

}  // namespace fst

#endif  // FST_LIB_RATIONAL_H__
