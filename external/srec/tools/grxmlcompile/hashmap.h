/* FILE:		hashmap.h
 *  DATE MODIFIED:	31-Aug-07
 *  DESCRIPTION:	Part of the  SREC graph compiler project source files.
 *
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

#ifndef __hashmap_h__
#define __hashmap_h__

#include <iostream>
#include <map>
#include <vector>


template <typename T1, typename T2>
class HashMap
{
public:
    //typedef T1	MapValue;
    HashMap();
    void setName(std::string s);
    bool insert( T1 const & index, T2 const & value);
    bool remove( T1 const & index);
    bool isEmpty();
    bool clear();
    bool getFirst( T1 *index, T2 *value );
    bool getNext( T1 *index, T2 *value );
    bool getValue( T1 const & index, T2 *value);	//returns value
    bool getIndex( T2 const & value, T1 *index );	//returns index
    void print();
    void writeFile( std::string fileName );

    typename std::map<T1,T2>::iterator begin();
    typename std::map<T1,T2>::iterator end();

    int size();

private:
    std::string m_Name;
    std::map<T1, T2> m_Map;
    typename std::map<T1,T2>::iterator m_pPos;

    unsigned int m_NextAutoIndex;
};


#endif // __hashmap_h__
