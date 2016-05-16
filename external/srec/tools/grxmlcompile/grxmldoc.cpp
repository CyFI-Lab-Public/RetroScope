/*---------------------------------------------------------------------------*
 *  grxmldoc.cpp  *
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

#include <assert.h>
#include <stdlib.h>
#include <fstream>
#include <sstream>
#include <iostream>
#include <algorithm> // for std::sort
#include "tinyxml.h"
#include "grph.h"       // The word graph object and interface
#include "sub_grph.h"	// The sub-graph object and interface
#include "hashmap.h"
#include "grxmldoc.h"
#include "ESR_Session.h"
//#include "LCHAR.h"

#define GRXML_DEBUG 0
#define MAX_PATH_NAME 512

#define FATAL_ERROR(x,y) { std::cout << (x) << std::endl; exit ((y)); }
#define WARNING(x) std::cout << (x) << std::endl;

#if GRXML_DEBUG
//#define DEBUG_PRINT(x) //
#define DEBUG_PRINT(x) std::cout << (x) << std::endl;
#define PRINT_EXPRESSION(x)
//#define PRINT_EXPRESSION(x) std::cout << (x) << std::endl;
#else
#define DEBUG_PRINT(x) //
#define PRINT_EXPRESSION(x) //

#endif

using namespace std;

#define CHECK_NOT_EMPTY(s, t) { if (s.empty()) \
				{ \
				std::cout << "ERROR: Empty string of type "  << t <<std::endl; \
				} \
			     }

int get_range(const std::string& s, int* minCnt, int* maxCnt)
{
  std::string sval;
  unsigned int p1 =s.find("-");
  if ( p1 !=string::npos ) {
    sval.assign( s, 0, p1 );
    if(strspn(sval.c_str(),"0123456789")<1) return 1;
    *minCnt = atoi( sval.c_str() );
    sval.assign( s, p1+1, s.size() );
    *maxCnt = -1;    // 0== any?
    // If max is given then use BeginCount otherwise use BeginItemRepeat
    if (!sval.empty() ) {
      if(strspn(sval.c_str(),"0123456789")<1) return 1;
      *maxCnt = atoi( sval.c_str() );
    }
    return 0;
  } 
  p1 = s.find("+");
  if( p1 != string::npos) {
    sval.assign( s, 0, p1 );
    if(strspn(sval.c_str(),"0123456789")<1) return 1;
    *minCnt = atoi( sval.c_str() );
    *maxCnt = -1; 
    return 0;
  }
  if(strspn(s.c_str(),"0123456789")<1) return 1;
  *minCnt = *maxCnt = atoi( s.c_str());
  return 0;
}

GRXMLDoc::GRXMLDoc()
{
    m_NodeKeyWords.insert(make_pair("grammar", NodeTypeGrammar));
    m_NodeKeyWords.insert(make_pair("rule", NodeTypeRule));
    m_NodeKeyWords.insert(make_pair("ruleref", NodeTypeRuleReference));
    m_NodeKeyWords.insert(make_pair("one-of", NodeTypeOneOf));
    m_NodeKeyWords.insert(make_pair("item", NodeTypeItem));
    m_NodeKeyWords.insert(make_pair("tag", NodeTypeTag));
    m_NodeKeyWords.insert(make_pair("count", NodeTypeCount));
    m_NodeKeyWords.insert(make_pair("meta", NodeTypeMeta));
    m_pGraph = 0;
    m_RuleAutoIndex = 0;
    m_TagAutoIndex = 0;
    m_LabelAutoIndex = 0;
    m_ExpandedRulesAutoIndex = 0;
    m_XMLFileName = "dummy.xml";
}


GRXMLDoc::~GRXMLDoc()
{
    deleteRules();
    if (m_pGraph) {
        delete m_pGraph;
    }
}


bool GRXMLDoc::parseGrammar( XMLNode &node, std::string & xMLFileName )
{
    m_XMLFileName = xMLFileName;
    // Set up the internally defined rules, etc.
    initializeLists();
    // The top level "document" node is given to this fn
    // Create the container for the word graph.
    if (m_pGraph) {
        delete m_pGraph;
    }
    m_pGraph = new Graph("XML grammar");
    SubGraph *p_SubGraph;

    parseNode( node, p_SubGraph, 1 );     // NB Subgraph pointed to will change in recursive fn.

    if (findSubGraph( m_RootRule, p_SubGraph )) {
	m_pGraph->ExpandRules (p_SubGraph);
	p_SubGraph->RemoveInternalConnections ();
	//Print the root rule.
	//printSubgraph( *p_SubGraph );
    }
    return true;
}


bool GRXMLDoc::parseNode( XMLNode &node, SubGraph *&p_SubGraph, const unsigned int level )
{
    // We will create a new subgraph for each rule node.
    // The "current" subgraph is substituted with the new subgraph for all ops on child nodes.
    // After processing child nodes the original subgraph is reinstated
    // for final operations in the endNode() fn.

    // Initial processing of the current node before processing children
#if 0 && GRXML_DEBUG
	if(node.Type() == TiXmlNode::ELEMENT) 
		node.ToElement()->Print( stdout, level);
	else if(node.Type() == TiXmlNode::DOCUMENT)
		node.ToDocument()->Print( stdout, level);
	else if(node.Type() == TiXmlNode::TEXT)
		node.ToText()->Print( stdout, level);
	else if(node.Type() == TiXmlNode::DECLARATION)
		node.ToDeclaration()->Print( stdout, level);
	else {
		const char* text = node.Value();
		if(!text) text = "__NULL__";
		printf("processing node type %d text %s\n", node.Type(), text);
	}
#endif
    beginNode( node, p_SubGraph, level );

    SubGraph *p_LocalSubGraph;
    p_LocalSubGraph = p_SubGraph;
	TiXmlNode* child;
	for( child = node.FirstChild(); child; child = child->NextSibling() )
    {
		parseNode ( *child, p_SubGraph, level+1 );
    }
    // Revert current node
    p_SubGraph = p_LocalSubGraph;

    // Finish processing current node
    endNode( node, p_SubGraph, level );

    return true;
} // parseNode


bool GRXMLDoc::beginNode( XMLNode &node, SubGraph *&p_SubGraph, const unsigned int level )
{
    std::string name = node.Value();
    DEBUG_PRINT("Element = " + name);

    // XMLNode::Type type = node.getType();
    if ( node.Type() == TiXmlNode::TEXT) // isCData()
    {
      const char* cc_name = node.Parent()->Value();
      std::string str_name(cc_name); 
      DEBUG_PRINT (std::string("CDATA ") + name);
      DEBUG_PRINT (std::string("CDATA ") + str_name);
      
      processCDATA( node, p_SubGraph );
    }
    else if ( node.Type()== TiXmlNode::ELEMENT /*isNode()*/ || node.NoChildren() /*isLeaf()*/)
      {
	//printNode(node, level);
	// Use enum value
	KEYWDPAIR::iterator pos;
	pos = m_NodeKeyWords.find( name );
	KeywordValues nodeType = NodeTypeBadValue;
	if ( pos != m_NodeKeyWords.end() )
	{
	    nodeType = (*pos).second;
	    DEBUG_PRINT("nodeType=" + nodeType);
	} else if(node.Type() == TiXmlNode::COMMENT) {
		return true;
	} else if(node.Type() == TiXmlNode::DECLARATION && name.length()==0) {
		return true;
	} else {
	  FATAL_ERROR( std::string("Error: unknown tag ") + name, ESR_INVALID_ARGUMENT);
	}

	switch ( nodeType )
	{
	case NodeTypeGrammar:
	    {
		beginParseGrammarNode( node );
	    }
	    break;
	case NodeTypeRule:
	    {
		// NB This fn creates a new subgraph.
		beginParseRuleNode( node, p_SubGraph );
	    }
	    break;
	    case NodeTypeRuleReference:
	    {
		// NB This fn creates a new subgraph.
		beginRuleRef( node, p_SubGraph );
	    }
	    break;
	    case NodeTypeOneOf:
	    {
		beginOneOf( node, p_SubGraph );
	    }
	    break;
	    case NodeTypeItem:
	    {
		beginItem( node, p_SubGraph );
	    }
	    break;
	    case NodeTypeTag:
	    {
		beginTag( node, p_SubGraph );
	    }
	    break;
	    case NodeTypeCount:
	    {
		beginCount( node, p_SubGraph );
	    }
	    break;
	    case NodeTypeMeta:
	    {
	        beginParseMetaNode( node );
	    }
	    break;
	    case NodeTypeBadValue:
	    default:
		DEBUG_PRINT( "UNKNOWN node name: " + name );
	    break;
	}; // switch
    } //is a Node or Leaf
    else if ( node.Type() == TiXmlNode::TEXT) // isCData()
      {
	DEBUG_PRINT (std::string("CDATA ") + name);
	processCDATA( node, p_SubGraph );
    }
    return true;
} // beginNode()


