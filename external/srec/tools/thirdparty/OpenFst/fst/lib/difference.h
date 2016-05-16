// difference.h
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
// Class to compute the difference between two FSAs

#ifndef FST_LIB_DIFFERENCE_H__
#define FST_LIB_DIFFERENCE_H__

#include "fst/lib/compose.h"
#include "fst/lib/complement.h"

namespace fst {

template <uint64 T = 0>
struct DifferenceFstOptions
    : public ComposeFstOptions<T | COMPOSE_FST2_RHO> { };


// Computes the difference between two FSAs. This version is a delayed
// Fst. Only strings that are in the first automaton but not in second
// are retained in the result.
//
// The first argument must be an acceptor; the second argument must be
// an unweighted, epsilon-free, deterministic acceptor. One of the
// arguments must be label-sorted.
//
// Complexity: same as ComposeFst.
//
// Caveats: same as ComposeFst.
template <class A>
class DifferenceFst : public ComposeFst<A> {
 public:
  using ComposeFst<A>::Impl;

  typedef A Arc;
  typedef typename A::Weight Weight;
  typedef typename A::StateId StateId;

  // A - B = A ^ B'.
  DifferenceFst(const Fst<A> &fst1, const Fst<A> &fst2)
      : ComposeFst<A>(fst1,
                      ComplementFst<A>(fst2),
                      ComposeFstOptions<COMPOSE_FST2_RHO>()) {
    if (!fst1.Properties(kAcceptor, true))
      LOG(FATAL) << "DifferenceFst: 1st argument not an acceptor";
    uint64 props1 = fst1.Properties(kFstProperties, false);
    uint64 props2 = fst2.Properties(kFstProperties, false);
    Impl()->SetProperties(DifferenceProperties(props1, props2),
                          kCopyProperties);
  }

  template <uint64 T>
  DifferenceFst(const Fst<A> &fst1, const Fst<A> &fst2,
                const DifferenceFstOptions<T> &opts)
      : ComposeFst<A>(fst1,
                      ComplementFst<A>(fst2),
                      ComposeFstOptions<T | COMPOSE_FST2_RHO>(opts)) {
    if (!fst1.Properties(kAcceptor, true))
      LOG(FATAL) << "DifferenceFst: 1st argument not an acceptor";
    uint64 props1 = fst1.Properties(kFstProperties, false);
    uint64 props2 = fst2.Properties(kFstProperties, false);
    Impl()->SetProperties(DifferenceProperties(props1, props2),
                          kCopyProperties);
  }

  DifferenceFst(const DifferenceFst<A> &fst)
      : ComposeFst<A>(fst) {}

  virtual DifferenceFst<A> *Copy() const {
    return new DifferenceFst<A>(*this);
  }
};


// Specialization for DifferenceFst.
template <class A>
class StateIterator< DifferenceFst<A> >
    : public StateIterator< ComposeFst<A> > {
 public:
  explicit StateIterator(const DifferenceFst<A> &fst)
      : StateIterator< ComposeFst<A> >(fst) {}
};


// Specialization for DifferenceFst.
template <class A>
class ArcIterator< DifferenceFst<A> >
    : public ArcIterator< ComposeFst<A> > {
 public:
  typedef typename A::StateId StateId;

  ArcIterator(const DifferenceFst<A> &fst, StateId s)
      : ArcIterator< ComposeFst<A> >(fst, s) {}
};

// Useful alias when using StdArc.
typedef DifferenceFst<StdArc> StdDifferenceFst;


typedef ComposeOptions DifferenceOptions;


// Computes the difference between two FSAs. This version is writes
// the difference to an output MutableFst. Only strings that are in
// the first automaton but not in second are retained in the result.
//
// The first argument must be an acceptor; the second argument must be
// an unweighted, epsilon-free, deterministic acceptor.  One of the
// arguments must be label-sorted.
//
// Complexity: same as Compose.
//
// Caveats: same as Compose.
template<class Arc>
void Difference(const Fst<Arc> &ifst1, const Fst<Arc> &ifst2,
             MutableFst<Arc> *ofst,
             const DifferenceOptions &opts = DifferenceOptions()) {
  DifferenceFstOptions<> nopts;
  nopts.gc_limit = 0;  // Cache only the last state for fastest copy.
  *ofst = DifferenceFst<Arc>(ifst1, ifst2, nopts);
  if (opts.connect)
    Connect(ofst);
}

}  // namespace fst

#endif  // FST_LIB_DIFFERENCE_H__
