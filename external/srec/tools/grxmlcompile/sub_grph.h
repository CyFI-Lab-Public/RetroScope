/* FILE:		sub_grph.h
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

#ifndef __sub_grph_h__
#define __sub_grph_h__

#include "netw_arc.h"

#include "vocab.h"

//  TODO:
//  Guards various stages 1.  Compiling  2.  Processing

#define SCOPE_ROOT         0
#define SCOPE_ITEM        -1
#define SCOPE_RULE        -2
#define SCOPE_ONEOF       -3
#define SCOPE_COUNT       -4
#define SCOPE_OPT         -5
#define SCOPE_REPEAT      -6

#define NONE_LABEL        -100000           //  Null transition
#define TAG_LABEL         -100001           //  Tag transition
#define WB_LABEL          -100002           //  Word boundary transition
#define TERMINAL_LABEL    -100003           //  Terminal transition
#define BEGINSCOPE_LABEL  -100004           //  Start of scope
#define ENDSCOPE_LABEL    -100005           //  End of scope
#define BEGINRULE_LABEL   -100006           //  Start of rule
#define ENDRULE_LABEL     -100007           //  End of rule
#define DISCARD_LABEL     -100010           //  Discard (internal)
#define INITIAL_LABEL     -100011           //  Initial silence
#define FINAL_LABEL       -100012           //  Final silence

#define MAXNUM             100000           //  A large number
#define BLKSIZE             10000           //  Buffer size
#define MAXITS                  5           //  Maximum equivalence reduction iterations

// #define MAX(X,Y)        ((X) > (Y) ? (X) : (Y))
// #define MIN(X,Y)        ((X) > (Y) ? (X) : (Y))

// General functions
bool IsSlot (std::string label);


class DetCache {
    public:

        DetCache () { num= 0; dataPack= 0; };

        void AddEntry (int comb, int first, int second) {
            if (num == 0)
                dataPack= new int [3*BLKSIZE];
            else if ((num%BLKSIZE) == 0) {
                int *newPack = new int [num+3*BLKSIZE];
                for (int ii= 0; ii < (3*num); ii++)
                    newPack[ii]= dataPack[ii];
                delete [] dataPack;
                dataPack= newPack;
            }
            dataPack[3*num]= first;
            dataPack[3*num+1]= second;
            dataPack[3*num+2]= comb;
	    num++;
            return;
        };

        int QueryEntry (int first, int second) {
            if (!dataPack)
                return -1;
            for (int ii= 0; ii < num; ii++)
                if (dataPack[3*ii] == first && dataPack[3*ii+1] == second)
                    return (dataPack[3*ii+2]);
            return -1;
        }

        ~DetCache () { if (dataPack) delete [] dataPack; };

    private:
        int num;
        int *dataPack;
};


class SubGraph
{
public:

    SubGraph (char *name, int ruleNo)
    {
        int count= strlen(name);
        title= new char [count+1];
        strcpy (title, name);
	// printf ("New Rule %s\n", title);
        ruleId= ruleNo;
        popOp= 0;
        numArc= 0;
        numVertex= 0;
        lastScope= SCOPE_ROOT;
        startId= NewVertexId();
        lastId= startId;
        endId= lastId;
        forwardList= 0;
        backwardList= 0;
        sortNum= 0;
        vertexProp= 0;
        sizeProp= 0;
        return;
    };

    SubGraph (SubGraph *baseG);

    ~SubGraph ()
    {
        delete [] title;
        for (int ii= 0; ii < numArc; ii++)
            delete arc[ii];
        if (numArc >0)
            delete [] arc;
        if (forwardList)
            delete [] forwardList;
        if (backwardList)
            delete [] backwardList;
        return;
    }

    void getName (char *name, int maxlen)
    {
        strncpy (name, title, maxlen);
    }

    int getRuleId () {
        return ruleId;
    }

    /**  Creates a copy of the graph */
    void CopyFastArcs (SubGraph *subG, int startLoc, int endLoc, int offset, int headId, int newHeadId, int tailId, int newTailId);

    /**  Graph construction routines */
    void BeginScope (int scopeType, int arg1, int arg2);
    void EndScope ();
    int  AddItem (int inputLabel, int tagLabel);
    int  AddTag (int tagLabel);
    void ExpandRules (SubGraph **subList, int *lookupList, int numSubs);

    /**  Reverse subgraph */
    void ReverseGraphForOutput ();
    void SortLanguage ();
    void SortLanguageForMin ();
    void SortLanguageReverse ();
    void UpdateSortForLanguage ();

    void RemoveRuleConnections ();
    void RemoveInternalConnections ();
    void RemoveUnreachedConnections (int startPoint, int endPoint);
    void RemoveUnreachedConnectionsDebug (int startPoint, int endPoint);
    void RemoveTagConnections (int startPoint, int endPoint);
    void AddTerminalConnections ();

    void Print ();
    void PrintText ();
    void PrintWithLabels( GRXMLDoc &p_Doc );
    void RemapForSortedOutput ( GRXMLDoc & doc );

    void WriteForwardGraphWithSemantic ( std::string & fileName, GRXMLDoc &p_Doc );
    void WriteForwardGraphFile ( std::string & fileName, GRXMLDoc &p_Doc );
    void WriteFile ( std::string & fileName, GRXMLDoc &p_Doc );
    void WritePhonemeGraphFile( std::string & fileName, GRXMLDoc &p_Doc );
    void WriteHMMGraphFile( std::string & fileName, GRXMLDoc &p_Doc );

    void DeterminizeArcs ();
    int IsDeterminized (int currId);
    void ReduceArcsByEquivalence ();

    void ExpandPhonemes ( GRXMLDoc & doc );
    void AddLeftContexts ();
    void AddRightContexts ();
    void ExpandToHMMs ( GRXMLDoc & doc );
    void ExpandIntraWordSilence ( GRXMLDoc & doc );
    void ClearOutputs ();
    void ShiftOutputsToLeft ();
    void FinalProcessing ( GRXMLDoc & doc );

    void DebugPrintDirective (char *dirrData);
    void DebugPrintLabel (int labId);