bool GRXMLDoc::endNode( XMLNode &node, SubGraph *&p_SubGraph, const unsigned int level )
{
    std::string name = node.Value();
    //XMLNode::Type type = node.getType();

    if ( node.Type()== TiXmlNode::ELEMENT /*isNode()*/ || node.NoChildren() )
    {
	KEYWDPAIR::iterator pos;
	pos = m_NodeKeyWords.find( name );
	KeywordValues nodeType = NodeTypeBadValue;
	if ( pos != m_NodeKeyWords.end() )
	{
	    nodeType = (*pos).second;
	}  else if(node.Type() == TiXmlNode::COMMENT) {
		return true;
	} else if(node.Type() == TiXmlNode::DECLARATION && name.length()==0) {
		return true;
	} else if(node.Type() == TiXmlNode::TEXT) {

	} else {
	  FATAL_ERROR( std::string("Error: unknown tag ") + name, ESR_INVALID_ARGUMENT );
	}

	switch ( nodeType )
	{
	case NodeTypeGrammar:
	{
	    endParseGrammarNode( node );
	}
	break;
	case NodeTypeRule:
	{
	    endParseRuleNode( node, p_SubGraph );
	}
	break;
	case NodeTypeRuleReference:
	{
	    endRuleRef( node, p_SubGraph );
	}
	break;
	case NodeTypeOneOf:
	{
	    endOneOf( node, p_SubGraph );
	}
	break;
	case NodeTypeItem:
	{
	    endItem(node, p_SubGraph );
	}
	break;
	case NodeTypeTag:
	{
	    endTag( node, p_SubGraph );
	}
	break;
	case NodeTypeCount:
	{
	    endCount( node, p_SubGraph );
	}
	break;
        case NodeTypeMeta:
	{
            endParseMetaNode( node );
	}
	break;
	case NodeTypeBadValue:
	default:
	    DEBUG_PRINT( "UNKNOWN node name: ");
	    DEBUG_PRINT( name.c_str() );
	//Extend the
	break;
	}; // switch
    } //isNode() or isLeaf()
    else
    {
	// Do nothing?
    }
    return true;
} // endNode()


