// shortest-distance.h
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
// Functions and classes to find shortest distance in an FST.

#ifndef FST_LIB_SHORTEST_DISTANCE_H__
#define FST_LIB_SHORTEST_DISTANCE_H__

#include <deque>

#include "fst/lib/arcfilter.h"
#include "fst/lib/cache.h"
#include "fst/lib/queue.h"
#include "fst/lib/reverse.h"
#include "fst/lib/test-properties.h"

namespace fst {

template <class Arc, class Queue, class ArcFilter>
struct ShortestDistanceOptions {
  typedef typename Arc::StateId StateId;

  Queue *state_queue;    // Queue discipline used; owned by caller
  ArcFilter arc_filter;  // Arc filter (e.g., limit to only epsilon graph)
  StateId source;        // If kNoStateId, use the Fst's initial state
  float delta;           // Determines the degree of convergence required

  ShortestDistanceOptions(Queue *q, ArcFilter filt, StateId src = kNoStateId,
                          float d = kDelta)
      : state_queue(q), arc_filter(filt), source(src), delta(d) {}
};


// Computation state of the shortest-distance algorithm. Reusable
// information is maintained across calls to member function
// ShortestDistance(source) when 'retain' is true for improved
// efficiency when calling multiple times from different source states
// (e.g., in epsilon removal). Vector 'distance' should not be
// modified by the user between these calls.
template<class Arc, class Queue, class ArcFilter>
class ShortestDistanceState {
 public:
  typedef typename Arc::StateId StateId;
  typedef typename Arc::Weight Weight;

  ShortestDistanceState(
      const Fst<Arc> &fst,
      vector<Weight> *distance,
      const ShortestDistanceOptions<Arc, Queue, ArcFilter> &opts,
      bool retain)
      : fst_(fst.Copy()), distance_(distance), state_queue_(opts.state_queue),
        arc_filter_(opts.arc_filter),
        delta_(opts.delta), retain_(retain) {
    distance_->clear();
  }

  ~ShortestDistanceState() {
    delete fst_;
  }

  void ShortestDistance(StateId source);

 private:
  const Fst<Arc> *fst_;
  vector<Weight> *distance_;
  Queue *state_queue_;
  ArcFilter arc_filter_;
  float delta_;
  bool retain_;                  // Retain and reuse information across calls

