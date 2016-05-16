// fst.h
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
// Finite-State Transducer (FST) - abstract base class definition,
// state and arc iterator interface, and suggested base implementation.

#ifndef FST_LIB_FST_H__
#define FST_LIB_FST_H__

#include "fst/lib/arc.h"
#include "fst/lib/compat.h"
#include "fst/lib/properties.h"
#include "fst/lib/register.h"
#include "fst/lib/symbol-table.h"
#include "fst/lib/util.h"

namespace fst {

class FstHeader;
template <class A> class StateIteratorData;
template <class A> class ArcIteratorData;

struct FstReadOptions  {
  string source;                // Where you're reading from
  const FstHeader *header;      // Pointer to Fst header (if non-zero)
  const SymbolTable* isymbols;  // Pointer to input symbols (if non-zero)
  const SymbolTable* osymbols;  // Pointer to output symbols (if non-zero)

  explicit FstReadOptions(const string& src = "<unspecified>",
                          const FstHeader *hdr = 0,
                          const SymbolTable* isym = 0,
                          const SymbolTable* osym = 0)
      : source(src), header(hdr), isymbols(isym), osymbols(osym) {}
};


struct FstWriteOptions {
  string source;                    // Where you're writing to
  bool write_header;                // Write the header?
  bool write_isymbols;              // Write input symbols?
  bool write_osymbols;              // Write output symbols?

  explicit FstWriteOptions(const string& src = "<unspecifed>",
                           bool hdr = true, bool isym = true,
                           bool osym = true)
      : source(src), write_header(hdr),
        write_isymbols(isym),  write_osymbols(osym) {}
};

//
// Fst HEADER CLASS
//
// This is the recommended Fst file header representation.
//

class FstHeader {
 public:
  enum {
    HAS_ISYMBOLS = 1,                           // Has input symbol table
    HAS_OSYMBOLS = 2                            // Has output symbol table
  } Flags;

  FstHeader() : version_(0), flags_(0), properties_(0), start_(-1),
                numstates_(0), numarcs_(0) {}
  const string &FstType() const { return fsttype_; }
  const string &ArcType() const { return arctype_; }
  int32 Version() const { return version_; }
  int32 GetFlags() const { return flags_; }
  uint64 Properties() const { return properties_; }
  int64 Start() const { return start_; }
  int64 NumStates() const { return numstates_; }
  int64 NumArcs() const { return numarcs_; }

  void SetFstType(const string& type) { fsttype_ = type; }
  void SetArcType(const string& type) { arctype_ = type; }
  void SetVersion(int32 version) { version_ = version; }
  void SetFlags(int32 flags) { flags_ = flags; }
  void SetProperties(uint64 properties) { properties_ = properties; }
  void SetStart(int64 start) { start_ = start; }
  void SetNumStates(int64 numstates) { numstates_ = numstates; }
  void SetNumArcs(int64 numarcs) { numarcs_ = numarcs; }

  bool Read(istream &strm, const string &source);
  bool Write(ostream &strm, const string &source) const;

 private:
  string fsttype_;                   // E.g. "vector"
  string arctype_;                   // E.g. "standard"
  int32 version_;                    // Type version #
  int32 flags_;                      // File format bits
  uint64 properties_;                // FST property bits
  int64 start_;                      // Start state
  int64 numstates_;                  // # of states
  int64 numarcs_;                    // # of arcs
};

//
// Fst INTERFACE CLASS DEFINITION
//

// A generic FST, templated on the arc definition, with
// common-demoninator methods (use StateIterator and ArcIterator to
// iterate over its states and arcs).
template <class A>
class Fst {
 public:
  typedef A Arc;
  typedef typename A::Weight Weight;
  typedef typename A::StateId StateId;

  virtual ~Fst() {}

  virtual StateId Start() const = 0;          // Initial state

  virtual Weight Final(StateId) const = 0;    // State's final weight

  virtual size_t NumArcs(StateId) const = 0;  // State's arc count

  virtual size_t NumInputEpsilons(StateId)
      const = 0;                              // State's input epsilon count

  virtual size_t NumOutputEpsilons(StateId)
      const = 0;                              // State's output epsilon count

