// encode.h
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
// Class to encode and decoder an fst.

#ifndef FST_LIB_ENCODE_H__
#define FST_LIB_ENCODE_H__

#include "fst/lib/map.h"
#include "fst/lib/rmfinalepsilon.h"

namespace fst {

static const uint32 kEncodeLabels = 0x00001;
static const uint32 kEncodeWeights  = 0x00002;

enum EncodeType { ENCODE = 1, DECODE = 2 };

// Identifies stream data as an encode table (and its endianity)
static const int32 kEncodeMagicNumber = 2129983209;


// The following class encapsulates implementation details for the
// encoding and decoding of label/weight tuples used for encoding
// and decoding of Fsts. The EncodeTable is bidirectional. I.E it
// stores both the Tuple of encode labels and weights to a unique
// label, and the reverse.
template <class A>  class EncodeTable {
 public:
  typedef typename A::Label Label;
  typedef typename A::Weight Weight;

  // Encoded data consists of arc input/output labels and arc weight
  struct Tuple {
    Tuple() {}
    Tuple(Label ilabel_, Label olabel_, Weight weight_)
        : ilabel(ilabel_), olabel(olabel_), weight(weight_) {}
    Tuple(const Tuple& tuple)
        : ilabel(tuple.ilabel), olabel(tuple.olabel), weight(tuple.weight) {}

    Label ilabel;
    Label olabel;
    Weight weight;
  };

  // Comparison object for hashing EncodeTable Tuple(s).
  class TupleEqual {
   public:
    bool operator()(const Tuple* x, const Tuple* y) const {
      return (x->ilabel == y->ilabel &&
              x->olabel == y->olabel &&
              x->weight == y->weight);
    }
  };

  // Hash function for EncodeTabe Tuples. Based on the encode flags
  // we either hash the labels, weights or compbination of them.
  class TupleKey {
    static const int kPrime = 7853;
   public:
    TupleKey()
        : encode_flags_(kEncodeLabels | kEncodeWeights) {}

    TupleKey(const TupleKey& key)
        : encode_flags_(key.encode_flags_) {}

    explicit TupleKey(uint32 encode_flags)
        : encode_flags_(encode_flags) {}

    size_t operator()(const Tuple* x) const {
      int lshift = x->ilabel % kPrime;
      int rshift = sizeof(size_t) - lshift;
      size_t hash = x->ilabel << lshift;
      if (encode_flags_ & kEncodeLabels) hash ^= x->olabel >> rshift;
      if (encode_flags_ & kEncodeWeights)  hash ^= x->weight.Hash();
      return hash;
    }

   private:
    int32 encode_flags_;
  };

  typedef hash_map<const Tuple*,
                   Label,
                   TupleKey,
                   TupleEqual> EncodeHash;

  explicit EncodeTable(uint32 encode_flags)
      : flags_(encode_flags),
        encode_hash_(1024, TupleKey(encode_flags)) {}

  ~EncodeTable() {
    for (size_t i = 0; i < encode_tuples_.size(); ++i) {
      delete encode_tuples_[i];
    }
  }

  // Given an arc encode either input/ouptut labels or input/costs or both
  Label Encode(const A &arc) {
    const Tuple tuple(arc.ilabel,
                      flags_ & kEncodeLabels ? arc.olabel : 0,
                      flags_ & kEncodeWeights ? arc.weight : Weight::One());
    typename EncodeHash::const_iterator it = encode_hash_.find(&tuple);
    if (it == encode_hash_.end()) {
      encode_tuples_.push_back(new Tuple(tuple));
      encode_hash_[encode_tuples_.back()] = encode_tuples_.size();
      return encode_tuples_.size();
    } else {
      return it->second;
    }
  }

  // Given an encode arc Label decode back to input/output labels and costs
  const Tuple* Decode(Label key) {
    return key <= (Label)encode_tuples_.size() ? encode_tuples_[key - 1] : 0; 
  }

  bool Write(ostream &strm, const string &source) const {
    WriteType(strm, kEncodeMagicNumber);
    WriteType(strm, flags_);
    int64 size = encode_tuples_.size();
    WriteType(strm, size);
    for (size_t i = 0;  i < size; ++i) {
      const Tuple* tuple = encode_tuples_[i];
      WriteType(strm, tuple->ilabel);
      WriteType(strm, tuple->olabel);
      tuple->weight.Write(strm);
    }
    strm.flush();
    if (!strm)
      LOG(ERROR) << "EncodeTable::Write: write failed: " << source;
    return strm;
  }

  bool Read(istream &strm, const string &source) {
    encode_tuples_.clear();
    encode_hash_.clear();
    int32 magic_number = 0;
    ReadType(strm, &magic_number);
    if (magic_number != kEncodeMagicNumber) {
      LOG(ERROR) << "EncodeTable::Read: Bad encode table header: " << source;
      return false;
    }
    ReadType(strm, &flags_);
    int64 size;
    ReadType(strm, &size);
    if (!strm) {
      LOG(ERROR) << "EncodeTable::Read: read failed: " << source;
      return false;
    }
    for (size_t i = 0; i < size; ++i) {
      Tuple* tuple = new Tuple();
      ReadType(strm, &tuple->ilabel);
      ReadType(strm, &tuple->olabel);
      tuple->weight.Read(strm);
      encode_tuples_.push_back(tuple);
      encode_hash_[encode_tuples_.back()] = encode_tuples_.size();
    }
    if (!strm)
      LOG(ERROR) << "EncodeTable::Read: read failed: " << source;
    return strm;
  }

  const uint32 flags() const { return flags_; }
 private:
  uint32 flags_;
  vector<Tuple*> encode_tuples_;
  EncodeHash encode_hash_;

  DISALLOW_EVIL_CONSTRUCTORS(EncodeTable);
};


// A mapper to encode/decode weighted transducers. Encoding of an
// Fst is useful for performing classical determinization or minimization
// on a weighted transducer by treating it as an unweighted acceptor over
// encoded labels.
//
// The Encode mapper stores the encoding in a local hash table (EncodeTable)
// This table is shared (and reference counted) between the encoder and
// decoder. A decoder has read only access to the EncodeTable.
//
// The EncodeMapper allows on the fly encoding of the machine. As the
// EncodeTable is generated the same table may by used to decode the machine
// on the fly. For example in the following sequence of operations
//
//  Encode -> Determinize -> Decode
//
// we will use the encoding table generated during the encode step in the
// decode, even though the encoding is not complete.
//
template <class A> class EncodeMapper {
  typedef typename A::Weight Weight;
  typedef typename A::Label  Label;
 public:
  EncodeMapper(uint32 flags, EncodeType type)
    : ref_count_(1), flags_(flags), type_(type),
      table_(new EncodeTable<A>(flags)) {}

  EncodeMapper(const EncodeMapper& mapper)
      : ref_count_(mapper.ref_count_ + 1),
        flags_(mapper.flags_),
        type_(mapper.type_),
        table_(mapper.table_) { }

  // Copy constructor but setting the type, typically to DECODE
  EncodeMapper(const EncodeMapper& mapper, EncodeType type)
      : ref_count_(mapper.ref_count_ + 1),
        flags_(mapper.flags_),
        type_(type),
        table_(mapper.table_) { }

  ~EncodeMapper() {
    if (--ref_count_ == 0) delete table_;
  }

  A operator()(const A &arc) {
    if (type_ == ENCODE) {  // labels and/or weights to single label
      if ((arc.nextstate == kNoStateId && !(flags_ & kEncodeWeights)) ||
          (arc.nextstate == kNoStateId && (flags_ & kEncodeWeights) &&
           arc.weight == Weight::Zero())) {
        return arc;
      } else {
        Label label = table_->Encode(arc);
        return A(label,
                 flags_ & kEncodeLabels ? label : arc.olabel,
                 flags_ & kEncodeWeights ? Weight::One() : arc.weight,
                 arc.nextstate);
      }
    } else {
      if (arc.nextstate == kNoStateId) {
        return arc;
      } else {
        const typename EncodeTable<A>::Tuple* tuple =
          table_->Decode(arc.ilabel);
        return A(tuple->ilabel,
                 flags_ & kEncodeLabels ? tuple->olabel : arc.olabel,
                 flags_ & kEncodeWeights ? tuple->weight : arc.weight,
                 arc.nextstate);;
      }
    }
  }

  uint64 Properties(uint64 props) {
    uint64 mask = kFstProperties;
    if (flags_ & kEncodeLabels)
      mask &= kILabelInvariantProperties & kOLabelInvariantProperties;
    if (flags_ & kEncodeWeights)
      mask &= kILabelInvariantProperties & kWeightInvariantProperties &
          (type_ == ENCODE ? kAddSuperFinalProperties :
           kRmSuperFinalProperties);
    return props & mask;
  }


  MapFinalAction FinalAction() const {
    return (type_ == ENCODE && (flags_ & kEncodeWeights)) ?
                   MAP_REQUIRE_SUPERFINAL : MAP_NO_SUPERFINAL;
  }

  const uint32 flags() const { return flags_; }
  const EncodeType type() const { return type_; }

  bool Write(ostream &strm, const string& source) {
    return table_->Write(strm, source);
  }

  bool Write(const string& filename) {
    ofstream strm(filename.c_str());
    if (!strm) {
      LOG(ERROR) << "EncodeMap: Can't open file: " << filename;
      return false;
    }
    return Write(strm, filename);
  }

  static EncodeMapper<A> *Read(istream &strm,
                               const string& source, EncodeType type) {
    EncodeTable<A> *table = new EncodeTable<A>(0);
    bool r = table->Read(strm, source);
    return r ? new EncodeMapper(table->flags(), type, table) : 0;
  }

  static EncodeMapper<A> *Read(const string& filename, EncodeType type) {
    ifstream strm(filename.c_str());
    if (!strm) {
      LOG(ERROR) << "EncodeMap: Can't open file: " << filename;
      return false;
    }
    return Read(strm, filename, type);
  }

 private:
  uint32  ref_count_;
  uint32  flags_;
  EncodeType type_;
  EncodeTable<A>* table_;

  explicit EncodeMapper(uint32 flags, EncodeType type, EncodeTable<A> *table)
      : ref_count_(1), flags_(flags), type_(type), table_(table) {}
  void operator=(const EncodeMapper &);  // Disallow.
};


// Complexity: O(nstates + narcs)
template<class A> inline
void Encode(MutableFst<A> *fst, EncodeMapper<A>* mapper) {
  Map(fst, mapper);
}


template<class A> inline
void Decode(MutableFst<A>* fst, const EncodeMapper<A>& mapper) {
  Map(fst, EncodeMapper<A>(mapper, DECODE));
  RmFinalEpsilon(fst);
}


// On the fly label and/or weight encoding of input Fst
//
// Complexity:
// - Constructor: O(1)
// - Traversal: O(nstates_visited + narcs_visited), assuming constant
//   time to visit an input state or arc.
template <class A>
class EncodeFst : public MapFst<A, A, EncodeMapper<A> > {
 public:
  typedef A Arc;
  typedef EncodeMapper<A> C;

