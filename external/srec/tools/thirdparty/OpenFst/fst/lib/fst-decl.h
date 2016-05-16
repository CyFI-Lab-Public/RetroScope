// fst-decl.h
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
// This file contains declarations of classes in the OpenFst library.

#ifndef FST_LIB_FST_DECL_H__
#define FST_LIB_FST_DECL_H__

namespace fst {

class SymbolTable;
class SymbolTableIterator;

class LogWeight;
class TropicalWeight;

class LogArc;
class StdArc;

template <class A> class ConstFst;
template <class A> class ExpandedFst;
template <class A> class Fst;
template <class A> class MutableFst;
template <class A> class VectorFst;

template <class A, class C> class ArcSortFst;
template <class A> class ClosureFst;
template <class A> class ComposeFst;
template <class A> class ConcatFst;
template <class A> class DeterminizeFst;
template <class A> class DeterminizeFst;
template <class A> class DifferenceFst;
template <class A> class IntersectFst;
template <class A> class InvertFst;
template <class A, class B, class C> class MapFst;
template <class A> class ProjectFst;
template <class A> class RelabelFst;
template <class A> class ReplaceFst;
template <class A> class RmEpsilonFst;
template <class A> class UnionFst;

template <class T, class Compare> class Heap;

typedef ConstFst<StdArc> StdConstFst;
typedef ExpandedFst<StdArc> StdExpandedFst;
typedef Fst<StdArc> StdFst;
typedef MutableFst<StdArc> StdMutableFst;
typedef VectorFst<StdArc> StdVectorFst;

template <class C> class StdArcSortFst;
typedef ClosureFst<StdArc> StdClosureFst;
typedef ComposeFst<StdArc> StdComposeFst;
typedef ConcatFst<StdArc> StdConcatFst;
typedef DeterminizeFst<StdArc> StdDeterminizeFst;
typedef DifferenceFst<StdArc> StdDifferenceFst;
typedef IntersectFst<StdArc> StdIntersectFst;
typedef InvertFst<StdArc> StdInvertFst;
typedef ProjectFst<StdArc> StdProjectFst;
typedef RelabelFst<StdArc> StdRelabelFst;
typedef ReplaceFst<StdArc> StdReplaceFst;
typedef RmEpsilonFst<StdArc> StdRmEpsilonFst;
typedef UnionFst<StdArc> StdUnionFst;

}

#endif  // FST_LIB_FST_DECL_H__
