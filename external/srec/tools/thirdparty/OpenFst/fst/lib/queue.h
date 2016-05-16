// queue.h
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
// Functions and classes for various Fst state queues with
// a unified interface.

#ifndef FST_LIB_QUEUE_H__
#define FST_LIB_QUEUE_H__

#include <deque>
#include <vector>

#include "fst/lib/arcfilter.h"
#include "fst/lib/connect.h"
#include "fst/lib/heap.h"
#include "fst/lib/topsort.h"

namespace fst {

// template <class S>
// class Queue {
//  public:
//   typedef typename S StateId;
//
//   // Ctr: may need args (e.g., Fst, comparator) for some queues
//   Queue(...);
//   // Returns the head of the queue
//   StateId Head() const;
//   // Inserts a state
//   void Enqueue(StateId s);
//   // Removes the head of the queue
//   void Dequeue();
//   // Updates ordering of state s when weight changes, if necessary
//   void Update(StateId s);
//   // Does the queue contain no elements?
//   bool Empty() const;
//   // Remove all states from queue
//   void Clear();
// };

// State queue types.
enum QueueType {
  TRIVIAL_QUEUE = 0,         // Single state queue
  FIFO_QUEUE = 1,            // First-in, first-out queue
  LIFO_QUEUE = 2,            // Last-in, first-out queue
  SHORTEST_FIRST_QUEUE = 3,  // Shortest-first queue
  TOP_ORDER_QUEUE = 4,       // Topologically-ordered queue
  STATE_ORDER_QUEUE = 5,     // State-ID ordered queue
  SCC_QUEUE = 6,             // Component graph top-ordered meta-queue
  AUTO_QUEUE = 7,            // Auto-selected queue
  OTHER_QUEUE = 8
 };


// QueueBase, templated on the StateId, is the base class shared by the
// queues considered by AutoQueue.
template <class S>
class QueueBase {
 public:
  typedef S StateId;

  QueueBase(QueueType type) : queue_type_(type) {}
  virtual ~QueueBase() {}
  StateId Head() const { return Head_(); }
  void Enqueue(StateId s) { Enqueue_(s); }
  void Dequeue() { Dequeue_(); }
  void Update(StateId s) { Update_(s); }
  bool Empty() const { return Empty_(); }
  void Clear() { Clear_(); }
  QueueType Type() { return queue_type_; }

 private:
  virtual StateId Head_() const = 0;
  virtual void Enqueue_(StateId s) = 0;
  virtual void Dequeue_() = 0;
  virtual void Update_(StateId s) = 0;
  virtual bool Empty_() const = 0;
  virtual void Clear_() = 0;

  QueueType queue_type_;
};


// Trivial queue discipline, templated on the StateId. You may enqueue
// at most one state at a time. It is used for strongly connected components
// with only one state and no self loops.
template <class S>
class TrivialQueue : public QueueBase<S> {
public:
  typedef S StateId;

  TrivialQueue() : QueueBase<S>(TRIVIAL_QUEUE), front_(kNoStateId) {}
  StateId Head() const { return front_; }
  void Enqueue(StateId s) { front_ = s; }
  void Dequeue() { front_ = kNoStateId; }
  void Update(StateId s) {}
  bool Empty() const { return front_ == kNoStateId; }
  void Clear() { front_ = kNoStateId; }


private:
  virtual StateId Head_() const { return Head(); }
  virtual void Enqueue_(StateId s) { Enqueue(s); }
  virtual void Dequeue_() { Dequeue(); }
  virtual void Update_(StateId s) { Update(s); }
  virtual bool Empty_() const { return Empty(); }
  virtual void Clear_() { return Clear(); }

  StateId front_;
};


// First-in, first-out queue discipline, templated on the StateId.
template <class S>
class FifoQueue : public QueueBase<S>, public deque<S> {
 public:
  using deque<S>::back;
  using deque<S>::push_front;
  using deque<S>::pop_back;
  using deque<S>::empty;
  using deque<S>::clear;

