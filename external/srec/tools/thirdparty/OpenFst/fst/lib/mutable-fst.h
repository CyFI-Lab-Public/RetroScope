// mutable-fst.h
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
// Expanded FST augmented with mutators - interface class definition
// and mutable arc iterator interface.

#ifndef FST_LIB_MUTABLE_FST_H__
#define FST_LIB_MUTABLE_FST_H__

#include <vector>

#include "fst/lib/expanded-fst.h"

namespace fst {

template <class A> class MutableArcIteratorData;

// An expanded FST plus mutators (use MutableArcIterator to modify arcs).
template <class A>
class MutableFst : public ExpandedFst<A> {
 public:
  typedef A Arc;
  typedef typename A::Weight Weight;
  typedef typename A::StateId StateId;

  virtual MutableFst<A> &operator=(const Fst<A> &fst) = 0;

  virtual void SetStart(StateId) = 0;           // Set the initial state
  virtual void SetFinal(StateId, Weight) = 0;   // Set a state's final weight
  virtual void SetProperties(uint64 props,
                             uint64 mask) = 0;  // Set property bits wrt mask
  virtual StateId AddState() = 0;               // Add a state, return its ID
  virtual void AddArc(StateId, const A &arc) = 0;   // Add an arc to state

  virtual void DeleteStates(const vector<StateId>&) = 0;  // Delete some states
  virtual void DeleteStates() = 0;              // Delete all states
  virtual void DeleteArcs(StateId, size_t n) = 0;  // Delete some arcs at state
  virtual void DeleteArcs(StateId) = 0;         // Delete all arcs at state

  // Return input label symbol table; return NULL if not specified
  virtual const SymbolTable* InputSymbols() const = 0;
  // Return output label symbol table; return NULL if not specified
  virtual const SymbolTable* OutputSymbols() const = 0;

  // Return input label symbol table; return NULL if not specified
  virtual SymbolTable* InputSymbols() = 0;
  // Return output label symbol table; return NULL if not specified
  virtual SymbolTable* OutputSymbols() = 0;

  // Set input label symbol table; NULL signifies not unspecified
  virtual void SetInputSymbols(const SymbolTable* isyms) = 0;
  // Set output label symbol table; NULL signifies not unspecified
  virtual void SetOutputSymbols(const SymbolTable* osyms) = 0;

  // Get a copy of this MutableFst.
  virtual MutableFst<A> *Copy() const = 0;
  // Read an MutableFst from an input stream; return NULL on error.
  static MutableFst<A> *Read(istream &strm, const FstReadOptions &opts) {
    FstReadOptions ropts(opts);
    FstHeader hdr;
    if (ropts.header)
      hdr = *opts.header;
    else {
      if (!hdr.Read(strm, opts.source))
        return 0;
      ropts.header = &hdr;
    }
    if (!(hdr.Properties() & kMutable)) {
      LOG(ERROR) << "MutableFst::Read: Not an MutableFst: " << ropts.source;
      return 0;
    }
    FstRegister<A> *registr = FstRegister<A>::GetRegister();
    const typename FstRegister<A>::Reader reader =
      registr->GetReader(hdr.FstType());
    if (!reader) {
      LOG(ERROR) << "MutableFst::Read: Unknown FST type \"" << hdr.FstType()
                 << "\" (arc type = \"" << A::Type()
                 << "\"): " << ropts.source;
      return 0;
    }
    Fst<A> *fst = reader(strm, ropts);
    if (!fst) return 0;
    return down_cast<MutableFst<A> *>(fst);
  }
  // Read an MutableFst from a file; returns NULL on error.
  static MutableFst<A> *Read(const string &filename) {
    ifstream strm(filename.c_str());
    if (!strm) {
      LOG(ERROR) << "MutableFst::Read: Can't open file: " << filename;
      return 0;
    }
    return Read(strm, FstReadOptions(filename));
  }

  // For generic mutuble arc iterator construction; not normally called
  // directly by users.
  virtual void InitMutableArcIterator(StateId s,
                                      MutableArcIteratorData<A> *) = 0;
};

// Mutable arc iterator interface, templated on the Arc definition.
template <class A>
class MutableArcIteratorBase : public ArcIteratorBase<A> {
 public:
  typedef A Arc;
  virtual void SetValue(const A &arc) = 0;  // Set current arc's contents
};

template <class A>
struct MutableArcIteratorData {
  MutableArcIteratorBase<A> *base;  // Specific iterator
};

// Generic mutable arc iterator, templated on the FST definition
// - a wrapper around pointer to specific one.
// Here is a typical use: \code
//   for (MutableArcIterator<StdFst> aiter(&fst, s));
//        !aiter.Done();
//         aiter.Next()) {
//     StdArc arc = aiter.Value();
//     arc.ilabel = 7;
//     aiter.SetValue(arc);
//     ...
//   } \endcode
// This version requires function calls.
template <class F>
class MutableArcIterator : public MutableArcIteratorBase<typename F::Arc> {
 public:
  typedef typename F::Arc Arc;
  typedef typename Arc::StateId StateId;

  MutableArcIterator(F *fst, StateId s) {
    fst->InitMutableArcIterator(s, &data_);
  }
  ~MutableArcIterator() { delete data_.base; }

  bool Done() const { return data_.base->Done(); }
  const Arc& Value() const { return data_.base->Value(); }
  void Next() { data_.base->Next(); }
  void Reset() { data_.base->Reset(); }
  void Seek(size_t a) { data_.base->Seek(a); }
  void SetValue(const Arc &a) { data_.base->SetValue(a); }

 private:
  MutableArcIteratorData<Arc> data_;
  DISALLOW_EVIL_CONSTRUCTORS(MutableArcIterator);
};

// A useful alias when using StdArc.
typedef MutableFst<StdArc> StdMutableFst;

}  // namespace fst;

#endif  // FST_LIB_MUTABLE_FST_H__
