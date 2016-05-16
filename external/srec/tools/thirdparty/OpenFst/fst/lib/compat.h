// compat.h
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
// Google compatibility declarations and inline definitions.

#ifndef FST_COMPAT_H__
#define FST_COMPAT_H__

// for STL
#include <cassert>
#include <cstdio>
#include <iostream>
#include <map>
#include <string>
#include <vector>

#include <ext/hash_map>
#include <fcntl.h>
#include <pthread.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>

// exact size types
typedef short int16;
typedef int int32;
typedef long long int64;

typedef unsigned short uint16;
typedef unsigned int uint32;
typedef unsigned long long uint64;

using namespace std;

// make copy constructor and operator= private
#define DISALLOW_EVIL_CONSTRUCTORS(type)    \
  type(const type&);                        \
  void operator=(const type&)

// thread control
class Mutex {
 public:
  Mutex();

 private:
  DISALLOW_EVIL_CONSTRUCTORS(Mutex);
};

class MutexLock {
 public:
  MutexLock(Mutex *);

 private:
  DISALLOW_EVIL_CONSTRUCTORS(MutexLock);
};


// flags
#define DECLARE_bool(name) extern bool FLAGS_ ## name
#define DECLARE_string(name) extern string FLAGS_ ## name
#define DECLARE_int32(name) extern int32 FLAGS_ ## name
#define DECLARE_int64(name) extern int64 FLAGS_ ## name
#define DECLARE_double(name) extern double FLAGS_ ## name

template <typename T>
struct FlagDescription {
  FlagDescription(T *addr, const char *doc, const char *type, const T val)
      : address(addr), doc_string(doc), type_name(type), default_value(val) {}

  T *address;
  const char *doc_string;
  const char *type_name;
  const T default_value;
};

template <typename T>
class FlagRegister {
 public:
  static FlagRegister<T> *GetRegister() {
    pthread_once(&register_init_, &FlagRegister<T>::Init);
    return register_;
  }

  const FlagDescription<T> &GetFlagDescription(const string &name) const {
    MutexLock l(register_lock_);
    typename map< string, FlagDescription<T> >::const_iterator it =
      flag_table_.find(name);
    return it != flag_table_.end() ? it->second : 0;
  }
  void SetDescription(const string &name, const FlagDescription<T> &desc) {
    MutexLock l(register_lock_);
    flag_table_.insert(make_pair(name, desc));
  }

  bool SetFlag(const string &val, bool *address) const {
    if (val == "true" || val == "1" || val.empty()) {
      *address = true;
      return true;
    } else if (val == "false" || val == "0") {
      *address = false;
      return true;
    }
    else {
      return false;
    }
  }
  bool SetFlag(const string &val, string *address) const {
    *address = val;
    return true;
  }
  bool SetFlag(const string &val, int32 *address) const {
    char *p = 0;
    *address = strtol(val.c_str(), &p, 0);
    return !val.empty() && *p == '\0';
  }
  bool SetFlag(const string &val, int64 *address) const {
    char *p = 0;
    *address = strtoll(val.c_str(), &p, 0);
    return !val.empty() && *p == '\0';
  }
  bool SetFlag(const string &val, double *address) const {
    char *p = 0;
    *address = strtod(val.c_str(), &p);
    return !val.empty() && *p == '\0';
  }

  bool InitFlag(const string &arg, const string &val) const {
    for (typename map< string, FlagDescription<T> >::const_iterator it =
           flag_table_.begin();
         it != flag_table_.end();
         ++it) {
      const string &name = it->first;
      const FlagDescription<T> &desc = it->second;
      if (arg == name)
        return SetFlag(val, desc.address);
    }
    return false;
  }

  void ShowDefault(bool default_value) const {
    std::cout << ", default = ";
    std::cout << (default_value ? "true" : "false");
  }
  void ShowDefault(const string &default_value) const {
    std::cout << ", default = ";
    std::cout << "\"" << default_value << "\"";
  }
  template<typename V> void ShowDefault(const V& default_value) const {
    std::cout << ", default = ";
    std::cout << default_value;
  }
  void ShowUsage() const {
    for (typename map< string, FlagDescription<T> >::const_iterator it =
           flag_table_.begin();
         it != flag_table_.end();
         ++it) {
      const string &name = it->first;
      const FlagDescription<T> &desc = it->second;
      std::cout << "    --" << name
           << ": type = " << desc.type_name;
      ShowDefault(desc.default_value);
      std::cout << "\n      " << desc.doc_string  << "\n";
    }
  }

