// vector-fst.h
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
// Simple concrete, mutable FST whose states and arcs are stored in STL
// vectors.

#ifndef FST_LIB_VECTOR_FST_H__
#define FST_LIB_VECTOR_FST_H__

#include <string>
#include <string.h>

#include "fst/lib/mutable-fst.h"
#include "fst/lib/test-properties.h"

namespace fst {

template <class A> class VectorFst;

// States and arcs implemented by STL vectors, templated on the
// State definition. This does not manage the Fst properties.
template <class State>
class VectorFstBaseImpl : public FstImpl<typename State::Arc> {
 public:
  typedef typename State::Arc Arc;
  typedef typename Arc::Weight Weight;
  typedef typename Arc::StateId StateId;

  VectorFstBaseImpl() : start_(kNoStateId) {}

  ~VectorFstBaseImpl() {
    for (StateId s = 0; s < (StateId)states_.size(); ++s) 
      delete states_[s];
  }

  StateId Start() const { return start_; }

  Weight Final(StateId s) const { return states_[s]->final; }

  StateId NumStates() const { return states_.size(); }

  size_t NumArcs(StateId s) const { return states_[s]->arcs.size(); }

  void SetStart(StateId s) { start_ = s; }

  void SetFinal(StateId s, Weight w) { states_[s]->final = w; }

  StateId AddState() {
    states_.push_back(new State);
    return states_.size() - 1;
  }

  StateId AddState(State *state) {
    states_.push_back(state);
    return states_.size() - 1;
  }

  void AddArc(StateId s, const Arc &arc) {
    states_[s]->arcs.push_back(arc);
  }

  void DeleteStates(const vector<StateId>& dstates) {
    vector<StateId> newid(states_.size(), 0);
    for (size_t i = 0; i < dstates.size(); ++i)
      newid[dstates[i]] = kNoStateId;
    StateId nstates = 0;
    for (StateId s = 0; s < (StateId)states_.size(); ++s) {
      if (newid[s] != kNoStateId) {
        newid[s] = nstates;
        if (s != nstates)
          states_[nstates] = states_[s];
        ++nstates;
      } else {
        delete states_[s];
      }
    }
    states_.resize(nstates);
    for (StateId s = 0; s < (StateId)states_.size(); ++s) {
      vector<Arc> &arcs = states_[s]->arcs;
      size_t narcs = 0;
      for (size_t i = 0; i < arcs.size(); ++i) {
        StateId t = newid[arcs[i].nextstate];
        if (t != kNoStateId) {
          arcs[i].nextstate = t;
          if (i != narcs)
            arcs[narcs] = arcs[i];
          ++narcs;
        } else {
          if (arcs[i].ilabel == 0)
            --states_[s]->niepsilons;
          if (arcs[i].olabel == 0)
            --states_[s]->noepsilons;
        }
      }
      arcs.resize(narcs);
    }
    if (Start() != kNoStateId)
      SetStart(newid[Start()]);
  }

  void DeleteStates() {
    for (StateId s = 0; s < (StateId)states_.size(); ++s)
      delete states_[s];
    states_.clear();
    SetStart(kNoStateId);
  }

  void DeleteArcs(StateId s, size_t n) {
    states_[s]->arcs.resize(states_[s]->arcs.size() - n);
  }

  void DeleteArcs(StateId s) { states_[s]->arcs.clear(); }

  State *GetState(StateId s) { return states_[s]; }

  const State *GetState(StateId s) const { return states_[s]; }

  void SetState(StateId s, State *state) { states_[s] = state; }

  void ReserveStates(StateId n) { states_.reserve(n); }

  void ReserveArcs(StateId s, size_t n) { states_[s]->arcs.reserve(n); }

  // Provide information needed for generic state iterator
  void InitStateIterator(StateIteratorData<Arc> *data) const {
    data->base = 0;
    data->nstates = states_.size();
  }

  // Provide information needed for generic arc iterator
  void InitArcIterator(StateId s, ArcIteratorData<Arc> *data) const {
    data->base = 0;
    data->narcs = states_[s]->arcs.size();
    data->arcs = data->narcs > 0 ? &states_[s]->arcs[0] : 0;
    data->ref_count = 0;
  }

 private:
  vector<State *> states_;      // States represenation.
  StateId start_;               // initial state