bool GRXMLDoc::beginParseGrammarNode(XMLNode &node)
{
	const char* attr;
#define GETATTR(nAmE) ((attr=node.ToElement()->Attribute(nAmE))!=NULL) ? attr:""
	m_XMLMode      = GETATTR("mode");
	m_XMLLanguage  = GETATTR("xml:lang");
    m_RootRule     = GETATTR("root");	// The root rule name

    DEBUG_PRINT("Root rule = " + m_RootRule);

    m_XMLTagFormat = GETATTR("tag-format");
    m_XMLVersion   = GETATTR("version");
    m_XMLBase      = GETATTR("xml:base");
    return true;
}

bool GRXMLDoc::beginParseMetaNode(XMLNode &node)
{
  const char* attr;
  std::string meta_name  = GETATTR("name");
  std::string meta_value = GETATTR("content");

  if(meta_name == "word_penalty") {
    m_MetaKeyValPairs.insert(meta_name,meta_value);
    // m_MetaKeyValPairs.print();
  } else if(meta_name == "do_skip_interword_silence") {
    for(int j = 0; j<(int)meta_value.size(); j++){
      meta_value[j] = tolower(meta_value[j]); //lower();
    }
    if(meta_value!="true" && meta_value!="false") 
      printf ("\nWarning: %s must be set to 'true' or 'false'; defaulting to 'false'\n", meta_name.c_str());
    else 
      m_MetaKeyValPairs.insert(meta_name,meta_value);
  } else if(meta_name == "userdict_name") {
    printf ("\nWarning: ignoring unsupported meta %s %s\n", meta_name.c_str(), meta_value.c_str());
  } else {
    printf ("\nWarning: ignoring unsupported meta %s %s\n", meta_name.c_str(), meta_value.c_str());
  }
  return true;
}


bool GRXMLDoc::endParseGrammarNode(XMLNode &node)
{
    // End parse operations
    return true;
}


bool GRXMLDoc::beginParseRuleNode( XMLNode &node, SubGraph *&p_SubGraph)
{
	const char* attr;
    // Note: The subGraph may change if there are forward references. This
    // is fine as we revert to the previous one when finished parsing the current node.
    DEBUG_PRINT ( "---- Rule\n" );
    std::string ruleName = GETATTR("id" );
    std::string s_tag    = GETATTR("tag" );
    if( s_tag.length()>0) {
      FATAL_ERROR("Error: unsupported tag= syntax, use <tag> ... </tag>", 1)
    }
    CHECK_NOT_EMPTY( ruleName, "id" );
    // Rule name must be unique within scope of entire grammar.
    // Put rule on stack - for context
    m_RuleListStack.push( ruleName );

    // Check whether a ruleref placeholder exists for this rule.
    int index;
    bool foundRule = findRuleIndex( ruleName, index );
    if (foundRule) {
	// Rule is already declared; it must have been forward referenced
	// so swap the placeholder subgraph in.
	// NB subgraph and rule name are already known to lists.
	SubGraph *p_ExistingSubgraph;
	if ( findSubGraph( ruleName, p_ExistingSubgraph ) ) {
	    p_SubGraph = p_ExistingSubgraph;
	}
	else {
	    FATAL_ERROR("ERROR! Subgraph without rule name entry found!", -1);
        }
    }
    else {
	// Create a Word Graph node for each rule node
	SubGraph *newGraph;
	addRuleToList( ruleName, newGraph );
	p_SubGraph = newGraph;
    }

    // Make a note of the scope or rules; public, etc - used in map file.
    findRuleIndex( ruleName, index );
    std::string ruleScope = GETATTR("scope" );
    if ( !ruleScope.empty() ) {
        m_RuleScope.insert(index, ruleScope);
    }

    // We must accommodate Rules that have CDATA without an <item> element.
    // We need to infer this element for all rules.
    m_pGraph->BeginItem( p_SubGraph );

    PRINT_EXPRESSION( ruleName + " = { " );
    return true;
} // beginParseRuleNode()


bool GRXMLDoc::endParseRuleNode( XMLNode &node, SubGraph *&p_SubGraph )
{
    // The rule expression has been built as a subgraph and ID added to the rule list.
    // Finished editing subgraph
    DEBUG_PRINT ( "---- /Rule\n" );
    //m_pGraph->EndRule(&p_SubGraph);
    // Tell the world
    //std::string ruleName = attr.get( "id" );
    std::string ruleName = m_RuleListStack.top();
    m_RuleListStack.pop();
    //CHECK_NOT_EMPTY( ruleName, "id" );
    // Must be unique rule name within scope of entire grammar.
    // Check whether a ruleref placeholder exists for this rule.
    m_pGraph->addSubGraph ( p_SubGraph );

    // We must accommodate Rules that have CDATA without an <item> element.
    // We need to infer this element for all rules.
    m_pGraph->EndItem( p_SubGraph );

    PRINT_EXPRESSION( " }\n" );
    return true;
}

