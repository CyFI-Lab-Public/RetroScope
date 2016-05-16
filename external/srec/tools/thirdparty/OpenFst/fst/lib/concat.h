// concat.h
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
// Functions and classes to compute the concat of two FSTs.

#ifndef FST_LIB_CONCAT_H__
#define FST_LIB_CONCAT_H__

#include <algorithm>

#include "fst/lib/mutable-fst.h"
#include "fst/lib/rational.h"

namespace fst {

// Computes the concatenation (product) of two FSTs; this version
// modifies its MutableFst argument.  If FST1 transduces string x to y
// with weight a and FST2 transduces string w to v with weight b, then
// their concatenation transduces string xw to yv with Times(a, b).
//
// Complexity:
// - Time: O(V1 + V2 + E2)
// - Space: O(V1 + V2 + E2)
// where Vi = # of states and Ei = # of arcs of the ith FST.
template<class Arc>
void Concat(MutableFst<Arc> *fst1, const Fst<Arc> &fst2) {
  typedef typename Arc::StateId StateId;
  typedef typename Arc::Label Label;
  typedef typename Arc::Weight Weight;

  StateId start1 = fst1->Start();
  if (start1 == kNoStateId)
    return;

  uint64 props1 = fst1->Properties(kFstProperties, false);
  uint64 props2 = fst2.Properties(kFstProperties, false);

  StateId numstates1= fst1->NumStates();

  for (StateIterator< Fst<Arc> > siter2(fst2);
       !siter2.Done();
       siter2.Next()) {
    StateId s1 = fst1->AddState();
    StateId s2 = siter2.Value();
    fst1->SetFinal(s1, fst2.Final(s2));
    for (ArcIterator< Fst<Arc> > aiter(fst2, s2);
         !aiter.Done();
         aiter.Next()) {
      Arc arc = aiter.Value();
      arc.nextstate += numstates1;
      fst1->AddArc(s1, arc);
    }
  }

  StateId start2 = fst2.Start();
  for (StateId s1 = 0; s1 < numstates1; ++s1) {
    Weight final = fst1->Final(s1);
    if (final != Weight::Zero()) {
      fst1->SetFinal(s1, Weight::Zero());
      if (start2 != kNoStateId)
        fst1->AddArc(s1, Arc(0, 0, final, start2 + numstates1));
    }
  }
  if (start2 != kNoStateId)
    fst1->SetProperties(ConcatProperties(props1, props2), kFstProperties);
}


// Computes the concatentation of two FSTs. This version modifies its
// RationalFst input.
template<class Arc>
void Concat(RationalFst<Arc> *fst1, const Fst<Arc> &fst2) {
  fst1->Impl()->AddConcat(fst2);
}


typedef RationalFstOptions ConcatFstOptions;


// Computes the concatenation (product) of two FSTs; this version is a
// delayed Fst. If FST1 transduces string x to y with weight a and FST2
// transduces string w to v with weight b, then their concatenation
// transduces string xw to yv with Times(a, b).
//
// Complexity:
// - Time: O(v1 + e1 + v2 + e2),
// - Space: O(v1 + v2)
// where vi = # of states visited and ei = # of arcs visited of the
// ith FST. Constant time and space to visit an input state or arc is
// assumed and exclusive of caching.
template <class A>
class ConcatFst : public RationalFst<A> {
 public:
  using RationalFst<A>::Impl;

  typedef A Arc;
  typedef typename A::Weight Weight;
  typedef typename A::StateId StateId;

  ConcatFst(const Fst<A> &fst1, const Fst<A> &fst2) {
    Impl()->InitConcat(fst1, fst2);
  }

  ConcatFst(const Fst<A> &fst1, const Fst<A> &fst2,
            const ConcatFstOptions &opts) : RationalFst<A>(opts) {
    Impl()->InitConcat(fst1, fst2);
  }

  ConcatFst(const ConcatFst<A> &fst) : RationalFst<A>(fst) {}

  virtual ConcatFst<A> *Copy() const { return new ConcatFst<A>(*this); }
};


// Specialization for ConcatFst.
template <class A>
class StateIterator< ConcatFst<A> > : public StateIterator< RationalFst<A> > {
 public:
  explicit StateIterator(const ConcatFst<A> &fst)
      : StateIterator< RationalFst<A> >(fst) {}
};


// Specialization for ConcatFst.
template <class A>
class ArcIterator< ConcatFst<A> > : public ArcIterator< RationalFst<A> > {
 public:
  typedef typename A::StateId StateId;

  ArcIterator(const ConcatFst<A> &fst, StateId s)
      : ArcIterator< RationalFst<A> >(fst, s) {}
};


// Useful alias when using StdArc.
typedef ConcatFst<StdArc> StdConcatFst;

}  // namespace fst

#endif  // FST_LIB_CONCAT_H__