  typedef S StateId;

  FifoQueue() : QueueBase<S>(FIFO_QUEUE) {}
  StateId Head() const { return back(); }
  void Enqueue(StateId s) { push_front(s); }
  void Dequeue() { pop_back(); }
  void Update(StateId s) {}
  bool Empty() const { return empty(); }
  void Clear() { clear(); }

 private:
  virtual StateId Head_() const { return Head(); }
  virtual void Enqueue_(StateId s) { Enqueue(s); }
  virtual void Dequeue_() { Dequeue(); }
  virtual void Update_(StateId s) { Update(s); }
  virtual bool Empty_() const { return Empty(); }
  virtual void Clear_() { return Clear(); }
};


// Last-in, first-out queue discipline, templated on the StateId.
template <class S>
class LifoQueue : public QueueBase<S>, public deque<S> {
 public:
  using deque<S>::front;
  using deque<S>::push_front;
  using deque<S>::pop_front;
  using deque<S>::empty;
  using deque<S>::clear;

  typedef S StateId;

  LifoQueue() : QueueBase<S>(LIFO_QUEUE) {}
  StateId Head() const { return front(); }
  void Enqueue(StateId s) { push_front(s); }
  void Dequeue() { pop_front(); }
  void Update(StateId s) {}
  bool Empty() const { return empty(); }
  void Clear() { clear(); }

 private:
  virtual StateId Head_() const { return Head(); }
  virtual void Enqueue_(StateId s) { Enqueue(s); }
  virtual void Dequeue_() { Dequeue(); }
  virtual void Update_(StateId s) { Update(s); }
  virtual bool Empty_() const { return Empty(); }
  virtual void Clear_() { return Clear(); }
};


// Shortest-first queue discipline, templated on the StateId and
// comparison function object.  Comparison function object COMP is
// used to compare two StateIds. If a (single) state's order changes,
// it can be reordered in the queue with a call to Update().
template <typename S, typename C>
class ShortestFirstQueue : public QueueBase<S> {
 public:
  typedef S StateId;
  typedef C Compare;

  ShortestFirstQueue(C comp)
      : QueueBase<S>(SHORTEST_FIRST_QUEUE), heap_(comp) {}

  StateId Head() const { return heap_.Top(); }

  void Enqueue(StateId s) {
    for (StateId i = key_.size(); i <= s; ++i)
      key_.push_back(kNoKey);
    key_[s] = heap_.Insert(s);
  }

  void Dequeue() {
    key_[heap_.Pop()] = kNoKey;
  }

  void Update(StateId s) {
    if (s >= (StateId)key_.size() || key_[s] == kNoKey) {
      Enqueue(s);
    } else {
      heap_.Update(key_[s], s);
    }
  }

  bool Empty() const { return heap_.Empty(); }

  void Clear() {
    heap_.Clear();
    key_.clear();
  }

 private:
  Heap<S, C> heap_;
  vector<ssize_t> key_;

  virtual StateId Head_() const { return Head(); }
  virtual void Enqueue_(StateId s) { Enqueue(s); }
  virtual void Dequeue_() { Dequeue(); }
  virtual void Update_(StateId s) { Update(s); }
  virtual bool Empty_() const { return Empty(); }
  virtual void Clear_() { return Clear(); }
};


// Given a vector that maps from states to weights and a Less
// comparison function object between weights, this class defines a
// comparison function object between states.
template <typename S, typename L>
class StateWeightCompare {
 public:
  typedef L Less;
  typedef typename L::Weight Weight;
  typedef S StateId;

  StateWeightCompare(const vector<Weight>* weights, const L &less)
      : weights_(weights), less_(less) {}

  bool operator()(const S x, const S y) const {
    return less_((*weights_)[x], (*weights_)[y]);
  }

