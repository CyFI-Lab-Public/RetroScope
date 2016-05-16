// prune.h
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
// Author: allauzen@cs.nyu.edu (Cyril Allauzen)
//
// \file
// Functions implementing pruning.

#ifndef FST_LIB_PRUNE_H__
#define FST_LIB_PRUNE_H__

#include "fst/lib/arcfilter.h"
#include "fst/lib/shortest-distance.h"

namespace fst {

template <class A, class ArcFilter>
class PruneOptions {
 public:
  typedef typename A::Weight Weight;

  // Pruning threshold.
  Weight threshold;
  // Arc filter.
  ArcFilter filter;
  // If non-zero, passes in pre-computed shortest distance from initial state
  // (possibly resized).
  vector<Weight> *idistance;
  // If non-zero, passes in pre-computed shortest distance to final states
  // (possibly resized).
  vector<Weight> *fdistance;

  PruneOptions(const Weight& t, ArcFilter f, vector<Weight> *id = 0,
               vector<Weight> *fd = 0)
      : threshold(t), filter(f), idistance(id), fdistance(fd) {}
};


// Pruning algorithm: this version modifies its input and it takes an
// options class as an argment. Delete states and arcs in 'fst' that
// do not belong to a successful path whose weight is no more than
// 'opts.threshold' Times() the weight of the shortest path. Weights
// need to be commutative and have the path property.
template <class Arc, class ArcFilter>
void Prune(MutableFst<Arc> *fst,
           const PruneOptions<Arc, ArcFilter> &opts) {
  typedef typename Arc::Weight Weight;
  typedef typename Arc::StateId StateId;

  if ((Weight::Properties() & (kPath | kCommutative))
      != (kPath | kCommutative))
    LOG(FATAL) << "Prune: Weight needs to have the path property and"
               << " be commutative: "
               << Weight::Type();

  StateId ns = fst->NumStates();
  if (ns == 0) return;

  vector<Weight> *idistance = opts.idistance;
  vector<Weight> *fdistance = opts.fdistance;

  if (!idistance) {
    idistance = new vector<Weight>(ns, Weight::Zero());
    ShortestDistance(*fst, idistance, false);
  } else {
    idistance->resize(ns, Weight::Zero());
  }

  if (!fdistance) {
    fdistance = new vector<Weight>(ns, Weight::Zero());
    ShortestDistance(*fst, fdistance, true);
  } else {
    fdistance->resize(ns, Weight::Zero());
  }

  vector<StateId> dead;
  dead.push_back(fst->AddState());
  NaturalLess<Weight> less;
  Weight ceiling = Times((*fdistance)[fst->Start()], opts.threshold);

  for (StateId state = 0; state < ns; ++state) {
    if (less(ceiling, Times((*idistance)[state], (*fdistance)[state]))) {
      dead.push_back(state);
      continue;
    }
    for (MutableArcIterator< MutableFst<Arc> > it(fst, state);
         !it.Done();
         it.Next()) {
      Arc arc = it.Value();
      if (!opts.filter(arc)) continue;
      Weight weight = Times(Times((*idistance)[state], arc.weight),
                           (*fdistance)[arc.nextstate]);
      if(less(ceiling, weight)) {
        arc.nextstate = dead[0];
        it.SetValue(arc);
      }
    }
    if (less(ceiling, Times((*idistance)[state], fst->Final(state))))
      fst->SetFinal(state, Weight::Zero());
  }

  fst->DeleteStates(dead);

  if (!opts.idistance)
    delete idistance;
  if (!opts.fdistance)
    delete fdistance;
}


// Pruning algorithm: this version modifies its input and simply takes
// the pruning threshold as an argument. Delete states and arcs in
// 'fst' that do not belong to a successful path whose weight is no
// more than 'opts.threshold' Times() the weight of the shortest
// path. Weights need to be commutative and have the path property.
template <class Arc>
void Prune(MutableFst<Arc> *fst, typename Arc::Weight threshold) {
  PruneOptions<Arc, AnyArcFilter<Arc> > opts(threshold, AnyArcFilter<Arc>());
  Prune(fst, opts);
}


// Pruning algorithm: this version writes the pruned input Fst to an
// output MutableFst and it takes an options class as an argument.
// 'ofst' contains states and arcs that belong to a successful path in
// 'ifst' whose weight is no more than 'opts.threshold' Times() the
// weight of the shortest path. Weights need to be commutative and
// have the path property.
template <class Arc, class ArcFilter>
void Prune(const Fst<Arc> &ifst,
           MutableFst<Arc> *ofst,
           const PruneOptions<Arc, ArcFilter> &opts) {
  typedef typename Arc::Weight Weight;
  typedef typename Arc::StateId StateId;

  if ((Weight::Properties() & (kPath | kCommutative))
      != (kPath | kCommutative))
    LOG(FATAL) << "Prune: Weight needs to have the path property and"
               << " be commutative: "
               << Weight::Type();

  ofst->DeleteStates();

  if (ifst.Start() == kNoStateId)
    return;

  vector<Weight> *idistance = opts.idistance;
  vector<Weight> *fdistance = opts.fdistance;

  if (!idistance) {
    idistance = new vector<Weight>;
    ShortestDistance(ifst, idistance, false);
  }

  if (!fdistance) {
    fdistance = new vector<Weight>;
    ShortestDistance(ifst, fdistance, true);
  }

  vector<StateId> copy;
  NaturalLess<Weight> less;
  while (fdistance->size() <= ifst.Start())
    fdistance->push_back(Weight::Zero());
  Weight ceiling = Times((*fdistance)[ifst.Start()], opts.threshold);

  for (StateIterator< Fst<Arc> > sit(ifst);
       !sit.Done();
       sit.Next()) {
    StateId state = sit.Value();
    while (idistance->size() <= state)
      idistance->push_back(Weight::Zero());
    while (fdistance->size() <= state)
      fdistance->push_back(Weight::Zero());
    while (copy.size() <= state)
      copy.push_back(kNoStateId);

    if (less(ceiling, Times((*idistance)[state], (*fdistance)[state])))
      continue;

    if (copy[state] == kNoStateId)
      copy[state] = ofst->AddState();
    if (!less(ceiling, Times((*idistance)[state], ifst.Final(state))))
      ofst->SetFinal(copy[state], ifst.Final(state));

    for (ArcIterator< Fst<Arc> > ait(ifst, state);
         !ait.Done();
         ait.Next()) {
      Arc arc = ait.Value();

      if (!opts.filter(arc)) continue;

      while (idistance->size() <= arc.nextstate)
        idistance->push_back(Weight::Zero());
      while (fdistance->size() <= arc.nextstate)
        fdistance->push_back(Weight::Zero());
      while (copy.size() <= arc.nextstate)
        copy.push_back(kNoStateId);

      Weight weight = Times(Times((*idistance)[state], arc.weight),
                           (*fdistance)[arc.nextstate]);

      if (!less(ceiling, weight)) {
        if (copy[arc.nextstate] == kNoStateId)
          copy[arc.nextstate] = ofst->AddState();
        arc.nextstate = copy[arc.nextstate];
        ofst->AddArc(copy[state], arc);
      }
    }
  }

  ofst->SetStart(copy[ifst.Start()]);

  if (!opts.idistance)
    delete idistance;
  if (!opts.fdistance)
    delete fdistance;
}


// Pruning algorithm: this version writes the pruned input Fst to an
// output MutableFst and simply takes the pruning threshold as an
// argument.  'ofst' contains states and arcs that belong to a
// successful path in 'ifst' whose weight is no more than
// 'opts.threshold' Times() the weight of the shortest path. Weights
// need to be commutative and have the path property.
template <class Arc>
void Prune(const Fst<Arc> &ifst,
           MutableFst<Arc> *ofst,
           typename Arc::Weight threshold) {
  PruneOptions<Arc, AnyArcFilter<Arc> > opts(threshold, AnyArcFilter<Arc>());
  Prune(ifst, ofst, opts);
}

} // namespace fst

#endif // FST_LIB_PRUNE_H_
