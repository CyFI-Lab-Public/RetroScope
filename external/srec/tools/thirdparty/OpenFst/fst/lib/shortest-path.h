// shortest-path.h
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
// Functions to find shortest paths in an FST.

#ifndef FST_LIB_SHORTEST_PATH_H__
#define FST_LIB_SHORTEST_PATH_H__

#include <functional>

#include "fst/lib/cache.h"
#include "fst/lib/queue.h"
#include "fst/lib/shortest-distance.h"
#include "fst/lib/test-properties.h"

namespace fst {

template <class Arc, class Queue, class ArcFilter>
struct ShortestPathOptions
    : public ShortestDistanceOptions<Arc, Queue, ArcFilter> {
  typedef typename Arc::StateId StateId;

  size_t nshortest;      // return n-shortest paths
  bool unique;           // only return paths with distinct input strings
  bool has_distance;     // distance vector already contains the
                         // shortest distance from the initial state

  ShortestPathOptions(Queue *q, ArcFilter filt, size_t n = 1, bool u = false,
                      bool hasdist = false, float d = kDelta)
      : ShortestDistanceOptions<Arc, Queue, ArcFilter>(q, filt, kNoStateId, d),
        nshortest(n), unique(u), has_distance(hasdist)  {}
};


// Shortest-path algorithm: normally not called directly; prefer
// 'ShortestPath' below with n=1. 'ofst' contains the shortest path in
// 'ifst'. 'distance' returns the shortest distances from the source
// state to each state in 'ifst'. 'opts' is used to specify options
// such as the queue discipline, the arc filter and delta.
//
// The shortest path is the lowest weight path w.r.t. the natural
// semiring order.
//
// The weights need to be right distributive and have the path (kPath)
// property.
template<class Arc, class Queue, class ArcFilter>
void SingleShortestPath(const Fst<Arc> &ifst,
                  MutableFst<Arc> *ofst,
                  vector<typename Arc::Weight> *distance,
                  ShortestPathOptions<Arc, Queue, ArcFilter> &opts) {
  typedef typename Arc::StateId StateId;
  typedef typename Arc::Weight Weight;

  ofst->DeleteStates();
  ofst->SetInputSymbols(ifst.InputSymbols());
  ofst->SetOutputSymbols(ifst.OutputSymbols());

  if (ifst.Start() == kNoStateId)
    return;

  vector<Weight> rdistance;
  vector<bool> enqueued;
  vector<StateId> parent;
  vector<Arc> arc_parent;

  Queue *state_queue = opts.state_queue;
  StateId source = opts.source == kNoStateId ? ifst.Start() : opts.source;
  Weight f_distance = Weight::Zero();
  StateId f_parent = kNoStateId;

  distance->clear();
  state_queue->Clear();
  if (opts.nshortest != 1)
    LOG(FATAL) << "SingleShortestPath: for nshortest > 1, use ShortestPath"
               << " instead";
  if ((Weight::Properties() & (kPath | kRightSemiring))
       != (kPath | kRightSemiring))
      LOG(FATAL) << "SingleShortestPath: Weight needs to have the path"
                 << " property and be right distributive: " << Weight::Type();

  while (distance->size() < source) {
    distance->push_back(Weight::Zero());
    enqueued.push_back(false);
    parent.push_back(kNoStateId);
    arc_parent.push_back(Arc(kNoLabel, kNoLabel, Weight::Zero(), kNoStateId));
  }
  distance->push_back(Weight::One());
  parent.push_back(kNoStateId);
  arc_parent.push_back(Arc(kNoLabel, kNoLabel, Weight::Zero(), kNoStateId));
  state_queue->Enqueue(source);
  enqueued.push_back(true);

  while (!state_queue->Empty()) {
    StateId s = state_queue->Head();
    state_queue->Dequeue();
    enqueued[s] = false;
    Weight sd = (*distance)[s];
    for (ArcIterator< Fst<Arc> > aiter(ifst, s);
         !aiter.Done();
         aiter.Next()) {
      const Arc &arc = aiter.Value();
      while (distance->size() <= arc.nextstate) {
        distance->push_back(Weight::Zero());
        enqueued.push_back(false);
        parent.push_back(kNoStateId);
        arc_parent.push_back(Arc(kNoLabel, kNoLabel, Weight::Zero(),
                                 kNoStateId));
      }
      Weight &nd = (*distance)[arc.nextstate];
      Weight w = Times(sd, arc.weight);
      if (nd != Plus(nd, w)) {
        nd = Plus(nd, w);
        parent[arc.nextstate] = s;
        arc_parent[arc.nextstate] = arc;
        if (!enqueued[arc.nextstate]) {
          state_queue->Enqueue(arc.nextstate);
          enqueued[arc.nextstate] = true;
        } else {
          state_queue->Update(arc.nextstate);
        }
      }
    }
    if (ifst.Final(s) != Weight::Zero()) {
      Weight w = Times(sd, ifst.Final(s));
      if (f_distance != Plus(f_distance, w)) {
        f_distance = Plus(f_distance, w);
        f_parent = s;
      }
    }
  }
  (*distance)[source] = Weight::One();
  parent[source] = kNoStateId;

  StateId s_p = kNoStateId, d_p = kNoStateId;
  for (StateId s = f_parent, d = kNoStateId;
       s != kNoStateId;
       d = s, s = parent[s]) {
    enqueued[s] = true;
    d_p = s_p;
    s_p = ofst->AddState();
    if (d == kNoStateId) {
      ofst->SetFinal(s_p, ifst.Final(f_parent));
    } else {
      arc_parent[d].nextstate = d_p;
      ofst->AddArc(s_p, arc_parent[d]);
    }
  }
  ofst->SetStart(s_p);
}


template <class S, class W>
class ShortestPathCompare {
 public:
  typedef S StateId;
  typedef W Weight;
  typedef pair<StateId, Weight> Pair;