 private:
  const vector<Weight>* weights_;
  L less_;
};


// Shortest-first queue discipline, templated on the StateId and Weight is
// specialized to use the weight's natural order for the comparion function.
template <typename S, typename W>
class NaturalShortestFirstQueue :
      public ShortestFirstQueue<S, StateWeightCompare<S, NaturalLess<W> > > {
 public:
  typedef StateWeightCompare<S, NaturalLess<W> > C;

  NaturalShortestFirstQueue(vector<W> *distance) :
      ShortestFirstQueue<S, C>(C(distance, less_)) {}

 private:
  NaturalLess<W> less_;
};


// Topological-order queue discipline, templated on the StateId.
// States are ordered in the queue topologically. The FST must be acyclic.
template <class S>
class TopOrderQueue : public QueueBase<S> {
 public:
  typedef S StateId;

  // This constructor computes the top. order. It accepts an arc filter
  // to limit the transitions considered in that computation (e.g., only
  // the epsilon graph).
  template <class Arc, class ArcFilter>

  TopOrderQueue(const Fst<Arc> &fst, ArcFilter filter)
      : QueueBase<S>(TOP_ORDER_QUEUE), front_(0), back_(kNoStateId),
        order_(0), state_(0) {
    bool acyclic;
    TopOrderVisitor<Arc> top_order_visitor(&order_, &acyclic);
    DfsVisit(fst, &top_order_visitor, filter);
    if (!acyclic)
      LOG(FATAL) << "TopOrderQueue: fst is not acyclic.";
    state_.resize(order_.size(), kNoStateId);
  }

  // This constructor is passed the top. order, useful when we know it
  // beforehand.
  TopOrderQueue(const vector<StateId> &order)
      : QueueBase<S>(TOP_ORDER_QUEUE), front_(0), back_(kNoStateId),
        order_(order), state_(order.size(), kNoStateId) {}

  StateId Head() const { return state_[front_]; }

  void Enqueue(StateId s) {
    if (front_ > back_) front_ = back_ = order_[s];
    else if (order_[s] > back_) back_ = order_[s];
    else if (order_[s] < front_) front_ = order_[s];
    state_[order_[s]] = s;
  }

  void Dequeue() {
    state_[front_] = kNoStateId;
    while ((front_ <= back_) && (state_[front_] == kNoStateId)) ++front_;
  }

  void Update(StateId s) {}

  bool Empty() const { return front_ > back_; }

  void Clear() {
    for (StateId i = front_; i <= back_; ++i) state_[i] = kNoStateId;
    back_ = kNoStateId;
    front_ = 0;
  }

 private:
  StateId front_;
  StateId back_;
  vector<StateId> order_;
  vector<StateId> state_;

  virtual StateId Head_() const { return Head(); }
  virtual void Enqueue_(StateId s) { Enqueue(s); }
  virtual void Dequeue_() { Dequeue(); }
  virtual void Update_(StateId s) { Update(s); }
  virtual bool Empty_() const { return Empty(); }
  virtual void Clear_() { return Clear(); }

};


// State order queue discipline, templated on the StateId.
// States are ordered in the queue by state Id.
template <class S>
class StateOrderQueue : public QueueBase<S> {
public:
  typedef S StateId;

  StateOrderQueue()
      : QueueBase<S>(STATE_ORDER_QUEUE), front_(0), back_(kNoStateId) {}

  StateId Head() const { return front_; }

  void Enqueue(StateId s) {
    if (front_ > back_) front_ = back_ = s;
    else if (s > back_) back_ = s;
    else if (s < front_) front_ = s;
    while ((StateId)enqueued_.size() <= s) enqueued_.push_back(false); 
    enqueued_[s] = true;
  }

  void Dequeue() {
    enqueued_[front_] = false;
    while ((front_ <= back_) && (enqueued_[front_] == false)) ++front_;
  }

  void Update(StateId s) {}

  bool Empty() const { return front_ > back_; }

