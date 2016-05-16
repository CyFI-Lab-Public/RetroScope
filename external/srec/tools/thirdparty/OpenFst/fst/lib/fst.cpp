// fst.cc
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
// FST definitions.

#include "fst/lib/fst.h"

// Include these so they are registered
#include "fst/lib/const-fst.h"
#include "fst/lib/vector-fst.h"

// FST flag definitions

DEFINE_bool(fst_verify_properties, false,
            "Verify fst properties queried by TestProperties");

DEFINE_string(fst_product_separator, ",",
              "Character separator between printed weights"
              " in a product semiring");

DEFINE_bool(fst_default_cache_gc, true, "Enable garbage collection of cache");

DEFINE_int64(fst_default_cache_gc_limit, 1<<20LL,
             "Cache byte size that triggers garbage collection");

namespace fst {

// Register VectorFst and ConstFst for common arcs types

REGISTER_FST(VectorFst, StdArc);
REGISTER_FST(VectorFst, LogArc);
REGISTER_FST(ConstFst, StdArc);
REGISTER_FST(ConstFst, LogArc);

// Identifies stream data as an FST (and its endianity)
static const int32 kFstMagicNumber = 2125659606;

// Check Fst magic number and read in Fst header.
bool FstHeader::Read(istream &strm, const string &source) {
  int32 magic_number = 0;
  ReadType(strm, &magic_number);
  if (magic_number != kFstMagicNumber) {
    LOG(ERROR) << "FstHeader::Read: Bad FST header: " << source;
    return false;
  }

  ReadType(strm, &fsttype_);
  ReadType(strm, &arctype_);
  ReadType(strm, &version_);
  ReadType(strm, &flags_);
  ReadType(strm, &properties_);
  ReadType(strm, &start_);
  ReadType(strm, &numstates_);
  ReadType(strm, &numarcs_);
  if (!strm)
    LOG(ERROR) << "FstHeader::Read: read failed: " << source;
  return strm;
}

// Write Fst magic number and Fst header.
bool FstHeader::Write(ostream &strm, const string &source) const {
  WriteType(strm, kFstMagicNumber);
  WriteType(strm, fsttype_);
  WriteType(strm, arctype_);
  WriteType(strm, version_);
  WriteType(strm, flags_);
  WriteType(strm, properties_);
  WriteType(strm, start_);
  WriteType(strm, numstates_);
  WriteType(strm, numarcs_);
  if (!strm)
    LOG(ERROR) << "FstHeader::Write: write failed: " << source;
  return strm;
}

}
