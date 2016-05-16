//===- InputAction.h ------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_MC_INPUT_ACTION_H
#define MCLD_MC_INPUT_ACTION_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

namespace mcld {

class SearchDirs;
class InputBuilder;

//===----------------------------------------------------------------------===//
// Base InputAction
//===----------------------------------------------------------------------===//
/** \class InputAction
 *  \brief InputAction is a command object to construct mcld::InputTree.
 */
class InputAction
{
protected:
  explicit InputAction(unsigned int pPosition);

public:
  virtual ~InputAction();

  virtual bool activate(InputBuilder&) const = 0;

  unsigned int position() const { return m_Position; }

  bool operator<(const InputAction& pOther) const
  { return (position() < pOther.position()); }

private:
  InputAction();                               // DO_NOT_IMPLEMENT
  InputAction(const InputAction& );            // DO_NOT_IMPLEMENT
  InputAction& operator=(const InputAction& ); // DO_NOT_IMPLEMENT

private:
  unsigned int m_Position;
};

} // namespace of mcld

#endif

