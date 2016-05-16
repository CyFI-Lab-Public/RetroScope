// map.h
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
// Class to map over/transform arcs e.g., change semirings or
// implement project/invert.

#ifndef FST_LIB_MAP_H__
#define FST_LIB_MAP_H__

#include "fst/lib/cache.h"
#include "fst/lib/mutable-fst.h"

namespace fst {

// This determines how final weights are mapped.
enum MapFinalAction {

  // A final weight is mapped into a final weight. An error
  // is raised if this is not possible.
  MAP_NO_SUPERFINAL,

  // A final weight is mapped to an arc to the superfinal state
  // when the result cannot be represented as a final weight.
  // The superfinal state will be added only if it is needed.
  MAP_ALLOW_SUPERFINAL,

  // A final weight is mapped to an arc to the superfinal state
  // unless the result can be represented as a final weight of weight
  // Zero(). The superfinal state is always added (if the input is
  // not the empty Fst).
  MAP_REQUIRE_SUPERFINAL
};

// Mapper Interface - class determinies how arcs and final weights
// are mapped.
//
// class Mapper {
//  public:
//   // Maps an arc type A to arc type B.
//   B operator()(const A &arc);
//   // Specifies final action the mapper requires (see above).
//   // The mapper will be passed final weights as arcs of the
//   // form A(0, 0, weight, kNoStateId).
//   MapFinalAction FinalAction() const;
//   // This specifies the known properties of an Fst mapped by this
//   // mapper. It takes as argument the input Fst's known properties.
//   uint64 Properties(uint64 props) const;
// }
//
// The Map functions and classes below will use the FinalAction()
// method of the mapper to determine how to treat final weights,
// e.g. whether to add a superfinal state. They will use the Properties()
// method to set the result Fst properties.
//
// We include a various map versions below. One dimension of
// variation is whether the mapping mutates its input, writes to a
// new result Fst, or is an on-the-fly Fst. Another dimension is how
// we pass the mapper. We allow passing the mapper by pointer
// for cases that we need to change the state of the user's mapper.
// This is the case with the encode mapper, which is reused during
// decoding. We also include map versions that pass the mapper
// by value or const reference when this suffices.


// Maps an arc type A using a mapper function object C, passed
// by pointer.  This version modifies its Fst input.
template<class A, class C>
void Map(MutableFst<A> *fst, C* mapper) {
  typedef typename A::StateId StateId;
  typedef typename A::Weight Weight;

  if (fst->Start() == kNoStateId)
    return;

  uint64 props = fst->Properties(kFstProperties, false);

  MapFinalAction final_action = mapper->FinalAction();
  StateId superfinal = kNoStateId;
  if (final_action == MAP_REQUIRE_SUPERFINAL) {
    superfinal = fst->AddState();
    fst->SetFinal(superfinal, Weight::One());
  }

  for (StateId s = 0; s < fst->NumStates(); ++s) {
    for (MutableArcIterator< MutableFst<A> > aiter(fst, s);
         !aiter.Done(); aiter.Next()) {
      const A &arc = aiter.Value();
      aiter.SetValue((*mapper)(arc));
    }

    switch (final_action) {
      case MAP_NO_SUPERFINAL:
      default: {
        A final_arc = (*mapper)(A(0, 0, fst->Final(s), kNoStateId));
        CHECK(final_arc.ilabel == 0 && final_arc.olabel == 0);
        fst->SetFinal(s, final_arc.weight);
        break;
      }
      case MAP_ALLOW_SUPERFINAL: {
        if (s != superfinal) {
          A final_arc = (*mapper)(A(0, 0, fst->Final(s), kNoStateId));
          if (final_arc.ilabel != 0 || final_arc.olabel != 0) {
            // Add a superfinal state if not already done.
            if (superfinal == kNoStateId) {
              superfinal = fst->AddState();
              fst->SetFinal(superfinal, Weight::One());
            }
            final_arc.nextstate = superfinal;
            fst->AddArc(s, final_arc);
            fst->SetFinal(s, Weight::Zero());
          } else {
            fst->SetFinal(s, final_arc.weight);
          }
          break;
        }
      }
      case MAP_REQUIRE_SUPERFINAL: {
        if (s != superfinal) {
          A final_arc = (*mapper)(A(0, 0, fst->Final(s), kNoStateId));
          if (final_arc.ilabel != 0 || final_arc.olabel != 0 ||
              final_arc.weight != Weight::Zero())
            fst->AddArc(s, A(final_arc.ilabel, final_arc.olabel,
                             final_arc.weight, superfinal));
            fst->SetFinal(s, Weight::Zero());
        }
        break;
      }
    }
  }
  fst->SetProperties(mapper->Properties(props), kFstProperties);
}

// Maps an arc type A using a mapper function object C, passed
// by value.  This version modifies its Fst input.
template<class A, class C>
void Map(MutableFst<A> *fst, C mapper) {
  Map(fst, &mapper);
}


// Maps an arc type A to an arc type B using mapper function
// object C, passed by pointer. This version writes the mapped
// input Fst to an output MutableFst.
template<class A, class B, class C>
void Map(const Fst<A> &ifst, MutableFst<B> *ofst, C* mapper) {
  typedef typename A::StateId StateId;
  typedef typename A::Weight Weight;

  ofst->DeleteStates();
  ofst->SetInputSymbols(ifst.InputSymbols());
  ofst->SetOutputSymbols(ifst.OutputSymbols());

  if (ifst.Start() == kNoStateId)
    return;

  // Add all states.
  for (StateIterator< Fst<A> > siter(ifst); !siter.Done(); siter.Next())
    ofst->AddState();

  MapFinalAction final_action = mapper->FinalAction();
  StateId superfinal = kNoStateId;
  if (final_action == MAP_REQUIRE_SUPERFINAL) {
    superfinal = ofst->AddState();
    ofst->SetFinal(superfinal, B::Weight::One());
  }
  for (StateIterator< Fst<A> > siter(ifst); !siter.Done(); siter.Next()) {
    StateId s = siter.Value();
    if (s == ifst.Start())
      ofst->SetStart(s);

    for (ArcIterator< Fst<A> > aiter(ifst, s); !aiter.Done(); aiter.Next())
      ofst->AddArc(s, (*mapper)(aiter.Value()));

    switch (final_action) {
      case MAP_NO_SUPERFINAL:
      default: {
        B final_arc = (*mapper)(A(0, 0, ifst.Final(s), kNoStateId));
        CHECK(final_arc.ilabel == 0 && final_arc.olabel == 0);
        ofst->SetFinal(s, final_arc.weight);
        break;
      }
      case MAP_ALLOW_SUPERFINAL: {
        B final_arc = (*mapper)(A(0, 0, ifst.Final(s), kNoStateId));
        if (final_arc.ilabel != 0 || final_arc.olabel != 0) {
            // Add a superfinal state if not already done.
          if (superfinal == kNoStateId) {
            superfinal = ofst->AddState();
            ofst->SetFinal(superfinal, B::Weight::One());
          }
          final_arc.nextstate = superfinal;
          ofst->AddArc(s, final_arc);
          ofst->SetFinal(s, B::Weight::Zero());
        } else {
          ofst->SetFinal(s, final_arc.weight);
        }
        break;
      }
      case MAP_REQUIRE_SUPERFINAL: {
        B final_arc = (*mapper)(A(0, 0, ifst.Final(s), kNoStateId));
        if (final_arc.ilabel != 0 || final_arc.olabel != 0 ||
            final_arc.weight != B::Weight::Zero())
          ofst->AddArc(s, B(final_arc.ilabel, final_arc.olabel,
                            final_arc.weight, superfinal));
        ofst->SetFinal(s, B::Weight::Zero());
        break;
      }
    }
  }
  uint64 iprops = ifst.Properties(kCopyProperties, false);
  uint64 oprops = ofst->Properties(kFstProperties, false);
  ofst->SetProperties(mapper->Properties(iprops) | oprops, kFstProperties);
}

// Maps an arc type A to an arc type B using mapper function
// object C, passed by value. This version writes the mapped input
// Fst to an output MutableFst.
template<class A, class B, class C>
void Map(const Fst<A> &ifst, MutableFst<B> *ofst, C mapper) {
  Map(ifst, ofst, &mapper);
}


struct MapFstOptions : public CacheOptions {
  // MapFst default caching behaviour is to do no caching. Most
  // mappers are cheap and therefore we save memory by not doing
  // caching.
  MapFstOptions() : CacheOptions(true, 0) {}
  MapFstOptions(const CacheOptions& opts) : CacheOptions(opts) {}
};


template <class A, class B, class C> class MapFst;

// Implementation of delayed MapFst.
template <class A, class B, class C>
class MapFstImpl : public CacheImpl<B> {
 public:
  using FstImpl<B>::SetType;
  using FstImpl<B>::SetProperties;
  using FstImpl<B>::Properties;
  using FstImpl<B>::SetInputSymbols;
  using FstImpl<B>::SetOutputSymbols;

