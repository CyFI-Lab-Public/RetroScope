//===- ZOption.h ----------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_ZOPTION_H
#define MCLD_ZOPTION_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif
#include <llvm/Support/DataTypes.h>

namespace mcld
{

/** \class ZOption
 *  \brief The -z options for GNU ld compatibility.
 */
class ZOption
{
public:
  enum Kind {
    CombReloc,
    NoCombReloc,
    Defs,
    ExecStack,
    NoExecStack,
    InitFirst,
    InterPose,
    LoadFltr,
    MulDefs,
    NoCopyReloc,
    NoDefaultLib,
    NoDelete,
    NoDLOpen,
    NoDump,
    Relro,
    NoRelro,
    Lazy,
    Now,
    Origin,
    CommPageSize,
    MaxPageSize,
    Unknown
  };

public:
  ZOption();

  ~ZOption();

  Kind kind() const
  { return m_Kind; }

  uint64_t pageSize() const
  { return m_PageSize; }

  void setKind(Kind pKind)
  { m_Kind = pKind; }

  void setPageSize(uint64_t pPageSize)
  { m_PageSize = pPageSize; }

private:
  Kind m_Kind;
  uint64_t m_PageSize;
};

} // namespace of mcld

#endif

