// symbol-table.cc
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

#include "fst/lib/symbol-table.h"
#include "fst/lib/util.h"

#include <string.h>

DEFINE_bool(fst_compat_symbols, true,
            "Require symbol tables to match when appropriate");

namespace fst {

// Maximum line length in textual symbols file.
const int kLineLen = 8096;

// Identifies stream data as a symbol table (and its endianity)
static const int32 kSymbolTableMagicNumber = 2125658996;

SymbolTableImpl* SymbolTableImpl::ReadText(const string &filename) {
  ifstream strm(filename.c_str());
  if (!strm) {
    LOG(ERROR) << "SymbolTable::ReadText: Can't open symbol file: "
               << filename;
    return 0;
  }

  SymbolTableImpl* impl = new SymbolTableImpl(filename);

  int64 nline = 0;
  char line[kLineLen];
  while (strm.getline(line, kLineLen)) {
    ++nline;
    vector<char *> col;
    SplitToVector(line, "\n\t ", &col, true);
    if (col.size() == 0)  // empty line
      continue;
    if (col.size() != 2) {
      LOG(ERROR) << "SymbolTable::ReadText: Bad number of columns (skipping), "
                 << "file = " << filename << ", line = " << nline;
      continue;
    }
    const char *symbol = col[0];
    const char *value = col[1];
    char *p;
    int64 key = strtoll(value, &p, 10);
    if (p < value + strlen(value) || key < 0) {
      LOG(ERROR) << "SymbolTable::ReadText: Bad non-negative integer \""
                 << value << "\" (skipping), "
                 << "file = " << filename << ", line = " << nline;
      continue;
    }
    impl->AddSymbol(symbol, key);
  }

  return impl;
}

void SymbolTableImpl::RecomputeCheckSum() const {
  check_sum_.Reset();
  for (size_t i = 0; i < symbols_.size(); ++i) {
    check_sum_.Update(symbols_[i], strlen(symbols_[i])+1);
  }
  check_sum_finalized_ = true;
}

int64 SymbolTableImpl::AddSymbol(const string& symbol, int64 key) {
  hash_map<string, int64>::const_iterator it =
    symbol_map_.find(symbol);
  if (it == symbol_map_.end()) {  // only add if not in table
    check_sum_finalized_ = false;

    char *csymbol = new char[symbol.size() + 1];
    strcpy(csymbol, symbol.c_str());
    symbols_.push_back(csymbol);
    key_map_[key] = csymbol;
    symbol_map_[csymbol] = key;

    if (key >= available_key_) {
      available_key_ = key + 1;
    }
  }

  return key;
}

SymbolTableImpl* SymbolTableImpl::Read(istream &strm,
                                       const string &source) {
  int32 magic_number = 0;
  ReadType(strm, &magic_number);
  if (magic_number != kSymbolTableMagicNumber) {
    LOG(ERROR) << "SymbolTable::Read: read failed";
    return 0;
  }
  string name;
  ReadType(strm, &name);
  SymbolTableImpl* impl = new SymbolTableImpl(name);
  ReadType(strm, &impl->available_key_);
  int64 size;
  ReadType(strm, &size);
  string symbol;
  int64 key = 0;
  for (size_t i = 0; i < size; ++i) {
    ReadType(strm, &symbol);
    ReadType(strm, &key);
    impl->AddSymbol(symbol, key);
  }
  if (!strm)
    LOG(ERROR) << "SymbolTable::Read: read failed";
  return impl;
}

bool SymbolTableImpl::Write(ostream &strm) const {
  WriteType(strm, kSymbolTableMagicNumber);
  WriteType(strm, name_);
  WriteType(strm, available_key_);
  int64 size = symbols_.size();
  WriteType(strm, size);
  for (size_t i = 0; i < symbols_.size(); ++i) {
    const string symbol = symbols_[i];
    WriteType(strm, symbol);
    hash_map<string, int64>::const_iterator it = symbol_map_.find(symbol);
    WriteType(strm, it->second);
  }
  strm.flush();
  if (!strm)
    LOG(ERROR) << "SymbolTable::Write: write failed";
  return strm;
}

bool SymbolTableImpl::WriteText(ostream &strm) const {
  for (size_t i = 0; i < symbols_.size(); ++i) {
    char line[kLineLen];
    snprintf(line, kLineLen, "%s\t%lld\n", symbols_[i], Find(symbols_[i]));
    strm.write(line, strlen(line));
  }
  strm.flush();
  if (!strm)
    LOG(ERROR) << "SymbolTable::WriteText: write failed";
  return strm;
}

}  // namespace fst