bool GRXMLDoc::processCDATA( XMLNode &node, SubGraph *&p_SubGraph )
{
    // Note the Item's CDATA
    // Strip leading and trailing whitespace
    const char* cc_name = node.Parent()->Value();
    std::string str_name(cc_name); // = node.Parent()->ValueStr(); // getName
    // std::string name = node.Parent()->Value(); // getName
    //if ( name == "item" ) {
    if ( str_name != "tag" ) {

	const char* const whitespace = " \t\r\n\v\f";
	std::string cdata = node.Value(); // getCData()
	std::string word; // Words are whitespace separated

	cdata.erase(0, cdata.find_first_not_of(whitespace) );
	cdata.erase(cdata.find_last_not_of(whitespace) + 1);
#if GRXML_DEBUG
        std::cout << "/--" << cdata << "--/\n";
#endif

	std::string::size_type begIdx, endIdx;

        //search beginning of the first word
        begIdx = cdata.find_first_not_of(whitespace);

        //while beginning of a word found
	while (begIdx != std::string::npos) {
            //search end of the actual word
            endIdx = cdata.find_first_of (whitespace, begIdx);
            if (endIdx == string::npos) {
                //end of word is end of line
                endIdx = cdata.length();
            }
            word.clear();
	    // word.assign(cdata,begIdx,endIdx);
	    word.append (cdata, begIdx, endIdx - begIdx);
	    if ( !word.empty() )
	    {
#if GRXML_DEBUG
		std::cout << " -->" << word << "<--\n";
#endif
		int index;
		// If a slot then take note of rule name
		if ( IsSlot( word ) ) {
		  const char* xmlBasename;
		  std::string ruleName = m_RuleListStack.top();
		  m_SlotList.insert(index, ruleName);
		  xmlBasename = strrchr(m_XMLFileName.c_str(),'/');
		  xmlBasename = xmlBasename ? xmlBasename+1 : m_XMLFileName.c_str();
		  word = (std::string)xmlBasename + "." + ruleName + "@" + word;
		  addLabelToList( word );
		  findLabelIndex( word, index );
		} else {
		  addLabelToList( word );
		  findLabelIndex( word, index );
		}
		m_pGraph->AddLabel( p_SubGraph, index );
	    }
	    begIdx = cdata.find_first_not_of (whitespace, endIdx);

	}
    } //tag
    else {
	// Do nothing with CDATA for elements that are not items.
	// In particular, do not strip whitespace from tag cdata.
	// However, CPPDOM appears to remove linefeeds. May need to tidy up.

    }
    return true;
} // cdata

bool GRXMLDoc::beginItem( XMLNode &node, SubGraph *&p_SubGraph )
{
	const char* attr;
    DEBUG_PRINT ("---- Item:\n");
    // First check whethere there is a count/repeat
    std::string s     = GETATTR("repeat" );
    int minCnt=0,maxCnt=0;
    std::string s_tag = GETATTR("tag" );
    if( s_tag.length()>0) {
      FATAL_ERROR("Error: unsupported tag= syntax, use <tag> ... </tag>", 1)
    }
    if( s.length()>0 && get_range( s, &minCnt, &maxCnt) ) {
      FATAL_ERROR(std::string("error: while parsing range ") + s,1);
    }
    if ( !s.empty() ) {
      // RED FLAG: max should not be 0! A +ve number should have been given.
      if( maxCnt>0) {
	m_pGraph->BeginCount( p_SubGraph, minCnt, maxCnt );
      }
      else {
	// NB: BeginItemRepeat  can only use min of 0 or 1!
	m_pGraph->BeginItemRepeat ( p_SubGraph, minCnt, -1);
      }
    }
    else {
	m_pGraph->BeginItem( p_SubGraph );
    }
    return true;
}


bool GRXMLDoc::endItem( XMLNode &node, SubGraph *&p_SubGraph )
{
    DEBUG_PRINT ( "---- /Item\n" );

    // What TODO if no tag for an item?

    m_pGraph->EndItem( p_SubGraph );
    return true;
}


bool GRXMLDoc::beginRuleRef( XMLNode &node, SubGraph *&p_SubGraph )
{
    // Extend word FST node with an entire FST subgraph.
    // Forward referencing of rules is supported.
    // NB Remove the leading # from the ruleref name!
    DEBUG_PRINT ( "---- Ruleref\n" );

	const char* attr;
    std::string s_tag = GETATTR("tag" );
    if( s_tag.length()>0) {
      FATAL_ERROR("Error: unsupported tag= syntax, use <tag> ... </tag>", 1)
    }
    std::string s = GETATTR("uri" );
    if (s.empty())
    {
	//
	FATAL_ERROR( "ERROR! Ruleref specifies no uri name!", -1 );
    }
    // Remove the #:
    int p1 = s.find("#");
    if ( p1 !=0 ) {
	FATAL_ERROR( "ERROR! bad ruleref name: '" + s + "'" + ". Rule reference must start with a '#'. External references are not supported.", -1 );
    }
    string ruleName;
    getRuleRefName( node, ruleName );

    //std::string parentRuleName = m_RuleListStack.top();
    //addRuleDependency( parentRuleName, ruleName );

    int index;
    bool foundRule = findRuleIndex( ruleName, index );
    if (!foundRule) {
	// Forward reference; create a placeholder subgraph ptr.
	//SubGraph *newGraph = new SubGraph( (char *) ruleName.c_str() );
	// RED FLAG:  Remember to check fwd ref rule was filled in at end.
	SubGraph *newGraph;
	addRuleToList( ruleName, newGraph );
	findRuleIndex( ruleName, index );
    }
    // We can now treat a forward-referenced graph as if it was defined.
    // We will add the subgraph when we have the tag - see endItem().
    m_pGraph->BeginRule( p_SubGraph );
    m_pGraph->AddRuleRef( p_SubGraph, index );
    m_pGraph->EndRule( p_SubGraph );

    return true;
}


