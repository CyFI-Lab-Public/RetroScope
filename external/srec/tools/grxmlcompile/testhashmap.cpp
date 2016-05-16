/*---------------------------------------------------------------------------*
 *  testhashmap.cpp  *
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
#include <fstream>
#include <iostream>
#include "../src/hashmap.h"
using namespace std;


#if 1
void test1();
void test2();


void main(int argc, char* argv[])
{
test2();
}

// (INT,INT) hash
void test1()
{
    HashMap<int,int > myHash;
    int value;
    int i;
    i=10;
    myHash.setName("TestHash");
    myHash.insert(1, i);
    myHash.getValue(1, &value);
    std::cout << "Index 1 has value= " << value <<std::endl;
    myHash.getIndex( 10, &i );
    std::cout << "value " << value << " has index " << i <<std::endl;
    unsigned int j;
    myHash.getNumericIndex(i, &j);
    std::cout << "index  " << i << " has numeric index " << j <<std::endl;
    myHash.getNumericIndexByValue(value, &j);
    std::cout << "value " << value << " has numeric index " << j <<std::endl;

    myHash.print();
    myHash.remove(1);
    myHash.print();
}


// (INT,STRING) hash
void test2()
{
    HashMap<int,string> myHash;
    string value = "hello";
    int i;
    i=10;
    myHash.setName("TestHash");
    myHash.insert(1, value);
    myHash.insert(2, "world");

    myHash.getValue(1, &value);
    std::cout << "Index 1 has value= " << value <<std::endl;
    myHash.getIndex( value, &i );
    std::cout << "value " << value << " has index " << i <<std::endl;
    unsigned int j;
    myHash.getNumericIndex(i, &j);
    std::cout << "index  " << i << " has numeric index " << j <<std::endl;
    myHash.getNumericIndexByValue(value, &j);
    std::cout << "value " << value << " has numeric index " << j <<std::endl;

    myHash.print();
    myHash.getFirst(&i, &value);
    std::cout << "First iterator values are " << i <<", " << value <<std::endl;
    if (myHash.getNext(&i, &value)) {
	std::cout << "Iterator values are " << i <<", " << value <<std::endl;
    }
    else {
	std::cout << "No first index - map is empty" <<std::endl;
    }
    myHash.remove(1);
    myHash.getFirst(&i, &value);
    std::cout << "First iterator values are " << i <<", " << value <<std::endl;
    if (myHash.getNext(&i, &value)) {
	std::cout << "Iterator values are " << i <<", " << value <<std::endl;
    }
    else {
	std::cout << "No next index - map is empty" <<std::endl;
    }


    myHash.print();
}










#else

void findi(string s);
void finds(int i);
void insert(int i, const string &s);
void remove( int i );

HashMap<int,string> myHash;

void main(int argc, char* argv[])
{
    string s;
    s = "hello";
    insert(1,s);
    insert(2,"world");

    finds(2);
    finds(1);
    finds(99);
    findi("hello");
    findi("world");
    findi("xox");

    s = "bollocks";
    findi("hello");
    finds(1);
    insert(3,s);
    finds(3);
    insert(3,"zzz");
    finds(3);
    remove(3);
    insert(3,"zzz");
    finds(3);

}


void findi(string s)
{
    int i;
    if ( myHash.getIndex(s, &i) ) {
	cout << "'" << s << "' has index of " << i <<endl;
    }
    else {
	cout << "'" << s << "' not found!" << endl;
    }
}

void finds(int i)
{
    string s;
    if ( myHash.getValue(i, &s) ) {
	cout << "'" << i << "' has value of " << s <<endl;
    }
    else {
	cout << "'" << i << "' not found!" << endl;
    }
}

void insert( int i, const string &s)
{
    string ss;
    if (!myHash.getValue(i, &ss) ) {
	if ( myHash.insert(i, s) ) {
	    cout << "Inserted: " << i << "," << s <<endl;
	}
    }
    else {
	cout << "Failed to insert '" << i << "," << s <<"'" << endl;
    }
}

void remove( int i )
{
    string ss;
    if (myHash.getValue(i, &ss) ) {
	if ( myHash.remove(i) ) {
	    cout << "Removed: " << i << endl;
	}
    }
    else {
	cout << "Failed to remove '" << i << "'" << endl;
    }
}


#endif