  DISALLOW_EVIL_CONSTRUCTORS(VectorFstBaseImpl);
};

// Arcs implemented by an STL vector per state.
template <class A>
struct VectorState {
  typedef A Arc;
  typedef typename A::Weight Weight;
  typedef typename A::StateId StateId;

  VectorState() : final(Weight::Zero()), niepsilons(0), noepsilons(0) {}

  Weight final;              // Final weight
  vector<A> arcs;            // Arcs represenation
  size_t niepsilons;         // # of input epsilons
  size_t noepsilons;         // # of output epsilons
};

// This is a VectorFstBaseImpl container that holds VectorState's.  It
// manages Fst properties and the # of input and output epsilons.
template <class A>
class VectorFstImpl : public VectorFstBaseImpl< VectorState<A> > {
 public:
  using FstImpl<A>::SetType;
  using FstImpl<A>::SetProperties;
  using FstImpl<A>::Properties;
  using FstImpl<A>::WriteHeaderAndSymbols;

  using VectorFstBaseImpl<VectorState<A> >::Start;
  using VectorFstBaseImpl<VectorState<A> >::NumStates;

  friend class MutableArcIterator< VectorFst<A> >;

  typedef VectorFstBaseImpl< VectorState<A> > BaseImpl;
  typedef typename A::Weight Weight;
  typedef typename A::StateId StateId;

  VectorFstImpl() {
    SetType("vector");
    SetProperties(kNullProperties | kStaticProperties);
  }
  explicit VectorFstImpl(const Fst<A> &fst);

  static VectorFstImpl<A> *Read(istream &strm, const FstReadOptions &opts);

  size_t NumInputEpsilons(StateId s) const { return this->GetState(s)->niepsilons; }

  size_t NumOutputEpsilons(StateId s) const { return this->GetState(s)->noepsilons; }

  bool Write(ostream &strm, const FstWriteOptions &opts) const;

  void SetStart(StateId s) {
    BaseImpl::SetStart(s);
    SetProperties(Properties() & kSetStartProperties);
    if (Properties() & kAcyclic)
      SetProperties(Properties() | kInitialAcyclic);
  }

  void SetFinal(StateId s, Weight w) {
    Weight ow = this->Final(s);
    if (ow != Weight::Zero() && ow != Weight::One())
      SetProperties(Properties() & ~kWeighted);
    BaseImpl::SetFinal(s, w);
    if (w != Weight::Zero() && w != Weight::One()) {
      SetProperties(Properties() | kWeighted);
      SetProperties(Properties() & ~kUnweighted);
    }
    SetProperties(Properties() &
                  (kSetFinalProperties | kWeighted | kUnweighted));
  }

  StateId AddState() {
    StateId s = BaseImpl::AddState();
    SetProperties(Properties() & kAddStateProperties);
    return s;
  }

  void AddArc(StateId s, const A &arc) {
    VectorState<A> *state = this->GetState(s);
    if (arc.ilabel != arc.olabel) {
      SetProperties(Properties() | kNotAcceptor);
      SetProperties(Properties() & ~kAcceptor);
    }
    if (arc.ilabel == 0) {
      ++state->niepsilons;
      SetProperties(Properties() | kIEpsilons);
      SetProperties(Properties() & ~kNoIEpsilons);
      if (arc.olabel == 0) {
        SetProperties(Properties() | kEpsilons);
        SetProperties(Properties() & ~kNoEpsilons);
      }
    }
    if (arc.olabel == 0) {
      ++state->noepsilons;
      SetProperties(Properties() | kOEpsilons);
      SetProperties(Properties() & ~kNoOEpsilons);
    }
    if (!state->arcs.empty()) {
      A &parc = state->arcs.back();
      if (parc.ilabel > arc.ilabel) {
        SetProperties(Properties() | kNotILabelSorted);
        SetProperties(Properties() & ~kILabelSorted);
      }
      if (parc.olabel > arc.olabel) {
        SetProperties(Properties() | kNotOLabelSorted);
        SetProperties(Properties() & ~kOLabelSorted);
      }
    }
    if (arc.weight != Weight::Zero() && arc.weight != Weight::One()) {
      SetProperties(Properties() | kWeighted);
      SetProperties(Properties() & ~kUnweighted);
    }
    if (arc.nextstate <= s) {
      SetProperties(Properties() | kNotTopSorted);
      SetProperties(Properties() & ~kTopSorted);
    }
    SetProperties(Properties() &
                  (kAddArcProperties | kAcceptor |
                   kNoEpsilons | kNoIEpsilons | kNoOEpsilons |
                   kILabelSorted | kOLabelSorted | kUnweighted | kTopSorted));
    if (Properties() & kTopSorted)
      SetProperties(Properties() | kAcyclic | kInitialAcyclic);
    BaseImpl::AddArc(s, arc);
  }

