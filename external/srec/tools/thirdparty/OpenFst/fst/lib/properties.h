// properties.h
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
// \file
// FST property bits.

#ifndef FST_LIB_PROPERTIES_H__
#define FST_LIB_PROPERTIES_H__

#include "fst/lib/compat.h"

namespace fst {

// The property bits here assert facts about an FST. If individual
// bits are added, then the composite properties below, the property
// functions and property names in properties.cc, and
// TestProperties() in test-properties.h should be updated.

//
// BINARY PROPERTIES
//
// For each property below, there is a single bit. If it is set,
// the property is true. If it is not set, the property is false.
//

// The Fst is an ExpandedFst
const uint64 kExpanded =          0x0000000000000001ULL;

// The Fst is a MutableFst
const uint64 kMutable =           0x0000000000000002ULL;

//
// TRINARY PROPERTIES
//
// For each of these properties below there is a pair of property bits
// - one positive and one negative. If the positive bit is set, the
// property is true. If the negative bit is set, the property is
// false. If neither is set, the property has unknown value. Both
// should never be simultaneously set. The individual positive and
// negative bit pairs should be adjacent with the positive bit
// at an odd and lower position.

// ilabel == olabel for each arc
const uint64 kAcceptor =          0x0000000000010000ULL;
// ilabel != olabel for some arc
const uint64 kNotAcceptor =       0x0000000000020000ULL;

// ilabels unique leaving each state
const uint64 kIDeterministic =    0x0000000000040000ULL;
// ilabels not unique leaving some state
const uint64 kNonIDeterministic = 0x0000000000080000ULL;

// olabels unique leaving each state
const uint64 kODeterministic =    0x0000000000100000ULL;
// olabels not unique leaving some state
const uint64 kNonODeterministic = 0x0000000000200000ULL;

// FST has input/output epsilons
const uint64 kEpsilons =          0x0000000000400000ULL;
// FST has no input/output epsilons
const uint64 kNoEpsilons =        0x0000000000800000ULL;

// FST has input epsilons
const uint64 kIEpsilons =         0x0000000001000000ULL;
// FST has no input epsilons
const uint64 kNoIEpsilons =       0x0000000002000000ULL;

// FST has output epsilons
const uint64 kOEpsilons =         0x0000000004000000ULL;
// FST has no output epsilons
const uint64 kNoOEpsilons =       0x0000000008000000ULL;

// ilabels sorted wrt < for each state
const uint64 kILabelSorted =      0x0000000010000000ULL;
// ilabels not sorted wrt < for some state
const uint64 kNotILabelSorted =   0x0000000020000000ULL;

// olabels sorted wrt < for each state
const uint64 kOLabelSorted =      0x0000000040000000ULL;
// olabels not sorted wrt < for some state
const uint64 kNotOLabelSorted =   0x0000000080000000ULL;

// Non-trivial arc or final weights
const uint64 kWeighted =          0x0000000100000000ULL;
// Only trivial arc and final weights
const uint64 kUnweighted =        0x0000000200000000ULL;

// FST has cycles
const uint64 kCyclic =            0x0000000400000000ULL;
// FST has no cycles
const uint64 kAcyclic =           0x0000000800000000ULL;

// FST has cycles containing the initial state
const uint64 kInitialCyclic =     0x0000001000000000ULL;
// FST has no cycles containing the initial state
const uint64 kInitialAcyclic =    0x0000002000000000ULL;

// FST is topologically sorted
const uint64 kTopSorted =         0x0000004000000000ULL;
// FST is not topologically sorted
const uint64 kNotTopSorted =      0x0000008000000000ULL;

// All states reachable from the initial state
const uint64 kAccessible =        0x0000010000000000ULL;
// Not all states reachable from the initial state
const uint64 kNotAccessible =     0x0000020000000000ULL;

// All states can reach a final state
const uint64 kCoAccessible =      0x0000040000000000ULL;
// Not all states can reach a final state
const uint64 kNotCoAccessible =   0x0000080000000000ULL;

// If NumStates() > 0, then state 0 is initial, state NumStates()-1 is
// final, there is a transition from each non-final state i to
// state i+1, and there are no other transitions.
const uint64 kString =            0x0000100000000000ULL;

// Not a string FST
const uint64 kNotString =         0x0000200000000000ULL;

//
// COMPOSITE PROPERTIES
//

// Properties of an empty machine

const uint64 kNullProperties
  = kAcceptor | kIDeterministic | kODeterministic | kNoEpsilons |
    kNoIEpsilons | kNoOEpsilons | kILabelSorted | kOLabelSorted |
    kUnweighted | kAcyclic | kInitialAcyclic | kTopSorted |
    kAccessible | kCoAccessible | kString;

// Properties that are preserved when an FST is copied
const uint64 kCopyProperties
  = kAcceptor | kNotAcceptor | kIDeterministic | kNonIDeterministic |
    kODeterministic | kNonODeterministic | kEpsilons | kNoEpsilons |
    kIEpsilons | kNoIEpsilons | kOEpsilons | kNoOEpsilons |
    kILabelSorted | kNotILabelSorted | kOLabelSorted |
    kNotOLabelSorted | kWeighted | kUnweighted | kCyclic | kAcyclic |
    kInitialCyclic | kInitialAcyclic | kTopSorted | kNotTopSorted |
    kAccessible | kNotAccessible | kCoAccessible | kNotCoAccessible |
    kString | kNotString;

// Properties that are preserved when an FST start state is set
const uint64 kSetStartProperties
  = kExpanded | kMutable | kAcceptor | kNotAcceptor | kIDeterministic |
    kNonIDeterministic | kODeterministic | kNonODeterministic |
    kEpsilons | kNoEpsilons | kIEpsilons | kNoIEpsilons | kOEpsilons |
    kNoOEpsilons | kILabelSorted | kNotILabelSorted | kOLabelSorted |
    kNotOLabelSorted | kWeighted | kUnweighted | kCyclic | kAcyclic |
    kTopSorted | kNotTopSorted | kCoAccessible | kNotCoAccessible;

// Properties that are preserved when an FST final weight is set
const uint64 kSetFinalProperties
  = kExpanded | kMutable | kAcceptor | kNotAcceptor | kIDeterministic |
    kNonIDeterministic | kODeterministic | kNonODeterministic |
    kEpsilons | kNoEpsilons | kIEpsilons | kNoIEpsilons | kOEpsilons |
    kNoOEpsilons | kILabelSorted | kNotILabelSorted | kOLabelSorted |
    kNotOLabelSorted | kCyclic | kAcyclic | kInitialCyclic |
    kInitialAcyclic | kTopSorted | kNotTopSorted | kAccessible |
    kNotAccessible;

// Properties that are preserved when an FST state is added
const uint64 kAddStateProperties
  = kExpanded | kMutable | kAcceptor | kNotAcceptor | kIDeterministic |
    kNonIDeterministic | kODeterministic | kNonODeterministic |
    kEpsilons | kNoEpsilons | kIEpsilons | kNoIEpsilons | kOEpsilons |
    kNoOEpsilons | kILabelSorted | kNotILabelSorted | kOLabelSorted |
    kNotOLabelSorted | kWeighted | kUnweighted | kCyclic | kAcyclic |
    kInitialCyclic | kInitialAcyclic | kTopSorted | kNotTopSorted |
    kNotAccessible | kNotCoAccessible | kNotString;

// Properties that are preserved when an FST arc is added
const uint64 kAddArcProperties = kExpanded | kMutable | kNotAcceptor |
    kNonIDeterministic | kNonODeterministic | kEpsilons | kIEpsilons |
    kOEpsilons | kNotILabelSorted | kNotOLabelSorted | kWeighted |
    kCyclic | kInitialCyclic | kNotTopSorted | kAccessible | kCoAccessible;

// Properties that are preserved when an FST arc is set
const uint64 kSetArcProperties = kExpanded | kMutable;

// Properties that are preserved when FST states are deleted
const uint64 kDeleteStatesProperties
  = kExpanded | kMutable | kAcceptor | kIDeterministic |
    kODeterministic | kNoEpsilons | kNoIEpsilons | kNoOEpsilons |
    kILabelSorted | kOLabelSorted | kUnweighted | kAcyclic |
    kInitialAcyclic | kTopSorted;

// Properties that are preserved when FST arcs are deleted
const uint64 kDeleteArcsProperties
  = kExpanded | kMutable | kAcceptor | kIDeterministic |
    kODeterministic | kNoEpsilons | kNoIEpsilons | kNoOEpsilons |
    kILabelSorted | kOLabelSorted | kUnweighted | kAcyclic |
    kInitialAcyclic | kTopSorted |  kNotAccessible | kNotCoAccessible;

// Properties that are preserved when an FST's states are reordered
const uint64 kStateSortProperties = kExpanded | kMutable | kAcceptor |
  kNotAcceptor | kIDeterministic | kNonIDeterministic |
  kODeterministic | kNonODeterministic | kEpsilons | kNoEpsilons |
  kIEpsilons | kNoIEpsilons | kOEpsilons | kNoOEpsilons |
  kILabelSorted | kNotILabelSorted | kOLabelSorted | kNotOLabelSorted
  | kWeighted | kUnweighted | kCyclic | kAcyclic | kInitialCyclic |
  kInitialAcyclic | kAccessible | kNotAccessible | kCoAccessible |
  kNotCoAccessible;

// Properties that are preserved when an FST's arcs are reordered
const uint64 kArcSortProperties =
  kExpanded | kMutable | kAcceptor | kNotAcceptor | kIDeterministic |
  kNonIDeterministic | kODeterministic | kNonODeterministic |
  kEpsilons | kNoEpsilons | kIEpsilons | kNoIEpsilons | kOEpsilons |
  kNoOEpsilons | kWeighted | kUnweighted | kCyclic | kAcyclic |
  kInitialCyclic | kInitialAcyclic | kTopSorted | kNotTopSorted |
  kAccessible | kNotAccessible | kCoAccessible | kNotCoAccessible |
  kString | kNotString;

// Properties that are preserved when an FST's input labels are changed.
const uint64 kILabelInvariantProperties =
  kExpanded | kMutable | kODeterministic | kNonODeterministic |
  kOEpsilons | kNoOEpsilons | kOLabelSorted | kNotOLabelSorted |
  kWeighted | kUnweighted | kCyclic | kAcyclic | kInitialCyclic |
  kInitialAcyclic | kTopSorted | kNotTopSorted | kAccessible |
  kNotAccessible | kCoAccessible | kNotCoAccessible | kString | kNotString;

// Properties that are preserved when an FST's output labels are changed.
const uint64 kOLabelInvariantProperties =
  kExpanded | kMutable | kIDeterministic | kNonIDeterministic |
  kIEpsilons | kNoIEpsilons | kILabelSorted | kNotILabelSorted |
  kWeighted | kUnweighted | kCyclic | kAcyclic | kInitialCyclic |
  kInitialAcyclic | kTopSorted | kNotTopSorted | kAccessible |
  kNotAccessible | kCoAccessible | kNotCoAccessible | kString | kNotString;

// Properties that are preserved when an FST's weights are changed.
// This assumes that the set of states that are non-final is not changed.
const uint64 kWeightInvariantProperties =
  kExpanded | kMutable | kAcceptor | kNotAcceptor | kIDeterministic |
  kNonIDeterministic | kODeterministic | kNonODeterministic |
  kEpsilons | kNoEpsilons | kIEpsilons | kNoIEpsilons | kOEpsilons |
  kNoOEpsilons | kILabelSorted | kNotILabelSorted | kOLabelSorted |
  kNotOLabelSorted | kCyclic | kAcyclic | kInitialCyclic | kInitialAcyclic |
  kTopSorted | kNotTopSorted | kAccessible | kNotAccessible | kCoAccessible |
  kNotCoAccessible | kString | kNotString;

// Properties that are preserved when a superfinal state is added
// and an FSTs final weights are directed to it via new transitions.
const uint64 kAddSuperFinalProperties  = kExpanded | kMutable | kAcceptor |
  kNotAcceptor | kNonIDeterministic | kNonODeterministic | kEpsilons |
  kIEpsilons | kOEpsilons | kNotILabelSorted | kNotOLabelSorted |
  kWeighted | kUnweighted | kCyclic | kAcyclic | kInitialCyclic |
  kInitialAcyclic | kNotTopSorted | kNotAccessible | kCoAccessible |
  kNotCoAccessible | kNotString;

// Properties that are preserved when a superfinal state is removed
// and the epsilon transitions directed to it are made final weights.
const uint64 kRmSuperFinalProperties  = kExpanded | kMutable | kAcceptor |
  kNotAcceptor | kIDeterministic | kODeterministic | kNoEpsilons |
  kNoIEpsilons | kNoOEpsilons | kILabelSorted | kOLabelSorted |
  kWeighted | kUnweighted | kCyclic | kAcyclic | kInitialCyclic |
  kInitialAcyclic | kTopSorted | kAccessible | kCoAccessible |
  kNotCoAccessible | kString;

// All binary properties
const uint64 kBinaryProperties =  0x0000000000000003ULL;

// All trinary properties
const uint64 kTrinaryProperties = 0x00003fffffff0000ULL;

//
// COMPUTED PROPERTIES
//

// 1st bit of trinary properties
const uint64 kPosTrinaryProperties =
  kTrinaryProperties & 0x5555555555555555ULL;

// 2nd bit of trinary properties
const uint64 kNegTrinaryProperties =
  kTrinaryProperties & 0xaaaaaaaaaaaaaaaaULL;

// All properties
const uint64 kFstProperties = kBinaryProperties | kTrinaryProperties;

//
// PROPERTY FUNCTIONS and STRING NAMES (defined in properties.cc)
//

uint64 ClosureProperties(uint64 inprops, bool star, bool delayed = false);
uint64 ComplementProperties(uint64 inprops);
uint64 ComposeProperties(uint64 inprops1, uint64 inprops2);
uint64 ConcatProperties(uint64 inprops1, uint64 inprops2,
                        bool delayed = false);
uint64 DeterminizeProperties(uint64 inprops);
uint64 DifferenceProperties(uint64 inprops1, uint64 inprops2);
uint64 FactorWeightProperties(uint64 inprops);
uint64 IntersectProperties(uint64 inprops1, uint64 inprops2);
uint64 InvertProperties(uint64 inprops);
uint64 ProjectProperties(uint64 inprops, bool project_input);
uint64 RelabelProperties(uint64 inprops);
uint64 ReplaceProperties(const vector<uint64>& inprops);
uint64 ReverseProperties(uint64 inprops);
uint64 ReweightProperties(uint64 inprops);
uint64 RmEpsilonProperties(uint64 inprops, bool delayed = false);
uint64 SynchronizeProperties(uint64 inprops);
uint64 UnionProperties(uint64 inprops1, uint64 inprops2, bool delayed = false);

extern const char *PropertyNames[];

}  // namespace fst

#endif  // FST_LIB_PROPERTIES_H__
