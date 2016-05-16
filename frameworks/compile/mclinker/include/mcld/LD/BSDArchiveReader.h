//===- BSDArchiveReader.h -------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_BSD_ARCHIVE_READER_H
#define MCLD_BSD_ARCHIVE_READER_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif
#include <mcld/LD/ArchiveReader.h>

namespace mcld
{

class Input;
class Archive;

/** \class BSDArchiveReader
 *  \brief BSDArchiveReader reads BSD-variant archive files.
 *
 */
class BSDArchiveReader : public ArchiveReader
{
public:
  BSDArchiveReader();
  ~BSDArchiveReader();

  bool readArchive(Archive& pArchive);
  bool isMyFormat(Input& pInput) const;
};

} // namespace of mcld

#endif