  void DeleteStates(const vector<StateId> &dstates) {
    BaseImpl::DeleteStates(dstates);
    SetProperties(Properties() & kDeleteStatesProperties);
  }

  void DeleteStates() {
    BaseImpl::DeleteStates();
    SetProperties(kNullProperties | kStaticProperties);
  }

  void DeleteArcs(StateId s, size_t n) {
    const vector<A> &arcs = this->GetState(s)->arcs;
    for (size_t i = 0; i < n; ++i) {
      size_t j = arcs.size() - i - 1;
      if (arcs[j].ilabel == 0)
        --this->GetState(s)->niepsilons;
      if (arcs[j].olabel == 0)
        --this->GetState(s)->noepsilons;
    }
    BaseImpl::DeleteArcs(s, n);
    SetProperties(Properties() & kDeleteArcsProperties);
  }

  void DeleteArcs(StateId s) {
    this->GetState(s)->niepsilons = 0;
    this->GetState(s)->noepsilons = 0;
    BaseImpl::DeleteArcs(s);
    SetProperties(Properties() & kDeleteArcsProperties);
  }

 private:
  // Properties always true of this Fst class
  static const uint64 kStaticProperties = kExpanded | kMutable;
  // Current file format version
  static const int kFileVersion = 2;
  // Minimum file format version supported
  static const int kMinFileVersion = 2;