  void Clear() {
        for (StateId i = front_; i <= back_; ++i) enqueued_[i] = false;
        front_ = 0;
        back_ = kNoStateId;
  }

private:
  StateId front_;
  StateId back_;
  vector<bool> enqueued_;

  virtual StateId Head_() const { return Head(); }
  virtual void Enqueue_(StateId s) { Enqueue(s); }
  virtual void Dequeue_() { Dequeue(); }
  virtual void Update_(StateId s) { Update(s); }
  virtual bool Empty_() const { return Empty(); }
  virtual void Clear_() { return Clear(); }

};


// SCC topological-order meta-queue discipline, templated on the StateId S
// and a queue Q, which is used inside each SCC.  It visits the SCC's
// of an FST in topological order. Its constructor is passed the queues to
// to use within an SCC.
template <class S, class Q>
class SccQueue : public QueueBase<S> {
 public:
  typedef S StateId;
  typedef Q Queue;

  // Constructor takes a vector specifying the SCC number per state
  // and a vector giving the queue to use per SCC number.
  SccQueue(const vector<StateId> &scc, vector<Queue*> *queue)
    : QueueBase<S>(SCC_QUEUE), queue_(queue), scc_(scc), front_(0),
      back_(kNoStateId) {}

  StateId Head() const {
    while ((front_ <= back_) && 
           (((*queue_)[front_] && (*queue_)[front_]->Empty())
            || (((*queue_)[front_] == 0) &&
                ((front_ > (StateId)trivial_queue_.size())
                 || (trivial_queue_[front_] == kNoStateId)))))
      ++front_;
    if (front_ > back_)
      LOG(FATAL) << "SccQueue: head of empty queue";
    if ((*queue_)[front_])
      return (*queue_)[front_]->Head();
    else
      return trivial_queue_[front_];
  }

  void Enqueue(StateId s) {
    if (front_ > back_) front_ = back_ = scc_[s];
    else if (scc_[s] > back_) back_ = scc_[s];
    else if (scc_[s] < front_) front_ = scc_[s];
    if ((*queue_)[scc_[s]]) {
      (*queue_)[scc_[s]]->Enqueue(s);
    } else {
      while ( (StateId)trivial_queue_.size() <= scc_[s]) 
        trivial_queue_.push_back(kNoStateId);
      trivial_queue_[scc_[s]] = s;
    }
  }

  void Dequeue() {
    if (front_ > back_)
      LOG(FATAL) << "SccQueue: dequeue of empty queue";
    if ((*queue_)[front_])
      (*queue_)[front_]->Dequeue();
    else if (front_ < (StateId)trivial_queue_.size()) 
      trivial_queue_[front_] = kNoStateId;
  }

  void Update(StateId s) {
    if ((*queue_)[scc_[s]])
      (*queue_)[scc_[s]]->Update(s);
  }

  bool Empty() const {
    if (front_ < back_)   // Queue scc # back_ not empty unless back_==front_
      return false;
    else if (front_ > back_)
      return true;
    else if ((*queue_)[front_])
      return (*queue_)[front_]->Empty();
    else
      return (front_ > (StateId)trivial_queue_.size())
        || (trivial_queue_[front_] == kNoStateId);
  }

  void Clear() {
    for (StateId i = front_; i <= back_; ++i)
      if ((*queue_)[i])
        (*queue_)[i]->Clear();
      else if (i < (StateId)trivial_queue_.size()) 
        trivial_queue_[i] = kNoStateId;
    front_ = 0;
    back_ = kNoStateId;
  }

private:
  vector<Queue*> *queue_;
  const vector<StateId> &scc_;
  mutable StateId front_;
  StateId back_;
  vector<StateId> trivial_queue_;

