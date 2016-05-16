/* FILE:        netw_arc.h
 *  DATE MODIFIED:    31-Aug-07
 *  DESCRIPTION:    Part of the  SREC graph compiler project source files.
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

#ifndef __netw_arc_h__
#define __netw_arc_h__

#undef assert
#define assert(X)
#include <cstdio>

class GRXMLDoc;
class NUANArc
{
public:
    friend class SubGraph;
    /* Constructors */

    /* Create arc with only input and output labels
    */
    NUANArc (int iLabel, int oLabel)
    {
        inputLabel= iLabel;
        outputLabel= oLabel;
        centre= -1;
        left= -1;
        right= -1;
        return;
    };

    /* Create arc with full data
    */
    NUANArc (int iLabel, int oLabel, int from, int to)
    {
        inputLabel= iLabel;
        outputLabel= oLabel;
        fromId= from;
        toId= to;
        left= -1;
        right= -1;
        centre= -1;
        return;
    };

    /* Copy an arc
    */
    NUANArc (NUANArc *arcsrc)
    {
        inputLabel= arcsrc->inputLabel;
        outputLabel= arcsrc->outputLabel;
        fromId= arcsrc->fromId;
        toId= arcsrc->toId;
        left= arcsrc->left;
        right= arcsrc->right;
        centre= arcsrc->centre;
        return;
    };

    /* Create arc based on another arc
    */
    NUANArc (NUANArc *arcsrc, int offset, int startId, int newStartId, int endId, int newEndId)
    {
        inputLabel= arcsrc->inputLabel;
        outputLabel= arcsrc->outputLabel;
        if (arcsrc->fromId == startId && newStartId >= 0)
            fromId= newStartId;
        else
            fromId= arcsrc->fromId + offset;
        if (arcsrc->toId == endId && newEndId >= 0)
            toId= newEndId;
        else
            toId= arcsrc->toId + offset;
        left= -1;
        right= -1;
        centre= -1;
        return;
    };

    /*  Assign non-terminal vertices
    */
    void AssignFromId (int Id)
    {
        fromId= Id;
    };

    /*  Assign non-terminal vertices
    */
    void AssignToId (int Id)
    {
        toId= Id;
    };

    void AssignInput (int Id)
    {
        inputLabel= Id;
    };

    void AssignOutput (int Id)
    {
        outputLabel= Id;
    };

    /*  Assign centre context
    */
    void AssignCentre (int centreData) { centre= centreData; };

    /*  Assign left context
    */
    void AssignLeft (int leftData) { left= leftData; };

    /*  Assign right context
    */
    void AssignRight (int rightData) { right= rightData; };

    /* Access functions */
    /* Get input label
    */
    int GetInput() { return inputLabel; };

    /* Get output label
    */
    int GetOutput()  { return outputLabel; };

    /* Get from Vertex
    */
    int GetFromId()  { return fromId; };

    /* Get to Vertex
    */
    int GetToId()  { return toId; };

    /* Get centre context
    */
    int GetCentre()  { return centre; };

    /* Get left context
    */
    int GetLeft()  { return left; };

    /* Get right context
    */
    int GetRight()  { return right; };

    /*  Transduction
    */
    int Transduce (int iLabel)
    {
        if (inputLabel == iLabel)
            return outputLabel;
        else
            return -1;
    };

    /*  Similarity checks
    */
    int Compare (NUANArc *test)
    {
        if (fromId > test->fromId)
            return 1;
        else if (fromId < test->fromId)
            return -1;
        else if (toId > test->toId)
            return 1;
        else if (toId < test->toId)
            return -1;
        else if (inputLabel > test->inputLabel)
            return 1;
        else if (inputLabel < test->inputLabel)
            return -1;
        else if (outputLabel > test->outputLabel)
            return 1;
        else if (outputLabel < test->outputLabel)
            return -1;
        else
            return 0;
        }

    int CompareSymbol (NUANArc *test)
    {
        if (inputLabel > test->inputLabel)
            return 1;
        else if (inputLabel < test->inputLabel)
            return -1;
        else if (outputLabel > test->outputLabel)
            return 1;
        else if (outputLabel < test->outputLabel)
            return -1;
        else
            return 0;
        }

    int CompareReverse (NUANArc *test)
    {
        if (toId > test->toId)
            return 1;
        else if (toId < test->toId)
            return -1;
        else if (fromId > test->fromId)
            return 1;
        else if (fromId < test->fromId)
            return -1;
        else if (inputLabel > test->inputLabel)
            return 1;
        else if (inputLabel < test->inputLabel)
            return -1;
        else if (outputLabel > test->outputLabel)
            return 1;
        else if (outputLabel < test->outputLabel)
            return -1;
        else
            return 0;
    }

    int CompareForMin (NUANArc *test)
    {
        if (fromId > test->fromId)
            return 1;
        else if (fromId < test->fromId)
            return -1;
        else if (inputLabel > test->inputLabel)
            return 1;
        else if (inputLabel < test->inputLabel)
            return -1;
        else if (outputLabel > test->outputLabel)
            return 1;
        else if (outputLabel < test->outputLabel)
            return -1;
        else if (toId > test->toId)
            return 1;
        else if (toId < test->toId)
            return -1;
        else
            return 0;
    }

    int CompareWithContext (NUANArc *test)
    {
        if (fromId > test->fromId)
            return 1;
        else if (fromId < test->fromId)
            return -1;
        else if (toId > test->toId)
            return 1;
        else if (toId < test->toId)
            return -1;
        else if (inputLabel > test->inputLabel)
            return 1;
        else if (inputLabel < test->inputLabel)
            return -1;
        else if (outputLabel > test->outputLabel)
            return 1;
        else if (outputLabel < test->outputLabel)
            return -1;
        else if (left > test->left)
            return 1;
        else if (left < test->left)
            return -1;
        else if (right > test->right)
            return 1;
        else if (right < test->right)
            return -1;
        else
            return 0;
    }

    bool IsSame (NUANArc *test)
    {
        if (inputLabel == test->inputLabel && outputLabel == test->outputLabel && fromId == test->fromId && toId == test->toId)
            return true;
        else
            return false;
    };

    bool HasSameLabels (NUANArc *test)
    {
        if (inputLabel == test->inputLabel && outputLabel == test->outputLabel)
            return true;
        else
            return false;
    };

    bool HasSameLabelsAndTo (NUANArc *test)
    {
        if (inputLabel == test->inputLabel && outputLabel == test->outputLabel && toId == test->toId)
            return true;
        else
            return false;
    };

    bool HasSameLabelsAndFrom (NUANArc *test)
    {
        if (inputLabel == test->inputLabel && outputLabel == test->outputLabel && fromId == test->fromId)
            return true;
        else
            return false;
    };

    /* Print
    */
    void Print()
    {
        printf ("%d %d %d %d (%d)\n", fromId, toId, inputLabel, outputLabel, centre);
        return;
    };
    void PrintText()
    {
        printf ("%d %d %c %d (%d)\n", fromId, toId, inputLabel, outputLabel, centre);
        return;
    };

    void Dump (GRXMLDoc &p_Doc );


protected:
    int     inputLabel;     /*  input label */
    int     outputLabel;    /*  output label */
    int     fromId;         /*  from node */
    int     toId;           /*  to node */
    int     centre;         /*  left context  */
    int     left;           /*  left context  */
    int     right;          /*  right context */
};

#endif // __netw_arc_h__