  DISALLOW_EVIL_CONSTRUCTORS(VectorFstImpl);
};

template <class A>
VectorFstImpl<A>::VectorFstImpl(const Fst<A> &fst) {
  SetType("vector");
  SetProperties(fst.Properties(kCopyProperties, false) | kStaticProperties);
  this->SetInputSymbols(fst.InputSymbols());
  this->SetOutputSymbols(fst.OutputSymbols());
  BaseImpl::SetStart(fst.Start());

  for (StateIterator< Fst<A> > siter(fst);
       !siter.Done();
       siter.Next()) {
    StateId s = siter.Value();
    BaseImpl::AddState();
    BaseImpl::SetFinal(s, fst.Final(s));
    this->ReserveArcs(s, fst.NumArcs(s));
    for (ArcIterator< Fst<A> > aiter(fst, s);
         !aiter.Done();
         aiter.Next()) {
      const A &arc = aiter.Value();
      BaseImpl::AddArc(s, arc);
      if (arc.ilabel == 0)
        ++this->GetState(s)->niepsilons;
      if (arc.olabel == 0)
        ++this->GetState(s)->noepsilons;
    }
  }
}

template <class A>
VectorFstImpl<A> *VectorFstImpl<A>::Read(istream &strm,
                                         const FstReadOptions &opts) {
  VectorFstImpl<A> *impl = new VectorFstImpl;
  FstHeader hdr;
  if (!impl->ReadHeaderAndSymbols(strm, opts, kMinFileVersion, &hdr))
    return 0;
  impl->BaseImpl::SetStart(hdr.Start());
  impl->ReserveStates(hdr.NumStates());

  for (StateId s = 0; s < hdr.NumStates(); ++s) {
    impl->BaseImpl::AddState();
    VectorState<A> *state = impl->GetState(s);
    state->final.Read(strm);
    int64 narcs;
    ReadType(strm, &narcs);
    if (!strm) {
      LOG(ERROR) << "VectorFst::Read: read failed: " << opts.source;
      return 0;
    }
    impl->ReserveArcs(s, narcs);
    for (size_t j = 0; j < narcs; ++j) {
      A arc;
      ReadType(strm, &arc.ilabel);
      ReadType(strm, &arc.olabel);
      arc.weight.Read(strm);
      ReadType(strm, &arc.nextstate);
      if (!strm) {
        LOG(ERROR) << "VectorFst::Read: read failed: " << opts.source;
        return 0;
      }
      impl->BaseImpl::AddArc(s, arc);
      if (arc.ilabel == 0)
        ++state->niepsilons;
      if (arc.olabel == 0)
        ++state->noepsilons;
    }
  }
  return impl;
}

// Converts a string into a weight.
template <class W> class WeightFromString {
 public:
  W operator()(const string &s);
};

// Generic case fails.
template <class W> inline
W WeightFromString<W>::operator()(const string &s) {
  LOG(FATAL) << "VectorFst::Read: Obsolete file format";
  return W();
}

// TropicalWeight version.
template <> inline
TropicalWeight WeightFromString<TropicalWeight>::operator()(const string &s) {
  float f;
  memcpy(&f, s.data(), sizeof(f));
  return TropicalWeight(f);
}

// LogWeight version.
template <> inline
LogWeight WeightFromString<LogWeight>::operator()(const string &s) {
  float f;
  memcpy(&f, s.data(), sizeof(f));
  return LogWeight(f);
}

template <class A>
bool VectorFstImpl<A>::Write(ostream &strm,
                             const FstWriteOptions &opts) const {
  FstHeader hdr;
  hdr.SetStart(Start());
  hdr.SetNumStates(NumStates());
  WriteHeaderAndSymbols(strm, opts, kFileVersion, &hdr);
  if (!strm)
    return false;

  for (StateId s = 0; s < NumStates(); ++s) {
    const VectorState<A> *state = this->GetState(s);
    state->final.Write(strm);
    int64 narcs = state->arcs.size();
    WriteType(strm, narcs);
    for (size_t a = 0; a < narcs; ++a) {
      const A &arc = state->arcs[a];
      WriteType(strm, arc.ilabel);
      WriteType(strm, arc.olabel);
      arc.weight.Write(strm);
      WriteType(strm, arc.nextstate);
    }
  }
  strm.flush();
  if (!strm)
    LOG(ERROR) << "VectorFst::Write: write failed: " << opts.source;
  return strm;
}

// Simple concrete, mutable FST. Supports additional operations:
// ReserveStates and ReserveArcs (cf. STL vectors). This class
// attaches interface to implementation and handles reference
// counting.
template <class A>
class VectorFst : public MutableFst<A> {
 public:
  friend class StateIterator< VectorFst<A> >;
  friend class ArcIterator< VectorFst<A> >;
  friend class MutableArcIterator< VectorFst<A> >;

  typedef A Arc;
  typedef typename A::Weight Weight;
  typedef typename A::StateId StateId;

  VectorFst() : impl_(new VectorFstImpl<A>) {}

  VectorFst(const VectorFst<A> &fst) : MutableFst<A>(fst), impl_(fst.impl_) {
    impl_->IncrRefCount();
  }
  explicit VectorFst(const Fst<A> &fst) : impl_(new VectorFstImpl<A>(fst)) {}

  virtual ~VectorFst() { if (!impl_->DecrRefCount()) delete impl_; }

  VectorFst<A> &operator=(const VectorFst<A> &fst) {
    if (this != &fst) {
      if (!impl_->DecrRefCount()) delete impl_;
      fst.impl_->IncrRefCount();
      impl_ = fst.impl_;
    }
    return *this;
  }

  virtual VectorFst<A> &operator=(const Fst<A> &fst) {
    if (this != &fst) {
      if (!impl_->DecrRefCount()) delete impl_;
      impl_ = new VectorFstImpl<A>(fst);
    }
    return *this;
  }

  virtual StateId Start() const { return impl_->Start(); }

  virtual Weight Final(StateId s) const { return impl_->Final(s); }

  virtual StateId NumStates() const { return impl_->NumStates(); }

  virtual size_t NumArcs(StateId s) const { return impl_->NumArcs(s); }

  virtual size_t NumInputEpsilons(StateId s) const {
    return impl_->NumInputEpsilons(s);
  }

  virtual size_t NumOutputEpsilons(StateId s) const {
    return impl_->NumOutputEpsilons(s);
  }

  virtual uint64 Properties(uint64 mask, bool test) const {
    if (test) {
      uint64 known, test = TestProperties(*this, mask, &known);
      impl_->SetProperties(test, known);
      return test & mask;
    } else {
      return impl_->Properties(mask);
    }
  }

