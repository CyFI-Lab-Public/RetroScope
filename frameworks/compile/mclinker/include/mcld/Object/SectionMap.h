//===- SectionMap.h -------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_SECTION_MAP_H
#define MCLD_SECTION_MAP_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

#include <vector>
#include <string>

#include <llvm/Support/DataTypes.h>

namespace mcld {

/** \class SectionMap
 *  \brief descirbe the mappings of input section's name (or prefix) to
 *         its associated output section's name and offset
 */
class SectionMap
{
public:
  // a mapping in SectionMap is the triple of
  // {input substr, output section's name, output section's offset}
  struct NamePair
  {
  public:
    NamePair();
    NamePair(const std::string& pFrom, const std::string& pTo);

    bool isNull() const;

  public:
    unsigned int hash;
    std::string from;
    std::string to;
  };

  typedef std::vector<NamePair> NamePairList;
  typedef NamePairList::iterator iterator;
  typedef NamePairList::const_iterator const_iterator;

  /// NullName - the null object of NamePair
  static NamePair NullName;

public:
  // get the possible output section name based on the mapping table
  // return NullPair if not found
  const NamePair& find(const std::string& pFrom) const;
  NamePair&       find(const std::string& pFrom);

  const NamePair& find(const std::string& pFrom, unsigned int pHash) const;
  NamePair&       find(const std::string& pFrom, unsigned int pHash);

  // add a mapping from input sub-string to output name.
  // @param [in]  pFrom  the given input sub-string
  // @param [in]  pTo    the mapped output string
  // @param [out] pExist does pFrom exist?
  NamePair& append(const std::string& pFrom,
                   const std::string& pTo,
                   bool& pExist);

  const_iterator begin() const { return m_NamePairList.begin(); }
  iterator       begin()       { return m_NamePairList.begin(); }
  const_iterator end  () const { return m_NamePairList.end(); }
  iterator       end  ()       { return m_NamePairList.end(); }

  bool           empty() const { return m_NamePairList.empty(); }
  size_t         size () const { return m_NamePairList.size(); }

  static unsigned int hash(const std::string& pString);

private:
  bool matched(const NamePair& pNamePair,
               const std::string& pInput,
               unsigned int pHash) const;
private:
  NamePairList m_NamePairList;
};

} // namespace of mcld

#endif