  using VectorFstBaseImpl<typename CacheImpl<B>::State>::NumStates;

  using CacheImpl<B>::HasArcs;
  using CacheImpl<B>::HasFinal;
  using CacheImpl<B>::HasStart;

  friend class StateIterator< MapFst<A, B, C> >;

  typedef B Arc;
  typedef typename B::Weight Weight;
  typedef typename B::StateId StateId;

  MapFstImpl(const Fst<A> &fst, const C &mapper,
                 const MapFstOptions& opts)
      : CacheImpl<B>(opts), fst_(fst.Copy()),
        mapper_(new C(mapper)),
        own_mapper_(true),
        superfinal_(kNoStateId),
        nstates_(0) {
    Init();
  }

  MapFstImpl(const Fst<A> &fst, C *mapper,
                 const MapFstOptions& opts)
      : CacheImpl<B>(opts), fst_(fst.Copy()),
        mapper_(mapper),
        own_mapper_(false),
        superfinal_(kNoStateId),
        nstates_(0) {
    Init();
  }


  ~MapFstImpl() {
    delete fst_;
    if (own_mapper_) delete mapper_;
  }

  StateId Start() {
    if (!HasStart())
      this->SetStart(FindOState(fst_->Start()));
    return CacheImpl<B>::Start();
  }

