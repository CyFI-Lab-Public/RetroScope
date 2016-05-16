// reweight.h
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
// Function to reweight an FST.

#ifndef FST_LIB_REWEIGHT_H__
#define FST_LIB_REWEIGHT_H__

#include "fst/lib/mutable-fst.h"

namespace fst {

enum ReweightType { REWEIGHT_TO_INITIAL, REWEIGHT_TO_FINAL };

// Reweight FST according to the potentials defined by the POTENTIAL
// vector in the direction defined by TYPE. Weight needs to be left
// distributive when reweighting towards the initial state and right
// distributive when reweighting towards the final states.
//
// An arc of weight w, with an origin state of potential p and
// destination state of potential q, is reweighted by p\wq when
// reweighting towards the initial state and by pw/q when reweighting
// towards the final states.
template <class Arc>
void Reweight(MutableFst<Arc> *fst, vector<typename Arc::Weight> potential,
              ReweightType type) {
  typedef typename Arc::Weight Weight;

  if (!fst->NumStates())
    return;
  while ( (int64)potential.size() < (int64)fst->NumStates()) 
    potential.push_back(Weight::Zero());

  if (type == REWEIGHT_TO_FINAL && !(Weight::Properties() & kRightSemiring))
    LOG(FATAL) << "Reweight: Reweighting to the final states requires "
               << "Weight to be right distributive: "
               << Weight::Type();

  if (type == REWEIGHT_TO_INITIAL && !(Weight::Properties() & kLeftSemiring))
    LOG(FATAL) << "Reweight: Reweighting to the initial state requires "
               << "Weight to be left distributive: "
               << Weight::Type();

  for (StateIterator< MutableFst<Arc> > sit(*fst);
       !sit.Done();
       sit.Next()) {
    typename Arc::StateId state = sit.Value();
    for (MutableArcIterator< MutableFst<Arc> > ait(fst, state);
         !ait.Done();
         ait.Next()) {
      Arc arc = ait.Value();
      if ((potential[state] == Weight::Zero()) ||
	  (potential[arc.nextstate] == Weight::Zero()))
	continue; //temp fix: needs to find best solution for zeros
      if ((type == REWEIGHT_TO_INITIAL)
	  && (potential[state] != Weight::Zero()))
        arc.weight = Divide(Times(arc.weight, potential[arc.nextstate]),
			    potential[state], DIVIDE_LEFT);
      else if ((type == REWEIGHT_TO_FINAL)
	       && (potential[arc.nextstate] != Weight::Zero()))
        arc.weight = Divide(Times(potential[state], arc.weight),
                            potential[arc.nextstate], DIVIDE_RIGHT);
      ait.SetValue(arc);
    }
    if ((type == REWEIGHT_TO_INITIAL)
	&& (potential[state] != Weight::Zero()))
      fst->SetFinal(state,
                    Divide(fst->Final(state), potential[state], DIVIDE_LEFT));
    else if (type == REWEIGHT_TO_FINAL)
      fst->SetFinal(state, Times(potential[state], fst->Final(state)));
  }

  if ((potential[fst->Start()] != Weight::One()) &&
      (potential[fst->Start()] != Weight::Zero())) {
    if (fst->Properties(kInitialAcyclic, true) & kInitialAcyclic) {
      typename Arc::StateId state = fst->Start();
      for (MutableArcIterator< MutableFst<Arc> > ait(fst, state);
           !ait.Done();
           ait.Next()) {
        Arc arc = ait.Value();
        if (type == REWEIGHT_TO_INITIAL)
          arc.weight = Times(potential[state], arc.weight);
        else
          arc.weight = Times(
              Divide(Weight::One(), potential[state], DIVIDE_RIGHT),
              arc.weight);
        ait.SetValue(arc);
      }
      if (type == REWEIGHT_TO_INITIAL)
        fst->SetFinal(state, Times(potential[state], fst->Final(state)));
      else
        fst->SetFinal(state, Times(Divide(Weight::One(), potential[state],
                                          DIVIDE_RIGHT),
                                   fst->Final(state)));
    }
    else {
      typename Arc::StateId state = fst->AddState();
      Weight w = type == REWEIGHT_TO_INITIAL ?
                 potential[fst->Start()] :
                 Divide(Weight::One(), potential[fst->Start()], DIVIDE_RIGHT);
      Arc arc (0, 0, w, fst->Start());
      fst->AddArc(state, arc);
      fst->SetStart(state);
    }
  }

  fst->SetProperties(ReweightProperties(
                         fst->Properties(kFstProperties, false)),
                     kFstProperties);
}

}  // namespace fst

#endif /* FST_LIB_REWEIGHT_H_ */
