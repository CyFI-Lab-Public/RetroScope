// cache.h
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
// An Fst implementation that caches FST elements of a delayed
// computation.

#ifndef FST_LIB_CACHE_H__
#define FST_LIB_CACHE_H__

#include <list>

#include "fst/lib/vector-fst.h"

DECLARE_bool(fst_default_cache_gc);
DECLARE_int64(fst_default_cache_gc_limit);

namespace fst {

struct CacheOptions {
  bool gc;          // enable GC
  size_t gc_limit;  // # of bytes allowed before GC


  CacheOptions(bool g, size_t l) : gc(g), gc_limit(l) {}
  CacheOptions()
      : gc(FLAGS_fst_default_cache_gc),
        gc_limit(FLAGS_fst_default_cache_gc_limit) {}
};


// This is a VectorFstBaseImpl container that holds a State similar to
// VectorState but additionally has a flags data member (see
// CacheState below). This class is used to cache FST elements with
// the flags used to indicate what has been cached. Use HasStart()
// HasFinal(), and HasArcs() to determine if cached and SetStart(),
// SetFinal(), AddArc(), and SetArcs() to cache. Note you must set the
// final weight even if the state is non-final to mark it as
// cached. If the 'gc' option is 'false', cached items have the extent
// of the FST - minimizing computation. If the 'gc' option is 'true',
// garbage collection of states (not in use in an arc iterator) is
// performed, in a rough approximation of LRU order, when 'gc_limit'
// bytes is reached - controlling memory use. When 'gc_limit' is 0,
// special optimizations apply - minimizing memory use.

template <class S>
class CacheBaseImpl : public VectorFstBaseImpl<S> {
 public:
  using FstImpl<typename S::Arc>::Type;
  using VectorFstBaseImpl<S>::NumStates;
  using VectorFstBaseImpl<S>::AddState;

  typedef S State;
  typedef typename S::Arc Arc;
  typedef typename Arc::Weight Weight;
  typedef typename Arc::StateId StateId;

  CacheBaseImpl()
      : cache_start_(false), nknown_states_(0), min_unexpanded_state_id_(0),
        cache_first_state_id_(kNoStateId), cache_first_state_(0),
        cache_gc_(FLAGS_fst_default_cache_gc),  cache_size_(0),
        cache_limit_(FLAGS_fst_default_cache_gc_limit > kMinCacheLimit ||
                     FLAGS_fst_default_cache_gc_limit == 0 ?
                     FLAGS_fst_default_cache_gc_limit : kMinCacheLimit) {}

  explicit CacheBaseImpl(const CacheOptions &opts)
      : cache_start_(false), nknown_states_(0),
        min_unexpanded_state_id_(0), cache_first_state_id_(kNoStateId),
        cache_first_state_(0), cache_gc_(opts.gc), cache_size_(0),
        cache_limit_(opts.gc_limit > kMinCacheLimit || opts.gc_limit == 0 ?
                     opts.gc_limit : kMinCacheLimit) {}

  ~CacheBaseImpl() {
    delete cache_first_state_;
  }

  // Gets a state from its ID; state must exist.
  const S *GetState(StateId s) const {
    if (s == cache_first_state_id_)
      return cache_first_state_;
    else
      return VectorFstBaseImpl<S>::GetState(s);
  }

  // Gets a state from its ID; state must exist.
  S *GetState(StateId s) {
    if (s == cache_first_state_id_)
      return cache_first_state_;
    else
      return VectorFstBaseImpl<S>::GetState(s);
  }

  // Gets a state from its ID; return 0 if it doesn't exist.
  const S *CheckState(StateId s) const {
    if (s == cache_first_state_id_)
      return cache_first_state_;
    else if (s < NumStates())
      return VectorFstBaseImpl<S>::GetState(s);
    else
      return 0;
  }

  // Gets a state from its ID; add it if necessary.
  S *ExtendState(StateId s) {
    if (s == cache_first_state_id_) {
      return cache_first_state_;                   // Return 1st cached state
    } else if (cache_limit_ == 0 && cache_first_state_id_ == kNoStateId) {
      cache_first_state_id_ = s;                   // Remember 1st cached state
      cache_first_state_ = new S;
      return cache_first_state_;
    } else if (cache_first_state_id_ != kNoStateId &&
               cache_first_state_->ref_count == 0) {
      cache_first_state_id_ = s;                   // Reuse 1st cached state
      cache_first_state_->Reset();
      return cache_first_state_;                   // Return 1st cached state
    } else {
      while (NumStates() <= s)                     // Add state to main cache
        AddState(0);
      if (!VectorFstBaseImpl<S>::GetState(s)) {
        this->SetState(s, new S);
        if (cache_first_state_id_ != kNoStateId) {  // Forget 1st cached state
          while (NumStates() <= cache_first_state_id_)
            AddState(0);
          this->SetState(cache_first_state_id_, cache_first_state_);
          if (cache_gc_) {
            cache_states_.push_back(cache_first_state_id_);
            cache_size_ += sizeof(S) +
                           cache_first_state_->arcs.capacity() * sizeof(Arc);
            cache_limit_ = kMinCacheLimit;
          }
          cache_first_state_id_ = kNoStateId;
          cache_first_state_ = 0;
        }
        if (cache_gc_) {
          cache_states_.push_back(s);
          cache_size_ += sizeof(S);
          if (cache_size_ > cache_limit_)
            GC(s, false);
        }
      }
      return VectorFstBaseImpl<S>::GetState(s);
    }
  }