  Weight Final(StateId s) {
    if (!HasFinal(s)) {
      switch (final_action_) {
        case MAP_NO_SUPERFINAL:
        default: {
          B final_arc = (*mapper_)(A(0, 0, fst_->Final(FindIState(s)),
                                        kNoStateId));
          CHECK(final_arc.ilabel == 0 && final_arc.olabel == 0);
          this->SetFinal(s, final_arc.weight);
          break;
        }
        case MAP_ALLOW_SUPERFINAL: {
          if (s == superfinal_) {
            this->SetFinal(s, Weight::One());
          } else {
            B final_arc = (*mapper_)(A(0, 0, fst_->Final(FindIState(s)),
                                          kNoStateId));
            if (final_arc.ilabel == 0 && final_arc.olabel == 0)
              this->SetFinal(s, final_arc.weight);
            else
              this->SetFinal(s, Weight::Zero());
          }
          break;
        }
        case MAP_REQUIRE_SUPERFINAL: {
          this->SetFinal(s, s == superfinal_ ? Weight::One() : Weight::Zero());
          break;
        }
      }
    }
    return CacheImpl<B>::Final(s);
  }

  size_t NumArcs(StateId s) {
    if (!HasArcs(s))
      Expand(s);
    return CacheImpl<B>::NumArcs(s);
  }

  size_t NumInputEpsilons(StateId s) {
    if (!HasArcs(s))
      Expand(s);
    return CacheImpl<B>::NumInputEpsilons(s);
  }

  size_t NumOutputEpsilons(StateId s) {
    if (!HasArcs(s))
      Expand(s);
    return CacheImpl<B>::NumOutputEpsilons(s);
  }

  void InitArcIterator(StateId s, ArcIteratorData<B> *data) {
    if (!HasArcs(s))
      Expand(s);
    CacheImpl<B>::InitArcIterator(s, data);
  }

  void Expand(StateId s) {
    // Add exiting arcs.
    if (s == superfinal_) { this->SetArcs(s); return; }

    for (ArcIterator< Fst<A> > aiter(*fst_, FindIState(s));
         !aiter.Done(); aiter.Next()) {
      A aarc(aiter.Value());
      aarc.nextstate = FindOState(aarc.nextstate);
      const B& barc = (*mapper_)(aarc);
      this->AddArc(s, barc);
    }

    // Check for superfinal arcs.
    if (!HasFinal(s) || Final(s) == Weight::Zero())
      switch (final_action_) {
        case MAP_NO_SUPERFINAL:
        default:
          break;
        case MAP_ALLOW_SUPERFINAL: {
          B final_arc = (*mapper_)(A(0, 0, fst_->Final(FindIState(s)),
                                        kNoStateId));
          if (final_arc.ilabel != 0 || final_arc.olabel != 0) {
            if (superfinal_ == kNoStateId)
              superfinal_ = nstates_++;
            final_arc.nextstate = superfinal_;
            this->AddArc(s, final_arc);
          }
          break;
        }
      case MAP_REQUIRE_SUPERFINAL: {
        B final_arc = (*mapper_)(A(0, 0, fst_->Final(FindIState(s)),
                                      kNoStateId));
        if (final_arc.ilabel != 0 || final_arc.olabel != 0 ||
            final_arc.weight != B::Weight::Zero())
          this->AddArc(s, B(final_arc.ilabel, final_arc.olabel,
                      final_arc.weight, superfinal_));
        break;
      }
    }
    this->SetArcs(s);
  }