private:

    int        NewVertexId ();
    int        numVertex;

    void       AllocateSpaceForArc ();
    NUANArc        *CreateArc (int iLabel, int oLabel, int from, int to);
    NUANArc        *InheritArc (NUANArc *arcBase, int id);
    NUANArc        *InheritReverseArc (NUANArc *arcBase, int id);
    NUANArc        *CreateCopyWithOutput (NUANArc *arcBase, int id);
    NUANArc        *InheritReverseArcWithTag (NUANArc *arcBase, int id, int tagId);
    int        numArc;
    NUANArc        **arc;
    int        sortNum;
    int        sortRevNum;
    int        *forwardList;
    int        *backwardList;

    int  ConnectLastScope (int beginScopeId, int endScopeId);
    int  CloseScope ();
    void UpdateVertexCount (int startLoc);

    int FindFromIndex (int element);
    int FindToIndex (int element);
    void PullUpBegins (int currId, int baseId, int endId, int procLabel, int *nodeList, int currNum, int maxNum);
    void ProcessBegins (int currId, int endId, int procLabel, int *nodeList, int currNum, int *visitMark, int maxNum);
    void PullUpEnds (int currId, int baseId, int initialId, int procLabel, int *nodeList, int currNum, int maxNum);
    void ProcessEnds (int currId, int initialId, int procLabel, int *nodeList, int currNum, int *visitMark, int maxNum);
    bool PullUpTags (int currId, int baseId, int initialId, int outTag, int *nodeList, int currNum, int maxNum);
    void ProcessTags (int currId, int initialId, int *nodeList, int currNum, int *visitMark, int maxNum);
    void ClearDuplicateArcs ();

    void RemoveRuleStarts (int startId, int endId);
    void RemoveRuleEnds (int startId, int endId);
    void RemoveNulls (int startPoint, int endPoint);
    void RemoveForwardConnections (int startId, int endId);
    void RemoveBackwardConnections (int startId, int endId);
    void ClearLabelledConnections (int labItem);
    void RemoveUnvisitedArcs (int initialId, int finalId);
    void RemoveMarkedNodes (int *forwardDepth, int *reverseDepth);
    void RemoveDiscardedArcs ();
    void RenumberNodes ();

    bool EquivalenceTestForward (int firstId, int secondId, int *equivMap);
    void ForwardDepthData (int startId, int *depthMap, int depth);
    void ReverseDepthData (int startId, int *depthMap, int depth);
    void ReverseDepthOnTerminal (int *depthMap);

    void MapGraphVertices (int *equivMap);
    void IdentifyEquivalence (int *depthMap, int *equivMap);
    void CheckForChangeAndResort (int currId, int *mapList);

    void SortLanguageAtIndices (int begIx, int endIx);
    void SortLanguageAtIndicesForMin (int begIx, int endIx);
    void SortLanguageAtSortIndices (int begIx, int endIx);
    bool CheckSort ();
    bool CheckSortReverse ();
    void RemoveDuplicatesAtNode (int rixBegin, int rixEnd);
    void MergeVertices (int newId, int firstId, int secondId);
    void DeterminizeAtVertex (int baseId, DetCache *cache);
    int  PairwiseDeterminize (int baseId, int firstId, int fixStart, int secondId, int sixStart, DetCache *cache);
    void ClearRuleIds ();
    void ViewNode (int currId);

    void ReverseMarkArcs ();
    void ReverseMarkOutput (int currId, int initialId, int outId);
    void MarkNodesByOutputAndClearArcs ();
    void ExpandWordBoundaries ( GRXMLDoc & doc );
    void AddInitialFinalSilences ( GRXMLDoc & doc );

    void UpdateVisitationCache (int startNo);
    void ClearVisitationCache ();
    void SetupVisitationCache ();
    void DecVisitationCache (int currId);
    void IncVisitationCache (int currId);
    int VisitationConsistencyCheck ();

    void CreateNodeProperty () {
        vertexProp= new int [BLKSIZE];
        vertexMinned= new bool [BLKSIZE];
        sizeProp= BLKSIZE;
        for (int ii= 0; ii < sizeProp; ii++) {
            vertexProp[ii]= 0;
            vertexMinned[ii]= false;
	}
        return;
    }

    void IncNodeProperty (int vertNo) {
        if (vertNo >= sizeProp) {
            int newSize= (vertNo/BLKSIZE + 1)*BLKSIZE;
            int *newPack = new int [newSize];
            bool *newMinned = new bool [newSize];
            int ii;
            for (ii= 0; ii < sizeProp; ii++) {
                newPack[ii]= vertexProp[ii];
                newMinned[ii]= vertexMinned[ii];
	    }
            for (ii= sizeProp; ii < newSize; ii++) {
                newPack[ii]= 0;
                newMinned[ii]= false;
	    }
            delete [] vertexProp;
            delete [] vertexMinned;
            vertexProp= newPack;
            vertexMinned= newMinned;
	    sizeProp= newSize;
        }
        vertexProp[vertNo]++;
        return;
    }

    void DecNodeProperty (int vertNo) {
        if (vertNo < sizeProp)
            vertexProp[vertNo]--;
        return;
    }

    void SetNodeProperty (int vertNo, int value) {
        if (vertNo < sizeProp)
            vertexProp[vertNo]= value;
        return;
    }

    void SetMinProperty (int vertNo) {
        if (vertNo < sizeProp)
            vertexMinned[vertNo]= true;
        return;
    }

    int QueryNodeProperty (int vertNo) {
        if (vertNo < sizeProp)
            return (vertexProp[vertNo]);
        return -1;
    }

    int QueryMinProperty (int vertNo) {
        if (vertNo < sizeProp)
            return (vertexMinned[vertNo]);
        return false;
    }

    void DeleteNodeProperty () {
        delete [] vertexProp;
        delete [] vertexMinned;
        vertexProp= 0;
        vertexMinned= 0;
    }


    /*  Stacking of operations and related data
    */
    char    *title;
    int     ruleId;
    int     lastScope;
    int     startId;
    int     endId;
    int     prevStartId;
    int     prevEndId;
    int     lastId;
    int     arg1;
    int     arg2;
    int     arcLoc;
    int     opStack[100000];      // TODO: remove hard coded number
    int     popOp;
    void    pushScope ();
    void    popScope ();
    int     silenceId;
    int     intraId;
    int     *vertexProp;          //  Vertex properties
    bool    *vertexMinned;          //  Vertex properties
    int     sizeProp;
};

#endif // __sub_grph_h__
