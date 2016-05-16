//===- StringEntry.cpp -----------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/ADT/StringEntry.h>

using namespace mcld;

//===----------------------------------------------------------------------===//
// StringEntry<llvm::StringRef>
//===----------------------------------------------------------------------===//
StringEntry<llvm::StringRef>::StringEntry()
{
}

StringEntry<llvm::StringRef>::StringEntry(const StringEntry::key_type& pKey)
{
}

StringEntry<llvm::StringRef>::StringEntry(const StringEntry<llvm::StringRef>& pCopy)
{
  assert("Copy constructor of StringEntry should not be called!");
}

StringEntry<llvm::StringRef>::~StringEntry()
{
  if (!m_Value.empty())
    free(const_cast<char*>(m_Value.data()));
}

void StringEntry<llvm::StringRef>::setValue(llvm::StringRef& pVal)
{
  char* data = (char*)malloc(pVal.size()+1);
  strcpy(data, pVal.data());
  m_Value = llvm::StringRef(data, pVal.size());
}

void StringEntry<llvm::StringRef>::setValue(const char* pVal)
{
  size_t length = strlen(pVal);
  char* data = (char*)malloc(length+1);
  strcpy(data, pVal);
  m_Value = llvm::StringRef(data, length);
}