bool GRXMLDoc::endRuleRef(XMLNode &grmNode, SubGraph *&p_SubGraph )
{
    DEBUG_PRINT ( "---- /Ruleref\n" );
    // Does nothing
    // NB The tag is not under the ruleref element - it is in the current item element.
    // We now add the tag of the AddRuleRef as we see the tag element. See EndTag().

    return true;
}


bool GRXMLDoc::beginOneOf(XMLNode &grmNode, SubGraph *&p_SubGraph)
{
    DEBUG_PRINT ( "----OneOf\n" );
    m_pGraph->BeginOneOf (p_SubGraph);
    return true;
}


bool GRXMLDoc::endOneOf(XMLNode &grmNode, SubGraph *&p_SubGraph)
{
    DEBUG_PRINT ( "----/OneOf\n" );
    m_pGraph->EndOneOf (p_SubGraph);
    return true;
}


bool GRXMLDoc::beginTag( XMLNode &node, SubGraph *&p_SubGraph )
{
    DEBUG_PRINT ("---- Tag\n");
    std::string s = node.ToElement()->GetText(); // getCdata();
#if GRXML_DEBUG
    std::cout << s;     // debug
#endif
    // Store the semantic tag info.
    // NB Do not strip whitespace from tag cdata
    if ( !s.empty() )
    {
	int index;
	addTagToList( s );
	findTagIndex( s, index );
	m_pGraph->AddTag ( p_SubGraph, index );
    }

    return true;
}


bool GRXMLDoc::endTag( XMLNode &node, SubGraph *&p_SubGraph )
{
    DEBUG_PRINT ("---- /Tag\n");
    return true;
}


bool GRXMLDoc::beginCount( XMLNode &node, SubGraph *&p_SubGraph )
{
	const char* attr;
    // Count of reps applies to the text elements in this count node
    DEBUG_PRINT ("---- Count\n");
    // Get number attr
    std::string s     = GETATTR("number");
    std::string s_tag = GETATTR("tag" );
    if( s_tag.length()>0) {
      FATAL_ERROR("Error: unsupported tag= syntax, use <tag> ... </tag>", 1)
    }
    if (s.empty()) {
		return false;
    }
    // not  in subgraph but in graph?!
    //graph.BeginCount(n);

    int minCnt=-1, maxCnt=-1;
    if( get_range( s, &minCnt, &maxCnt) ) {
      FATAL_ERROR(std::string("error: while parsing range ") + s,1);
    }
    if ( s.c_str() == std::string("optional") )
    {
	m_pGraph->BeginOptional( p_SubGraph );
    }
    else if ( minCnt>0 && maxCnt>0) 
    {
	m_pGraph->BeginCount( p_SubGraph, minCnt, maxCnt );
    }
    else if( minCnt>0 ) 
      {
	m_pGraph->BeginItemRepeat ( p_SubGraph, minCnt, -1);
      }
    else { //
    	m_pGraph->BeginOptional ( p_SubGraph );
    }

    return true;
}


bool GRXMLDoc::endCount( XMLNode &node, SubGraph *&p_SubGraph )
{
    DEBUG_PRINT ("---- /Count\n");
    m_pGraph->EndCount( p_SubGraph );
    return true;
}

bool GRXMLDoc::endParseMetaNode(XMLNode &node)
{
  // End parse operations
  return true;
}

void GRXMLDoc::printNode(XMLNode &node, int level)
{
    std::string name = node.Value();
    int type = node.Type();
    std::string c_data;

    for(int i=0;i<level;i++) std::cout << " ";

    char c = ' ';
    switch(type)
    {
    case TiXmlNode::ELEMENT:
	// case XMLNode::xml_nt_node: // grammar, rule, one-of, item, count
	 c = '+';
	 break;
	/* case TiXmlNode::TEXT:
	// case XMLNode::xml_nt_leaf:
	c = '-';
	break; */
    case TiXmlNode::DOCUMENT:
    // case XMLNode::xml_nt_document:
	c = '\\';
	break;
    case TiXmlNode::TEXT:
    // case XMLNode::xml_nt_cdata:
	c = '#';
	c_data = node.Value(); // getCdata();
	break;
	case TiXmlNode::UNKNOWN:
	case TiXmlNode::COMMENT:
	case TiXmlNode::TYPECOUNT:
	case TiXmlNode::DECLARATION:
	default:
		std::cout << "Error: not sure what to do here" << std::endl;
		break;
    }
	if(node.Type() == TiXmlNode::TEXT)  // isCData()
	  std::cout << c << name.c_str() << "[" << c_data << "]" << std::endl;
	//Extend the tag hashtable
    else
	  std::cout << c << name.c_str() << std::endl;

	if( node.Type() == TiXmlNode::ELEMENT) {

		for(TiXmlAttribute* attr=node.ToElement()->FirstAttribute();
			attr; attr=attr->Next() ) {

		  // guru: added output of attributes
			for (int i=0; i<level; i++)
				std::cout << " ";
			std::cout << "   ";
			std::cout << attr->Name() << ": " << attr->Value() << std::endl;
		}
	}

}