 private:
  void Init() {
    SetType("map");
    SetInputSymbols(fst_->InputSymbols());
    SetOutputSymbols(fst_->OutputSymbols());
    if (fst_->Start() == kNoStateId) {
      final_action_ = MAP_NO_SUPERFINAL;
      SetProperties(kNullProperties);
    } else {
      final_action_ = mapper_->FinalAction();
      uint64 props = fst_->Properties(kCopyProperties, false);
      SetProperties(mapper_->Properties(props));
      if (final_action_ == MAP_REQUIRE_SUPERFINAL)
        superfinal_ = 0;
    }
  }

  // Maps from output state to input state.
  StateId FindIState(StateId s) {
    if (superfinal_ == kNoStateId || s < superfinal_)
      return s;
    else
      return s - 1;
  }

  // Maps from input state to output state.
  StateId FindOState(StateId is) {
    StateId os;
    if (superfinal_ == kNoStateId || is < superfinal_)
      os = is;
    else
      os = is + 1;

    if (os >= nstates_)
      nstates_ = os + 1;

    return os;
  }


  const Fst<A> *fst_;
  C*   mapper_;
  bool own_mapper_;
  MapFinalAction final_action_;

  StateId superfinal_;
  StateId nstates_;
};


// Maps an arc type A to an arc type B using Mapper function object
// C. This version is a delayed Fst.
template <class A, class B, class C>
class MapFst : public Fst<B> {
 public:
  friend class ArcIterator< MapFst<A, B, C> >;
  friend class StateIterator< MapFst<A, B, C> >;
  friend class CacheArcIterator< MapFst<A, B, C> >;

  typedef B Arc;
  typedef typename B::Weight Weight;
  typedef typename B::StateId StateId;
  typedef CacheState<B> State;

  MapFst(const Fst<A> &fst, const C &mapper,
             const MapFstOptions& opts)
      : impl_(new MapFstImpl<A, B, C>(fst, mapper, opts)) {}

  MapFst(const Fst<A> &fst, C* mapper,
             const MapFstOptions& opts)
      : impl_(new MapFstImpl<A, B, C>(fst, mapper, opts)) {}

  MapFst(const Fst<A> &fst, const C &mapper)
      : impl_(new MapFstImpl<A, B, C>(fst, mapper,
                                          MapFstOptions())) {}

  MapFst(const Fst<A> &fst, C* mapper)
      : impl_(new MapFstImpl<A, B, C>(fst, mapper,
                                          MapFstOptions())) {}

  MapFst(const MapFst<A, B, C> &fst) : Fst<B>(fst), impl_(fst.impl_) {
    impl_->IncrRefCount();
  }

  virtual ~MapFst() { if (!impl_->DecrRefCount()) delete impl_;  }

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

  virtual MapFst<A, B, C> *Copy() const {
    return new MapFst<A, B, C>(*this);
  }

  virtual const SymbolTable* InputSymbols() const {
    return impl_->InputSymbols();
  }

  virtual const SymbolTable* OutputSymbols() const {
    return impl_->OutputSymbols();
  }

 virtual inline void InitStateIterator(StateIteratorData<B> *data) const;

  virtual void InitArcIterator(StateId s, ArcIteratorData<B> *data) const {
    impl_->InitArcIterator(s, data);
  }

 private:
  MapFstImpl<A, B, C> *impl_;

  void operator=(const MapFst<A, B, C> &fst);  // disallow
};


// Specialization for MapFst.
template<class A, class B, class C>
class StateIterator< MapFst<A, B, C> > : public StateIteratorBase<B> {
 public:
  typedef typename B::StateId StateId;

