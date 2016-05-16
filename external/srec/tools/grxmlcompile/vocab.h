/*---------------------------------------------------------------------------*
 *  vocab.h                                                                  *
 *                                                                           *
 *  Copyright 2007, 2008 Nuance Communciations, Inc.                         *
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


/** FILE:	        vocab.h
**  DATE MODIFIED:	31-Aug-07
**  DESCRIPTION:	Container class for Nuance Vocabulary access
**
**			All rights reserved
*/

#ifndef __vocab_h__
#define  __vocab_h__

#include <vector>
#include <string>
#include "SR_Session.h"
#include "SR_Vocabulary.h"

//#include "srec_arb.h"

#include "simapi.h"


#define GENERIC_CONTEXT '_'
#define SILENCE_CONTEXT '#'
#define INTRA_SILENCE_CONTEXT '&'

class Vocabulary
{
 public:
    Vocabulary();
    Vocabulary( std::string const & vocFileName );
    ~Vocabulary();
    SR_Vocabulary *getSRVocabularyHandle() { return m_hVocab; }
 private:
    SR_Vocabulary *m_hVocab;
};


class AcousticModel
{
    public:
        AcousticModel( std::string & arbFileName );
        ~AcousticModel();
        CA_Arbdata *getCAModelHandle() {return m_CA_Arbdata; }
        int getStateIndices(int id, std::vector<int> & stateIDs);
    private:
        CA_Arbdata* m_CA_Arbdata;
};


class Pronunciation
{
    public:
        typedef enum PelPosition {LEFT, RIGHT, MIDDLE};
        Pronunciation();
        //    Pronunciation( Vocabulary & vocab );
        int lookup( Vocabulary & vocab, std::string  & phrase );
        ~Pronunciation();
        int addPron( std::string & s );
        int getPronCount(); // returns number of prons
        bool getPron( int index, std::string &s );
        void clear();
        void print();
        void printModelIDs();
        int getPhonemeCount( int pronIndex );
        bool getPhoneme( int pronIndex, int picIndex , std::string &phoneme );
        int lookupModelIDs( AcousticModel &acoustic );
        int getModelCount( int pronIndex ); //
        int getModelID( int pronIndex, int modelPos );
        bool getPIC( int pronIndex, int picIndex, std::string &pic );

    private:
        Vocabulary *m_pVocab;

        std::string m_Phrase;
        std::vector<std::string> m_Prons;
        std::vector< std::vector<int> > m_ModelIDs;
};

#endif // __vocab_h__


