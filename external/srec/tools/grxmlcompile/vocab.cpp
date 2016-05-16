/*---------------------------------------------------------------------------*
 *  vocab.cpp                                                                *
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

#include <string>
#include <iostream>
#include <stdexcept>
#include "ESR_Locale.h"
#include "LCHAR.h"
#include "pstdio.h"
#include "ESR_Session.h"
#include "SR_Vocabulary.h"

#include "vocab.h"

#define MAX_LINE_LENGTH     256
#define MAX_PRONS_LENGTH 1024

#define DEBUG	0

#define GENERIC CONTEXT "#"

Vocabulary::Vocabulary( std::string const & vocFileName )
{
    ESR_ReturnCode rc;
    rc = SR_VocabularyLoad(vocFileName.c_str(), &m_hVocab);
    if (rc != ESR_SUCCESS)
    {
        std::cout << "Error: " << ESR_rc2str(rc) <<std::endl;
        exit (-1);
    }
}

Vocabulary::~Vocabulary()
{
    SR_VocabularyDestroy(m_hVocab);
}

Pronunciation::Pronunciation()
{
}

Pronunciation::~Pronunciation()
{
}

void Pronunciation::clear()
{
    m_Prons.clear();
    for (unsigned int ii=0;ii<m_ModelIDs.size();ii++ )
    {
        m_ModelIDs[ii].clear();
    }
    m_ModelIDs.clear();
}

int Pronunciation::lookup(  Vocabulary & vocab, std::string  & phrase )
{
    ESR_ReturnCode rc;
    LCHAR prons[MAX_PRONS_LENGTH];
    LCHAR* c_phrase;
    size_t len;

    LCHAR s[MAX_LINE_LENGTH];
    strcpy (s, phrase.c_str() ); // No conversion for std::string to wchar
    //clear();

    memset (prons, 0x00, sizeof(LCHAR));

    c_phrase = s;
    SR_Vocabulary *p_SRVocab = vocab.getSRVocabularyHandle();
#if DEBUG
    std::cout << "DEBUG: " << phrase <<" to be looked up" << std::endl;
#endif
    rc = SR_VocabularyGetPronunciation( p_SRVocab, c_phrase, prons, &len );
    if (rc != ESR_SUCCESS)
        //  std::cout <<"ERORORORORROOR!" <<std::endl;
        std::cout <<"ERROR: " << ESR_rc2str(rc) << std::endl;
    else {
#if DEBUG
        std::cout <<"OUTPUT: " << prons << " num " << len << std::endl;
#endif
        size_t len_used;
        LCHAR *pron = 0;
        for(len_used=0; len_used <len; ) {
            pron = &prons[0]+len_used;
            len_used += LSTRLEN(pron)+1;
#if DEBUG
            std::cout << "DEBUG: used " << len_used << " now " << LSTRLEN(pron) << std::endl;
#endif
            std::string pronString( pron ); // wstring conversion if needed
            addPron( pronString );
#if DEBUG
            std::cout << "DEBUG: " << phrase << " " << pron << std::endl;
#endif
        }
    }
    return getPronCount();
}


int Pronunciation::addPron( std::string & s )
{
    m_Prons.push_back( s );
    return m_Prons.size();
}

int Pronunciation::getPronCount()
{  // returns number of prons
    return m_Prons.size();
}

bool Pronunciation::getPron( int index, std::string &s )
{
 // returns string length used
    try {
      s = m_Prons.at(index);
    }
    catch(std::out_of_range& err) {
      std::cerr << "out_of_range: " << err.what() << std::endl;
    }
    return true;
}

void Pronunciation::print()
{
  std::string s;
  for (int ii=0; ii< getPronCount(); ii++) {
    getPron(ii, s);
#if DEBUG
    std::cout << "Pron #" << ii << ": " << s << std::endl;
#endif
  }
}

void Pronunciation::printModelIDs()
{
  std::string s;
  for (int ii=0; ii< getPronCount(); ii++) {
    getPron(ii, s);
#if DEBUG
    std::cout << "  Pron #" << ii << ": " << s << std::endl;
    std::cout << "    Model IDs: ";
#endif
    for (int jj=0;jj<getModelCount(ii);jj++) {
      std::cout << " " << getModelID(ii,jj);
    }
#if DEBUG
    std::cout <<  std::endl;
#endif
  }
}

int Pronunciation::getPhonemeCount( int pronIndex )
{
  std::string s;
  getPron(pronIndex, s);
  return s.size();
}

bool Pronunciation::getPhoneme( int pronIndex, int picIndex , std::string &phoneme )
{
  std::string s;
  getPron(pronIndex, s);
  phoneme= s.at(picIndex);
  return true;
}


bool Pronunciation::getPIC( int pronIndex, int picIndex, std::string &pic )
{
  std::string pron;
  char lphon;
  char cphon;
  char rphon;

  getPron( pronIndex, pron );
  int numPhonemes = pron.size();
  if ( 1==numPhonemes ) {
    lphon=GENERIC_CONTEXT;
    rphon=GENERIC_CONTEXT;
    cphon = pron.at(0);
  }
  else
    {
      if ( 0==picIndex ) {
	lphon=GENERIC_CONTEXT;
	rphon=GENERIC_CONTEXT;
      }
      else if( numPhonemes-1==picIndex ) {
	lphon = pron.at(picIndex-1);
	rphon=GENERIC_CONTEXT;
      }
      else {
	lphon = pron.at(picIndex-1);
	rphon = pron.at(picIndex+1);
      }
      cphon = pron.at(picIndex);
      pic = lphon + cphon + rphon;
    }
  return true;
}

int Pronunciation::lookupModelIDs( AcousticModel &acoustic )
{
  // Looks up all hmms for all prons
  std::string pron;
  char lphon;
  char cphon;
  char rphon;

  int numProns = getPronCount();
  int totalCount=0;
  for (int ii=0;ii < numProns; ii++ )
    {
      getPron( ii, pron );
      std::vector<int> idList; // Create storage
      int numPhonemes = getPhonemeCount(ii);
      if (1==numPhonemes) {
	lphon=GENERIC_CONTEXT;
	rphon=GENERIC_CONTEXT;
	cphon = pron.at(0);
      }
      else
      for ( int jj=0;jj<numPhonemes;jj++ )
	{
	  std::string pic;
	  getPIC(ii, jj, pic);
	  lphon = pron.at(0);
	  cphon = pron.at(1);
	  rphon = pron.at(2);
	  int id = CA_ArbdataGetModelIdsForPIC( acoustic.getCAModelHandle(), lphon, cphon,  rphon );
#if DEBUG
	  std::cout <<"DEBUG model id: " << lphon <<cphon << rphon << "  "<< id << std::endl;
#endif

	  idList.push_back(id);
	}
      m_ModelIDs.push_back(idList);
      totalCount+=numPhonemes;
    }
  return totalCount;
}

int Pronunciation::getModelCount( int pronIndex )
{
  return m_ModelIDs[pronIndex].size();
}

int Pronunciation::getModelID( int pronIndex, int modelPos )
{
  return m_ModelIDs[pronIndex][modelPos];
}

AcousticModel::AcousticModel( std::string & arbFileName )
{
  m_CA_Arbdata = CA_LoadArbdata( arbFileName.c_str() );
  if (!m_CA_Arbdata)
    {
      std::cout << "Error: while trying to load " << arbFileName.c_str() << std::endl;
      exit (-1);
    }

}

AcousticModel::~AcousticModel()
{
  CA_FreeArbdata( m_CA_Arbdata);
}

int AcousticModel::getStateIndices(int id, std::vector<int> & stateIDs)
{
  srec_arbdata *allotree = (srec_arbdata*) m_CA_Arbdata;
  int numStates = allotree->hmm_infos[id].num_states;
#if DEBUG
  std::cout << "getStateIndices: count = " << numStates <<std::endl;
#endif
  for (int ii=0; ii <numStates; ii++ ) {
    stateIDs.push_back( allotree->hmm_infos[id].state_indices[ii] );
#if DEBUG
    std::cout <<  allotree->hmm_infos[id].state_indices[ii] ;
#endif
  }
#if DEBUG
  std::cout << std::endl;
#endif
    return stateIDs.size();
}