  void SetStart(StateId s) {
    VectorFstBaseImpl<S>::SetStart(s);
    cache_start_ = true;
    if (s >= nknown_states_)
      nknown_states_ = s + 1;
  }

  void SetFinal(StateId s, Weight w) {
    S *state = ExtendState(s);
    state->final = w;
    state->flags |= kCacheFinal | kCacheRecent;
  }

  void AddArc(StateId s, const Arc &arc) {
    S *state = ExtendState(s);
    state->arcs.push_back(arc);
  }

  // Marks arcs of state s as cached.
  void SetArcs(StateId s) {
    S *state = ExtendState(s);
    vector<Arc> &arcs = state->arcs;
    state->niepsilons = state->noepsilons = 0;
    for (unsigned int a = 0; a < arcs.size(); ++a) { 
      const Arc &arc = arcs[a];
      if (arc.nextstate >= nknown_states_)
        nknown_states_ = arc.nextstate + 1;
      if (arc.ilabel == 0)
        ++state->niepsilons;
      if (arc.olabel == 0)
        ++state->noepsilons;
    }
    ExpandedState(s);
    state->flags |= kCacheArcs | kCacheRecent;
    if (cache_gc_ && s != cache_first_state_id_) {
      cache_size_ += arcs.capacity() * sizeof(Arc);
      if (cache_size_ > cache_limit_)
        GC(s, false);
    }
  };

  void ReserveArcs(StateId s, size_t n) {
    S *state = ExtendState(s);
    state->arcs.reserve(n);
  }

  // Is the start state cached?
  bool HasStart() const { return cache_start_; }
  // Is the final weight of state s cached?

  bool HasFinal(StateId s) const {
    const S *state = CheckState(s);
    if (state && state->flags & kCacheFinal) {
      state->flags |= kCacheRecent;
      return true;
    } else {
      return false;
    }
  }

  // Are arcs of state s cached?
  bool HasArcs(StateId s) const {
    const S *state = CheckState(s);
    if (state && state->flags & kCacheArcs) {
      state->flags |= kCacheRecent;
      return true;
    } else {
      return false;
    }
  }

  Weight Final(StateId s) const {
    const S *state = GetState(s);
    return state->final;
  }

  size_t NumArcs(StateId s) const {
    const S *state = GetState(s);
    return state->arcs.size();
  }

  size_t NumInputEpsilons(StateId s) const {
    const S *state = GetState(s);
    return state->niepsilons;
  }

  size_t NumOutputEpsilons(StateId s) const {
    const S *state = GetState(s);
    return state->noepsilons;
  }

  // Provides information needed for generic arc iterator.
  void InitArcIterator(StateId s, ArcIteratorData<Arc> *data) const {
    const S *state = GetState(s);
    data->base = 0;
    data->narcs = state->arcs.size();
    data->arcs = data->narcs > 0 ? &(state->arcs[0]) : 0;
    data->ref_count = &(state->ref_count);
    ++(*data->ref_count);
  }

  // Number of known states.
  StateId NumKnownStates() const { return nknown_states_; }
  // Find the mininum never-expanded state Id
  StateId MinUnexpandedState() const {
    while (min_unexpanded_state_id_ < (StateId)expanded_states_.size() && 
          expanded_states_[min_unexpanded_state_id_])
      ++min_unexpanded_state_id_;
    return min_unexpanded_state_id_;
  }

  // Removes from cache_states_ and uncaches (not referenced-counted)
  // states that have not been accessed since the last GC until
  // cache_limit_/3 bytes are uncached.  If that fails to free enough,
  // recurs uncaching recently visited states as well. If still
  // unable to free enough memory, then widens cache_limit_.
  void GC(StateId current, bool free_recent) {
    if (!cache_gc_)
      return;
    VLOG(2) << "CacheImpl: Enter GC: object = " << Type() << "(" << this
            << "), free recently cached = " << free_recent
            << ", cache size = " << cache_size_
            << ", cache limit = " << cache_limit_ << "\n";
    typename list<StateId>::iterator siter = cache_states_.begin();

    size_t cache_target = (2 * cache_limit_)/3 + 1;
    while (siter != cache_states_.end()) {
      StateId s = *siter;
      S* state = VectorFstBaseImpl<S>::GetState(s);
      if (cache_size_ > cache_target && state->ref_count == 0 &&
          (free_recent || !(state->flags & kCacheRecent)) && s != current) {
        cache_size_ -= sizeof(S) + state->arcs.capacity() * sizeof(Arc);
        delete state;
        this->SetState(s, 0);
        cache_states_.erase(siter++);
      } else {
        state->flags &= ~kCacheRecent;
        ++siter;
      }
    }
    if (!free_recent && cache_size_ > cache_target) {
      GC(current, true);
    } else {
      while (cache_size_ > cache_target) {
        cache_limit_ *= 2;
        cache_target *= 2;
      }
    }
    VLOG(2) << "CacheImpl: Exit GC: object = " << Type() << "(" << this
            << "), free recently cached = " << free_recent
            << ", cache size = " << cache_size_
            << ", cache limit = " << cache_limit_ << "\n";
  }