  // If test=false, return stored properties bits for mask (some poss. unknown)
  // If test=true, return property bits for mask (computing o.w. unknown)
  virtual uint64 Properties(uint64 mask, bool test)
      const = 0;  // Property bits

  virtual const string& Type() const = 0;    // Fst type name

  // Get a copy of this Fst.
  virtual Fst<A> *Copy() const = 0;
  // Read an Fst from an input stream; returns NULL on error

  static Fst<A> *Read(istream &strm, const FstReadOptions &opts) {
    FstReadOptions ropts(opts);
    FstHeader hdr;
    if (ropts.header)
      hdr = *opts.header;
    else {
      if (!hdr.Read(strm, opts.source))
        return 0;
      ropts.header = &hdr;
    }
    FstRegister<A> *registr = FstRegister<A>::GetRegister();
    const typename FstRegister<A>::Reader reader =
        registr->GetReader(hdr.FstType());
    if (!reader) {
      LOG(ERROR) << "Fst::Read: Unknown FST type \"" << hdr.FstType()
                 << "\" (arc type = \"" << A::Type()
                 << "\"): " << ropts.source;
      return 0;
    }
    return reader(strm, ropts);
  };

  // Read an Fst from a file; return NULL on error
  static Fst<A> *Read(const string &filename) {
    ifstream strm(filename.c_str());
    if (!strm) {
      LOG(ERROR) << "Fst::Read: Can't open file: " << filename;
      return 0;
    }
    return Read(strm, FstReadOptions(filename));
  }

  // Write an Fst to an output stream; return false on error
  virtual bool Write(ostream &strm, const FstWriteOptions &opts) const {
    LOG(ERROR) << "Fst::Write: No write method for " << Type() << " Fst type";
    return false;
  }

  // Write an Fst to a file; return false on error
  virtual bool Write(const string &filename) const {
    LOG(ERROR) << "Fst::Write: No write method for "
               << Type() << " Fst type: "
               << (filename.empty() ? "standard output" : filename);
    return false;
  }

  // Return input label symbol table; return NULL if not specified
  virtual const SymbolTable* InputSymbols() const = 0;

  // Return output label symbol table; return NULL if not specified
  virtual const SymbolTable* OutputSymbols() const = 0;

  // For generic state iterator construction; not normally called
  // directly by users.
  virtual void InitStateIterator(StateIteratorData<A> *) const = 0;

  // For generic arc iterator construction; not normally called
  // directly by users.
  virtual void InitArcIterator(StateId s, ArcIteratorData<A> *) const = 0;
};


//
// STATE and ARC ITERATOR DEFINITIONS
//

// State iterator interface templated on the Arc definition; used
// for StateIterator specializations returned by InitStateIterator.
template <class A>
class StateIteratorBase {
 public:
  typedef A Arc;
  typedef typename A::StateId StateId;

  virtual ~StateIteratorBase() {}
  virtual bool Done() const = 0;      // End of iterator?
  virtual StateId Value() const = 0;  // Current state (when !Done)
  virtual void Next() = 0;            // Advance to next state (when !Done)
  virtual void Reset() = 0;           // Return to initial condition
};


// StateIterator initialization data
template <class A> struct StateIteratorData {
  StateIteratorBase<A> *base;   // Specialized iterator if non-zero
  typename A::StateId nstates;  // O.w. total # of states
};


// Generic state iterator, templated on the FST definition
// - a wrapper around pointer to specific one.
// Here is a typical use: \code
//   for (StateIterator<StdFst> siter(fst);
//        !siter.Done();
//        siter.Next()) {
//     StateId s = siter.Value();
//     ...
//   } \endcode
template <class F>
class StateIterator {
 public:
  typedef typename F::Arc Arc;
  typedef typename Arc::StateId StateId;

  explicit StateIterator(const F &fst) : s_(0) {
    fst.InitStateIterator(&data_);
  }

  ~StateIterator() { if (data_.base) delete data_.base; }

  bool Done() const {
    return data_.base ? data_.base->Done() : s_ >= data_.nstates;
  }

  StateId Value() const { return data_.base ? data_.base->Value() : s_; }

