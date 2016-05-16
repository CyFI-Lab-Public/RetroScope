//===- TargetOptions.cpp --------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/TargetOptions.h>

using namespace mcld;

//===----------------------------------------------------------------------===//
// TargetOptions
//===----------------------------------------------------------------------===//
TargetOptions::TargetOptions()
  : m_Endian(Unknown),
    m_BitClass(0) {
}

TargetOptions::TargetOptions(const std::string& pTriple)
  : m_Triple(pTriple),
    m_Endian(Unknown),
    m_BitClass(0) {
}

TargetOptions::~TargetOptions()
{
}

void TargetOptions::setTriple(const llvm::Triple& pTriple)
{
  m_Triple = pTriple;
}

void TargetOptions::setTriple(const std::string& pTriple)
{
  m_Triple.setTriple(pTriple);
}

void TargetOptions::setTargetCPU(const std::string& pCPU)
{
  m_TargetCPU = pCPU;
}

void TargetOptions::setTargetFeatureString(const std::string& pFS)
{
  m_TargetFS = pFS;
}

