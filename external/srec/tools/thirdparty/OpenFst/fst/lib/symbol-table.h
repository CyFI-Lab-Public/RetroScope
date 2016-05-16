// symbol-table.h
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
// Classes to provide symbol-to-integer and integer-to-symbol mappings.

#ifndef FST_LIB_SYMBOL_TABLE_H__
#define FST_LIB_SYMBOL_TABLE_H__

#include <ext/hash_map>
using __gnu_cxx::hash_map;
#include <fstream>
#include <iostream>
#include <string>
#include <vector>

#include "fst/lib/compat.h"



DECLARE_bool(fst_compat_symbols);

namespace fst {

class SymbolTableImpl {
  friend class SymbolTableIterator;
 public:
  SymbolTableImpl(const string &name)
      : name_(name), available_key_(0), ref_count_(1),
        check_sum_finalized_(false) {}
  ~SymbolTableImpl() {
    for (size_t i = 0; i < symbols_.size(); ++i)
      delete[] symbols_[i];
  }

  int64 AddSymbol(const string& symbol, int64 key);

  int64 AddSymbol(const string& symbol) {
    int64 key = Find(symbol);
    return (key == -1) ? AddSymbol(symbol, available_key_++) : key;
  }

  void AddTable(SymbolTableImpl* table) {
    for (size_t i = 0; i < table->symbols_.size(); ++i) {
      AddSymbol(table->symbols_[i]);
    }
  }

  static SymbolTableImpl* ReadText(const string& filename);

  static SymbolTableImpl* Read(istream &strm, const string& source);

  bool Write(ostream &strm) const;

  bool WriteText(ostream &strm) const;

  //
  // Return the string associated with the key. If the key is out of
  // range (<0, >max), return an empty string.
  string Find(int64 key) const {
    hash_map<int64, string>::const_iterator it =
      key_map_.find(key);
    if (it == key_map_.end()) {
      return "";
    }
    return it->second;
  }

  //
  // Return the key associated with the symbol. If the symbol
  // does not exists, return -1.
  int64 Find(const string& symbol) const {
    return Find(symbol.c_str());
  }

  //
  // Return the key associated with the symbol. If the symbol
  // does not exists, return -1.
  int64 Find(const char* symbol) const {
    hash_map<string, int64>::const_iterator it =
      symbol_map_.find(symbol);
    if (it == symbol_map_.end()) {
      return -1;
    }
    return it->second;
  }

  const string& Name() const { return name_; }

  int IncrRefCount() const {
    return ++ref_count_;
  }
  int DecrRefCount() const {
    return --ref_count_;
  }

  string CheckSum() const {
    if (!check_sum_finalized_) {
      RecomputeCheckSum();
      check_sum_string_ = check_sum_.Digest();
    }
    return check_sum_string_;
  }

  int64 AvailableKey() const {
    return available_key_;
  }

  // private support methods
 private:
  void RecomputeCheckSum() const;
  static SymbolTableImpl* Read1(istream &, const string &);

  string name_;
  int64 available_key_;
  vector<const char *> symbols_;
  hash_map<int64, string> key_map_;
  hash_map<string, int64> symbol_map_;

  mutable int ref_count_;
  mutable bool check_sum_finalized_;
  mutable MD5 check_sum_;
  mutable string check_sum_string_;

  DISALLOW_EVIL_CONSTRUCTORS(SymbolTableImpl);
};


class SymbolTableIterator;

//
// \class SymbolTable
// \brief Symbol (string) to int and reverse mapping
//
// The SymbolTable implements the mappings of labels to strings and reverse.
// SymbolTables are used to describe the alphabet of the input and output
// labels for arcs in a Finite State Transducer.
//
// SymbolTables are reference counted and can therefore be shared across
// multiple machines. For example a language model grammar G, with a
// SymbolTable for the words in the language model can share this symbol
// table with the lexical representation L o G.
//
class SymbolTable {
  friend class SymbolTableIterator;
 public:
  static const int64 kNoSymbol = -1;

  // Construct symbol table with a unique name.
  SymbolTable(const string& name) : impl_(new SymbolTableImpl(name)) {}

  // Create a reference counted copy.
  SymbolTable(const SymbolTable& table) : impl_(table.impl_) {
    impl_->IncrRefCount();
  }

  // Derefence implentation object. When reference count hits 0, delete
  // implementation.
  ~SymbolTable() {
    if (!impl_->DecrRefCount()) delete impl_;
  }

  // create a reference counted copy
  SymbolTable* Copy() const {
    return new SymbolTable(*this);
  }

  // Add a symbol with given key to table. A symbol table also
  // keeps track of the last available key (highest key value in
  // the symbol table).
  //
  // \param symbol string symbol to add
  // \param key associated key for string symbol
  // \return the key created by the symbol table. Symbols allready added to
  //         the symbol table will not get a different key.
  int64 AddSymbol(const string& symbol, int64 key) {
    return impl_->AddSymbol(symbol, key);
  }

  // Add a symbol to the table. The associated value key is automatically
  // assigned by the symbol table.
  //
  // \param symbol string to add to the table
  // \return the value key assigned to the associated string symbol
  int64 AddSymbol(const string& symbol) {
    return impl_->AddSymbol(symbol);
  }

