//===- Flags.h ------------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_FLAGS_H
#define MCLD_FLAGS_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

namespace mcld
{

template<typename Enum>
class Flags
{
public:
  typedef Enum enum_type;

public:
  Flags(const Flags& pOther)
  : m_Data(pOther.m_Data) {}

  Flags(Enum pFlag)
  : m_Data(pFlag) {}

  Flags(unsigned int pFlag = 0x0)
  : m_Data(pFlag) {}

  operator unsigned int () const
  { return m_Data; }

  bool operator! () const
  { return (m_Data == 0x0); }

  Flags operator& (int pMask ) const
  { return Flags(m_Data & pMask); }

  Flags operator& (unsigned int pMask ) const
  { return Flags(m_Data & pMask); }

  Flags operator& (Enum pMask ) const
  { return Flags(m_Data & pMask); }

  Flags& operator&= (unsigned int pMask ) {
    m_Data &= pMask;
    return *this;
  }

  Flags& operator=(Flags pOther) {
    m_Data = pOther.m_Data;
    return *this;
  }

  Flags operator^ (Flags pOther) const
  { return Flags(m_Data^pOther.m_Data); }

  Flags operator^ (Enum pOther) const
  { return Flags(m_Data^pOther); }

  Flags& operator^= (Flags pOther) {
    m_Data ^= pOther.m_Data;
    return *this;
  } 

  Flags& operator^= (Enum pOther) {
    m_Data ^= pOther;
    return *this;
  }

  Flags operator| (Flags pOther) const
  { return Flags(m_Data | pOther.m_Data); }

  Flags operator| (Enum pOther ) const
  { return Flags(m_Data | pOther); }

  Flags& operator|= (Flags pOther) {
    m_Data |= pOther.m_Data;
    return *this;
  }

  Flags& operator|= (Enum pOther) {
    m_Data |= pOther;
    return *this;
  }

  Flags operator~ () const
  { return Flags(~m_Data); }

private:
  unsigned int m_Data;
};

} // namespace of mcld

#endif

