// util.h
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
// FST utility inline definitions.

#ifndef FST_LIB_UTIL_H__
#define FST_LIB_UTIL_H__

#include <iostream>

namespace fst {

//
// UTILITIES FOR TYPE I/O
//

// Read some types from an input stream.

// Generic case.
template <typename T>
inline istream &ReadType(istream &strm, T *t) {
  return strm.read(reinterpret_cast<char *>(t), sizeof(T));
}

// String case.
inline istream &ReadType(istream &strm, string *s) {
  s->clear();
  int32 ns = 0;
  strm.read(reinterpret_cast<char *>(&ns), sizeof(ns));
  for (int i = 0; i < ns; ++i) {
    char c;
    strm.read(&c, 1);
    *s += c;
  }
  return strm;
}

// Write some types to an output stream.

// Generic case.
template <typename T>
inline ostream &WriteType(ostream &strm, const T t) {
  return strm.write(reinterpret_cast<const char *>(&t), sizeof(T));
}

// String case.
inline ostream &WriteType(ostream &strm, const string s) {
  int32 ns = s.size();
  strm.write(reinterpret_cast<const char *>(&ns), sizeof(ns));
  return strm.write(s.data(), ns);
}


}  // namespace fst;

#endif  // FST_LIB_UTIL_H__