  explicit StateIterator(const MapFst<A, B, C> &fst)
      : impl_(fst.impl_), siter_(*impl_->fst_), s_(0),
        superfinal_(impl_->final_action_ == MAP_REQUIRE_SUPERFINAL)
  { CheckSuperfinal(); }

  bool Done() const { return siter_.Done() && !superfinal_; }

  StateId Value() const { return s_; }

  void Next() {
    ++s_;
    if (!siter_.Done()) {
      siter_.Next();
      CheckSuperfinal();
    }
    else if (superfinal_)
      superfinal_ = false;
  }

  void Reset() {
    s_ = 0;
    siter_.Reset();
    superfinal_ = impl_->final_action_ == MAP_REQUIRE_SUPERFINAL;
    CheckSuperfinal();
  }

 private:
  void CheckSuperfinal() {
    if (impl_->final_action_ != MAP_ALLOW_SUPERFINAL || superfinal_)
      return;
    if (!siter_.Done()) {
      B final_arc = (*impl_->mapper_)(A(0, 0, impl_->fst_->Final(s_),
                                           kNoStateId));
      if (final_arc.ilabel != 0 || final_arc.olabel != 0)
        superfinal_ = true;
    }
  }

  const MapFstImpl<A, B, C> *impl_;
  StateIterator< Fst<A> > siter_;
  StateId s_;
  bool superfinal_;    // true if there is a superfinal state and not done

  DISALLOW_EVIL_CONSTRUCTORS(StateIterator);
};

// Specialization for MapFst.
template <class A, class B, class C>
class ArcIterator< MapFst<A, B, C> >
    : public CacheArcIterator< MapFst<A, B, C> > {
 public:
  typedef typename A::StateId StateId;

  ArcIterator(const MapFst<A, B, C> &fst, StateId s)
      : CacheArcIterator< MapFst<A, B, C> >(fst, s) {
    if (!fst.impl_->HasArcs(s))
      fst.impl_->Expand(s);
  }

 private:
  DISALLOW_EVIL_CONSTRUCTORS(ArcIterator);
};

template <class A, class B, class C> inline
void MapFst<A, B, C>::InitStateIterator(StateIteratorData<B> *data)
    const {
  data->base = new StateIterator< MapFst<A, B, C> >(*this);
}


//
// Utility Mappers
//

// Mapper that returns its input.
template <class A>
struct IdentityMapper {
  typedef A FromArc;
  typedef A ToArc;

  A operator()(const A &arc) const { return arc; }

  MapFinalAction FinalAction() const { return MAP_NO_SUPERFINAL; }

  uint64 Properties(uint64 props) const { return props; }
};


// Mapper that returns its input with final states redirected to
// a single super-final state.
template <class A>
struct SuperFinalMapper {
  typedef A FromArc;
  typedef A ToArc;

  A operator()(const A &arc) const { return arc; }

  MapFinalAction FinalAction() const { return MAP_REQUIRE_SUPERFINAL; }

  uint64 Properties(uint64 props) const {
    return props & kAddSuperFinalProperties;
  }
};


// Mapper from StdArc to LogArc.
struct StdToLogMapper {
  typedef StdArc FromArc;
  typedef LogArc ToArc;

  LogArc operator()(const StdArc &arc) const {
    return LogArc(arc.ilabel, arc.olabel, arc.weight.Value(), arc.nextstate);
  }

  MapFinalAction FinalAction() const { return MAP_NO_SUPERFINAL; }

  uint64 Properties(uint64 props) const { return props; }
};


// Mapper from LogArc to StdArc.
struct LogToStdMapper {
  typedef LogArc FromArc;
  typedef StdArc ToArc;

  StdArc operator()(const LogArc &arc) const {
    return StdArc(arc.ilabel, arc.olabel, arc.weight.Value(), arc.nextstate);
  }

  MapFinalAction FinalAction() const { return MAP_NO_SUPERFINAL; }

  uint64 Properties(uint64 props) const { return props; }
};


// Mapper from A to GallicArc<A>.
template <class A, StringType S = STRING_LEFT>
struct ToGallicMapper {
  typedef A FromArc;
  typedef GallicArc<A, S> ToArc;

  typedef StringWeight<typename A::Label, S> SW;
  typedef typename A::Weight AW;
  typedef typename GallicArc<A, S>::Weight GW;