  // Add another symbol table to this table. All key values will be offset
  // by the current available key (highest key value in the symbol table).
  // Note string symbols with the same key value with still have the same
  // key value after the symbol table has been merged, but a different
  // value. Adding symbol tables do not result in changes in the base table.
  //
  // Merging N symbol tables is often useful when combining the various
  // name spaces of transducers to a unified representation.
  //
  // \param table the symbol table to add to this table
  void AddTable(const SymbolTable& table) {
    return impl_->AddTable(table.impl_);
  }

  // return the name of the symbol table
  const string& Name() const {
    return impl_->Name();
  }

  // return the MD5 check-sum for this table. All new symbols added to
  // the table will result in an updated checksum.
  string CheckSum() const {
    return impl_->CheckSum();
  }

  // read an ascii representation of the symbol table
  static SymbolTable* ReadText(const string& filename) {
    SymbolTableImpl* impl = SymbolTableImpl::ReadText(filename);
    if (!impl)
      return 0;
    else
      return new SymbolTable(impl);
  }

  // read a binary dump of the symbol table
  static SymbolTable* Read(istream &strm, const string& source) {
    SymbolTableImpl* impl = SymbolTableImpl::Read(strm, source);
    if (!impl)
      return 0;
    else
      return new SymbolTable(impl);
  }

  // read a binary dump of the symbol table
  static SymbolTable* Read(const string& filename) {
    ifstream strm(filename.c_str());
    if (!strm) {
      LOG(ERROR) << "SymbolTable::Read: Can't open file " << filename;
      return 0;
    }
    return Read(strm, filename);
  }

  bool Write(ostream  &strm) const {
    return impl_->Write(strm);
  }

  bool Write(const string& filename) const {
    ofstream strm(filename.c_str());
    if (!strm) {
      LOG(ERROR) << "SymbolTable::Write: Can't open file " << filename;
      return false;
    }
    return Write(strm);
  }

  // Dump an ascii text representation of the symbol table
  bool WriteText(ostream &strm) const {
    return impl_->WriteText(strm);
  }

  // Dump an ascii text representation of the symbol table
  bool WriteText(const string& filename) const {
    ofstream strm(filename.c_str());
    if (!strm) {
      LOG(ERROR) << "SymbolTable::WriteText: Can't open file " << filename;
      return false;
    }
    return WriteText(strm);
  }

  // Return the string associated with the key. If the key is out of
  // range (<0, >max), log error and return an empty string.
  string Find(int64 key) const {
    return impl_->Find(key);
  }

  // Return the key associated with the symbol. If the symbol
  // does not exists, log error and  return -1
  int64 Find(const string& symbol) const {
    return impl_->Find(symbol);
  }

  // Return the key associated with the symbol. If the symbol
  // does not exists, log error and  return -1
  int64 Find(const char* symbol) const {
    return impl_->Find(symbol);
  }

  // return the current available key (i.e highest key number) in
  // the symbol table
  int64 AvailableKey(void) const {
    return impl_->AvailableKey();
  }

 protected:
  explicit SymbolTable(SymbolTableImpl* impl) : impl_(impl) {}

  const SymbolTableImpl* Impl() const {
    return impl_;
  }

 private:
  SymbolTableImpl* impl_;


  void operator=(const SymbolTable &table);  // disallow
};


//
// \class SymbolTableIterator
// \brief Iterator class for symbols in a symbol table
class SymbolTableIterator {
 public:
  // Constructor creates a refcounted copy of underlying implementation
  SymbolTableIterator(const SymbolTable& symbol_table) {
    impl_ = symbol_table.Impl();
    impl_->IncrRefCount();
    pos_ = 0;
    size_ = impl_->symbols_.size();
  }

  // decrement implementation refcount, and delete if 0
  ~SymbolTableIterator() {
    if (!impl_->DecrRefCount()) delete impl_;
  }

  // is iterator done
  bool Done(void) {
    return (pos_ == size_);
  }

  // return the Value() of the current symbol (in64 key)
  int64 Value(void) {
    return impl_->Find(impl_->symbols_[pos_]);
  }

  // return the string of the current symbol
  const char* Symbol(void) {
    return impl_->symbols_[pos_];
  }

  // advance iterator forward
  void Next(void) {
    if (Done()) return;
    ++pos_;
  }

  // reset iterator
  void Reset(void) {
    pos_ = 0;
  }

 private:
  const SymbolTableImpl* impl_;
  size_t pos_;
  size_t size_;
};


// Tests compatibilty between two sets of symbol tables
inline bool CompatSymbols(const SymbolTable *syms1,
                          const SymbolTable *syms2) {
  if (!FLAGS_fst_compat_symbols)
    return true;
  else if (!syms1 && !syms2)
    return true;
  else if (syms1 && !syms2 || !syms1 && syms2)
    return false;
  else
    return syms1->CheckSum() == syms2->CheckSum();
}

}  // namespace fst

#endif  // FST_LIB_SYMBOL_TABLE_H__
