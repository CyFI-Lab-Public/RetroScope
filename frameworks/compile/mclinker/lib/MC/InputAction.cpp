//===- InputAction.cpp ----------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/MC/InputAction.h>

using namespace mcld;

//===----------------------------------------------------------------------===//
// Base Positional Option
//===----------------------------------------------------------------------===//
InputAction::InputAction(unsigned int pPosition)
  : m_Position(pPosition) {
}

InputAction::~InputAction()
{
}

