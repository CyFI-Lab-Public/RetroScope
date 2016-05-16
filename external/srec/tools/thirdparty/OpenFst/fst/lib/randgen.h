// randgen.h
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
// Function to generate random paths through an FST.

#ifndef FST_LIB_RANDGEN_H__
#define FST_LIB_RANDGEN_H__

#include <cmath>
#include <cstdlib>
#include <ctime>

#include "fst/lib/mutable-fst.h"

namespace fst {

//
// ARC SELECTORS - these function objects are used to select a random
// transition to take from an FST's state. They should return a number
// N s.t. 0 <= N <= NumArcs(). If N < NumArcs(), then the N-th
// transition is selected. If N == NumArcs(), then the final weight at
// that state is selected (i.e., the 'super-final' transition is selected).
// It can be assumed these will not be called unless either there
// are transitions leaving the state and/or the state is final.
//

// Randomly selects a transition using the uniform distribution.
template <class A>
struct UniformArcSelector {
  typedef typename A::StateId StateId;
  typedef typename A::Weight Weight;

  UniformArcSelector(int seed = time(0)) { srand(seed); }

  size_t operator()(const Fst<A> &fst, StateId s) const {
    double r = rand()/(RAND_MAX + 1.0);
    size_t n = fst.NumArcs(s);
    if (fst.Final(s) != Weight::Zero())
      ++n;
    return static_cast<size_t>(r * n);
  }
};

// Randomly selects a transition w.r.t. the weights treated as negative
// log probabilities after normalizing for the total weight leaving
// the state). Weight::zero transitions are disregarded.
// Assumes Weight::Value() accesses the floating point
// representation of the weight.
template <class A>
struct LogProbArcSelector {
  typedef typename A::StateId StateId;
  typedef typename A::Weight Weight;

  LogProbArcSelector(int seed = time(0)) { srand(seed); }

  size_t operator()(const Fst<A> &fst, StateId s) const {
    // Find total weight leaving state
    double sum = 0.0;
    for (ArcIterator< Fst<A> > aiter(fst, s); !aiter.Done();
         aiter.Next()) {
      const A &arc = aiter.Value();
      sum += exp(-arc.weight.Value());
    }
    sum += exp(-fst.Final(s).Value());

    double r = rand()/(RAND_MAX + 1.0);
    double p = 0.0;
    int n = 0;
    for (ArcIterator< Fst<A> > aiter(fst, s); !aiter.Done();
         aiter.Next(), ++n) {
      const A &arc = aiter.Value();
      p += exp(-arc.weight.Value());
      if (p > r * sum) return n;
    }
    return n;
  }
};

// Convenience definitions
typedef LogProbArcSelector<StdArc> StdArcSelector;
typedef LogProbArcSelector<LogArc> LogArcSelector;


// Options for random path generation.
template <class S>
struct RandGenOptions {
  const S &arc_selector;  // How an arc is selected at a state
  int max_length;         // Maximum path length
  size_t npath;           // # of paths to generate

  // These are used internally by RandGen
  int64 source;           // 'ifst' state to expand
  int64 dest;             // 'ofst' state to append

  RandGenOptions(const S &sel, int len = INT_MAX, size_t n = 1)
    : arc_selector(sel), max_length(len), npath(n),
       source(kNoStateId), dest(kNoStateId) {}
};


// Randomly generate paths through an FST; details controlled by
// RandGenOptions.
template<class Arc, class ArcSelector>
void RandGen(const Fst<Arc> &ifst, MutableFst<Arc> *ofst,
	     const RandGenOptions<ArcSelector> &opts) {
  typedef typename Arc::Weight Weight;

  if (opts.npath == 0 || opts.max_length == 0 || ifst.Start() == kNoStateId)
    return;

  if (opts.source == kNoStateId) {   // first call
    ofst->DeleteStates();
    ofst->SetInputSymbols(ifst.InputSymbols());
    ofst->SetOutputSymbols(ifst.OutputSymbols());
    ofst->SetStart(ofst->AddState());
    RandGenOptions<ArcSelector> nopts(opts);
    nopts.source = ifst.Start();
    nopts.dest = ofst->Start();
    for (; nopts.npath > 0; --nopts.npath)
      RandGen(ifst, ofst, nopts);
  } else {
    if (ifst.NumArcs(opts.source) == 0 &&
	ifst.Final(opts.source) == Weight::Zero())  // Non-coaccessible
      return;
    // Pick a random transition from the source state
    size_t n = opts.arc_selector(ifst, opts.source);
    if (n == ifst.NumArcs(opts.source)) {  // Take 'super-final' transition
      ofst->SetFinal(opts.dest, Weight::One());
    } else {
      ArcIterator< Fst<Arc> > aiter(ifst, opts.source);
      aiter.Seek(n);
      const Arc &iarc = aiter.Value();
      Arc oarc(iarc.ilabel, iarc.olabel, Weight::One(), ofst->AddState());
      ofst->AddArc(opts.dest, oarc);

      RandGenOptions<ArcSelector> nopts(opts);
      nopts.source = iarc.nextstate;
      nopts.dest = oarc.nextstate;
      --nopts.max_length;
      RandGen(ifst, ofst, nopts);
    }
  }
}

// Randomly generate a path through an FST with the uniform distribution
// over the transitions.
template<class Arc>
void RandGen(const Fst<Arc> &ifst, MutableFst<Arc> *ofst) {
  UniformArcSelector<Arc> uniform_selector;
  RandGenOptions< UniformArcSelector<Arc> > opts(uniform_selector);
  RandGen(ifst, ofst, opts);
}

}  // namespace fst

#endif  // FST_LIB_RANDGEN_H__
