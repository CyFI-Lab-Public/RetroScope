// relabel.h
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
// Functions and classes to relabel an Fst (either on input or output)
//
#ifndef FST_LIB_RELABEL_H__
#define FST_LIB_RELABEL_H__

#include <ext/hash_map>
using __gnu_cxx::hash_map;

#include "fst/lib/cache.h"
#include "fst/lib/test-properties.h"


namespace fst {

//
// Relabels either the input labels or output labels. The old to
// new labels are specified using a vector of pair<Label,Label>.
// Any label associations not specified are assumed to be identity
// mapping.
//
// \param fst input fst, must be mutable
// \param relabel_pairs vector of pairs indicating old to new mapping
// \param relabel_flags whether to relabel input or output
//
template <class A>
void Relabel(
    MutableFst<A> *fst,
    const vector<pair<typename A::Label, typename A::Label> >& ipairs,
    const vector<pair<typename A::Label, typename A::Label> >& opairs) {
  typedef typename A::StateId StateId;
  typedef typename A::Label   Label;

  uint64 props = fst->Properties(kFstProperties, false);

  // construct label to label hash. Could
  hash_map<Label, Label> input_map;
  for (size_t i = 0; i < ipairs.size(); ++i) {
    input_map[ipairs[i].first] = ipairs[i].second;
  }

  hash_map<Label, Label> output_map;
  for (size_t i = 0; i < opairs.size(); ++i) {
    output_map[opairs[i].first] = opairs[i].second;
  }

  for (StateIterator<MutableFst<A> > siter(*fst);
       !siter.Done(); siter.Next()) {
    StateId s = siter.Value();
    for (MutableArcIterator<MutableFst<A> > aiter(fst, s);
         !aiter.Done(); aiter.Next()) {
      A arc = aiter.Value();

      // relabel input
      // only relabel if relabel pair defined
      typename hash_map<Label, Label>::iterator it =
        input_map.find(arc.ilabel);
      if (it != input_map.end()) {arc.ilabel = it->second; }

      // relabel output
      it = output_map.find(arc.olabel);
      if (it != output_map.end()) { arc.olabel = it->second; }

      aiter.SetValue(arc);
    }
  }

  fst->SetProperties(RelabelProperties(props), kFstProperties);
}



//
// Relabels either the input labels or output labels. The old to
// new labels mappings are specified using an input Symbol set.
// Any label associations not specified are assumed to be identity
// mapping.
//
// \param fst input fst, must be mutable
// \param new_symbols symbol set indicating new mapping
// \param relabel_flags whether to relabel input or output
//
template<class A>
void Relabel(MutableFst<A> *fst,
             const SymbolTable* new_isymbols,
             const SymbolTable* new_osymbols) {
  typedef typename A::StateId StateId;
  typedef typename A::Label   Label;

  const SymbolTable* old_isymbols = fst->InputSymbols();
  const SymbolTable* old_osymbols = fst->OutputSymbols();

  vector<pair<Label, Label> > ipairs;
  if (old_isymbols && new_isymbols) {
    for (SymbolTableIterator syms_iter(*old_isymbols); !syms_iter.Done();
         syms_iter.Next()) {
      ipairs.push_back(make_pair(syms_iter.Value(),
                                 new_isymbols->Find(syms_iter.Symbol())));
    }
    fst->SetInputSymbols(new_isymbols);
  }

  vector<pair<Label, Label> > opairs;
  if (old_osymbols && new_osymbols) {
    for (SymbolTableIterator syms_iter(*old_osymbols); !syms_iter.Done();
         syms_iter.Next()) {
      opairs.push_back(make_pair(syms_iter.Value(),
                                 new_osymbols->Find(syms_iter.Symbol())));
    }
    fst->SetOutputSymbols(new_osymbols);
  }

  // call relabel using vector of relabel pairs.
  Relabel(fst, ipairs, opairs);
}


typedef CacheOptions RelabelFstOptions;

template <class A> class RelabelFst;

//
// \class RelabelFstImpl
// \brief Implementation for delayed relabeling
//
// Relabels an FST from one symbol set to another. Relabeling
// can either be on input or output space. RelabelFst implements
// a delayed version of the relabel. Arcs are relabeled on the fly
// and not cached. I.e each request is recomputed.
//
template<class A>
class RelabelFstImpl : public CacheImpl<A> {
  friend class StateIterator< RelabelFst<A> >;
 public:
  using FstImpl<A>::SetType;
  using FstImpl<A>::SetProperties;
  using FstImpl<A>::Properties;
  using FstImpl<A>::SetInputSymbols;
  using FstImpl<A>::SetOutputSymbols;