  EncodeFst(const Fst<A> &fst, EncodeMapper<A>* encoder)
      : MapFst<A, A, C>(fst, encoder, MapFstOptions()) {}

  EncodeFst(const Fst<A> &fst, const EncodeMapper<A>& encoder)
      : MapFst<A, A, C>(fst, encoder, MapFstOptions()) {}

  EncodeFst(const EncodeFst<A> &fst)
      : MapFst<A, A, C>(fst) {}

  virtual EncodeFst<A> *Copy() const { return new EncodeFst(*this); }
};


// On the fly label and/or weight encoding of input Fst
//
// Complexity:
// - Constructor: O(1)
// - Traversal: O(nstates_visited + narcs_visited), assuming constant
//   time to visit an input state or arc.
template <class A>
class DecodeFst : public MapFst<A, A, EncodeMapper<A> > {
 public:
  typedef A Arc;
  typedef EncodeMapper<A> C;

  DecodeFst(const Fst<A> &fst, const EncodeMapper<A>& encoder)
      : MapFst<A, A, C>(fst,
                            EncodeMapper<A>(encoder, DECODE),
                            MapFstOptions()) {}

  DecodeFst(const EncodeFst<A> &fst)
      : MapFst<A, A, C>(fst) {}

  virtual DecodeFst<A> *Copy() const { return new DecodeFst(*this); }
};


// Specialization for EncodeFst.
template <class A>
class StateIterator< EncodeFst<A> >
    : public StateIterator< MapFst<A, A, EncodeMapper<A> > > {
 public:
  explicit StateIterator(const EncodeFst<A> &fst)
      : StateIterator< MapFst<A, A, EncodeMapper<A> > >(fst) {}
};


// Specialization for EncodeFst.
template <class A>
class ArcIterator< EncodeFst<A> >
    : public ArcIterator< MapFst<A, A, EncodeMapper<A> > > {
 public:
  ArcIterator(const EncodeFst<A> &fst, typename A::StateId s)
      : ArcIterator< MapFst<A, A, EncodeMapper<A> > >(fst, s) {}
};


// Specialization for DecodeFst.
template <class A>
class StateIterator< DecodeFst<A> >
    : public StateIterator< MapFst<A, A, EncodeMapper<A> > > {
 public:
  explicit StateIterator(const DecodeFst<A> &fst)
      : StateIterator< MapFst<A, A, EncodeMapper<A> > >(fst) {}
};


// Specialization for DecodeFst.
template <class A>
class ArcIterator< DecodeFst<A> >
    : public ArcIterator< MapFst<A, A, EncodeMapper<A> > > {
 public:
  ArcIterator(const DecodeFst<A> &fst, typename A::StateId s)
      : ArcIterator< MapFst<A, A, EncodeMapper<A> > >(fst, s) {}
};


// Useful aliases when using StdArc.
typedef EncodeFst<StdArc> StdEncodeFst;

typedef DecodeFst<StdArc> StdDecodeFst;

}

#endif  // FST_LIB_ENCODE_H__