  virtual StateId Head_() const { return Head(); }
  virtual void Enqueue_(StateId s) { Enqueue(s); }
  virtual void Dequeue_() { Dequeue(); }
  virtual void Update_(StateId s) { Update(s); }
  virtual bool Empty_() const { return Empty(); }
  virtual void Clear_() { return Clear(); }
};


// Automatic queue discipline, templated on the StateId. It selects a
// queue discipline for a given FST based on its properties.
template <class S>
class AutoQueue : public QueueBase<S> {
public:
  typedef S StateId;

  // This constructor takes a state distance vector that, if non-null and if
  // the Weight type has the path property, will entertain the
  // shortest-first queue using the natural order w.r.t to the distance.
  template <class Arc, class ArcFilter>
  AutoQueue(const Fst<Arc> &fst, const vector<typename Arc::Weight> *distance,
            ArcFilter filter) : QueueBase<S>(AUTO_QUEUE) {
    typedef typename Arc::Weight Weight;
    typedef StateWeightCompare< StateId, NaturalLess<Weight> > Compare;

    //  First check if the FST is known to have these properties.
    uint64 props = fst.Properties(kAcyclic | kCyclic |
                                  kTopSorted | kUnweighted, false);
    if ((props & kTopSorted) || fst.Start() == kNoStateId) {
      queue_ = new StateOrderQueue<StateId>();
      VLOG(2) << "AutoQueue: using state-order discipline";
    } else if (props & kAcyclic) {
      queue_ = new TopOrderQueue<StateId>(fst, filter);
      VLOG(2) << "AutoQueue: using top-order discipline";
    } else if ((props & kUnweighted) && (Weight::Properties() & kIdempotent)) {
      queue_ = new LifoQueue<StateId>();
      VLOG(2) << "AutoQueue: using LIFO discipline";
    } else {
      uint64 props;
      // Decompose into strongly-connected components.
      SccVisitor<Arc> scc_visitor(&scc_, 0, 0, &props);
      DfsVisit(fst, &scc_visitor, filter);
      StateId nscc = *max_element(scc_.begin(), scc_.end()) + 1;
      vector<QueueType> queue_types(nscc);
      NaturalLess<Weight> *less = 0;
      Compare *comp = 0;
      if (distance && (Weight::Properties() & kPath)) {
        less = new NaturalLess<Weight>;
        comp = new Compare(distance, *less);
      }
      // Find the queue type to use per SCC.
      bool unweighted;
      bool all_trivial;
      SccQueueType(fst, scc_, &queue_types, filter, less, &all_trivial,
                                      &unweighted);
      // If unweighted and semiring is idempotent, use lifo queue.
      if (unweighted) {
          queue_ = new LifoQueue<StateId>();
          VLOG(2) << "AutoQueue: using LIFO discipline";
          delete comp;
          delete less;
          return;
      }
      // If all the scc are trivial, FST is acyclic and the scc# gives
      // the topological order.
      if (all_trivial) {
          queue_ = new TopOrderQueue<StateId>(scc_);
          VLOG(2) << "AutoQueue: using top-order discipline";
          delete comp;
          delete less;
          return;
      }
      VLOG(2) << "AutoQueue: using SCC meta-discipline";
      queues_.resize(nscc);
      for (StateId i = 0; i < nscc; ++i) {
        switch(queue_types[i]) {
          case TRIVIAL_QUEUE:
            queues_[i] = 0;
            VLOG(3) << "AutoQueue: SCC #" << i
                    << ": using trivial discipline";
            break;
          case SHORTEST_FIRST_QUEUE:
            CHECK(comp);
            queues_[i] = new ShortestFirstQueue<StateId, Compare>(*comp);
            VLOG(3) << "AutoQueue: SCC #" << i <<
              ": using shortest-first discipline";
            break;
          case LIFO_QUEUE:
            queues_[i] = new LifoQueue<StateId>();
            VLOG(3) << "AutoQueue: SCC #" << i
                    << ": using LIFO disciplle";
            break;
          case FIFO_QUEUE:
          default:
            queues_[i] = new FifoQueue<StateId>();
            VLOG(3) << "AutoQueue: SCC #" << i
                    << ": using FIFO disciplle";
            break;
        }
      }
      queue_ = new SccQueue< StateId, QueueBase<StateId> >(scc_, &queues_);
      delete comp;
      delete less;
    }
  }

