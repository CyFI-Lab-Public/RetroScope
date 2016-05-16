// complement.h
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
// Class to complement an Fst.

#ifndef FST_LIB_COMPLEMENT_H__
#define FST_LIB_COMPLEMENT_H__

#include <algorithm>

#include "fst/lib/fst.h"
#include "fst/lib/test-properties.h"

namespace fst {

template <class A> class ComplementFst;

// Implementation of delayed ComplementFst. The algorithm used
// completes the (deterministic) FSA and then exchanges final and
// non-final states.  Completion, i.e. ensuring that all labels can be
// read from every state, is accomplished by using RHO labels, which
// match all labels that are otherwise not found leaving a state. The
// first state in the output is reserved to be a new state that is the
// destination of all RHO labels. Each remaining output state s
// corresponds to input state s - 1. The first arc in the output at
// these states is the rho label, the remaining arcs correspond to the
// input arcs.
template<class A>
class ComplementFstImpl : public FstImpl<A> {
 public:
  using FstImpl<A>::SetType;
  using FstImpl<A>::SetProperties;
  using FstImpl<A>::Properties;
  using FstImpl<A>::SetInputSymbols;
  using FstImpl<A>::SetOutputSymbols;

  friend class StateIterator< ComplementFst<A> >;
  friend class ArcIterator< ComplementFst<A> >;

  typedef typename A::Label Label;
  typedef typename A::Weight Weight;
  typedef typename A::StateId StateId;

  explicit ComplementFstImpl(const Fst<A> &fst) : fst_(fst.Copy()) {
    SetType("complement");
    uint64 props = fst.Properties(kILabelSorted, false);
    SetProperties(ComplementProperties(props), kCopyProperties);
    SetInputSymbols(fst.InputSymbols());
    SetOutputSymbols(fst.OutputSymbols());
  }

  ~ComplementFstImpl() { delete fst_; }

  StateId Start() const {
    StateId start = fst_->Start();
    if (start != kNoStateId)
      return start + 1;
    else
      return 0;
  }

  // Exchange final and non-final states; make rho destination state final.
  Weight Final(StateId s) const {
    if (s == 0 || fst_->Final(s - 1) == Weight::Zero())
      return Weight::One();
    else
      return Weight::Zero();
  }

  size_t NumArcs(StateId s) const {
    if (s == 0)
      return 1;
    else
      return fst_->NumArcs(s - 1) + 1;
  }

  size_t NumInputEpsilons(StateId s) const {
    return s == 0 ? 0 : fst_->NumInputEpsilons(s - 1);
  }

  size_t NumOutputEpsilons(StateId s) const {
    return s == 0 ? 0 : fst_->NumOutputEpsilons(s - 1);
  }

 private:
  const Fst<A> *fst_;

  DISALLOW_EVIL_CONSTRUCTORS(ComplementFstImpl);
};


// Complements an automaton; this is a library-internal operation
// that introduces the rho label. This version is a delayed Fst.
template <class A>
class ComplementFst : public Fst<A> {
 public:
  friend class StateIterator< ComplementFst<A> >;
  friend class ArcIterator< ComplementFst<A> >;

  typedef A Arc;
  typedef typename A::Label Label;
  typedef typename A::Weight Weight;
  typedef typename A::StateId StateId;

  explicit ComplementFst(const Fst<A> &fst)
      : impl_(new ComplementFstImpl<A>(fst)) {
    uint64 props = kUnweighted | kNoEpsilons | kIDeterministic | kAcceptor;
    if (fst.Properties(props, true) != props)
      LOG(FATAL) << "ComplementFst: argument not an unweighted"
                 << " epsilon-free deterministic acceptor";
  }

  ComplementFst(const ComplementFst<A> &fst) : impl_(fst.impl_) {
    impl_->IncrRefCount();
  }

  virtual ~ComplementFst() { if (!impl_->DecrRefCount()) { delete impl_;  }}

  virtual StateId Start() const { return impl_->Start(); }

  virtual Weight Final(StateId s) const { return impl_->Final(s); }

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

