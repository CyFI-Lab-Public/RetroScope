// fst-register.h
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
// Classes for registering derived Fsts for generic reading
//

#ifndef FST_LIB_REGISTER_H__
#define FST_LIB_REGISTER_H__

#include <map>

#include <dlfcn.h>
#include <pthread.h>

#include "fst/lib/compat.h"

extern "C" {
  typedef void (*FstInitFunc)();
}

namespace fst {

template <class A> class Fst;
struct FstReadOptions;

// This class holds the mapping from Fst name string to its reader
// and converter.
template <class A>
class FstRegister {
 public:
  typedef Fst<A> *(*Reader)(istream &strm, const FstReadOptions &opts);
  typedef Fst<A> *(*Converter)(const Fst<A> &fst);

  struct Entry {
    Reader reader;
    Converter converter;
    Entry() : reader(0), converter(0) {}
  };

  static FstRegister<A> *GetRegister() {
    pthread_once(&register_init_, &FstRegister<A>::Init);
    return register_;
  }

  const Reader GetReader(const string &type) const {
    return GetEntry(type).reader;
  }

  const Converter GetConverter(const string &type) const {
    return GetEntry(type).converter;
  }

  void SetEntry(const string &type, const Entry &entry) {
    MutexLock l(register_lock_);
    fst_table_.insert(make_pair(type, entry));
  }

 private:
  static void Init() {
    register_lock_ = new Mutex;
    register_ = new FstRegister<A>;
  }

  Entry LookupEntry(const string &type) const {
    MutexLock l(register_lock_);
    typename map<string, Entry>::const_iterator it = fst_table_.find(type);
    if (it != fst_table_.end())
      return it->second;
    else
      return Entry();
  }

  Entry GetEntry(const string &type) const {
#ifdef FST_DL
    Entry entry = LookupEntry(type);
    if (entry.reader)
      return entry;
    string so_file = type + "-fst.so";
    void *handle = dlopen(so_file.c_str(), RTLD_LAZY);
    if (handle == 0) {
      LOG(ERROR) << "FstRegister::GetEntry: " << dlerror();
      return entry;
    }
    string init_name = type + "_fst_init";
    FstInitFunc init_func =
        bit_cast<FstInitFunc>(dlsym(handle, init_name.c_str()));
    if (init_func == 0) {
      LOG(ERROR) << "FstRegister::GetEntry: " << dlerror();
      return entry;
    }
    (*init_func)();
#endif  // FST_DL
    return LookupEntry(type);
  }

  static pthread_once_t register_init_;   // ensures only called once
  static Mutex* register_lock_;           // multithreading lock
  static FstRegister<A> *register_;

  map<string, Entry> fst_table_;
};

template <class A>
pthread_once_t FstRegister<A>::register_init_ = PTHREAD_ONCE_INIT;

template <class A>
Mutex *FstRegister<A>::register_lock_ = 0;

template <class A>
FstRegister<A> *FstRegister<A>::register_ = 0;

// This class registers an Fst type for generic reading and creating.
// The Fst type must have a default constructor and a copy constructor
// from 'Fst<Arc>' for this to work.
template <class F>
class FstRegisterer {
 public:
  typedef typename F::Arc Arc;
  typedef typename FstRegister<Arc>::Entry Entry;
  typedef typename FstRegister<Arc>::Reader Reader;

  FstRegisterer() {
    F fst;
    F *(*reader)(istream &strm,
                 const FstReadOptions &opts) = &F::Read;
    Entry entry;
    entry.reader = reinterpret_cast<Reader>(reader);
    entry.converter = &FstRegisterer<F>::Convert;
    FstRegister<Arc> *registr = FstRegister<Arc>::GetRegister();
    registr->SetEntry(fst.Type(), entry);
  }

 private:
  static Fst<Arc> *Convert(const Fst<Arc> &fst) { return new F(fst); }
};


// Convenience macro to generate static FstRegisterer instance.
#define REGISTER_FST(F, A) \
static fst::FstRegisterer< F<A> > F ## _ ## A ## _registerer


// Converts an fst to type 'type'.
template <class A>
Fst<A> *Convert(const Fst<A> &fst, const string &ftype) {
  FstRegister<A> *registr = FstRegister<A>::GetRegister();
  const typename FstRegister<A>::Converter
      converter = registr->GetConverter(ftype);
  if (!converter) {
    string atype = A::Type();
    LOG(ERROR) << "Fst::Convert: Unknown FST type \"" << ftype
               << "\" (arc type = \"" << atype << "\")";
    return 0;
  }
  return converter(fst);
}

}  // namespace fst;

#endif  // FST_LIB_REGISTER_H__
