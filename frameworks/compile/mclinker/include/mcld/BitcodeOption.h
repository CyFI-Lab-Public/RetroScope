//===- BitcodeOption.h ----------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_BITCODE_OPTIONS_H
#define MCLD_BITCODE_OPTIONS_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

#include <mcld/Support/Path.h>

namespace mcld {

/** \class BitcodeOption
 *  \brief BitcodeOption represents the options of bitcode on the command line.
 */
class BitcodeOption
{
public:
  BitcodeOption();

  ~BitcodeOption();

  void setPosition(unsigned int pPosition) { m_Position = pPosition; }

  unsigned int getPosition() const { return m_Position; }

  void setPath(const sys::fs::Path& pPath) { m_Path = pPath; }

  const sys::fs::Path& getPath() const { return m_Path; }

  bool hasDefined() const;

private:
  int m_Position;

  sys::fs::Path m_Path;

};

} // namespace of mcld

#endif