  ToArc operator()(const A &arc) const {
    // 'Super-final' arc.
    if (arc.nextstate == kNoStateId && arc.weight != AW::Zero())
      return ToArc(0, 0, GW(SW::One(), arc.weight), kNoStateId);
    // 'Super-non-final' arc.
    else if (arc.nextstate == kNoStateId)
      return ToArc(0, 0, GW(SW::Zero(), arc.weight), kNoStateId);
    // Epsilon label.
    else if (arc.olabel == 0)
      return ToArc(arc.ilabel, arc.ilabel,
                   GW(SW::One(), arc.weight), arc.nextstate);
    // Regular label.
    else
      return ToArc(arc.ilabel, arc.ilabel,
                   GW(SW(arc.olabel), arc.weight), arc.nextstate);
  }

  MapFinalAction FinalAction() const { return MAP_NO_SUPERFINAL; }

  uint64 Properties(uint64 props) const {
    return ProjectProperties(props, true) & kWeightInvariantProperties;
  }
};


// Mapper from GallicArc<A> to A.
template <class A, StringType S = STRING_LEFT>
struct FromGallicMapper {
  typedef GallicArc<A, S> FromArc;
  typedef A ToArc;

  typedef typename A::Label Label;
  typedef StringWeight<Label, S> SW;
  typedef typename A::Weight AW;
  typedef typename GallicArc<A, S>::Weight GW;

  A operator()(const FromArc &arc) const {
    // 'Super-non-final' arc.
    if (arc.nextstate == kNoStateId && arc.weight == GW::Zero())
      return A(arc.ilabel, 0, AW::Zero(), kNoStateId);

    SW w1 = arc.weight.Value1();
    AW w2 = arc.weight.Value2();
    StringWeightIterator<Label, S> iter1(w1);

    Label l = w1.Size() == 1 ? iter1.Value() : 0;

    CHECK(l != kStringInfinity);
    CHECK(l != kStringBad);
    CHECK(arc.ilabel == arc.olabel);
    CHECK(w1.Size() <= 1);

    return A(arc.ilabel, l, w2, arc.nextstate);
  }

  MapFinalAction FinalAction() const { return MAP_ALLOW_SUPERFINAL; }

  uint64 Properties(uint64 props) const {
    return props & kOLabelInvariantProperties &
      kWeightInvariantProperties & kAddSuperFinalProperties;
  }
};


// Mapper from GallicArc<A> to A.
template <class A, StringType S = STRING_LEFT>
struct GallicToNewSymbolsMapper {
  typedef GallicArc<A, S> FromArc;
  typedef A ToArc;

  typedef typename A::StateId StateId;
  typedef typename A::Label Label;
  typedef StringWeight<Label, S> SW;
  typedef typename A::Weight AW;
  typedef typename GallicArc<A, S>::Weight GW;

  GallicToNewSymbolsMapper(MutableFst<ToArc> *fst)
      : fst_(fst), lmax_(0), osymbols_(fst->OutputSymbols()), isymbols_(0) {
    fst_->DeleteStates();
    state_ = fst_->AddState();
    fst_->SetStart(state_);
    fst_->SetFinal(state_, AW::One());
    if (osymbols_) {
      string name = osymbols_->Name() + "_from_gallic";
      isymbols_ = new SymbolTable(name);
      isymbols_->AddSymbol(osymbols_->Find((int64) 0), 0);
   }
    fst_->SetInputSymbols(isymbols_);
  }

  A operator()(const FromArc &arc) {
    // 'Super-non-final' arc.
    if (arc.nextstate == kNoStateId && arc.weight == GW::Zero())
      return A(arc.ilabel, 0, AW::Zero(), kNoStateId);

    SW w1 = arc.weight.Value1();
    AW w2 = arc.weight.Value2();
    Label l;

    if (w1.Size() == 0) {
      l = 0;
    } else {
      typename Map::iterator miter = map_.find(w1);
      if (miter != map_.end()) {
        l = (*miter).second;
      } else {
        l = ++lmax_;
        map_.insert(pair<const SW, Label>(w1, l));
        StringWeightIterator<Label, S> iter1(w1);
        StateId n;
        string s;
        for(ssize_t i = 0, p = state_;
            i < w1.Size();
            ++i, iter1.Next(), p = n) {
          n = i == w1.Size() - 1 ? state_ : fst_->AddState();
          fst_->AddArc(p, ToArc(i ? 0 : l, iter1.Value(), AW::One(), n));
          if (isymbols_) {
            if (i) s = s + "_";
            s = s + osymbols_->Find(iter1.Value());
          }
        }
        if (isymbols_)
          isymbols_->AddSymbol(s, l);
      }
    }

    CHECK(l != kStringInfinity);
    CHECK(l != kStringBad);
    CHECK(arc.ilabel == arc.olabel);

    return A(arc.ilabel, l, w2, arc.nextstate);
  }

