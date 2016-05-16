/*---------------------------------------------------------------------------*
 *  grxmldoc.h  *
 *                                                                           *
 *  Copyright 2007, 2008 Nuance Communciations, Inc.                               *
 *                                                                           *
 *  Licensed under the Apache License, Version 2.0 (the 'License');          *
 *  you may not use this file except in compliance with the License.         *
 *                                                                           *
 *  You may obtain a copy of the License at                                  *
 *      http://www.apache.org/licenses/LICENSE-2.0                           *
 *                                                                           *
 *  Unless required by applicable law or agreed to in writing, software      *
 *  distributed under the License is distributed on an 'AS IS' BASIS,        *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 *  See the License for the specific language governing permissions and      *
 *  limitations under the License.                                           *
 *                                                                           *
 *---------------------------------------------------------------------------*/


#ifndef __grxmldoc_h__
#define  __grxmldoc_h__

// #define MEMTRACE // Uses mtrace() to detect leaks

#include "hashmap.h"
#include "tinyxml.h"
#include <stack>
#include "vocab.h"

#define SCRIPT_LABEL_PREFIX "_"
#define SCRIPT_LABEL_PREFIX_LEN 1
class Node;
template <typename T1, typename T2> class HashMap;
class Graph;
class SubGraph;

class GRXMLDoc
{
public:
    typedef TiXmlNode XMLNode;
    // Some convenience items for string comparison
    typedef enum KeywordValues {NodeTypeGrammar, NodeTypeRule, NodeTypeRuleReference, NodeTypeOneOf, NodeTypeItem, NodeTypeTag, NodeTypeCount, NodeTypeMeta, NodeTypeBadValue};
    typedef  std::map<std::string, KeywordValues> KEYWDPAIR;

    typedef struct {
	bool hasRuleRef;
	std::string RuleRefName;
	int tagID;
    } ItemData;

    GRXMLDoc();
    ~GRXMLDoc();

    // Optional use of voc and model files
   // TODO: Rearrange access to voc and models
#ifndef OPENFSTSDK
    void initialize_SR(char* parfile);
    void shutdown_SR();
    Vocabulary *getVocabulary() { return m_pVocab;}
    AcousticModel* getModel() { return m_pModel;}
    int addPhonemeToList( std::string const& s );
    bool findPhoneme( int i, std::string & s );
    bool getHMMSequence (int centre, int left, int right, std::vector<int> & modelSequence);
#endif

    //  Lookup functions
    bool findSubGraph(std::string & s, SubGraph *&p_SubGraph);
    bool findRule(int i, std::string &s );
    bool findTag(int i, std::string &s );
    bool findLabel(int i, std::string &s );
    bool findSubGraphIndex( SubGraph *p_SubGraph, std::string &s );
    bool findRuleIndex( std::string s, int &i );
    bool findTagIndex( std::string s, int &i );
    bool findLabelIndex( std::string s, int &i );
    bool findSortedLabel(int i, std::string &s );
    bool findSortedLabelIndex( int i, int &sortedIndex );
    bool findMeta(const std::string & sn, std::string &s);
    bool setMeta(const std::string & sn, const std::string &s);
    void sortLabels();
    void addOLabelToOList( std::string & s);
    bool WriteOLabels(const std::string& fileName);

    // Take DOM object and create word graph. Creates SubGraph, rule, tag and label lists.
    bool parseGrammar( XMLNode &node, std::string & xMLFileName );

