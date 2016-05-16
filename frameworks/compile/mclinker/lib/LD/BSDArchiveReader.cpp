//===- BSDArchiveReader.cpp -----------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/MC/MCLDInput.h>
#include <mcld/LD/BSDArchiveReader.h>
#include <mcld/LD/Archive.h>

using namespace mcld;

BSDArchiveReader::BSDArchiveReader()
{
}

BSDArchiveReader::~BSDArchiveReader()
{
}

bool BSDArchiveReader::readArchive(Archive& pArchive)
{
  // TODO
  return true;
}

bool BSDArchiveReader::isMyFormat(Input& pInput) const
{
  // TODO
  return false;
}