  virtual ComplementFst<A> *Copy() const {
    return new ComplementFst<A>(*this);
  }

  virtual const SymbolTable* InputSymbols() const {
    return impl_->InputSymbols();
  }

  virtual const SymbolTable* OutputSymbols() const {
    return impl_->OutputSymbols();
  }

  virtual size_t NumArcs(StateId s) const { return impl_->NumArcs(s); }

  virtual size_t NumInputEpsilons(StateId s) const {
    return impl_->NumInputEpsilons(s);
  }

  virtual size_t NumOutputEpsilons(StateId s) const {
    return impl_->NumOutputEpsilons(s);
  }

  virtual inline void InitStateIterator(StateIteratorData<A> *data) const;

  virtual inline void InitArcIterator(StateId s,
                                      ArcIteratorData<A> *data) const;

 private:
  ComplementFstImpl<A> *impl_;

  void operator=(const ComplementFst<A> &fst);  // disallow
};


// Specialization for ComplementFst.
template <class A>
class StateIterator< ComplementFst<A> > : public StateIteratorBase<A> {
 public:
  typedef typename A::StateId StateId;
  typedef typename A::Label Label;

  explicit StateIterator(const ComplementFst<A> &fst)
      : siter_(*fst.impl_->fst_), s_(0) {
  }

  virtual bool Done() const { return s_ > 0 && siter_.Done(); }
  virtual StateId Value() const { return s_; }
  virtual void Next() {
    if (s_ != 0)
      siter_.Next();
    ++s_;
  }
  virtual void Reset() {
    siter_.Reset();
    s_ = 0;
  }

 private:
  StateIterator< Fst<A> > siter_;
  StateId s_;

  DISALLOW_EVIL_CONSTRUCTORS(StateIterator);
};


// Specialization for ComplementFst.
template <class A>
class ArcIterator< ComplementFst<A> > : public ArcIteratorBase<A> {
 public:
  typedef typename A::StateId StateId;
  typedef typename A::Label Label;
  typedef typename A::Weight Weight;

  ArcIterator(const ComplementFst<A> &fst, StateId s)
      : aiter_(0), s_(s), pos_(0) {
    if (s_ != 0)
      aiter_ = new ArcIterator< Fst<A> >(*fst.impl_->fst_, s - 1);
  }
  virtual ~ArcIterator() { delete aiter_; }

  virtual bool Done() const {
    if (s_ != 0)
      return pos_ > 0 && aiter_->Done();
    else
      return pos_ > 0;
  }

  // Adds the rho label to the rho destination state.
  virtual const A& Value() const {
    if (pos_ == 0) {
      arc_.ilabel = arc_.olabel = kRhoLabel;
      arc_.weight = Weight::One();
      arc_.nextstate = 0;
    } else {
      arc_ = aiter_->Value();
      ++arc_.nextstate;
    }
    return arc_;
  }
  virtual void Next() {
    if (s_ != 0 && pos_ > 0)
      aiter_->Next();
    ++pos_;
  }
  virtual void Reset() {
    if (s_ != 0)
      aiter_->Reset();
    pos_ = 0;
  }
  virtual void Seek(size_t a) {
    if (s_ != 0) {
      if (a == 0) {
        aiter_->Reset();
      } else {
        aiter_->Seek(a - 1);
      }
    }
    pos_ = a;
  }

 private:
  ArcIterator< Fst<A> > *aiter_;
  StateId s_;
  size_t pos_;
  mutable A arc_;
  DISALLOW_EVIL_CONSTRUCTORS(ArcIterator);
};


template <class A> inline void
ComplementFst<A>::InitStateIterator(StateIteratorData<A> *data) const {
  data->base = new StateIterator< ComplementFst<A> >(*this);
}

template <class A> inline void
ComplementFst<A>::InitArcIterator(StateId s, ArcIteratorData<A> *data) const {
  data->base = new ArcIterator< ComplementFst<A> >(*this, s);
}


// Useful alias when using StdArc.
typedef ComplementFst<StdArc> StdComplementFst;

}  // namespace fst

#endif  // FST_LIB_COMPLEMENT_H__
