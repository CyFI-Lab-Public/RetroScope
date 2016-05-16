//===- StringEntry.h -------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_STRING_ENTRY_H
#define MCLD_STRING_ENTRY_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif
#include <llvm/ADT/StringRef.h>
#include <llvm/Support/DataTypes.h>
#include <cstdlib>
#include <cstring>
#include <cassert>

namespace mcld
{
template<typename DataType>
class StringEntryFactory;

/** \class StringEntry
 *  \brief StringEntry is a pair of strings which is designed for high locality.
 */
template<typename DataType>
class StringEntry
{
public:
  typedef llvm::StringRef   key_type;
  typedef DataType value_type;

public:
  key_type key()
  { return key_type(m_Key, m_KeyLen); }

  const key_type key() const
  { return key_type(m_Key, m_KeyLen); }

  value_type& value()
  { return m_Value; }

  const value_type& value() const
  { return m_Value; }

  size_t getKeyLength() const
  { return m_KeyLen; }

  size_t getValueLength() const
  { return m_Value.size(); }

  void setValue(const DataType& pVal)
  { m_Value = pVal; }

  bool compare(const llvm::StringRef& pX)
  { return (0 == key().compare(pX)); }

  bool compare(const llvm::StringRef& pX) const
  { return (0 == key().compare(pX)); }

private:
  StringEntry();
  StringEntry(const key_type& pKey);
  StringEntry(const StringEntry& pCopy);
  ~StringEntry();

private:
  DataType m_Value;
  uint16_t m_KeyLen;
  char m_Key[];

  friend class StringEntryFactory<DataType>;
};


template<>
class StringEntry<llvm::StringRef>
{
public:
  typedef llvm::StringRef key_type;
  typedef llvm::StringRef value_type;

public:
  key_type key()
  { return key_type(m_Key, m_KeyLen); }

  const key_type key() const
  { return key_type(m_Key, m_KeyLen); }

  value_type& value()
  { return m_Value; }

  const value_type& value() const
  { return m_Value; }

  size_t getKeyLength() const
  { return m_KeyLen; }

  size_t getValueLength() const
  { return m_Value.size(); }

  void setValue(const std::string& pVal)
  { setValue(pVal.c_str()); }

  void setValue(const char* pVal);

  void setValue(llvm::StringRef& pVal);

  bool compare(const llvm::StringRef& pX)
  { return (0 == key().compare(pX)); }

  bool compare(const llvm::StringRef& pX) const
  { return (0 == key().compare(pX)); }

private:
  StringEntry();
  StringEntry(const key_type& pKey);
  StringEntry(const StringEntry& pCopy);
  ~StringEntry();

private:
  llvm::StringRef m_Value;
  uint16_t m_KeyLen;
  char m_Key[];

  friend class StringEntryFactory<llvm::StringRef>;
};

template<typename DataType>
class StringEntryFactory
{
public:
  typedef StringEntry<DataType>             entry_type;
  typedef typename StringEntry<DataType>::key_type   key_type;
  typedef typename StringEntry<DataType>::value_type value_type;

public:
  StringEntryFactory();
  ~StringEntryFactory();

  StringEntry<DataType>* produce(const key_type& pKey);
  void destroy(StringEntry<DataType>* pEntry);
};

#include "StringEntry.tcc"

} // namespace of mcld

#endif