/** Function: addRuleToList
    Extends list of SubGraphs with given subGraph
    and extends list of rule names too.
    TODO: Can we use one hash and use internal numeric index for rule IDs?
*/


bool GRXMLDoc::addRuleToList(std::string const & ruleName, SubGraph *&p_SubGraph)
{
    int index;
    if ( findRuleIndex ( ruleName, index ) ) {
	FATAL_ERROR("ERROR! Rule name " + ruleName + " is already defined!", -1 );
    }

    addLabelToList( m_XMLFileName + "@" + ruleName);
    findLabelIndex( m_XMLFileName + "@" + ruleName, index );
#if GRXML_DEBUG
    std::cout << "Rule " << ruleName << std::endl;
#endif
    // Create the new subgraph and update lists
    m_RuleList.insert( ruleName, index );
    p_SubGraph = new SubGraph( (char *) ruleName.c_str(), index );

    bool success = m_SubgraphList.insert( ruleName, p_SubGraph );
    if (!success) {
	FATAL_ERROR("ERROR! subgraph for " + ruleName + " is already defined!", -1 );
    }
#if ADD_BRACES
    addLabelToList( "{" );
    std::stringstream  ss;
    ss << "}(" << index << ")";
    addLabelToList( ss.str());
#endif
    return success;
}


bool GRXMLDoc::deleteRules()
{
    // Delete all allocated subgraphs.
    // The rule strings are part of the hashtables and get deleted by them.
    int index;
    SubGraph *p_SubGraph;
    std::string ruleName;
    while ( !m_RuleList.isEmpty() ) {
	m_RuleList.getFirst( &ruleName, &index );
	m_RuleList.remove( ruleName );
	if (m_SubgraphList.getValue( ruleName, &p_SubGraph ) ) {
	    delete p_SubGraph;
	}
	else {
	    FATAL_ERROR("No subgraph for rule " + ruleName + "! Mismatched rules and subgraph hashtables!", -1);
	}
    }
    m_SubgraphList.clear();
    m_RuleList.clear();
    m_LabelList.clear();
    m_TagList.clear();
    return true;
}

bool GRXMLDoc::findSubGraph(std::string & s, SubGraph *&p_SubGraph)
{
    return m_SubgraphList.getValue(s, &p_SubGraph);
}

bool GRXMLDoc::findRule(int i, std::string &s )
{
    return m_RuleList.getIndex( i, &s );
}

bool GRXMLDoc::findTag(int i, std::string &s )
{
    return m_TagList.getValue( i, &s );
}

bool GRXMLDoc::findLabel(int i, std::string &s )
{
    return m_LabelList.getValue( i, &s );
}

bool GRXMLDoc::findSubGraphIndex( SubGraph *p_SubGraph, std::string &s )
{
    return m_SubgraphList.getIndex( p_SubGraph, &s );
}

bool GRXMLDoc::findRuleIndex( std::string s, int &i )
{
    return m_RuleList.getValue( s, &i );
}
bool GRXMLDoc::findTagIndex( std::string s, int &i )
{
    return m_TagList.getIndex( s, &i );
}
bool GRXMLDoc::findLabelIndex( std::string s, int &i )
{
    return m_LabelList.getIndex( s, &i );
}
bool GRXMLDoc::findMeta(const std::string & sn, std::string &s)
{
    return m_MetaKeyValPairs.getValue( sn, &s );
}
bool GRXMLDoc::setMeta(const std::string & sn, const std::string &s)
{
  std::string tmp;
  if(findMeta(sn,tmp)) 
    m_MetaKeyValPairs.remove(sn);
  return m_MetaKeyValPairs.insert(sn,s);
}

bool GRXMLDoc::addTagToList( std::string const& s )
{
    bool success = true;
    // Make values unique
    int index;
    if ( !findTagIndex( s, index ) ) 
	success = m_TagList.insert( m_TagAutoIndex++, s );
    return success;
}


bool GRXMLDoc::addLabelToList( std::string const& s )
{
  // TODO: Labels should be unique. Change key.
  int index;
  bool bRes = m_LabelList.getIndex( s, &index );
  if(bRes == true) {
    return false; // exists
  }
  bRes = m_LabelList.insert( m_LabelAutoIndex++, s );
  return  bRes;
}

void GRXMLDoc::printLists()
{
    m_SubgraphList.print();
    m_RuleList.print();
    m_TagList.print();
    m_LabelList.print();
}


void GRXMLDoc::printSubgraphs()
{
    SubGraph *p_SubGraph;
    std::string rule;
    int index;
    if ( m_RuleList.getFirst( &rule, &index) ) {
	if ( findSubGraph( rule, p_SubGraph ) ) {
	    DEBUG_PRINT("============ Rule: " + rule + "============");
	    printSubgraph( *p_SubGraph );
	    while ( m_RuleList.getNext( &rule, &index) ) {
		if ( findSubGraph( rule, p_SubGraph ) ) {
		    printSubgraph( *p_SubGraph );
		}
	    }
	}
    }
}


void GRXMLDoc::printSubgraph( SubGraph &p_SubGraph )
{
    p_SubGraph.PrintWithLabels( *this );
}