  ShortestPathCompare(const vector<Pair>& pairs,
                      const vector<Weight>& distance,
                      StateId sfinal, float d)
      : pairs_(pairs), distance_(distance), superfinal_(sfinal), delta_(d)  {}

  bool operator()(const StateId x, const StateId y) const {
    const Pair &px = pairs_[x];
    const Pair &py = pairs_[y];
    Weight wx = Times(distance_[px.first], px.second);
    Weight wy = Times(distance_[py.first], py.second);
    // Penalize complete paths to ensure correct results with inexact weights.
    // This forms a strict weak order so long as ApproxEqual(a, b) =>
    // ApproxEqual(a, c) for all c s.t. less_(a, c) && less_(c, b).
    if (px.first == superfinal_ && py.first != superfinal_) {
      return less_(wy, wx) || ApproxEqual(wx, wy, delta_);
    } else if (py.first == superfinal_ && px.first != superfinal_) {
      return less_(wy, wx) && !ApproxEqual(wx, wy, delta_);
    } else {
      return less_(wy, wx);
    }
  }

 private:
  const vector<Pair> &pairs_;
  const vector<Weight> &distance_;
  StateId superfinal_;
  float delta_;
  NaturalLess<Weight> less_;
};


// N-Shortest-path algorithm:  this version allow fine control
// via the otpions argument. See below for a simpler interface.
//
// 'ofst' contains the n-shortest paths in 'ifst'. 'distance' returns
// the shortest distances from the source state to each state in
// 'ifst'. 'opts' is used to specify options such as the number of
// paths to return, whether they need to have distinct input
// strings, the queue discipline, the arc filter and the convergence
// delta.
//
// The n-shortest paths are the n-lowest weight paths w.r.t. the
// natural semiring order. The single path that can be
// read from the ith of at most n transitions leaving the initial
// state of 'ofst' is the ith shortest path.

// The weights need to be right distributive and have the path (kPath)
// property. They need to be left distributive as well for nshortest
// > 1.
//
// The algorithm is from Mohri and Riley, "An Efficient Algorithm for
// the n-best-strings problem", ICSLP 2002. The algorithm relies on
// the shortest-distance algorithm. There are some issues with the
// pseudo-code as written in the paper (viz., line 11).
template<class Arc, class Queue, class ArcFilter>
void ShortestPath(const Fst<Arc> &ifst, MutableFst<Arc> *ofst,
                  vector<typename Arc::Weight> *distance,
                  ShortestPathOptions<Arc, Queue, ArcFilter> &opts) {
  typedef typename Arc::StateId StateId;
  typedef typename Arc::Weight Weight;
  typedef pair<StateId, Weight> Pair;
  typedef ReverseArc<Arc> ReverseArc;
  typedef typename ReverseArc::Weight ReverseWeight;

  size_t n = opts.nshortest;

  if (n == 1) {
    SingleShortestPath(ifst, ofst, distance, opts);
    return;
  }
  ofst->DeleteStates();
  ofst->SetInputSymbols(ifst.InputSymbols());
  ofst->SetOutputSymbols(ifst.OutputSymbols());
  if (n <= 0) return;
  if ((Weight::Properties() & (kPath | kSemiring)) != (kPath | kSemiring))
    LOG(FATAL) << "ShortestPath: n-shortest: Weight needs to have the "
                 << "path property and be distributive: "
                 << Weight::Type();
  if (opts.unique)
    LOG(FATAL) << "ShortestPath: n-shortest-string algorithm not "
               << "currently implemented";

  // Algorithm works on the reverse of 'fst' : 'rfst' 'distance' is
  // the distance to the final state in 'rfst' 'ofst' is built as the
  // reverse of the tree of n-shortest path in 'rfst'.

  if (!opts.has_distance)
    ShortestDistance(ifst, distance, opts);
  VectorFst<ReverseArc> rfst;
  Reverse(ifst, &rfst);
  distance->insert(distance->begin(), Weight::One());
  while (distance->size() < rfst.NumStates())
    distance->push_back(Weight::Zero());


  // Each state in 'ofst' corresponds to a path with weight w from the
  // initial state of 'rfst' to a state s in 'rfst', that can be
  // characterized by a pair (s,w).  The vector 'pairs' maps each
  // state in 'ofst' to the corresponding pair maps states in OFST to
  // the corresponding pair (s,w).
  vector<Pair> pairs;
  // 'r[s]', 's' state in 'fst', is the number of states in 'ofst'
  // which corresponding pair contains 's' ,i.e. , it is number of
  // paths computed so far to 's'.
  StateId superfinal = distance->size();  // superfinal must be handled
  distance->push_back(Weight::One());     // differently when unique=true
  ShortestPathCompare<StateId, Weight>
    compare(pairs, *distance, superfinal, opts.delta);
  vector<StateId> heap;
  vector<int> r;
  while (r.size() < distance->size())
    r.push_back(0);
  ofst->SetStart(ofst->AddState());
  StateId final = ofst->AddState();
  ofst->SetFinal(final, Weight::One());
  while (pairs.size() <= final)
    pairs.push_back(Pair(kNoStateId, Weight::Zero()));
  pairs[final] = Pair(rfst.Start(), Weight::One());
  heap.push_back(final);

  while (!heap.empty()) {
    pop_heap(heap.begin(), heap.end(), compare);
    StateId state = heap.back();
    Pair p = pairs[state];
    heap.pop_back();

    ++r[p.first];
    if (p.first == superfinal)
      ofst->AddArc(ofst->Start(), Arc(0, 0, Weight::One(), state));
    if ((p.first == superfinal) &&  (r[p.first] == n)) break;
    if (r[p.first] > n) continue;
    if (p.first == superfinal)
      continue;

    for (ArcIterator< Fst<ReverseArc> > aiter(rfst, p.first);
         !aiter.Done();
         aiter.Next()) {
      const ReverseArc &rarc = aiter.Value();
      Arc arc(rarc.ilabel, rarc.olabel, rarc.weight.Reverse(), rarc.nextstate);
      Weight w = Times(p.second, arc.weight);
      StateId next = ofst->AddState();
      pairs.push_back(Pair(arc.nextstate, w));
      arc.nextstate = state;
      ofst->AddArc(next, arc);
      heap.push_back(next);
      push_heap(heap.begin(), heap.end(), compare);
    }

    Weight finalw = rfst.Final(p.first).Reverse();
    if (finalw != Weight::Zero()) {
      Weight w = Times(p.second, finalw);
      StateId next = ofst->AddState();
      pairs.push_back(Pair(superfinal, w));
      ofst->AddArc(next, Arc(0, 0, finalw, state));
      heap.push_back(next);
      push_heap(heap.begin(), heap.end(), compare);
    }
  }
  Connect(ofst);
  distance->erase(distance->begin());
  distance->pop_back();
}

// Shortest-path algorithm: simplified interface. See above for a
// version that allows finer control.

// 'ofst' contains the 'n'-shortest paths in 'ifst'. The queue
// discipline is automatically selected. When 'unique' == true, only
// paths with distinct input labels are returned.
//
// The n-shortest paths are the n-lowest weight paths w.r.t. the
// natural semiring order. The single path that can be read from the
// ith of at most n transitions leaving the initial state of 'ofst' is
// the ith best path.
//
// The weights need to be right distributive and have the path
// (kPath) property.
template<class Arc>
void ShortestPath(const Fst<Arc> &ifst, MutableFst<Arc> *ofst,
                  size_t n = 1, bool unique = false) {
  vector<typename Arc::Weight> distance;
  AnyArcFilter<Arc> arc_filter;
  AutoQueue<typename Arc::StateId> state_queue(ifst, &distance, arc_filter);
  ShortestPathOptions< Arc, AutoQueue<typename Arc::StateId>,
    AnyArcFilter<Arc> > opts(&state_queue, arc_filter, n, unique);
  ShortestPath(ifst, ofst, &distance, opts);
}

}  // namespace fst

#endif  // FST_LIB_SHORTEST_PATH_H__