 private:
  static const uint32 kCacheFinal =  0x0001;  // Final weight has been cached
  static const uint32 kCacheArcs =   0x0002;  // Arcs have been cached
  static const uint32 kCacheRecent = 0x0004;  // Mark as visited since GC

  static const size_t kMinCacheLimit;         // Minimum (non-zero) cache limit

  void ExpandedState(StateId s) {
    if (s < min_unexpanded_state_id_)
      return;
    while ((StateId)expanded_states_.size() <= s) 
      expanded_states_.push_back(false);
    expanded_states_[s] = true;
  }

  bool cache_start_;                         // Is the start state cached?
  StateId nknown_states_;                    // # of known states
  vector<bool> expanded_states_;             // states that have been expanded
  mutable StateId min_unexpanded_state_id_;  // minimum never-expanded state Id
  StateId cache_first_state_id_;             // First cached state id
  S *cache_first_state_;                     // First cached state
  list<StateId> cache_states_;               // list of currently cached states
  bool cache_gc_;                            // enable GC
  size_t cache_size_;                        // # of bytes cached
  size_t cache_limit_;                       // # of bytes allowed before GC

  void InitStateIterator(StateIteratorData<Arc> *);  // disallow
  DISALLOW_EVIL_CONSTRUCTORS(CacheBaseImpl);
};

template <class S>
const size_t CacheBaseImpl<S>::kMinCacheLimit = 8096;


// Arcs implemented by an STL vector per state. Similar to VectorState
// but adds flags and ref count to keep track of what has been cached.
template <class A>
struct CacheState {
  typedef A Arc;
  typedef typename A::Weight Weight;
  typedef typename A::StateId StateId;

  CacheState() :  final(Weight::Zero()), flags(0), ref_count(0) {}

  void Reset() {
    flags = 0;
    ref_count = 0;
    arcs.resize(0);
  }

  Weight final;              // Final weight
  vector<A> arcs;            // Arcs represenation
  size_t niepsilons;         // # of input epsilons
  size_t noepsilons;         // # of output epsilons
  mutable uint32 flags;
  mutable int ref_count;
};

// A CacheBaseImpl with a commonly used CacheState.
template <class A>
class CacheImpl : public CacheBaseImpl< CacheState<A> > {
 public:
  typedef CacheState<A> State;

  CacheImpl() {}

  explicit CacheImpl(const CacheOptions &opts)
      : CacheBaseImpl< CacheState<A> >(opts) {}

 private:
  DISALLOW_EVIL_CONSTRUCTORS(CacheImpl);
};


// Use this to make a state iterator for a CacheBaseImpl-derived Fst.
// You'll need to make this class a friend of your derived Fst.
// Note this iterator only returns those states reachable from
// the initial state, so consider implementing a class-specific one.
template <class F>
class CacheStateIterator : public StateIteratorBase<typename F::Arc> {
 public:
  typedef typename F::Arc Arc;
  typedef typename Arc::StateId StateId;

  explicit CacheStateIterator(const F &fst) : fst_(fst), s_(0) {}

  virtual bool Done() const {
    if (s_ < fst_.impl_->NumKnownStates())
      return false;
    fst_.Start();  // force start state
    if (s_ < fst_.impl_->NumKnownStates())
      return false;
    for (int u = fst_.impl_->MinUnexpandedState();
         u < fst_.impl_->NumKnownStates();
         u = fst_.impl_->MinUnexpandedState()) {
      ArcIterator<F>(fst_, u);  // force state expansion
      if (s_ < fst_.impl_->NumKnownStates())
        return false;
    }
    return true;
  }

  virtual StateId Value() const { return s_; }

  virtual void Next() { ++s_; }

  virtual void Reset() { s_ = 0; }

 private:
  const F &fst_;
  StateId s_;
};


// Use this to make an arc iterator for a CacheBaseImpl-derived Fst.
// You'll need to make this class a friend of your derived Fst and
// define types Arc and State.
template <class F>
class CacheArcIterator {
 public:
  typedef typename F::Arc Arc;
  typedef typename F::State State;
  typedef typename Arc::StateId StateId;

  CacheArcIterator(const F &fst, StateId s) : i_(0) {
    state_ = fst.impl_->ExtendState(s);
    ++state_->ref_count;
  }

  ~CacheArcIterator() { --state_->ref_count;  }

  bool Done() const { return i_ >= state_->arcs.size(); }

  const Arc& Value() const { return state_->arcs[i_]; }

  void Next() { ++i_; }

  void Reset() { i_ = 0; }

  void Seek(size_t a) { i_ = a; }

 private:
  const State *state_;
  size_t i_;

  DISALLOW_EVIL_CONSTRUCTORS(CacheArcIterator);
};

}  // namespace fst

#endif  // FST_LIB_CACHE_H__