  using CacheImpl<A>::HasStart;
  using CacheImpl<A>::HasArcs;

  typedef typename A::Label   Label;
  typedef typename A::Weight  Weight;
  typedef typename A::StateId StateId;
  typedef CacheState<A> State;

  RelabelFstImpl(const Fst<A>& fst,
                 const vector<pair<Label, Label> >& ipairs,
                 const vector<pair<Label, Label> >& opairs,
                 const RelabelFstOptions &opts)
      : CacheImpl<A>(opts), fst_(fst.Copy()),
        relabel_input_(false), relabel_output_(false) {
    uint64 props = fst.Properties(kCopyProperties, false);
    SetProperties(RelabelProperties(props));
    SetType("relabel");

    // create input label map
    if (ipairs.size() > 0) {
      for (size_t i = 0; i < ipairs.size(); ++i) {
        input_map_[ipairs[i].first] = ipairs[i].second;
      }
      relabel_input_ = true;
    }

    // create output label map
    if (opairs.size() > 0) {
      for (size_t i = 0; i < opairs.size(); ++i) {
        output_map_[opairs[i].first] = opairs[i].second;
      }
      relabel_output_ = true;
    }
  }

  RelabelFstImpl(const Fst<A>& fst,
                 const SymbolTable* new_isymbols,
                 const SymbolTable* new_osymbols,
                 const RelabelFstOptions &opts)
      : CacheImpl<A>(opts), fst_(fst.Copy()),
        relabel_input_(false), relabel_output_(false) {
    SetType("relabel");

    uint64 props = fst.Properties(kCopyProperties, false);
    SetProperties(RelabelProperties(props));
    SetInputSymbols(fst.InputSymbols());
    SetOutputSymbols(fst.OutputSymbols());

    const SymbolTable* old_isymbols = fst.InputSymbols();
    const SymbolTable* old_osymbols = fst.OutputSymbols();

    if (old_isymbols && new_isymbols &&
        old_isymbols->CheckSum() != new_isymbols->CheckSum()) {
      for (SymbolTableIterator syms_iter(*old_isymbols); !syms_iter.Done();
           syms_iter.Next()) {
        input_map_[syms_iter.Value()] = new_isymbols->Find(syms_iter.Symbol());
      }
      SetInputSymbols(new_isymbols);
      relabel_input_ = true;
    }

    if (old_osymbols && new_osymbols &&
        old_osymbols->CheckSum() != new_osymbols->CheckSum()) {
      for (SymbolTableIterator syms_iter(*old_osymbols); !syms_iter.Done();
           syms_iter.Next()) {
        output_map_[syms_iter.Value()] =
          new_osymbols->Find(syms_iter.Symbol());
      }
      SetOutputSymbols(new_osymbols);
      relabel_output_ = true;
    }
  }

  ~RelabelFstImpl() { delete fst_; }

  StateId Start() {
    if (!HasStart()) {
      StateId s = fst_->Start();
      SetStart(s);
    }
    return CacheImpl<A>::Start();
  }

  Weight Final(StateId s) {
    if (!HasFinal(s)) {
      SetFinal(s, fst_->Final(s));
    }
    return CacheImpl<A>::Final(s);
  }

  size_t NumArcs(StateId s) {
    if (!HasArcs(s)) {
      Expand(s);
    }
    return CacheImpl<A>::NumArcs(s);
  }

  size_t NumInputEpsilons(StateId s) {
    if (!HasArcs(s)) {
      Expand(s);
    }
    return CacheImpl<A>::NumInputEpsilons(s);
  }

  size_t NumOutputEpsilons(StateId s) {
    if (!HasArcs(s)) {
      Expand(s);
    }
    return CacheImpl<A>::NumOutputEpsilons(s);
  }

  void InitArcIterator(StateId s, ArcIteratorData<A>* data) {
    if (!HasArcs(s)) {
      Expand(s);
    }
    CacheImpl<A>::InitArcIterator(s, data);
  }

  void Expand(StateId s) {
    for (ArcIterator<Fst<A> > aiter(*fst_, s); !aiter.Done(); aiter.Next()) {
      A arc = aiter.Value();

      // relabel input
      if (relabel_input_) {
        typename hash_map<Label, Label>::iterator it =
          input_map_.find(arc.ilabel);
        if (it != input_map_.end()) { arc.ilabel = it->second; }
      }

      // relabel output
      if (relabel_output_) {
        typename hash_map<Label, Label>::iterator it =
          output_map_.find(arc.olabel);
        if (it != output_map_.end()) { arc.olabel = it->second; }
      }

      AddArc(s, arc);
    }
    SetArcs(s);
  }


 private:
  const Fst<A> *fst_;