  virtual const string& Type() const { return impl_->Type(); }

  // Get a copy of this VectorFst
  virtual VectorFst<A> *Copy() const {
    impl_->IncrRefCount();
    return new VectorFst<A>(impl_);
  }

  // Read a VectorFst from an input stream; return NULL on error
  static VectorFst<A> *Read(istream &strm, const FstReadOptions &opts) {
    VectorFstImpl<A>* impl = VectorFstImpl<A>::Read(strm, opts);
    return impl ? new VectorFst<A>(impl) : 0;
  }

  // Read a VectorFst from a file; return NULL on error
  static VectorFst<A> *Read(const string &filename) {
    ifstream strm(filename.c_str());
    if (!strm) {
      LOG(ERROR) << "VectorFst::Read: Can't open file: " << filename;
      return 0;
    }
    return Read(strm, FstReadOptions(filename));
  }

  // Write a VectorFst to an output stream; return false on error
  virtual bool Write(ostream &strm, const FstWriteOptions &opts) const {
    return impl_->Write(strm, opts);
  }

  // Write a VectorFst to a file; return false on error
  virtual bool Write(const string &filename) const {
    if (!filename.empty()) {
      ofstream strm(filename.c_str());
      if (!strm) {
        LOG(ERROR) << "VectorFst::Write: Can't open file: " << filename;
        return false;
      }
      return Write(strm, FstWriteOptions(filename));
    } else {
      return Write(std::cout, FstWriteOptions("standard output"));
    }
  }

  virtual SymbolTable* InputSymbols() {
    return impl_->InputSymbols();
  }

  virtual SymbolTable* OutputSymbols() {
    return impl_->OutputSymbols();
  }

  virtual const SymbolTable* InputSymbols() const {
    return impl_->InputSymbols();
  }

  virtual const SymbolTable* OutputSymbols() const {
    return impl_->OutputSymbols();
  }

  virtual void SetStart(StateId s) {
    MutateCheck();
    impl_->SetStart(s);
  }

  virtual void SetFinal(StateId s, Weight w) {
    MutateCheck();
    impl_->SetFinal(s, w);
  }

  virtual void SetProperties(uint64 props, uint64 mask) {
    impl_->SetProperties(props, mask);
  }

  virtual StateId AddState() {
    MutateCheck();
    return impl_->AddState();
  }

  virtual void AddArc(StateId s, const A &arc) {
    MutateCheck();
    impl_->AddArc(s, arc);
  }

  virtual void DeleteStates(const vector<StateId> &dstates) {
    MutateCheck();
    impl_->DeleteStates(dstates);
  }

  virtual void DeleteStates() {
    MutateCheck();
    impl_->DeleteStates();
  }

  virtual void DeleteArcs(StateId s, size_t n) {
    MutateCheck();
    impl_->DeleteArcs(s, n);
  }

  virtual void DeleteArcs(StateId s) {
    MutateCheck();
    impl_->DeleteArcs(s);
  }

  virtual void SetInputSymbols(const SymbolTable* isyms) {
    MutateCheck();
    impl_->SetInputSymbols(isyms);
  }

  virtual void SetOutputSymbols(const SymbolTable* osyms) {
    MutateCheck();
    impl_->SetOutputSymbols(osyms);
  }

  void ReserveStates(StateId n) {
    MutateCheck();
    impl_->ReserveStates(n);
  }

  void ReserveArcs(StateId s, size_t n) {
    MutateCheck();
    impl_->ReserveArcs(s, n);
  }

  virtual void InitStateIterator(StateIteratorData<A> *data) const {
    impl_->InitStateIterator(data);
  }

  virtual void InitArcIterator(StateId s, ArcIteratorData<A> *data) const {
    impl_->InitArcIterator(s, data);
  }

  virtual inline
  void InitMutableArcIterator(StateId s, MutableArcIteratorData<A> *);

 private:
  explicit VectorFst(VectorFstImpl<A> *impl) : impl_(impl) {}

  void MutateCheck() {
    // Copy on write
    if (impl_->RefCount() > 1) {
      impl_->DecrRefCount();
      impl_ = new VectorFstImpl<A>(*this);
    }
  }