  void Next() {
    if (data_.base)
      data_.base->Next();
    else
      ++s_;
  }

  void Reset() {
    if (data_.base)
      data_.base->Reset();
    else
      s_ = 0;
  }

 private:
  StateIteratorData<Arc> data_;
  StateId s_;
  DISALLOW_EVIL_CONSTRUCTORS(StateIterator);
};


// Arc iterator interface, templated on the Arc definition; used
// for Arc iterator specializations that are returned by InitArcIterator.
template <class A>
class ArcIteratorBase {
 public:
  typedef A Arc;
  typedef typename A::StateId StateId;

  virtual ~ArcIteratorBase() {}
  virtual bool Done() const = 0;       // End of iterator?
  virtual const A& Value() const = 0;  // Current state (when !Done)
  virtual void Next() = 0;             // Advance to next arc (when !Done)
  virtual void Reset() = 0;            // Return to initial condition
  virtual void Seek(size_t a) = 0;     // Random arc access by position
};


// ArcIterator initialization data
template <class A> struct ArcIteratorData {
  ArcIteratorBase<A> *base;  // Specialized iterator if non-zero
  const A *arcs;             // O.w. arcs pointer
  size_t narcs;              // ... and arc count
  int *ref_count;            // ... and reference count if non-zero
};


// Generic arc iterator, templated on the FST definition
// - a wrapper around pointer to specific one.
// Here is a typical use: \code
//   for (ArcIterator<StdFst> aiter(fst, s));
//        !aiter.Done();
//         aiter.Next()) {
//     StdArc &arc = aiter.Value();
//     ...
//   } \endcode
template <class F>
class ArcIterator {
   public:
  typedef typename F::Arc Arc;
  typedef typename Arc::StateId StateId;

  ArcIterator(const F &fst, StateId s) : i_(0) {
    fst.InitArcIterator(s, &data_);
  }

  ~ArcIterator() {
    if (data_.base)
      delete data_.base;
    else if (data_.ref_count)
    --(*data_.ref_count);
  }

  bool Done() const {
    return data_.base ?  data_.base->Done() : i_ >= data_.narcs;
  }

  const Arc& Value() const {
    return data_.base ? data_.base->Value() : data_.arcs[i_];
  }

  void Next() {
    if (data_.base)
      data_.base->Next();
    else
      ++i_;
  }

  void Reset() {
    if (data_.base)
      data_.base->Reset();
    else
      i_ = 0;
  }

  void Seek(size_t a) {
    if (data_.base)
      data_.base->Seek(a);
    else
      i_ = a;
  }