  MapFinalAction FinalAction() const { return MAP_ALLOW_SUPERFINAL; }

  uint64 Properties(uint64 props) const {
    return props & kOLabelInvariantProperties &
      kWeightInvariantProperties & kAddSuperFinalProperties;
  }

 private:
  class StringKey {
   public:
    size_t operator()(const SW &x) const {
      return x.Hash();
    }
  };

  typedef hash_map<SW, Label, StringKey> Map;

  MutableFst<ToArc> *fst_;
  Map map_;
  Label lmax_;
  StateId state_;
  SymbolTable *osymbols_, *isymbols_;
};


// Mapper to add a constant to all weights.
template <class A>
struct PlusMapper {
  typedef typename A::Weight Weight;

  explicit PlusMapper(Weight w) : weight_(w) {}

  A operator()(const A &arc) const {
    if (arc.weight == Weight::Zero())
      return arc;
    Weight w = Plus(arc.weight, weight_);
    return A(arc.ilabel, arc.olabel, w, arc.nextstate);
  }

  MapFinalAction FinalAction() const { return MAP_NO_SUPERFINAL; }

  uint64 Properties(uint64 props) const {
    return props & kWeightInvariantProperties;
  }

  Weight weight_;
};


// Mapper to (right) multiply a constant to all weights.
template <class A>
struct TimesMapper {
  typedef typename A::Weight Weight;

  explicit TimesMapper(Weight w) : weight_(w) {}

  A operator()(const A &arc) const {
    if (arc.weight == Weight::Zero())
      return arc;
    Weight w = Times(arc.weight, weight_);
    return A(arc.ilabel, arc.olabel, w, arc.nextstate);
  }

  MapFinalAction FinalAction() const { return MAP_NO_SUPERFINAL; }

  uint64 Properties(uint64 props) const {
    return props & kWeightInvariantProperties;
  }

  Weight weight_;
};


// Mapper to map all non-Zero() weights to One().
template <class A, class B = A>
struct RmWeightMapper {
  typedef A FromArc;
  typedef B ToArc;
  typedef typename FromArc::Weight FromWeight;
  typedef typename ToArc::Weight ToWeight;

  B operator()(const A &arc) const {
    ToWeight w = arc.weight != FromWeight::Zero() ?
                   ToWeight::One() : ToWeight::Zero();
    return B(arc.ilabel, arc.olabel, w, arc.nextstate);
  }

  MapFinalAction FinalAction() const { return MAP_NO_SUPERFINAL; }

  uint64 Properties(uint64 props) const {
    return props & kWeightInvariantProperties | kUnweighted;
  }
};


// Mapper to quantize all weights.
template <class A, class B = A>
struct QuantizeMapper {
  typedef A FromArc;
  typedef B ToArc;
  typedef typename FromArc::Weight FromWeight;
  typedef typename ToArc::Weight ToWeight;

  QuantizeMapper() : delta_(kDelta) {}

  explicit QuantizeMapper(float d) : delta_(d) {}

  B operator()(const A &arc) const {
    ToWeight w = arc.weight.Quantize(delta_);
    return B(arc.ilabel, arc.olabel, w, arc.nextstate);
  }

  MapFinalAction FinalAction() const { return MAP_NO_SUPERFINAL; }

  uint64 Properties(uint64 props) const {
    return props & kWeightInvariantProperties;
  }

  float delta_;
};


// Mapper from A to B under the assumption:
//    B::Weight = A::Weight::ReverseWeight
//    B::Label == A::Label
//    B::StateId == A::StateId
// The weight is reversed, while the label and nextstate preserved
// in the mapping.
template <class A, class B>
struct ReverseWeightMapper {
  typedef A FromArc;
  typedef B ToArc;

  B operator()(const A &arc) const {
    return B(arc.ilabel, arc.olabel, arc.weight.Reverse(), arc.nextstate);
  }

  MapFinalAction FinalAction() const { return MAP_NO_SUPERFINAL; }

  uint64 Properties(uint64 props) const { return props; }
};

}  // namespace fst

#endif  // FST_LIB_MAP_H__
