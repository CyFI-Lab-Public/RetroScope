//===- HexagonAbsoluteStub.h -----------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//

#ifndef MCLD_HEXAGON_ABSOLUTE_STUB_H
#define MCLD_HEXAGON_ABSOLUTE_STUB_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

#include <llvm/Support/DataTypes.h>
#include <mcld/Fragment/Stub.h>
#include <string>
#include <vector>

namespace mcld
{

class Relocation;
class ResolveInfo;

/** \class HexagonAbsoluteStub
 *  \brief Hexagon stub for abs long call from source to target
 *
 */
class HexagonAbsoluteStub : public Stub
{
public:
  HexagonAbsoluteStub(bool pIsOutputPIC);

  ~HexagonAbsoluteStub();

  // isMyDuty
  bool isMyDuty(const class Relocation& pReloc,
                uint64_t pSource,
                uint64_t pTargetSymValue) const;

  // observers
  const std::string& name() const;

  const uint8_t* getContent() const;

  size_t size() const;

  size_t alignment() const;

private:
  HexagonAbsoluteStub(const HexagonAbsoluteStub&);

  HexagonAbsoluteStub& operator=(const HexagonAbsoluteStub&);

  /// for doClone
  HexagonAbsoluteStub(const uint32_t* pData,
               size_t pSize,
               const_fixup_iterator pBegin,
               const_fixup_iterator pEnd);

  /// doClone
  Stub* doClone();

private:
  std::string m_Name;
  static const uint32_t TEMPLATE[];
  const uint32_t* m_pData;
  size_t m_Size;
};

} // namespace of mcld

#endif