bool GRXMLDoc::getRuleRefName(XMLNode &node, std::string &ruleName)
{
  const char* attr;
  std::string s = GETATTR("uri" );
  if (s.empty()) {
    FATAL_ERROR( "ERROR! Ruleref specifies no uri name!", -1 );
  }
  // Remove the #:
  int p1 = s.find("#");
  if ( p1 !=0 ) {
    FATAL_ERROR( "ERROR! bad ruleref name: '" + s + "'", -1 );
  }
  ruleName.assign( s, 1, s.size() );
  return true;
}

void GRXMLDoc::initializeLists()
{
  m_SubgraphList.setName("Subgraphs");
  m_RuleList.setName("Rules");
  m_TagList.setName("Tags");
  m_LabelList.setName("Labels");
  
  /* Predefined rules. NB Labels are also created for each rule added.
  // The required order for these labels in the .map output file is:
  //     0   eps
  //     next come slots
  //     pau and pau2
  //     everything else
  // We will add all these now in case they are referenced and we will
  // reindex after we have parsed the grammar -- when we have the list
  // of slots. This re-indexing is for the output files .map and .P.txt.
  //
  */
    addLabelToList( "eps" );

    addLabelToList( "-pau-" );
    addLabelToList( "-pau2-" );
}

void GRXMLDoc::writeMapFile( std::string & fileName )
{
    // We need to re-index in order to put the labels in correct order:
    // 1. eps
    // 2. all slots
    // 3. all rules
    // 4. -pau- words
    // 5. remaining labels
    ofstream outfile;
    int index, origIndex;
    std::string label;
    std::string slotRuleName;
    std::string scope; // For rules
    HashMap<int,std::string> orderedList;
    int orderedIndex=0;
    // 1. eps
    orderedList.insert( orderedIndex++, "eps" );

    // 2. slots
    if ( m_LabelList.getFirst( &origIndex, &label ) ) {
	if ( IsSlot( label ) ) {
	    orderedList.insert( orderedIndex++, label );
	}
	while (m_LabelList.getNext( &origIndex, &label ) ) {
	    if ( IsSlot( label ) ) {
		orderedList.insert( orderedIndex++, label );
	    }
	}
    }

    // 3.  Now rules, or anything with @
    if ( m_LabelList.getFirst( &origIndex, &label ) ) {
	do {
#if GRXML_DEBUG
	    std::cout << label << " "<< label.find_first_of ("@") << std::endl;
#endif
            if (!IsSlot(label) && label.find_first_of ("@") != string::npos) {
#if GRXML_DEBUG
		std::cout << "    Adding " << label << std::endl;
#endif
		orderedList.insert( orderedIndex++, label );
	    }
	} while (m_LabelList.getNext( &origIndex, &label ) );
    }

    // 4. pau
    orderedList.insert( orderedIndex++, "-pau-" );
    orderedList.insert( orderedIndex++, "-pau2-" );

    // 5. Remaining stuff. NB We depend upon the label not
    //    being added twice.
    if ( m_LabelList.getFirst( &origIndex, &label ) ) {
	if ( !orderedList.getIndex( label, &index ) ) {
	  orderedList.insert( orderedIndex++, label );
	}
	while (m_LabelList.getNext( &origIndex, &label ) ) {
	    if ( !orderedList.getIndex( label, &index ) ) {
	      orderedList.insert( orderedIndex++, label );
	    }
	}
    }
    outfile.open ( fileName.c_str() );

    bool bRes = orderedList.getFirst( &index, &label );
    do {
      if(!bRes) break;
      // Look up scope using original index
      m_LabelList.getIndex( label, &origIndex );
      if (m_RuleScope.getValue(origIndex, &scope) ) 
	label = scope + ":" + label;
      outfile << label << " " << index << std::endl;
      bRes = orderedList.getNext( &index, &label );
    } while(bRes);

    outfile.close();
}


void GRXMLDoc::writeScriptFile( std::string & fileName )
{
    ofstream outfile;
    int index;
    std::string label;
    outfile.open ( fileName.c_str() );
    if ( m_TagList.getFirst( &index, &label ) ) {
    	outfile << index << " " << label << std::endl;
    }
    while (m_TagList.getNext( &index, &label ) ) {
    	outfile << index << " " << label << std::endl;
    }
    outfile.close();

    //m_LabelList.writeFile( fileName );
}

void GRXMLDoc::writeParamsFile( std::string & fileName )
{
  std::string wtw;
  ofstream outfile;
  bool bRes;
  
  outfile.open(fileName.c_str());

  std::string metaname = "word_penalty";
  bRes = findMeta(metaname, wtw);
  if(bRes)
    outfile << metaname.c_str() << "\t=\t" << wtw.c_str() << std::endl;

  // outfile << "locale"  << "\t=\t" << m_XMLLanguage << std::endl;
  outfile.close();
}

void GRXMLDoc::writeGraphFiles( std::string& prefix, bool bDoWriteRecogGraphs)
{
    SubGraph *p_SubGraph;
    SubGraph *p_SemGraph;
    std::string fileName;
    if ( !findSubGraph( m_RootRule, p_SubGraph ) ) {
	FATAL_ERROR ("ERROR: writeGraphFiles - no root rule "+ m_RootRule + " defined. No file created", -1 );
    }

    //  Create .P.txt
    printf ("\nCreating semantic graph file\n");
    p_SemGraph = new SubGraph( (char *) "Main", -1);
    m_pGraph->BeginRule( p_SemGraph );
    m_pGraph->AddRuleRef( p_SemGraph, p_SubGraph->getRuleId());
    m_pGraph->EndRule( p_SemGraph );
    m_pGraph->ExpandRules (p_SemGraph);
    p_SemGraph->RemoveInternalConnections ();

    p_SemGraph->AddTerminalConnections ();
    p_SemGraph->ReduceArcsByEquivalence();
    p_SemGraph->RemoveUnreachedConnections (-1, -1);
    p_SemGraph->DeterminizeArcs();
    p_SemGraph->RemoveUnreachedConnections (-1, -1);
    p_SemGraph->ReduceArcsByEquivalence();
    p_SemGraph->RemoveUnreachedConnections (-1, -1);
    fileName = prefix + ".P.txt";
    p_SemGraph->WriteForwardGraphWithSemantic( fileName, *this );
    delete p_SemGraph;

    fileName = prefix + ".omap";
    this->WriteOLabels(fileName);
}