 private:
  static void Init() {
    register_lock_ = new Mutex;
    register_ = new FlagRegister<T>;
  }
  static pthread_once_t register_init_;   // ensures only called once
  static Mutex* register_lock_;           // multithreading lock
  static FlagRegister<T> *register_;

  map< string, FlagDescription<T> > flag_table_;
};

template <class T>
pthread_once_t FlagRegister<T>::register_init_ = PTHREAD_ONCE_INIT;

template <class T>
Mutex *FlagRegister<T>::register_lock_ = 0;

template <class T>
FlagRegister<T> *FlagRegister<T>::register_ = 0;


template <typename T>
class FlagRegisterer {
 public:
  FlagRegisterer(const string &name, const FlagDescription<T> &desc) {
    FlagRegister<T> *registr = FlagRegister<T>::GetRegister();
    registr->SetDescription(name, desc);
  }

 private:
  DISALLOW_EVIL_CONSTRUCTORS(FlagRegisterer);
};


#define DEFINE_VAR(type, name, value, doc)                                \
  type FLAGS_ ## name = value;                                            \
  static FlagRegisterer<type>                                             \
  name ## _flags_registerer(#name, FlagDescription<type>(&FLAGS_ ## name, \
                                                         doc,             \
                                                         #type,           \
                                                         value))

#define DEFINE_bool(name, value, doc) DEFINE_VAR(bool, name, value, doc)
#define DEFINE_string(name, value, doc) DEFINE_VAR(string, name, value, doc)
#define DEFINE_int32(name, value, doc) DEFINE_VAR(int32, name, value, doc)
#define DEFINE_int64(name, value, doc) DEFINE_VAR(int64, name, value, doc)
#define DEFINE_double(name, value, doc) DEFINE_VAR(double, name, value, doc)

void InitFst(const char *usage, int *argc, char ***argv, bool remove_flags);

void ShowUsage();


// checking
#define CHECK(x) assert(x)
#define CHECK_EQ(x, y) assert((x) == (y))

// logging
DECLARE_int32(v);

// tmp directory
DECLARE_string(tmpdir);

class LogMessage {
 public:
  LogMessage(const string &type) : fatal_(type == "FATAL") {
    std::cerr << type << ": ";
  }
  ~LogMessage() {
    std::cerr << endl;
    if(fatal_)
      exit(1);
  }
  ostream &stream() { return std::cerr; }

 private:
  bool fatal_;
};

#define LOG(type) LogMessage(#type).stream()
#define VLOG(level) if ((level) <= FLAGS_v) LOG(INFO)


// string utilities
void SplitToVector(char *line, const char *delim,
                   vector<char *> *vec, bool omit_empty_strings);

// Downcasting
template<typename To, typename From>
inline To down_cast(From* f) {
  return static_cast<To>(f);
}

// Bitcasting
template <class Dest, class Source>
inline Dest bit_cast(const Source& source) {
  // Compile time assertion: sizeof(Dest) == sizeof(Source)
  // A compile error here means your Dest and Source have different sizes.
  typedef char VerifySizesAreEqual [sizeof(Dest) == sizeof(Source) ? 1 :
                                    -1];
  Dest dest;
  memcpy(&dest, &source, sizeof(dest));
  return dest;
}

// MD5 checksums
class MD5 {
 public:
  MD5();
  void Reset();
  void Update(void const *data, int size);
  string Digest();

 private:
  DISALLOW_EVIL_CONSTRUCTORS(MD5);
};

// Hashing functions
namespace __gnu_cxx {

template<> struct hash<int64> {
  size_t operator()(int64 x) const {
    return x;
  }
};

template<> struct hash<string> {
  size_t operator()(const string &s) const {
    return __stl_hash_string(s.c_str());
  }
};

}  // namespace __gnu_cxx

#endif  // FST_COMPAT_H__
