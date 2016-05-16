// properties.cc
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
// Functions for updating property bits for various FST operations and
// string names of the properties.

#include <vector>

#include "fst/lib/properties.h"

namespace fst {

// These functions determine the properties associated with the FST
// result of various finite-state operations. The property arguments
// correspond to the operation's FST arguments. The properties
// returned assume the operation modifies its first argument.
// Bitwise-and this result with kCopyProperties for the case when a
// new (possibly delayed) FST is instead constructed.

// Properties for a concatenatively-closed FST.
uint64 ClosureProperties(uint64 inprops, bool star, bool delayed) {
  uint64 outprops = (kAcceptor | kUnweighted | kAccessible) & inprops;
  if (!delayed)
       outprops |= (kExpanded | kMutable | kCoAccessible |
                    kNotTopSorted | kNotString) & inprops;
  if (!delayed || inprops & kAccessible)
    outprops |= (kNotAcceptor | kNonIDeterministic | kNonODeterministic |
                 kNotILabelSorted | kNotOLabelSorted | kWeighted |
                 kNotAccessible | kNotCoAccessible) & inprops;
  return outprops;
}

// Properties for a complemented FST.
uint64 ComplementProperties(uint64 inprops) {
  uint64 outprops = kAcceptor | kUnweighted | kNoEpsilons |
                    kNoIEpsilons | kNoOEpsilons |
                    kIDeterministic | kODeterministic | kAccessible;
  outprops |= (kILabelSorted | kOLabelSorted | kInitialCyclic) & inprops;
  if (inprops & kAccessible)
    outprops |= kNotILabelSorted | kNotOLabelSorted | kCyclic;
  return outprops;
}

// Properties for a composed FST.
uint64 ComposeProperties(uint64 inprops1, uint64 inprops2) {
  uint64 outprops = kAccessible;
  outprops |= (kAcceptor | kNoIEpsilons | kAcyclic | kInitialAcyclic) &
              inprops1 & inprops2;
  if ((kNoIEpsilons & inprops1 & inprops2)) {
    outprops |= kIDeterministic & inprops1 & inprops2;
  }
  return outprops;
}

// Properties for a concatenated FST.
uint64 ConcatProperties(uint64 inprops1, uint64 inprops2, bool delayed) {
  uint64 outprops =
    (kAcceptor | kUnweighted | kAcyclic) & inprops1 & inprops2;

  bool empty1 = delayed;  // Can fst1 be the empty machine?
  bool empty2 = delayed;  // Can fst2 be the empty machine?

  if (!delayed) {
    outprops |= (kExpanded | kMutable | kNotTopSorted | kNotString) & inprops1;
    outprops |= (kNotTopSorted | kNotString) & inprops2;
  }
  if (!empty1)
    outprops |= (kInitialAcyclic | kInitialCyclic) & inprops1;
  if (!delayed || inprops1 & kAccessible)
    outprops |= (kNotAcceptor | kNonIDeterministic | kNonODeterministic |
                 kEpsilons | kIEpsilons | kOEpsilons | kNotILabelSorted |
                 kNotOLabelSorted | kWeighted | kCyclic |
                 kNotAccessible | kNotCoAccessible) & inprops1;
  if ((inprops1 & (kAccessible | kCoAccessible)) ==
      (kAccessible | kCoAccessible) && !empty1) {
    outprops |= kAccessible & inprops2;
    if (!empty2)
      outprops |= kCoAccessible & inprops2;
    if (!delayed || inprops2 & kAccessible)
      outprops |= (kNotAcceptor | kNonIDeterministic | kNonODeterministic |
                   kEpsilons | kIEpsilons | kOEpsilons | kNotILabelSorted |
                   kNotOLabelSorted | kWeighted | kCyclic |
                   kNotAccessible | kNotCoAccessible) & inprops2;
  }
  return outprops;
}

// Properties for a determinized FST.
uint64 DeterminizeProperties(uint64 inprops) {
  uint64 outprops = kIDeterministic | kAccessible;
  outprops |= (kAcceptor | kNoEpsilons | kAcyclic |
               kInitialAcyclic | kCoAccessible | kString) & inprops;
  if (inprops & kAccessible)
     outprops |= (kNotAcceptor | kEpsilons | kIEpsilons | kOEpsilons |
                  kCyclic) & inprops;
  if (inprops & kAcceptor)
    outprops |= (kNoIEpsilons | kNoOEpsilons | kAccessible) & inprops;
  return outprops;
}

// Properties for a differenced FST.
uint64 DifferenceProperties(uint64 inprops1, uint64 inprops2) {
  return IntersectProperties(inprops1, ComplementProperties(inprops2));
}

// Properties for factored weight FST.
uint64 FactorWeightProperties(uint64 inprops) {
  uint64 outprops = (kExpanded | kMutable | kAcceptor |
                     kAcyclic | kAccessible | kCoAccessible) & inprops;
  if (inprops & kAccessible)
    outprops |= (kNotAcceptor | kNonIDeterministic | kNonODeterministic |
                 kEpsilons | kIEpsilons | kOEpsilons | kCyclic |
                 kNotILabelSorted | kNotOLabelSorted)
        & inprops;
  return outprops;
}

// Properties for an intersected FST.
uint64 IntersectProperties(uint64 inprops1, uint64 inprops2) {
  uint64 outprops = kAcceptor | kAccessible;

  outprops |= (kNoEpsilons | kNoIEpsilons | kNoOEpsilons | kAcyclic |
               kInitialAcyclic) & inprops1 & inprops2;

  if ((kNoIEpsilons & inprops1 & inprops2))
    outprops |= (kIDeterministic | kODeterministic) & inprops1 & inprops2;
  return outprops;
}

// Properties for an inverted FST.
uint64 InvertProperties(uint64 inprops) {
  uint64 outprops = (kExpanded | kMutable | kAcceptor | kNotAcceptor |
                     kEpsilons | kNoEpsilons | kWeighted | kUnweighted |
                     kCyclic | kAcyclic | kInitialCyclic | kInitialAcyclic |
                     kTopSorted | kNotTopSorted |
                     kAccessible | kNotAccessible |
                     kCoAccessible | kNotCoAccessible |
                     kString | kNotString) & inprops;
  if (kIDeterministic & inprops)
    outprops |= kODeterministic;
  if (kNonIDeterministic & inprops)
    outprops |= kNonODeterministic;
  if (kODeterministic & inprops)
    outprops |= kIDeterministic;
  if (kNonODeterministic & inprops)
    outprops |= kNonIDeterministic;

  if (kIEpsilons & inprops)
    outprops |= kOEpsilons;
  if (kNoIEpsilons & inprops)
    outprops |= kNoOEpsilons;
  if (kOEpsilons & inprops)
    outprops |= kIEpsilons;
  if (kNoOEpsilons & inprops)
    outprops |= kNoIEpsilons;

  if (kILabelSorted & inprops)
    outprops |= kOLabelSorted;
  if (kNotILabelSorted & inprops)
    outprops |= kNotOLabelSorted;
  if (kOLabelSorted & inprops)
    outprops |= kILabelSorted;
  if (kNotOLabelSorted & inprops)
    outprops |= kNotILabelSorted;
  return outprops;
}

// Properties for a projected FST.
uint64 ProjectProperties(uint64 inprops, bool project_input) {
  uint64 outprops = kAcceptor;
  outprops |= (kExpanded | kMutable | kWeighted | kUnweighted |
               kCyclic | kAcyclic | kInitialCyclic | kInitialAcyclic |
               kTopSorted | kNotTopSorted | kAccessible | kNotAccessible |
               kCoAccessible | kNotCoAccessible |
               kString | kNotString) & inprops;
  if (project_input) {
    outprops |= (kIDeterministic | kNonIDeterministic |
                 kIEpsilons | kNoIEpsilons |
                 kILabelSorted | kNotILabelSorted) & inprops;

    if (kIDeterministic & inprops)
      outprops |= kODeterministic;
    if (kNonIDeterministic & inprops)
      outprops |= kNonODeterministic;

    if (kIEpsilons & inprops)
      outprops |= kOEpsilons | kEpsilons;
    if (kNoIEpsilons & inprops)
      outprops |= kNoOEpsilons | kNoEpsilons;

    if (kILabelSorted & inprops)
      outprops |= kOLabelSorted;
    if (kNotILabelSorted & inprops)
      outprops |= kNotOLabelSorted;
  } else {
    outprops |= (kODeterministic | kNonODeterministic |
                 kOEpsilons | kNoOEpsilons |
                 kOLabelSorted | kNotOLabelSorted) & inprops;

    if (kODeterministic & inprops)
      outprops |= kIDeterministic;
    if (kNonODeterministic & inprops)
      outprops |= kNonIDeterministic;

    if (kOEpsilons & inprops)
      outprops |= kIEpsilons | kEpsilons;
    if (kNoOEpsilons & inprops)
      outprops |= kNoIEpsilons | kNoEpsilons;

    if (kOLabelSorted & inprops)
      outprops |= kILabelSorted;
    if (kNotOLabelSorted & inprops)
      outprops |= kNotILabelSorted;
  }
  return outprops;
}

// Properties for a replace FST.
uint64 ReplaceProperties(const vector<uint64>& inprops) {
  return 0;
}

// Properties for a relabeled FST.
uint64 RelabelProperties(uint64 inprops) {
  uint64 outprops = (kExpanded | kMutable |
                     kWeighted | kUnweighted |
                     kCyclic | kAcyclic |
                     kInitialCyclic | kInitialAcyclic |
                     kTopSorted | kNotTopSorted |
                     kAccessible | kNotAccessible |
                     kCoAccessible | kNotCoAccessible |
                     kString | kNotString) & inprops;
  return outprops;
}

// Properties for a reversed FST. (the superinitial state limits this set)
uint64 ReverseProperties(uint64 inprops) {
  uint64 outprops =
    (kExpanded | kMutable | kAcceptor | kNotAcceptor | kEpsilons |
     kIEpsilons | kOEpsilons | kWeighted | kUnweighted |
     kCyclic | kAcyclic) & inprops;
  return outprops;
}

// Properties for re-weighted FST.
uint64 ReweightProperties(uint64 inprops) {
  uint64 outprops = inprops & kWeightInvariantProperties;
  outprops = outprops & ~kCoAccessible;
  return outprops;
}

// Properties for an epsilon-removed FST.
uint64 RmEpsilonProperties(uint64 inprops, bool delayed) {
  uint64 outprops = kNoEpsilons;
  outprops |= (kAcceptor | kAcyclic | kInitialAcyclic) & inprops;
  if (inprops & kAcceptor)
    outprops |= kNoIEpsilons | kNoOEpsilons;
  if (!delayed) {
    outprops |= kExpanded | kMutable;
    outprops |= kTopSorted & inprops;
  }
  if (!delayed || inprops & kAccessible)
    outprops |= kNotAcceptor & inprops;
  return outprops;
}

// Properties for a synchronized FST.
uint64 SynchronizeProperties(uint64 inprops) {
  uint64 outprops = (kAcceptor | kAcyclic | kAccessible | kCoAccessible |
                     kUnweighted) & inprops;
  if (inprops & kAccessible)
    outprops |= (kCyclic | kNotCoAccessible | kWeighted) & inprops;
  return outprops;
}

// Properties for a unioned FST.
uint64 UnionProperties(uint64 inprops1, uint64 inprops2, bool delayed) {
  uint64 outprops = (kAcceptor | kUnweighted | kAcyclic | kAccessible)
                    & inprops1 & inprops2;
  bool empty1 = delayed;  // Can fst1 be the empty machine?
  bool empty2 = delayed;  // Can fst2 be the empty machine?
  if (!delayed) {
    outprops |= (kExpanded | kMutable | kNotTopSorted | kNotString) & inprops1;
    outprops |= (kNotTopSorted | kNotString) & inprops2;
  }
  if (!empty1 && !empty2) {
    outprops |= kEpsilons | kIEpsilons | kOEpsilons;
    outprops |= kCoAccessible & inprops1 & inprops2;
  }
  // Note kNotCoAccessible does not hold because of kInitialAcyclic opt.
  if (!delayed || inprops1 & kAccessible)
    outprops |= (kNotAcceptor | kNonIDeterministic | kNonODeterministic |
                 kEpsilons | kIEpsilons | kOEpsilons | kNotILabelSorted |
                 kNotOLabelSorted | kWeighted | kCyclic |
                 kNotAccessible) & inprops1;
  if (!delayed || inprops2 & kAccessible)
    outprops |= (kNotAcceptor | kNonIDeterministic | kNonODeterministic |
                 kEpsilons | kIEpsilons | kOEpsilons | kNotILabelSorted |
                 kNotOLabelSorted | kWeighted | kCyclic |
                 kNotAccessible | kNotCoAccessible) & inprops2;
  return outprops;
}

// Property string names (indexed by bit position).
const char *PropertyNames[] = {
  // binary
  "expanded", "mutable", "", "", "", "", "", "",
  "", "", "", "", "", "", "", "",
  // trinary
  "acceptor", "not acceptor",
  "input deterministic", "non input deterministic",
  "output deterministic", "non output deterministic",
  "input/output epsilons", "no input/output epsilons",
  "input epsilons", "no input epsilons",
  "output epsilons", "no output epsilons",
  "input label sorted", "not input label sorted",
  "output label sorted", "not output label sorted",
  "weighted", "unweighted",
  "cyclic", "acyclic",
  "cyclic at initial state", "acyclic at initial state",
  "top sorted", "not top sorted",
  "accessible", "not accessible",
  "coaccessible", "not coaccessible",
  "string", "not string",
};

}