  vector<Weight> rdistance_;    // Relaxation distance.
  vector<bool> enqueued_;       // Is state enqueued?
  vector<StateId> sources_;     // Source state for ith state in 'distance_',
                                //  'rdistance_', and 'enqueued_' if retained.
};

// Compute the shortest distance. If 'source' is kNoStateId, use
// the initial state of the Fst.
template <class Arc, class Queue, class ArcFilter>
void ShortestDistanceState<Arc, Queue, ArcFilter>::ShortestDistance(
    StateId source) {
  if (fst_->Start() == kNoStateId)
    return;

  if (!(Weight::Properties() & kRightSemiring))
    LOG(FATAL) << "ShortestDistance: Weight needs to be right distributive: "
               << Weight::Type();

  state_queue_->Clear();

  if (!retain_) {
    distance_->clear();
    rdistance_.clear();
    enqueued_.clear();
  }

  if (source == kNoStateId)
    source = fst_->Start();

  while ((StateId)distance_->size() <= source) {
    distance_->push_back(Weight::Zero());
    rdistance_.push_back(Weight::Zero());
    enqueued_.push_back(false);
  }
  if (retain_) {
    while ((StateId)sources_.size() <= source)
      sources_.push_back(kNoStateId);
    sources_[source] = source;
  }
  (*distance_)[source] = Weight::One();
  rdistance_[source] = Weight::One();
  enqueued_[source] = true;

  state_queue_->Enqueue(source);

  while (!state_queue_->Empty()) {
    StateId s = state_queue_->Head();
    state_queue_->Dequeue();
    while ((StateId)distance_->size() <= s) {
      distance_->push_back(Weight::Zero());
      rdistance_.push_back(Weight::Zero());
      enqueued_.push_back(false);
    }
    enqueued_[s] = false;
    Weight r = rdistance_[s];
    rdistance_[s] = Weight::Zero();
    for (ArcIterator< Fst<Arc> > aiter(*fst_, s);
         !aiter.Done();
         aiter.Next()) {
      const Arc &arc = aiter.Value();
      if (!arc_filter_(arc) || arc.weight == Weight::Zero())
        continue;
      while ((StateId)distance_->size() <= arc.nextstate) {
        distance_->push_back(Weight::Zero());
        rdistance_.push_back(Weight::Zero());
        enqueued_.push_back(false);
      }
      if (retain_) {
        while ((StateId)sources_.size() <= arc.nextstate)
          sources_.push_back(kNoStateId);
        if (sources_[arc.nextstate] != source) {
          (*distance_)[arc.nextstate] = Weight::Zero();
          rdistance_[arc.nextstate] = Weight::Zero();
          enqueued_[arc.nextstate] = false;
          sources_[arc.nextstate] = source;
        }
      }
      Weight &nd = (*distance_)[arc.nextstate];
      Weight &nr = rdistance_[arc.nextstate];
      Weight w = Times(r, arc.weight);
      if (!ApproxEqual(nd, Plus(nd, w), delta_)) {
        nd = Plus(nd, w);
        nr = Plus(nr, w);
        if (!enqueued_[arc.nextstate]) {
          state_queue_->Enqueue(arc.nextstate);
          enqueued_[arc.nextstate] = true;
        } else {
          state_queue_->Update(arc.nextstate);
        }
      }
    }
  }
}


// Shortest-distance algorithm: this version allows fine control
// via the options argument. See below for a simpler interface.
//
// This computes the shortest distance from the 'opts.source' state to
// each visited state S and stores the value in the 'distance' vector.
// An unvisited state S has distance Zero(), which will be stored in
// the 'distance' vector if S is less than the maximum visited state.
// The state queue discipline, arc filter, and convergence delta are
// taken in the options argument.

// The weights must must be right distributive and k-closed (i.e., 1 +
// x + x^2 + ... + x^(k +1) = 1 + x + x^2 + ... + x^k).
//
// The algorithm is from Mohri, "Semiring Framweork and Algorithms for
// Shortest-Distance Problems", Journal of Automata, Languages and
// Combinatorics 7(3):321-350, 2002. The complexity of algorithm
// depends on the properties of the semiring and the queue discipline
// used. Refer to the paper for more details.
template<class Arc, class Queue, class ArcFilter>
void ShortestDistance(
    const Fst<Arc> &fst,
    vector<typename Arc::Weight> *distance,
    const ShortestDistanceOptions<Arc, Queue, ArcFilter> &opts) {

  ShortestDistanceState<Arc, Queue, ArcFilter>
    sd_state(fst, distance, opts, false);
  sd_state.ShortestDistance(opts.source);
}

// Shortest-distance algorithm: simplified interface. See above for a
// version that allows finer control.
//
// If 'reverse' is false, this computes the shortest distance from the
// initial state to each state S and stores the value in the
// 'distance' vector. If 'reverse' is true, this computes the shortest
// distance from each state to the final states.  An unvisited state S
// has distance Zero(), which will be stored in the 'distance' vector
// if S is less than the maximum visited state.  The state queue
// discipline is automatically-selected.
//
// The weights must must be right (left) distributive if reverse is
// false (true) and k-closed (i.e., 1 + x + x^2 + ... + x^(k +1) = 1 +
// x + x^2 + ... + x^k).
//
// The algorithm is from Mohri, "Semiring Framweork and Algorithms for
// Shortest-Distance Problems", Journal of Automata, Languages and
// Combinatorics 7(3):321-350, 2002. The complexity of algorithm
// depends on the properties of the semiring and the queue discipline
// used. Refer to the paper for more details.
template<class Arc>
void ShortestDistance(const Fst<Arc> &fst,
                      vector<typename Arc::Weight> *distance,
                      bool reverse = false) {
  typedef typename Arc::StateId StateId;
  typedef typename Arc::Weight Weight;

  if (!reverse) {
    AnyArcFilter<Arc> arc_filter;
    AutoQueue<StateId> state_queue(fst, distance, arc_filter);
    ShortestDistanceOptions< Arc, AutoQueue<StateId>, AnyArcFilter<Arc> >
      opts(&state_queue, arc_filter);
    ShortestDistance(fst, distance, opts);
  } else {
    typedef ReverseArc<Arc> ReverseArc;
    typedef typename ReverseArc::Weight ReverseWeight;
    AnyArcFilter<ReverseArc> rarc_filter;
    VectorFst<ReverseArc> rfst;
    Reverse(fst, &rfst);
    vector<ReverseWeight> rdistance;
    AutoQueue<StateId> state_queue(rfst, &rdistance, rarc_filter);
    ShortestDistanceOptions< ReverseArc, AutoQueue<StateId>,
      AnyArcFilter<ReverseArc> >
      ropts(&state_queue, rarc_filter);
    ShortestDistance(rfst, &rdistance, ropts);
    distance->clear();
    while (distance->size() < rdistance.size() - 1)
      distance->push_back(rdistance[distance->size() + 1].Reverse());
  }
}

}  // namespace fst

#endif  // FST_LIB_SHORTEST_DISTANCE_H__
