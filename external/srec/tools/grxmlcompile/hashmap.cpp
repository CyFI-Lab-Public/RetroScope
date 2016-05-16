/* FILE:		hashmap.cpp
 *  DATE MODIFIED:	31-Aug-07
 *  DESCRIPTION:	Helper template for compiling FST data structure 
 *                      from a GRXML file.
 *			A doubly indexed map class using two maps.
 *			Indices are a user-defined  type and an int index. 
 *                      Both are unique indices.
 *			The int index has automatic non-reusable numbering.
 *
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

#include <map>
#include <string>
#include <iostream>
#include <fstream>

#include "hashmap.h"
#include "sub_grph.h"
using namespace std;


template <typename T1, typename T2>
HashMap<T1,T2>::HashMap():
m_NextAutoIndex(0)
{
}

template <typename T1, typename T2>
void HashMap<T1,T2>::setName(std::string s)
{
  m_Name = s;
}

template <typename T1, typename T2>
bool HashMap<T1,T2>::insert( T1 const & index, T2 const & value)
{

  pair<typename std::map<T1,T2>::iterator,bool> result = m_Map.insert( make_pair(index, value) );
    if (!result.second) {
	return false;
    }

    return true;
}

template <typename T1, typename T2>
bool HashMap<T1,T2>::remove( T1 const & index )
{
  m_Map.erase( index );
  return true;
}

template <typename T1, typename T2>
bool HashMap<T1,T2>::isEmpty()
{
    return m_Map.empty();
}


template <typename T1, typename T2>
bool HashMap<T1,T2>::clear()
{
    m_Map.clear();
    return true;
}



template <typename T1, typename T2>
bool HashMap<T1,T2>::getIndex( T2 const & value, T1 *index )
{
   //do something with all elements having a certain value
   typename std::map<T1,T2>::iterator pos;
   for (pos = m_Map.begin(); pos != m_Map.end(); ++pos) {
      if (pos->second == value) {
	  *index = (pos->first);
	  return true;
      }
   }
   return false;
}

template <typename T1, typename T2>
bool HashMap<T1,T2>::getFirst( T1 *index, T2 *value )
{
    if (m_Map.empty() ) {
	return false;
    }
    //do something with all elements having a certain value
    typename std::map<T1,T2>::iterator pos;
    m_pPos= m_Map.begin();
    *index = m_pPos->first;
    *value=  m_pPos->second;
    return true;
}

template <typename T1, typename T2>
bool HashMap<T1,T2>::getNext( T1 *index, T2 *value )
{
    if ( m_Map.empty() ) {
	return false;
    }
    if ( ++m_pPos == m_Map.end() )  {
	return false;
    }
    *index = m_pPos->first;
    *value=  m_pPos->second;
    return true;
}

template <typename T1, typename T2>
bool HashMap<T1,T2>::getValue(T1 const & index, T2 *value)
{
    typename std::map<T1,T2>::iterator pos;
    pos = m_Map.find(index);
    if (m_Map.end() != pos) {
	*value = pos->second;
	return true;
    }
    return false;
}

template <typename T1, typename T2>
int HashMap<T1,T2>::size()
{
    return m_Map.size();
}

template <typename T1, typename T2>
void HashMap<T1,T2>::print()
{
    typename std::map<T1,T2>::iterator pos;
    cout << "======= '" <<  m_Name <<"' =======" << std::endl;
    for (pos = m_Map.begin(); pos != m_Map.end(); ++pos) {
	cout << pos->first <<" : " << pos->second << std::endl;
   }
}

template <typename T1, typename T2>
void HashMap<T1,T2>::writeFile( std::string fileName )
{
    ofstream outfile;
    outfile.open ( fileName.c_str() );
    typename std::map<T1,T2>::iterator pos;
    for (pos = m_Map.begin(); pos != m_Map.end(); ++pos) {
	outfile << pos->first << " " << pos->second << std::endl;
    }
    outfile.close();
}

template <typename T1, typename T2>
typename std::map<T1,T2>::iterator HashMap<T1,T2>::begin()
{
  m_pPos = m_Map.begin();
  return m_pPos;
}

template <typename T1, typename T2>
typename std::map<T1,T2>::iterator HashMap<T1,T2>::end()
{
  m_pPos = m_Map.end();
  return m_pPos;
}

// Declare known data types so that we don't need to put this in hashmap.h.
// If user needs others the put the declaration in a separate user file.
template class HashMap<int,string>;
template class HashMap<int, int>;
template class HashMap<string, SubGraph* >;
template class HashMap<std::string,int>;
template class HashMap<std::string, HashMap<std::string, int>*>;
template class HashMap<std::string, std::string>;