  hash_map<Label, Label> input_map_;
  hash_map<Label, Label> output_map_;
  bool relabel_input_;
  bool relabel_output_;

  DISALLOW_EVIL_CONSTRUCTORS(RelabelFstImpl);
};


//
// \class RelabelFst
// \brief Delayed implementation of arc relabeling
//
// This class attaches interface to implementation and handles
// reference counting.
template <class A>
class RelabelFst : public Fst<A> {
 public:
  friend class ArcIterator< RelabelFst<A> >;
  friend class StateIterator< RelabelFst<A> >;
  friend class CacheArcIterator< RelabelFst<A> >;

  typedef A Arc;
  typedef typename A::Label   Label;
  typedef typename A::Weight  Weight;
  typedef typename A::StateId StateId;
  typedef CacheState<A> State;

  RelabelFst(const Fst<A>& fst,
             const vector<pair<Label, Label> >& ipairs,
             const vector<pair<Label, Label> >& opairs) :
      impl_(new RelabelFstImpl<A>(fst, ipairs, opairs, RelabelFstOptions())) {}

  RelabelFst(const Fst<A>& fst,
             const vector<pair<Label, Label> >& ipairs,
             const vector<pair<Label, Label> >& opairs,
             const RelabelFstOptions &opts)
      : impl_(new RelabelFstImpl<A>(fst, ipairs, opairs, opts)) {}

  RelabelFst(const Fst<A>& fst,
             const SymbolTable* new_isymbols,
             const SymbolTable* new_osymbols) :
      impl_(new RelabelFstImpl<A>(fst, new_isymbols, new_osymbols,
                                  RelabelFstOptions())) {}

  RelabelFst(const Fst<A>& fst,
             const SymbolTable* new_isymbols,
             const SymbolTable* new_osymbols,
             const RelabelFstOptions &opts)
    : impl_(new RelabelFstImpl<A>(fst, new_isymbols, new_osymbols, opts)) {}

  RelabelFst(const RelabelFst<A> &fst) : impl_(fst.impl_) {
    impl_->IncrRefCount();
  }

  virtual ~RelabelFst() { if (!impl_->DecrRefCount()) delete impl_;  }

  virtual StateId Start() const { return impl_->Start(); }

  virtual Weight Final(StateId s) const { return impl_->Final(s); }

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

  virtual RelabelFst<A> *Copy() const {
    return new RelabelFst<A>(*this);
  }

  virtual const SymbolTable* InputSymbols() const {
    return impl_->InputSymbols();
  }

  virtual const SymbolTable* OutputSymbols() const {
    return impl_->OutputSymbols();
  }

  virtual void InitStateIterator(StateIteratorData<A> *data) const;

  virtual void InitArcIterator(StateId s, ArcIteratorData<A> *data) const {
    return impl_->InitArcIterator(s, data);
  }

 private:
  RelabelFstImpl<A> *impl_;

  void operator=(const RelabelFst<A> &fst);  // disallow
};

// Specialization for RelabelFst.
template<class A>
class StateIterator< RelabelFst<A> > : public StateIteratorBase<A> {
 public:
  typedef typename A::StateId StateId;

  explicit StateIterator(const RelabelFst<A> &fst)
      : impl_(fst.impl_), siter_(*impl_->fst_), s_(0) {}

  bool Done() const { return siter_.Done(); }

  StateId Value() const { return s_; }

  void Next() {
    if (!siter_.Done()) {
      ++s_;
      siter_.Next();
    }
  }

  void Reset() {
    s_ = 0;
    siter_.Reset();
  }

 private:
  const RelabelFstImpl<A> *impl_;
  StateIterator< Fst<A> > siter_;
  StateId s_;

  DISALLOW_EVIL_CONSTRUCTORS(StateIterator);
};


// Specialization for RelabelFst.
template <class A>
class ArcIterator< RelabelFst<A> >
    : public CacheArcIterator< RelabelFst<A> > {
 public:
  typedef typename A::StateId StateId;

  ArcIterator(const RelabelFst<A> &fst, StateId s)
      : CacheArcIterator< RelabelFst<A> >(fst, s) {
    if (!fst.impl_->HasArcs(s))
      fst.impl_->Expand(s);
  }

 private:
  DISALLOW_EVIL_CONSTRUCTORS(ArcIterator);
};

template <class A> inline
void RelabelFst<A>::InitStateIterator(StateIteratorData<A> *data) const {
  data->base = new StateIterator< RelabelFst<A> >(*this);
}

// Useful alias when using StdArc.
typedef RelabelFst<StdArc> StdRelabelFst;

}  // namespace fst

#endif  // FST_LIB_RELABEL_H__
