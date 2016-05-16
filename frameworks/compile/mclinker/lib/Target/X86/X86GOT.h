//===- X86GOT.h -----------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_TARGET_X86_GOT_H
#define MCLD_TARGET_X86_GOT_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

#include <mcld/Target/GOT.h>

namespace mcld {

class LDSection;
class SectionData;

/** \class X86_32GOTEntry
 *  \brief GOT Entry with size of 4 bytes
 */
class X86_32GOTEntry : public GOT::Entry<4>
{
public:
  X86_32GOTEntry(uint64_t pContent, SectionData* pParent)
   : GOT::Entry<4>(pContent, pParent)
  {}
};

/** \class X86_32GOT
 *  \brief X86_32 Global Offset Table.
 */

class X86_32GOT : public GOT
{
public:
  X86_32GOT(LDSection& pSection);

  ~X86_32GOT();

  void reserve(size_t pNum = 1);

  X86_32GOTEntry* consume();

private:
  X86_32GOTEntry* m_pLast; ///< the last consumed entry
};

/** \class X86_64GOTEntry
 *  \brief GOT Entry with size of 8 bytes
 */
class X86_64GOTEntry : public GOT::Entry<8>
{
public:
  X86_64GOTEntry(uint64_t pContent, SectionData* pParent)
   : GOT::Entry<8>(pContent, pParent)
  {}
};

/** \class X86_64GOT
 *  \brief X86_64 Global Offset Table.
 */

class X86_64GOT : public GOT
{
public:
  X86_64GOT(LDSection& pSection);

  ~X86_64GOT();

  void reserve(size_t pNum = 1);

  X86_64GOTEntry* consume();

private:
  X86_64GOTEntry* m_pLast; ///< the last consumed entry
};

} // namespace of mcld

#endif