  ~AutoQueue() {
    for (StateId i = 0; i < (StateId)queues_.size(); ++i) /*naucen-edit*/
      delete queues_[i];
    delete queue_;
  }

  StateId Head() const { return queue_->Head(); }

  void Enqueue(StateId s) { queue_->Enqueue(s); }

  void Dequeue() { queue_->Dequeue(); }

  void Update(StateId s) { queue_->Update(s); }

  bool Empty() const { return queue_->Empty(); }

  void Clear() { queue_->Clear(); }


 private:
  QueueBase<StateId> *queue_;
  vector< QueueBase<StateId>* > queues_;
  vector<StateId> scc_;

  template <class Arc, class ArcFilter, class Less>
  static void SccQueueType(const Fst<Arc> &fst,
                           const vector<StateId> &scc,
                           vector<QueueType> *queue_types,
                           ArcFilter filter, Less *less,
                           bool *all_trivial, bool *unweighted);

  virtual StateId Head_() const { return Head(); }

  virtual void Enqueue_(StateId s) { Enqueue(s); }

  virtual void Dequeue_() { Dequeue(); }

  virtual void Update_(StateId s) { Update(s); }

  virtual bool Empty_() const { return Empty(); }

  virtual void Clear_() { return Clear(); }
};


// Examines the states in an Fst's strongly connected components and
// determines which type of queue to use per SCC. Stores result in
// vector QUEUE_TYPES, which is assumed to have length equal to the
// number of SCCs. An arc filter is used to limit the transitions
// considered (e.g., only the epsilon graph).  ALL_TRIVIAL is set
// to true if every queue is the trivial queue. UNWEIGHTED is set to
// true if the semiring is idempotent and all the arc weights are equal to
// Zero() or One().
template <class StateId>
template <class A, class ArcFilter, class Less>
void AutoQueue<StateId>::SccQueueType(const Fst<A> &fst,
                                      const vector<StateId> &scc,
                                      vector<QueueType> *queue_type,
                                      ArcFilter filter, Less *less,
                                      bool *all_trivial, bool *unweighted) {
  typedef A Arc;
  typedef typename A::StateId StateId;
  typedef typename A::Weight Weight;

  *all_trivial = true;
  *unweighted = true;

  for (StateId i = 0; i < (StateId)queue_type->size(); ++i)
    (*queue_type)[i] = TRIVIAL_QUEUE;

  for (StateIterator< Fst<Arc> > sit(fst); !sit.Done(); sit.Next()) {
    StateId state = sit.Value();
    for (ArcIterator< Fst<Arc> > ait(fst, state);
	 !ait.Done();
	 ait.Next()) {
      const Arc &arc = ait.Value();
      if (!filter(arc)) continue;
      if (scc[state] == scc[arc.nextstate]) {
        QueueType &type = (*queue_type)[scc[state]];
        if (!less || ((*less)(arc.weight, Weight::One())))
          type = FIFO_QUEUE;
        else if ((type == TRIVIAL_QUEUE) || (type == LIFO_QUEUE))
          if (!(Weight::Properties() & kIdempotent) ||
              (arc.weight != Weight::Zero() && arc.weight != Weight::One()))
            type = SHORTEST_FIRST_QUEUE;
          else
            type = LIFO_QUEUE;
        if (type != TRIVIAL_QUEUE) *all_trivial = false;
      }
      if (!(Weight::Properties() & kIdempotent) ||
          (arc.weight != Weight::Zero() && arc.weight != Weight::One()))
        *unweighted = false;
    }
  }
}

}  // namespace fst

#endif