    // Generate output files
    void writeMapFile( std::string & fileName );
    void writeScriptFile( std::string & fileName );
    void writeGraphFiles( std::string & fileName, bool bDoWriteRecogGraphs );
    void writeParamsFile( std::string & fileName );
    void printLists();
    void printSubgraphs();

protected:
    void initializeLists();
    bool parseNode( XMLNode &node, SubGraph *&p_SubGraph, const unsigned int level );
    bool beginNode( XMLNode &node, SubGraph *&p_SubGraph, const unsigned int level );
    bool endNode( XMLNode &node, SubGraph *&p_SubGraph, const unsigned int level );
    bool beginParseGrammarNode( XMLNode &node );
    bool endParseGrammarNode( XMLNode &node );
    bool beginParseMetaNode( XMLNode &node );
    bool endParseMetaNode( XMLNode &node );
    bool beginParseRuleNode( XMLNode &node, SubGraph *&p_SubGraph);
    bool endParseRuleNode( XMLNode &node, SubGraph *&p_SubGraph );
    bool beginItem( XMLNode &node, SubGraph *&p_SubGraph );
    bool endItem( XMLNode &node, SubGraph *&p_SubGraph );
    bool processCDATA( XMLNode &node, SubGraph *&p_SubGraph );
    bool beginOneOf( XMLNode &node, SubGraph *&p_SubGraph );
    bool endOneOf( XMLNode &node, SubGraph *&p_SubGraph );
    bool beginRuleRef( XMLNode &grmNode, SubGraph *&p_SubGraph );
    bool endRuleRef(XMLNode &node, SubGraph *&p_SubGraph );
    bool fixRuleRef( SubGraph *&p_SubGraph );
    bool getRuleRefName(XMLNode &node, std::string &ruleName);
    bool extendAltExpression( XMLNode &node, int level );
    bool beginTag( XMLNode &node, SubGraph *&p_SubGraph );
    bool endTag( XMLNode &node, SubGraph *&p_SubGraph );
    bool beginCount( XMLNode &node, SubGraph *&p_SubGraph );
    bool endCount( XMLNode &node, SubGraph *&p_SubGraph );
    void printNode( XMLNode &node, int level );
    bool addRuleToList(std::string const& ruleName, SubGraph *&p_SubGraph);

    bool deleteRules();
    bool addTagToList( std::string const& s );
    bool addLabelToList( std::string const& s );
    void printSubgraph( SubGraph &p_SubGraph );

private:

    Graph *m_pGraph;	// The top-level container object for the word graph;
    KEYWDPAIR  m_NodeKeyWords;
    // The unique attributes of the GRML doc
    std::string m_XMLMode;
    std::string m_XMLLanguage;
    std::string m_RootRule;
    std::string m_XMLTagFormat;
    std::string m_XMLVersion;
    std::string m_XMLBase;
    std::string m_XMLFileName;

    //  We store indices for all labels used in the word graph.
    //  Store all these labels in the m_LabelList table, which is auto-indexed.
    //  We need a list of the rule names so that we can distinguish them from other labels.
    //  Store these rule names in the m_RuleList table with an index equal to the label index for the rule.
    //  Thus, when we need the index of a rule, we go straight to m_RuleList
    //	and when we need the label of a rule or any other item we use m_LabelList.

    HashMap<std::string,SubGraph*> m_SubgraphList;
    HashMap<int,std::string> m_TagList;	// <item tag = ...
    HashMap<int,std::string> m_LabelList; // Stores all network label IDs, including rule names
    HashMap<int,std::string> m_SortedLabelList; // Used to sort the labels fo
    HashMap<int, std::string> m_PhonemeList;    // Stores triphones
    HashMap<std::string,int> m_RuleList; // Stores rule name and index used in the LabelList. Use to distinguish which are rules.
    HashMap<int, std::string> m_RuleScope;
    HashMap<int, std::string> m_SlotList;
    HashMap<std::string, std::string> m_MetaKeyValPairs; //Store word-penalty value
    HashMap<std::string, int> m_OutputPtxtLabels;

    std::stack<ItemData*> m_ItemVarsStack;
    std::stack<std::string> m_RuleListStack;
    int m_RuleAutoIndex;
    int m_TagAutoIndex;
    int m_LabelAutoIndex;
    int m_PhonemeAutoIndex;
    int m_ExpandedRulesAutoIndex;
    int m_TagID; // Use to stash tag index for items.
    // Note the subgraph list does not have an auto-index as it is string-indexed.
    // All these lists also have an internal numeric index which can be used.

#ifndef OPENFSTSDK
    Vocabulary *m_pVocab;
    AcousticModel *m_pModel;
#endif

};

#endif // __grxmldoc_h__



