// const-fst.h
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
// Simple concrete immutable FST whose states and arcs are each stored
// in single arrays.

#ifndef FST_LIB_CONST_FST_H__
#define FST_LIB_CONST_FST_H__

#include "fst/lib/expanded-fst.h"
#include "fst/lib/test-properties.h"

namespace fst {

template <class A> class ConstFst;

// States and arcs each implemented by single arrays, templated on the
// Arc definition.
template <class A>
class ConstFstImpl : public FstImpl<A> {
 public:
  using FstImpl<A>::SetType;
  using FstImpl<A>::SetProperties;
  using FstImpl<A>::Properties;
  using FstImpl<A>::WriteHeaderAndSymbols;

  typedef typename A::Weight Weight;
  typedef typename A::StateId StateId;

  ConstFstImpl()
      : states_(0), arcs_(0), nstates_(0), narcs_(0), start_(kNoStateId) {
    SetType("const");
    SetProperties(kNullProperties | kStaticProperties);
  }

  explicit ConstFstImpl(const Fst<A> &fst);

  ~ConstFstImpl() {
    delete[] states_;
    delete[] arcs_;
  }

  StateId Start() const { return start_; }

  Weight Final(StateId s) const { return states_[s].final; }

  StateId NumStates() const { return nstates_; }

  size_t NumArcs(StateId s) const { return states_[s].narcs; }

  size_t NumInputEpsilons(StateId s) const { return states_[s].niepsilons; }

  size_t NumOutputEpsilons(StateId s) const { return states_[s].noepsilons; }

  static ConstFstImpl<A> *Read(istream &strm, const FstReadOptions &opts);

  bool Write(ostream &strm, const FstWriteOptions &opts) const;

  A *Arcs(StateId s) { return arcs_ + states_[s].pos; }

  // Provide information needed for generic state iterator
  void InitStateIterator(StateIteratorData<A> *data) const {
    data->base = 0;
    data->nstates = nstates_;
  }

  // Provide information needed for the generic arc iterator
  void InitArcIterator(StateId s, ArcIteratorData<A> *data) const {
    data->base = 0;
    data->arcs = arcs_ + states_[s].pos;
    data->narcs = states_[s].narcs;
    data->ref_count = 0;
  }

 private:
  // States implemented by array *states_ below, arcs by (single) *arcs_.
  struct State {
    Weight final;                // Final weight
    uint32 pos;                  // Start of state's arcs in *arcs_
    uint32 narcs;                // Number of arcs (per state)
    uint32 niepsilons;           // # of input epsilons
    uint32 noepsilons;           // # of output epsilons
    State() : final(Weight::Zero()), niepsilons(0), noepsilons(0) {}
  };

  // Properties always true of this Fst class
  static const uint64 kStaticProperties = kExpanded;
  // Current file format version
  static const int kFileVersion = 1;
  // Minimum file format version supported
  static const int kMinFileVersion = 1;
  // Byte alignment for states and arcs in file format
  static const int kFileAlign = 16;

  State *states_;                // States represenation
  A *arcs_;                      // Arcs representation
  StateId nstates_;              // Number of states
  size_t narcs_;                 // Number of arcs (per FST)
  StateId start_;                // Initial state

