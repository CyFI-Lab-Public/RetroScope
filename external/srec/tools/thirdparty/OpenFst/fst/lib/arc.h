// arc.h
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
// Commonly used Fst arc types.

#ifndef FST_LIB_ARC_H__
#define FST_LIB_ARC_H__

#include "fst/lib/float-weight.h"
#include "fst/lib/product-weight.h"
#include "fst/lib/string-weight.h"

namespace fst {

// Arc with integer labels and state Ids and float weights over the
// tropical semiring.
struct StdArc {
  typedef int Label;
  typedef TropicalWeight Weight;
  typedef int StateId;

  StdArc(Label i, Label o, Weight w, StateId s)
      : ilabel(i), olabel(o), weight(w), nextstate(s) {}

  StdArc() {}

  static const string &Type() {  // Arc type name
    static const string type = "standard";
    return type;
  }

  Label ilabel;       // Transition input label
  Label olabel;       // Transition output label
  Weight weight;      // Transition weight
  StateId nextstate;  // Transition destination state
};


// Arc with integer labels and state Ids and float weights over the
// log semiring.
struct LogArc {
  typedef int Label;
  typedef LogWeight Weight;
  typedef int StateId;

  LogArc(Label i, Label o, Weight w, StateId s)
      : ilabel(i), olabel(o), weight(w), nextstate(s) {}

  LogArc() {}

  static const string &Type() {  // Arc type name
    static const string type = "log";
    return type;
  }

  Label ilabel;       // Transition input label
  Label olabel;       // Transition output label
  Weight weight;      // Transition weight
  StateId nextstate;  // Transition destination state
};


// Arc with integer labels and state Ids and string weights.
template <StringType S = STRING_LEFT>
class StringArc {
 public:
  typedef int Label;
  typedef StringWeight<int, S> Weight;
  typedef int StateId;

  StringArc(Label i, Label o, Weight w, StateId s)
      : ilabel(i), olabel(o), weight(w), nextstate(s) {}

  StringArc() {}

  static const string &Type() {  // Arc type name
    static const string type =
        S == STRING_LEFT ? "standard_string" :
        (S == STRING_RIGHT ? "right_standard_string" :
         (S == STRING_LEFT_RESTRICT ? "restricted_string" :
          "right_restricted_string"));
    return type;
  }

  Label ilabel;       // Transition input label
  Label olabel;       // Transition output label
  Weight weight;      // Transition weight
  StateId nextstate;  // Transition destination state
};


// Arc with label and state Id type the same as template arg and with
// weights over the Gallic semiring w.r.t the output labels and weights of A.
template <class A, StringType S = STRING_LEFT>
struct GallicArc {
  typedef A Arc;
  typedef typename A::Label Label;
  typedef typename A::StateId StateId;
  typedef GallicWeight<Label, typename A::Weight, S> Weight;

  GallicArc() {}

  GallicArc(Label i, Label o, Weight w, StateId s)
      : ilabel(i), olabel(o), weight(w), nextstate(s) {}

  GallicArc(const A &arc)
      : ilabel(arc.ilabel), olabel(arc.ilabel),
        weight(arc.olabel, arc.weight), nextstate(arc.nextstate) {}

  static const string &Type() {  // Arc type name
    static const string type =
        (S == STRING_LEFT ? "gallic_" :
         (S == STRING_RIGHT ? "right_gallic_" :
          (S == STRING_LEFT_RESTRICT ? "restricted_gallic_" :
           "right_restricted_gallic_"))) + A::Type();
    return type;
  }

  Label ilabel;       // Transition input label
  Label olabel;       // Transition output label
  Weight weight;      // Transition weight
  StateId nextstate;  // Transition destination state
};


// Arc with the reverse of the weight found in its template arg.
template <class A> struct ReverseArc {
  typedef A Arc;
  typedef typename A::Label Label;
  typedef typename A::Weight AWeight;
  typedef typename AWeight::ReverseWeight Weight;
  typedef typename A::StateId StateId;

  ReverseArc(Label i, Label o, Weight w, StateId s)
      : ilabel(i), olabel(o), weight(w), nextstate(s) {}

  ReverseArc() {}

  static const string &Type() {  // Arc type name
    static const string type = "reverse_" + Arc::Type();
    return type;
  }

  Label ilabel;       // Transition input label
  Label olabel;       // Transition output label
  Weight weight;      // Transition weight
  StateId nextstate;  // Transition destination state
};

}  // namespace fst;

#endif  // FST_LIB_ARC_H__
