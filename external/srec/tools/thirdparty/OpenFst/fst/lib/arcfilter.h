// arcfilter.h
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
// Function objects to restrict which arcs are traversed in an FST.

#ifndef FST_LIB_ARCFILTER_H__
#define FST_LIB_ARCFILTER_H__

namespace fst {

// True for all arcs.
template <class A>
class AnyArcFilter {
public:
  bool operator()(const A &arc) const { return true; }
};


// True for (input/output) epsilon arcs.
template <class A>
class EpsilonArcFilter {
public:
  bool operator()(const A &arc) const {
    return arc.ilabel == 0 && arc.olabel == 0;
  }
};

}  // namespace fst

#endif  // FST_LIB_ARCFILTER_H__
