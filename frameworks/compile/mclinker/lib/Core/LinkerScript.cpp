//===- LinkerScript.cpp ---------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/LinkerScript.h>

using namespace mcld;

//===----------------------------------------------------------------------===//
// LinkerScript
//===----------------------------------------------------------------------===//
LinkerScript::LinkerScript()
{
}

LinkerScript::~LinkerScript()
{
}

const mcld::sys::fs::Path& LinkerScript::sysroot() const
{
  return m_SearchDirs.sysroot();
}

void LinkerScript::setSysroot(const mcld::sys::fs::Path &pSysroot)
{
  m_SearchDirs.setSysRoot(pSysroot);
}

bool LinkerScript::hasSysroot() const
{
  return !sysroot().empty();
}