void GRXMLDoc::sortLabels()
{
    // We need to re-index in order to put the labels in correct order:
    int index=0, origIndex;
    std::string label;
    std::string slotRuleName;
    std::string scope; // For rules
    std::vector <std::string> orderedList;
    if ( m_LabelList.getFirst( &origIndex, &label ) ) {
        // Look up scope using original index
        orderedList.push_back( label );
        while (m_LabelList.getNext( &origIndex, &label ) ) {
            orderedList.push_back( label );
        }
    }
    std::sort(orderedList.begin(), orderedList.end() );
    m_SortedLabelList.clear();
    index=0;
    for (std::vector<std::string>::const_iterator citer = orderedList.begin();
     citer != orderedList.end(); ++citer) {
        label = *citer;
        m_LabelList.getIndex( label, &origIndex );
        m_SortedLabelList.insert( index, label );
        index++;
        // std::cout <<"Sorted: " << index <<" " << label <<std::endl;
    }
    return;
}

bool GRXMLDoc::findSortedLabel(int i, std::string &s )
{
    if (m_SortedLabelList.isEmpty() ) {
        sortLabels(); // Create the sorted label list.
    }
    return m_SortedLabelList.getValue( i, &s );
}

bool GRXMLDoc::findSortedLabelIndex( int i, int &sortedIndex )
{
    std::string s;
    if (m_SortedLabelList.isEmpty() ) {
        sortLabels(); // Create the sorted label list.
    }
    if ( m_LabelList.getValue( i, &s ) ) {
        if ( m_SortedLabelList.getIndex(s, &sortedIndex )) {
            return true;
        }
    }
    return false;
}

void GRXMLDoc::addOLabelToOList( std::string &s)
{
    m_OutputPtxtLabels.insert( s, 0);
}

bool GRXMLDoc::WriteOLabels(const std::string& fileName)
{
  HashMap<int,std::string> invMap;
  int count = 0;
  int max_script_label = 0;
  int scriptID = 0;
  std::map<std::string, int>::iterator iter;
  bool bFound;
  int tmp;

  std::string strIndex = "eps";
  bFound = m_OutputPtxtLabels.getValue(strIndex, &tmp);
  if(bFound) 
    m_OutputPtxtLabels.remove(strIndex);
  m_OutputPtxtLabels.insert(strIndex, count); 
  invMap.insert( count, strIndex);
  count++;

  strIndex = "{";
  bFound = m_OutputPtxtLabels.getValue(strIndex, &tmp);
  if(bFound) 
    m_OutputPtxtLabels.remove(strIndex);
  m_OutputPtxtLabels.insert(strIndex, count); 
  invMap.insert( count, strIndex);
  count++;

  iter = m_OutputPtxtLabels.begin(); 
  for( ; iter!=m_OutputPtxtLabels.end(); iter++) {
    const char* label = iter->first.c_str();
    if( !strncmp(label,SCRIPT_LABEL_PREFIX, SCRIPT_LABEL_PREFIX_LEN)
	&& strspn(label+SCRIPT_LABEL_PREFIX_LEN,"0123456789")==strlen(label+SCRIPT_LABEL_PREFIX_LEN) ) {
      scriptID = atoi(label+SCRIPT_LABEL_PREFIX_LEN);
      if(max_script_label < scriptID)
	max_script_label = scriptID;
    }/* else if( !strncmp(label,SCRIPT_LABEL_PREFIX, SCRIPT_LABEL_PREFIX_LEN)) {
      invMap.insert(count, iter->first);
      iter->second = count;
      count++;
      }*/
    else if(!invMap.getIndex((iter->first), &tmp)){
      invMap.insert(count, iter->first);
      iter->second = count;
      count++;
    }
  }

  cout << "found max_script_label " << max_script_label << endl;
  for(int j=0; j<=max_script_label; j++) {
    std::stringstream ss;
    ss << SCRIPT_LABEL_PREFIX << j;
    if(!invMap.getIndex( ss.str(), &tmp)) {
      invMap.insert( count++, ss.str());
    }
  }

  std::ofstream outfile(fileName.c_str());
  std::string outscript;
  if(!outfile) {
    FATAL_ERROR( "Error: opening the omap file for output", 1);
    WARNING( "Error: opening the omap file for output");
    return 1;
  } 
  for(int i=0; i<count; i++) {
    outscript = "";
    invMap.getValue(i,&outscript);
    if(outscript.length() == 0) {
      cout << "error: internal error while making .omap " << i << endl;
      FATAL_ERROR("error",1);
    }
    outfile << outscript.c_str() << " " << i << std::endl;
  }
  outfile.close();
  return 0;
}
