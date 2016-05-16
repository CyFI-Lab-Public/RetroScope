// arcsum.h
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
// Functions to sum arcs (sum weights) in an fst.

#ifndef FST_LIB_ARCSUM_H__
#define FST_LIB_ARCSUM_H__

#include "fst/lib/mutable-fst.h"
#include "fst/lib/weight.h"

namespace fst {

template <class A>
class ArcSumCompare {
 public:
  bool operator()(const A& x, const A& y) {
    if (x.ilabel < y.ilabel) return true;
    if (x.ilabel > y.ilabel) return false;
    if (x.olabel < y.olabel) return true;
    if (x.olabel < y.olabel) return false;
    if (x.nextstate < y.nextstate) return true;
    if (x.nextstate > y.nextstate) return false;
    return false;
  }
};


template <class A>
class ArcSumEqual {
 public:
  bool operator()(const A& x, const A& y) {
    return (x.ilabel == y.ilabel &&
            x.olabel == y.olabel &&
            x.nextstate == y.nextstate);
  }
};


// Combines identically labeled arcs, summing weights. For each state
// we combine arcs with the same input and output label, summing their
// weights using Weight:::Plus().
template <class A>
void ArcSum(MutableFst<A>* fst) {
  typedef typename A::StateId StateId;

  vector<A> arcs;
  for (StateIterator<Fst<A> > siter(*fst); !siter.Done(); siter.Next()) {
    StateId s = siter.Value();
    if (fst->NumArcs(s) == 0) continue;

    // Sums arcs into arcs array.
    arcs.clear();
    arcs.reserve(fst->NumArcs(s));
    for (ArcIterator<Fst<A> > aiter(*fst, s); !aiter.Done();
         aiter.Next())
      arcs.push_back(aiter.Value());

    // At each state, first sorts the exiting arcs by input label, output label
    // and destination state and then combines arcs identical in these
    // attributes.
    ArcSumCompare<A> comp;
    sort(arcs.begin(), arcs.end(), comp);

    // Deletes current arcs and copy in sumed arcs.
    fst->DeleteArcs(s);
    A current_arc = arcs[0];
    ArcSumEqual<A> equal;
    for (size_t i = 1; i < arcs.size(); ++i) {
      if (equal(current_arc, arcs[i])) {
        current_arc.weight = Plus(current_arc.weight, arcs[i].weight);
      } else {
        fst->AddArc(s, current_arc);
        current_arc = arcs[i];
      }
    }
    fst->AddArc(s, current_arc);
  }
}

}

#endif  // FST_LIB_ARCSUM_H__