  DISALLOW_EVIL_CONSTRUCTORS(ConstFstImpl);
};

template<class A>
ConstFstImpl<A>::ConstFstImpl(const Fst<A> &fst) : nstates_(0), narcs_(0) {
  SetType("const");
  uint64 copy_properties = fst.Properties(kCopyProperties, true);
  SetProperties(copy_properties | kStaticProperties);
  this->SetInputSymbols(fst.InputSymbols());
  this->SetOutputSymbols(fst.OutputSymbols());
  start_ = fst.Start();

  // count # of states and arcs
  for (StateIterator< Fst<A> > siter(fst);
       !siter.Done();
       siter.Next()) {
    ++nstates_;
    StateId s = siter.Value();
    for (ArcIterator< Fst<A> > aiter(fst, s);
         !aiter.Done();
         aiter.Next())
      ++narcs_;
  }
  states_ = new State[nstates_];
  arcs_ = new A[narcs_];
  size_t pos = 0;
  for (StateId s = 0; s < nstates_; ++s) {
    states_[s].final = fst.Final(s);
    states_[s].pos = pos;
    states_[s].narcs = 0;
    states_[s].niepsilons = 0;
    states_[s].noepsilons = 0;
    for (ArcIterator< Fst<A> > aiter(fst, s);
         !aiter.Done();
         aiter.Next()) {
      const A &arc = aiter.Value();
      ++states_[s].narcs;
      if (arc.ilabel == 0)
        ++states_[s].niepsilons;
      if (arc.olabel == 0)
        ++states_[s].noepsilons;
      arcs_[pos++] = arc;
    }
  }
}

template<class A>
ConstFstImpl<A> *ConstFstImpl<A>::Read(istream &strm,
                                       const FstReadOptions &opts) {
  ConstFstImpl<A> *impl = new ConstFstImpl<A>;
  FstHeader hdr;
  if (!impl->ReadHeaderAndSymbols(strm, opts, kMinFileVersion, &hdr))
    return 0;
  impl->start_ = hdr.Start();
  impl->nstates_ = hdr.NumStates();
  impl->narcs_ = hdr.NumArcs();
  impl->states_ = new State[impl->nstates_];
  impl->arcs_ = new A[impl->narcs_];

  char c;
  for (int i = 0; i < kFileAlign && strm.tellg() % kFileAlign; ++i)
    strm.read(&c, 1);
  // TODO: memory map this
  size_t b = impl->nstates_ * sizeof(typename ConstFstImpl<A>::State);
  strm.read(reinterpret_cast<char *>(impl->states_), b);
  if (!strm) {
    LOG(ERROR) << "ConstFst::Read: Read failed: " << opts.source;
    return 0;
  }
  // TODO: memory map this
  b = impl->narcs_ * sizeof(A);
  for (int i = 0; i < kFileAlign && strm.tellg() % kFileAlign; ++i)
    strm.read(&c, 1);
  strm.read(reinterpret_cast<char *>(impl->arcs_), b);
  if (!strm) {
    LOG(ERROR) << "ConstFst::Read: Read failed: " << opts.source;
    return 0;
  }
  return impl;
}

template<class A>
bool ConstFstImpl<A>::Write(ostream &strm,
                            const FstWriteOptions &opts) const {
  FstHeader hdr;
  hdr.SetStart(start_);
  hdr.SetNumStates(nstates_);
  hdr.SetNumArcs(narcs_);
  WriteHeaderAndSymbols(strm, opts, kFileVersion, &hdr);
  if (!strm)
    return false;

  for (int i = 0; i < kFileAlign && strm.tellp() % kFileAlign; ++i)
    strm.write("", 1);
  strm.write(reinterpret_cast<char *>(states_),
             nstates_ * sizeof(State));
  for (int i = 0; i < kFileAlign && strm.tellp() % kFileAlign; ++i)
    strm.write("", 1);
  strm.write(reinterpret_cast<char *>(arcs_), narcs_ * sizeof(A));
  strm.flush();
  if (!strm)
    LOG(ERROR) << "ConstFst::Write: Write failed: " << opts.source;
  return strm;
}

// Simple concrete immutable FST.  This class attaches interface to
// implementation and handles reference counting.
template <class A>
class ConstFst : public ExpandedFst<A> {
 public:
  friend class StateIterator< ConstFst<A> >;
  friend class ArcIterator< ConstFst<A> >;

  typedef A Arc;
  typedef typename A::Weight Weight;
  typedef typename A::StateId StateId;

  ConstFst() : impl_(new ConstFstImpl<A>()) {}

  ConstFst(const ConstFst<A> &fst) : impl_(fst.impl_) {
    impl_->IncrRefCount();
  }

  explicit ConstFst(const Fst<A> &fst) : impl_(new ConstFstImpl<A>(fst)) {}

