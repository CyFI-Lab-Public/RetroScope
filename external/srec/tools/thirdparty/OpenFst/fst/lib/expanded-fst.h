// expanded-fst.h
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
// Generic FST augmented with state count - interface class definition.

#ifndef FST_LIB_EXPANDED_FST_H__
#define FST_LIB_EXPANDED_FST_H__

#include "fst/lib/fst.h"

namespace fst {

// A generic FST plus state count.
template <class A>
class ExpandedFst : public Fst<A> {
 public:
  typedef A Arc;
  typedef typename A::StateId StateId;

  virtual StateId NumStates() const = 0;  // State count

  // Get a copy of this ExpandedFst.
  virtual ExpandedFst<A> *Copy() const = 0;
  // Read an ExpandedFst from an input stream; return NULL on error.
  static ExpandedFst<A> *Read(istream &strm, const FstReadOptions &opts) {
    FstReadOptions ropts(opts);
    FstHeader hdr;
    if (ropts.header)
      hdr = *opts.header;
    else {
      if (!hdr.Read(strm, opts.source))
        return 0;
      ropts.header = &hdr;
    }
    if (!(hdr.Properties() & kExpanded)) {
      LOG(ERROR) << "ExpandedFst::Read: Not an ExpandedFst: " << ropts.source;
      return 0;
    }
    FstRegister<A> *registr = FstRegister<A>::GetRegister();
    const typename FstRegister<A>::Reader reader =
      registr->GetReader(hdr.FstType());
    if (!reader) {
      LOG(ERROR) << "ExpandedFst::Read: Unknown FST type \"" << hdr.FstType()
                 << "\" (arc type = \"" << A::Type()
                 << "\"): " << ropts.source;
      return 0;
    }
    Fst<A> *fst = reader(strm, ropts);
    if (!fst) return 0;
    return down_cast<ExpandedFst<A> *>(fst);
  }
  // Read an ExpandedFst from a file; return NULL on error.
  static ExpandedFst<A> *Read(const string &filename) {
    ifstream strm(filename.c_str());
    if (!strm) {
      LOG(ERROR) << "ExpandedFst::Read: Can't open file: " << filename;
      return 0;
    }
    return Read(strm, FstReadOptions(filename));
  }
};

// A useful alias when using StdArc.
typedef ExpandedFst<StdArc> StdExpandedFst;

// Function to return the number of states in an FST, counting them
// if necessary.
template <class Arc>
typename Arc::StateId CountStates(const Fst<Arc> &fst) {
  if (fst.Properties(kExpanded, false)) {
    const ExpandedFst<Arc> *efst = down_cast<const ExpandedFst<Arc> *>(&fst);
    return efst->NumStates();
  } else {
    typename Arc::StateId nstates = 0;
    for (StateIterator< Fst<Arc> > siter(fst); !siter.Done(); siter.Next())
      ++nstates;
    return nstates;
  }
}

}  // FST_LIB_FST_H__

#endif  // FST_LIB_EXPANDED_FST_H__