 private:
  ArcIteratorData<Arc> data_;
  size_t i_;
  DISALLOW_EVIL_CONSTRUCTORS(ArcIterator);
};


// A useful alias when using StdArc.
typedef Fst<StdArc> StdFst;


//
//  CONSTANT DEFINITIONS
//

const int kNoStateId   =  -1;  // Not a valid state ID
const int kNoLabel     =  -1;  // Not a valid label
const int kPhiLabel    =  -2;  // Failure transition label
const int kRhoLabel    =  -3;  // Matches o.w. unmatched labels (lib. internal)
const int kSigmaLabel  =  -4;  // Matches all labels in alphabet.


//
// Fst IMPLEMENTATION BASE
//
// This is the recommended Fst implementation base class. It will
// handle reference counts, property bits, type information and symbols.
//

template <class A> class FstImpl {
 public:
  typedef typename A::Weight Weight;
  typedef typename A::StateId StateId;

  FstImpl()
      : properties_(0), type_("null"), isymbols_(0), osymbols_(0),
        ref_count_(1) {}

  FstImpl(const FstImpl<A> &impl)
      : properties_(impl.properties_), type_(impl.type_),
        isymbols_(impl.isymbols_ ? new SymbolTable(impl.isymbols_) : 0),
        osymbols_(impl.osymbols_ ? new SymbolTable(impl.osymbols_) : 0),
        ref_count_(1) {}

  ~FstImpl() {
    delete isymbols_;
    delete osymbols_;
  }

  const string& Type() const { return type_; }

  void SetType(const string &type) { type_ = type; }

  uint64 Properties() const { return properties_; }

  uint64 Properties(uint64 mask) const { return properties_ & mask; }

  void SetProperties(uint64 props) { properties_ = props; }

  void SetProperties(uint64 props, uint64 mask) {
    properties_ &= ~mask;
    properties_ |= props & mask;
  }

  const SymbolTable* InputSymbols() const { return isymbols_; }

  const SymbolTable* OutputSymbols() const { return osymbols_; }

  SymbolTable* InputSymbols() { return isymbols_; }

  SymbolTable* OutputSymbols() { return osymbols_; }

  void SetInputSymbols(const SymbolTable* isyms) {
    if (isymbols_) delete isymbols_;
    isymbols_ = isyms ? isyms->Copy() : 0;
  }

  void SetOutputSymbols(const SymbolTable* osyms) {
    if (osymbols_) delete osymbols_;
    osymbols_ = osyms ? osyms->Copy() : 0;
  }

  int RefCount() const { return ref_count_; }

  int IncrRefCount() { return ++ref_count_; }

  int DecrRefCount() { return --ref_count_; }

  // Read-in header and symbols, initialize Fst, and return the header.
  // If opts.header is non-null, skip read-in and use the option value.
  // If opts.[io]symbols is non-null, read-in but use the option value.
  bool ReadHeaderAndSymbols(istream &strm, const FstReadOptions& opts,
                  int min_version, FstHeader *hdr) {
    if (opts.header)
      *hdr = *opts.header;
    else if (!hdr->Read(strm, opts.source))
      return false;
    if (hdr->FstType() != type_) {
      LOG(ERROR) << "FstImpl::ReadHeaderAndSymbols: Fst not of type \""
                 << type_ << "\": " << opts.source;
      return false;
    }
    if (hdr->ArcType() != A::Type()) {
      LOG(ERROR) << "FstImpl::ReadHeaderAndSymbols: Arc not of type \""
                 << A::Type()
                 << "\": " << opts.source;
      return false;
    }
    if (hdr->Version() < min_version) {
      LOG(ERROR) << "FstImpl::ReadHeaderAndSymbols: Obsolete "
                 << type_ << " Fst version: " << opts.source;
      return false;
    }
    properties_ = hdr->Properties();
    if (hdr->GetFlags() & FstHeader::HAS_ISYMBOLS)
      isymbols_ = SymbolTable::Read(strm, opts.source);
    if (hdr->GetFlags() & FstHeader::HAS_OSYMBOLS)
      osymbols_ =SymbolTable::Read(strm, opts.source);

    if (opts.isymbols) {
      delete isymbols_;
      isymbols_ = opts.isymbols->Copy();
    }
    if (opts.osymbols) {
      delete osymbols_;
      osymbols_ = opts.osymbols->Copy();
    }
    return true;
  }

  // Write-out header and symbols.
  // If a opts.header is false, skip writing header.
  // If opts.[io]symbols is false, skip writing those symbols.
  void WriteHeaderAndSymbols(ostream &strm, const FstWriteOptions& opts,
                             int version, FstHeader *hdr) const {
    if (opts.write_header) {
      hdr->SetFstType(type_);
      hdr->SetArcType(A::Type());
      hdr->SetVersion(version);
      hdr->SetProperties(properties_);
      int32 file_flags = 0;
      if (isymbols_ && opts.write_isymbols)
        file_flags |= FstHeader::HAS_ISYMBOLS;
      if (osymbols_ && opts.write_osymbols)
        file_flags |= FstHeader::HAS_OSYMBOLS;
      hdr->SetFlags(file_flags);
      hdr->Write(strm, opts.source);
    }
    if (isymbols_ && opts.write_isymbols) isymbols_->Write(strm);
    if (osymbols_ && opts.write_osymbols) osymbols_->Write(strm);
  }

 protected:
  uint64 properties_;           // Property bits

 private:
  string type_;                 // Unique name of Fst class
  SymbolTable *isymbols_;       // Ilabel symbol table
  SymbolTable *osymbols_;       // Olabel symbol table
  int ref_count_;               // Reference count

  void operator=(const FstImpl<A> &impl);  // disallow
};

}  // namespace fst;

#endif  // FST_LIB_FST_H__