  virtual ~ConstFst() { if (!impl_->DecrRefCount()) delete impl_;  }

  virtual StateId Start() const { return impl_->Start(); }

  virtual Weight Final(StateId s) const { return impl_->Final(s); }

  StateId NumStates() const { return impl_->NumStates(); }

  size_t NumArcs(StateId s) const { return impl_->NumArcs(s); }

  size_t NumInputEpsilons(StateId s) const {
    return impl_->NumInputEpsilons(s);
  }

  size_t NumOutputEpsilons(StateId s) const {
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

  // Get a copy of this ConstFst
  virtual ConstFst<A> *Copy() const {
    impl_->IncrRefCount();
    return new ConstFst<A>(impl_);
  }

  // Read a ConstFst from an input stream; return NULL on error
  static ConstFst<A> *Read(istream &strm, const FstReadOptions &opts) {
    ConstFstImpl<A>* impl = ConstFstImpl<A>::Read(strm, opts);
    return impl ? new ConstFst<A>(impl) : 0;
  }

  // Read a ConstFst from a file; returno NULL on error
  static ConstFst<A> *Read(const string &filename) {
    ifstream strm(filename.c_str());
    if (!strm) {
      LOG(ERROR) << "ConstFst::Write: Can't open file: " << filename;
      return 0;
    }
    return Read(strm, FstReadOptions(filename));
  }

  // Write a ConstFst to an output stream; return false on error
  virtual bool Write(ostream &strm, const FstWriteOptions &opts) const {
    return impl_->Write(strm, opts);
  }

  // Write a ConstFst to a file; return false on error
  virtual bool Write(const string &filename) const {
    if (!filename.empty()) {
      ofstream strm(filename.c_str());
      if (!strm) {
        LOG(ERROR) << "ConstrFst::Write: Can't open file: " << filename;
        return false;
      }
      return Write(strm, FstWriteOptions(filename));
    } else {
      return Write(std::cout, FstWriteOptions("standard output"));
    }
  }

  virtual const SymbolTable* InputSymbols() const {
    return impl_->InputSymbols();
  }

  virtual const SymbolTable* OutputSymbols() const {
    return impl_->OutputSymbols();
  }

  virtual void InitStateIterator(StateIteratorData<A> *data) const {
    impl_->InitStateIterator(data);
  }

  virtual void InitArcIterator(StateId s, ArcIteratorData<A> *data) const {
    impl_->InitArcIterator(s, data);
  }

 private:
  ConstFst(ConstFstImpl<A> *impl) : impl_(impl) {}

  ConstFstImpl<A> *impl_;  // FST's impl

  void operator=(const ConstFst<A> &fst);  // disallow
};

// Specialization for ConstFst; see generic version in fst.h
// for sample usage (but use the ConstFst type!). This version
// should inline.
template <class A>
class StateIterator< ConstFst<A> > {
 public:
  typedef typename A::StateId StateId;

  explicit StateIterator(const ConstFst<A> &fst)
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

// Specialization for ConstFst; see generic version in fst.h
// for sample usage (but use the ConstFst type!). This version
// should inline.
template <class A>
class ArcIterator< ConstFst<A> > {
 public:
  typedef typename A::StateId StateId;

  ArcIterator(const ConstFst<A> &fst, StateId s)
    : arcs_(fst.impl_->Arcs(s)), narcs_(fst.impl_->NumArcs(s)), i_(0) {}

  bool Done() const { return i_ >= narcs_; }

  const A& Value() const { return arcs_[i_]; }

  void Next() { ++i_; }

  void Reset() { i_ = 0; }

  void Seek(size_t a) { i_ = a; }

 private:
  const A *arcs_;
  size_t narcs_;
  size_t i_;

  DISALLOW_EVIL_CONSTRUCTORS(ArcIterator);
};

// A useful alias when using StdArc.
typedef ConstFst<StdArc> StdConstFst;

}  // namespace fst;

#endif  // FST_LIB_CONST_FST_H__
