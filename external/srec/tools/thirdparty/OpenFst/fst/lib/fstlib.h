// fstlib.h
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
// \page OpenFst - Weighted Finite State Transducers
// This is a library for constructing, combining, optimizing, and
// searching "weighted finite-state transducers" (FSTs). Weighted
// finite-state transducers are automata where each transition has an
// input label, an output label, and a weight. The more familiar
// finite-state acceptor is represented as a transducer with each
// transition's input and output the same.  Finite-state acceptors
// are used to represent sets of strings (specifically, "regular" or
// "rational sets"); finite-state transducers are used to represent
// binary relations between pairs of strings (specifically, "rational
// transductions"). The weights can be used to represent the cost of
// taking a particular transition.
//
// In this library, the transducers are templated on the Arc
// (transition) definition, which allows changing the label, weight,
// and state ID sets. Labels and state IDs are restricted to signed
// integral types but the weight can be an arbitrary type whose
// members satisfy certain algebraic ("semiring") properties.
//
// For more information, see the OpenFst web site:
// http://www.openfst.org.

// \file
// This convenience file includes all other OpenFst header files.

#ifndef FST_LIB_FSTLIB_H__
#define FST_LIB_FSTLIB_H__

// Abstract FST classes
#include "fst/lib/fst.h"
#include "fst/lib/expanded-fst.h"
#include "fst/lib/mutable-fst.h"

// Concrete FST classes
#include "fst/lib/vector-fst.h"
#include "fst/lib/const-fst.h"

// FST algorithms and delayed FST classes
#include "fst/lib/arcsort.h"
#include "fst/lib/closure.h"
#include "fst/lib/compose.h"
#include "fst/lib/concat.h"
#include "fst/lib/connect.h"
#include "fst/lib/determinize.h"
#include "fst/lib/difference.h"
#include "fst/lib/encode.h"
#include "fst/lib/epsnormalize.h"
#include "fst/lib/equal.h"
#include "fst/lib/equivalent.h"
#include "fst/lib/factor-weight.h"
#include "fst/lib/intersect.h"
#include "fst/lib/invert.h"
#include "fst/lib/map.h"
#include "fst/lib/minimize.h"
#include "fst/lib/project.h"
#include "fst/lib/prune.h"
#include "fst/lib/push.h"
#include "fst/lib/randgen.h"
#include "fst/lib/relabel.h"
#include "fst/lib/replace.h"
#include "fst/lib/reverse.h"
#include "fst/lib/reweight.h"
#include "fst/lib/rmepsilon.h"
#include "fst/lib/rmfinalepsilon.h"
#include "fst/lib/shortest-distance.h"
#include "fst/lib/shortest-path.h"
#include "fst/lib/synchronize.h"
#include "fst/lib/topsort.h"
#include "fst/lib/union.h"
#include "fst/lib/verify.h"

#endif  // FST_LIB_FSTLIB_H__