  VectorFstImpl<A> *impl_;  // FST's impl
};

// Specialization for VectorFst; see generic version in fst.h
// for sample usage (but use the VectorFst type!). This version
// should inline.
template <class A>
class StateIterator< VectorFst<A> > {
 public:
  typedef typename A::StateId StateId;

  explicit StateIterator(const VectorFst<A> &fst)
    : nstates_(fst.impl_->NumStates()), s_(0) {}

  bool Done() const { return s_ >= nstates_; }

  StateId Value() const { return s_; }

  void Next() { ++s_; }

  void Reset() { s_ = 0; }

 private:
  StateId nstates_;
  StateId s_;

  DISALLOW_EVIL_CONSTRUCTORS(StateIterator);
};

// Specialization for VectorFst; see generic version in fst.h
// for sample usage (but use the VectorFst type!). This version
// should inline.
template <class A>
class ArcIterator< VectorFst<A> > {
 public:
  typedef typename A::StateId StateId;

  ArcIterator(const VectorFst<A> &fst, StateId s)
    : arcs_(fst.impl_->GetState(s)->arcs), i_(0) {}

  bool Done() const { return i_ >= arcs_.size(); }

  const A& Value() const { return arcs_[i_]; }

  void Next() { ++i_; }

  void Reset() { i_ = 0; }

  void Seek(size_t a) { i_ = a; }

 private:
  const vector<A>& arcs_;
  size_t i_;

  DISALLOW_EVIL_CONSTRUCTORS(ArcIterator);
};

// Specialization for VectorFst; see generic version in fst.h
// for sample usage (but use the VectorFst type!). This version
// should inline.
template <class A>
class MutableArcIterator< VectorFst<A> >
  : public MutableArcIteratorBase<A> {
 public:
  typedef typename A::StateId StateId;
  typedef typename A::Weight Weight;

  MutableArcIterator(VectorFst<A> *fst, StateId s) : i_(0) {
    fst->MutateCheck();
    state_ = fst->impl_->GetState(s);
    properties_ = &fst->impl_->properties_;
  }

  virtual bool Done() const { return i_ >= state_->arcs.size(); }

  virtual const A& Value() const { return state_->arcs[i_]; }

  virtual void Next() { ++i_; }

  virtual void Reset() { i_ = 0; }

  virtual void Seek(size_t a) { i_ = a; }

  virtual void SetValue(const A &arc) {
    A& oarc = state_->arcs[i_];
    if (oarc.ilabel != oarc.olabel)
      *properties_ &= ~kNotAcceptor;
    if (oarc.ilabel == 0) {
      --state_->niepsilons;
      *properties_ &= ~kIEpsilons;
      if (oarc.olabel == 0)
        *properties_ &= ~kEpsilons;
    }
    if (oarc.olabel == 0) {
      --state_->noepsilons;
      *properties_ &= ~kOEpsilons;
    }
    if (oarc.weight != Weight::Zero() && oarc.weight != Weight::One())
      *properties_ &= ~kWeighted;
    oarc = arc;
    if (arc.ilabel != arc.olabel)
      *properties_ |= kNotAcceptor;
    if (arc.ilabel == 0) {
      ++state_->niepsilons;
      *properties_ |= kIEpsilons;
      if (arc.olabel == 0)
        *properties_ |= kEpsilons;
    }
    if (arc.olabel == 0) {
      ++state_->noepsilons;
      *properties_ |= kOEpsilons;
    }
    if (arc.weight != Weight::Zero() && arc.weight != Weight::One())
      *properties_ |= kWeighted;
    *properties_ &= kSetArcProperties | kNotAcceptor |
                    kEpsilons | kIEpsilons | kOEpsilons | kWeighted;
  }

 private:
  struct VectorState<A> *state_;
  uint64 *properties_;
  size_t i_;

  DISALLOW_EVIL_CONSTRUCTORS(MutableArcIterator);
};

// Provide information needed for the generic mutable arc iterator
template <class A> inline
void VectorFst<A>::InitMutableArcIterator(
    StateId s, MutableArcIteratorData<A> *data) {
  data->base = new MutableArcIterator< VectorFst<A> >(this, s);
}

// A useful alias when using StdArc.
typedef VectorFst<StdArc> StdVectorFst;

}  // namespace fst

#endif  // FST_LIB_VECTOR_FST_H__
