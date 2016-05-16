// compat.cc
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
// Google compatibility definitions.

#include <cstring>

#include "fst/lib/compat.h"

DEFINE_int32(v, 0, "verbose level");
DEFINE_bool(help, false, "verbose level");
DEFINE_string(tmpdir, "/tmp/", "temporary directory");

static string prog_name;
static string flag_usage;

static void out_of_memory() {
  fprintf(stderr, "%s: Memory allocation failed\n", prog_name.c_str());
  exit(1);
}

void InitFst(const char *usage, int *argc, char ***argv, bool remove_flags) {
  prog_name = (*argv)[0];
  set_new_handler(out_of_memory);

  flag_usage = usage;
  int index = 1;
  for (; index < *argc; ++index) {
    string argval = (*argv)[index];

    if (argval[0] != '-' || argval == "-")
      break;
    while (argval[0] == '-')
      argval = argval.substr(1);  // remove initial '-'s

    string arg = argval;
    string val = "";

    // split argval (arg=val) into arg and val
    size_t pos = argval.find("=");
    if (pos != string::npos) {
      arg = argval.substr(0, pos);
      val = argval.substr(pos + 1);
    }

    FlagRegister<bool> *bool_register =
      FlagRegister<bool>::GetRegister();
    if (bool_register->InitFlag(arg, val))
      continue;
    FlagRegister<string> *string_register =
      FlagRegister<string>::GetRegister();
    if (string_register->InitFlag(arg, val))
      continue;
    FlagRegister<int32> *int32_register =
      FlagRegister<int32>::GetRegister();
    if (int32_register->InitFlag(arg, val))
      continue;
    FlagRegister<int64> *int64_register =
      FlagRegister<int64>::GetRegister();
    if (int64_register->InitFlag(arg, val))
      continue;
    FlagRegister<double> *double_register =
      FlagRegister<double>::GetRegister();
    if (double_register->InitFlag(arg, val))
      continue;

    LOG(FATAL) << "FlagInit: Bad option: " << (*argv)[index];
  }

  if (remove_flags) {
    for (int i = 0; i < *argc - index; ++i)
      (*argv)[i + 1] = (*argv)[i + index];
    *argc -= index - 1;
  }

  if (FLAGS_help) {
    ShowUsage();
    exit(1);
  }
}

void ShowUsage() {
  std::cout << flag_usage << "\n";
  std::cout << "  Flags Description:\n";
  FlagRegister<bool> *bool_register = FlagRegister<bool>::GetRegister();
  bool_register->ShowUsage();
  FlagRegister<string> *string_register = FlagRegister<string>::GetRegister();
  string_register->ShowUsage();
  FlagRegister<int32> *int32_register = FlagRegister<int32>::GetRegister();
  int32_register->ShowUsage();
  FlagRegister<int64> *int64_register = FlagRegister<int64>::GetRegister();
  int64_register->ShowUsage();
  FlagRegister<double> *double_register = FlagRegister<double>::GetRegister();
  double_register->ShowUsage();
}

void SplitToVector(char* full, const char* delim, vector<char*>* vec,
                   bool omit_empty_strings) {
  char* next  = full;
  while((next = strsep(&full, delim)) != NULL) {
   if (omit_empty_strings && next[0] == '\0') continue;
   vec->push_back(next);
  }
  // Add last element (or full string if no delimeter found):
  if (full != NULL) {
    vec->push_back(full);
  }
}


MD5::MD5() {}  // ?OP?

void MD5::Reset() {}  // ?OP?

void MD5::Update(void const *data, int size) {}  // ?OP?

string MD5::Digest() { return ""; }  // every matches! ?OP?


Mutex::Mutex() {}  // ?OP?

MutexLock::MutexLock(Mutex *mutex) {}  // ?OP?
