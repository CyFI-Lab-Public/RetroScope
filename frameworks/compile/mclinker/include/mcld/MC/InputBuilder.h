//===- InputBuilder.h -----------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_MC_INPUT_BUILDER_H
#define MCLD_MC_INPUT_BUILDER_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

#include <string>
#include <stack>

#include <mcld/InputTree.h>
#include <mcld/MC/MCLDInput.h>
#include <mcld/Support/FileHandle.h>

namespace mcld {

class LinkerConfig;
class InputFactory;
class ContextFactory;
class MemoryAreaFactory;
class AttrConstraint;
class raw_mem_ostream;

/** \class InputBuilder
 *  \brief InputBuilder recieves InputActions and build the InputTree.
 *
 *  InputBuilder build input tree and inputs.
 */
class InputBuilder
{
public:
  explicit InputBuilder(const LinkerConfig& pConfig);

  InputBuilder(const LinkerConfig& pConfig,
               InputFactory& pInputFactory,
               ContextFactory& pContextFactory,
               MemoryAreaFactory& pMemoryFactory,
               bool pDelegate = true);

  virtual ~InputBuilder();

  // -----  input tree operations  ----- //
  const InputTree& getCurrentTree() const;
  InputTree&       getCurrentTree();

  void setCurrentTree(InputTree& pInputTree);

  // -----  root of input tree  ----- //
  const InputTree::iterator& getCurrentNode() const { return m_Root; }
  InputTree::iterator&       getCurrentNode()       { return m_Root; }

  template<InputTree::Direction DIRECTION>
  InputTree& createNode(const std::string& pName,
                        const sys::fs::Path& pPath,
                        unsigned int pType = Input::Unknown);

  // -----  input operations  ----- //
  Input* createInput(const std::string& pName,
                     const sys::fs::Path& pPath,
                     unsigned int pType = Input::Unknown,
                     off_t pFileOffset = 0);

  bool setContext(Input& pInput, bool pCheck = true);

  bool setMemory(Input& pInput,
                 FileHandle::OpenMode pMode,
		 FileHandle::Permission pPerm = FileHandle::System);

  bool setMemory(Input& pInput, void* pMemBuffer, size_t pSize);

  InputTree& enterGroup();

  InputTree& exitGroup();

  bool isInGroup() const;

  const AttrConstraint& getConstraint() const;

  const AttributeProxy& getAttributes() const;
  AttributeProxy&       getAttributes();

private:
  const LinkerConfig& m_Config;

  InputFactory* m_pInputFactory;
  MemoryAreaFactory* m_pMemFactory;
  ContextFactory* m_pContextFactory;

  InputTree* m_pCurrentTree;
  InputTree::Mover* m_pMove;
  InputTree::iterator m_Root;
  std::stack<InputTree::iterator> m_ReturnStack;

  bool m_bOwnFactory;

};

//===----------------------------------------------------------------------===//
// Template implement
//===----------------------------------------------------------------------===//
template<> inline InputTree&
InputBuilder::createNode<InputTree::Inclusive>(const std::string& pName,
                                               const sys::fs::Path& pPath,
                                               unsigned int pType)
{
  assert(NULL != m_pCurrentTree && NULL != m_pMove);

  Input* input = createInput(pName, pPath, pType);
  m_pCurrentTree->insert(m_Root, *m_pMove, *input);
  m_pMove->move(m_Root);
  m_pMove = &InputTree::Downward;

  return *m_pCurrentTree;
}

template<> inline InputTree&
InputBuilder::createNode<InputTree::Positional>(const std::string& pName,
                                               const sys::fs::Path& pPath,
                                               unsigned int pType)
{
  assert(NULL != m_pCurrentTree && NULL != m_pMove);

  Input* input = createInput(pName, pPath, pType);
  m_pCurrentTree->insert(m_Root, *m_pMove, *input);
  m_pMove->move(m_Root);
  m_pMove = &InputTree::Afterward;

  return *m_pCurrentTree;
}

} // end of namespace mcld

#endif

