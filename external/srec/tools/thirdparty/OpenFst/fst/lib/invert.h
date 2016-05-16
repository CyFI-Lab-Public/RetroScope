// invert.h
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
// Functions and classes to invert an Fst.

#ifndef FST_LIB_INVERT_H__
#define FST_LIB_INVERT_H__

#include "fst/lib/map.h"
#include "fst/lib/mutable-fst.h"

namespace fst {

// Mapper to implement inversion of an arc.
template <class A> struct InvertMapper {
  InvertMapper() {}

  A operator()(const A &arc) {
    return A(arc.olabel, arc.ilabel, arc.weight, arc.nextstate);
  }

  uint64 Properties(uint64 props) { return InvertProperties(props); }

  MapFinalAction FinalAction() const { return MAP_NO_SUPERFINAL; }
};


// Inverts the transduction corresponding to an FST by exchanging the
// FST's input and output labels. This version modifies its input.
//
// Complexity:
// - Time: O(V + E)
// - Space: O(1)
// where V = # of states and E = # of arcs.
template<class Arc> inline
void Invert(MutableFst<Arc> *fst) { Map(fst, InvertMapper<Arc>()); }


// Inverts the transduction corresponding to an FST by exchanging the
// FST's input and output labels.  This version is a delayed Fst.
//
// Complexity:
// - Time: O(v + e)
// - Space: O(1)
// where v = # of states visited, e = # of arcs visited. Constant
// time and to visit an input state or arc is assumed and exclusive
// of caching.
template <class A>
class InvertFst : public MapFst<A, A, InvertMapper<A> > {
 public:
  typedef A Arc;
  typedef InvertMapper<A> C;

  explicit InvertFst(const Fst<A> &fst) : MapFst<A, A, C>(fst, C()) {}

  InvertFst(const InvertFst<A> &fst) : MapFst<A, A, C>(fst) {}

  virtual InvertFst<A> *Copy() const { return new InvertFst(*this); }
};


// Specialization for InvertFst.
template <class A>
class StateIterator< InvertFst<A> >
    : public StateIterator< MapFst<A, A, InvertMapper<A> > > {
 public:
  explicit StateIterator(const InvertFst<A> &fst)
      : StateIterator< MapFst<A, A, InvertMapper<A> > >(fst) {}
};


// Specialization for InvertFst.
template <class A>
class ArcIterator< InvertFst<A> >
    : public ArcIterator< MapFst<A, A, InvertMapper<A> > > {
 public:
  ArcIterator(const InvertFst<A> &fst, typename A::StateId s)
      : ArcIterator< MapFst<A, A, InvertMapper<A> > >(fst, s) {}
};


// Useful alias when using StdArc.
typedef InvertFst<StdArc> StdInvertFst;

}  // namespace fst

#endif  // FST_LIB_INVERT_H__
