//===- FragmentGraph.h ----------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_FRAGMENTGRAPH_H
#define MCLD_FRAGMENTGRAPH_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

#include <vector>

#include <mcld/ADT/HashTable.h>
#include <mcld/ADT/HashEntry.h>
#include <mcld/Config/Config.h>
#include <mcld/Fragment/FGNode.h>
#include <mcld/Fragment/FGEdge.h>
#include <mcld/Support/GCFactory.h>

#include <llvm/Support/DataTypes.h>

namespace mcld
{
class Module;
class ResolveInfo;
class Relocation;
class LinkerConfig;

/** \class FragmentGraph
 *  \brief FragmentGraph describes the references between fragments.
 */
class FragmentGraph
{
public:
  typedef FGNode::Slot   Slot;
  typedef FGNode::Signal Signal;

  typedef GCFactory<FGNode, MCLD_SECTIONS_PER_INPUT> NodeFactoryType;
  typedef NodeFactoryType::iterator node_iterator;
  typedef NodeFactoryType::const_iterator const_node_iterator;

  typedef std::vector<FGEdge> EdgeListType;
  typedef EdgeListType::iterator edge_iterator;
  typedef EdgeListType::const_iterator const_edge_iterator;


public:
  FragmentGraph();
  ~FragmentGraph();

  /// construct - construct the whole graph from input Fragments, relocations
  /// and symbols
  bool construct(const LinkerConfig& pConfig, Module& pModule);

  /// connect - connect two nodes
  bool connect(Signal pSignal, Slot pSlot);
  bool connect(FGNode& pFrom, Slot pSlot);

  /// getEdges - given a node, get the list of edges which are the fan-out of
  /// this node
  /// @param pEdges - the edge list which contains the found edges
  /// @return false - the given node
  bool getEdges(FGNode& pNode, EdgeListType& pEdges);

  /// ----- observers -----///
  /// getNode - given a fragment, finde the node which the fragment is belong to
  FGNode* getNode(const Fragment& pFrag);
  const FGNode* getNode(const Fragment& pFrag) const;

  FGNode* getNode(const ResolveInfo& pSym);
  const FGNode* getNode(const ResolveInfo& pSym) const;

private:
  typedef std::vector<Relocation*> RelocationListType;
  typedef RelocationListType::iterator reloc_iterator;
  typedef RelocationListType::const_iterator const_reloc_iterator;

  struct PtrCompare
  {
    bool operator()(const void* X, const void* Y) const
    { return (X==Y); }
  };

  struct PtrHash
  {
    size_t operator()(const void* pKey) const
    {
      return (unsigned((uintptr_t)pKey) >> 4) ^
             (unsigned((uintptr_t)pKey) >> 9);
    }
  };

  /// HashTable for Fragment* to Node*
  typedef HashEntry<const Fragment*, FGNode*, PtrCompare> FragHashEntryType;
  typedef HashTable<FragHashEntryType,
                    PtrHash,
                    EntryFactory<FragHashEntryType> > FragHashTableType;

  /// HashTable for ResolveInfo* to Node*
  typedef HashEntry<const ResolveInfo*, FGNode*, PtrCompare> SymHashEntryType;
  typedef HashTable<SymHashEntryType,
                    PtrHash,
                    EntryFactory<SymHashEntryType> > SymHashTableType;

  /** \class ReachMatrix
   *  \brief ReachMatrix is the reachability matrix which describes the relation
   *   of Nodes in FragmentGraph
   */
  class ReachMatrix
  {
  public:
    typedef std::vector<uint32_t> MatrixDataType;

  public:
    ReachMatrix(size_t pSize);
    ~ReachMatrix();
    uint32_t& at(uint32_t pX, uint32_t pY);
    uint32_t at(uint32_t pX, uint32_t pY) const;

    uint32_t getN() const
    { return m_N; }

    void print();

  private:
    // m_Data - the contents of the matrix. Here we use a one dimensional array
    // to represent the two dimensional matrix
    MatrixDataType m_Data;

    // m_N - this is an m_N x m_N matrix
    size_t m_N;
  };

private:
  FGNode* producePseudoNode();
  FGNode* produceRegularNode();
  void destroyPseudoNode();
  void destroyRegularNode();

  void initMatrix();

  bool createRegularNodes(Module& pModule);
  bool setNodeSlots(Module& pModule);
  bool createPseudoNodes(Module& pModule);

  bool createRegularEdges(Module& pModule);
  bool createPseudoEdges(Module& pModule);

private:
  NodeFactoryType* m_pPseudoNodeFactory;
  NodeFactoryType* m_pRegularNodeFactory;

  /// m_pFragNodeMap - HashTable to map the fragment to the node it belongs to
  FragHashTableType* m_pFragNodeMap;

  /// m_pSymNodeMap - HashTable to map the ResolveInfo to the node. The node is
  /// the pseudo node which the contains it's fan-out is to the ResolveInfo
  SymHashTableType* m_pSymNodeMap;

  ReachMatrix* m_pMatrix;

  /// m_NumOfPNodes - number of pseudo nodes
  size_t m_NumOfPNodes;
  /// m_NumOfRNodes - number of regular nodes
  size_t m_NumOfRNodes;
  /// m_NumOfEdges - number of edges
  size_t m_NumOfEdges;
};

} // namespace of mcld

#endif

