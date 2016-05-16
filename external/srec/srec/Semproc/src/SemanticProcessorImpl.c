/*---------------------------------------------------------------------------*
 *  SemanticProcessorImpl.c                                                  *
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

#include "SR_SemanticProcessor.h"
#include "SR_SemanticProcessorImpl.h"
#include "SR_SemanticGraphImpl.h"
#include "SR_SemanticResultImpl.h"
#include "ESR_ReturnCode.h"
#include "plog.h"
static const char* MTAG = __FILE__;

/**************************************************

    Internal data structures and functions

  ************************************************/

/**
 * A partial path holds olables from a start arc, until it reaches a
 * fork (i.e. multiple next arc possiblities). For each possibility
 * a new partial path is created and concatenated to this one.
 */
typedef struct sem_partial_path_t
{
  struct sem_partial_path_t* next; /* linked list */
  arc_token* arc_for_pp;           /* which arc was taken */
}
sem_partial_path;

#define DEBUG_CPF 0
#if DEBUG_CPF
static arc_token* debug_base_arc_token = 0;
static int debug_depth = 0;
static const char* spaces(int n) {
  const char* sp = "                                                         ";
  int nsp = strlen(sp);
  if (n > nsp) n = nsp;
  return sp + nsp - n;
}
#endif

/**
 * A holder for accumulated scripts
 */
typedef struct script_t
{
  const LCHAR* expression;
  const LCHAR* ruleName;
}
script;

/**
 * A list of accumulated scripts
 */
typedef struct script_list_t
{
  script list[MAX_SCRIPTS];
  size_t num_scripts;
}
script_list;

static const LCHAR* WORD_NOT_FOUND = L("word_not_found");

/**
 * Initialize the list of partial paths
 */
static ESR_ReturnCode sem_partial_path_list_init(sem_partial_path* heap, int nheap);
static sem_partial_path* sem_partial_path_create(sem_partial_path* heap);
static ESR_ReturnCode sem_partial_path_free(sem_partial_path* heap, sem_partial_path* path);
static void sem_partial_path_print(sem_partial_path* path, sem_partial_path* paths, int npaths, wordmap* ilabels);

/**
 * Look up the word string given the id
 */
static const LCHAR* lookUpWord(SR_SemanticGraphImpl* semgraph, wordID wdid);

/**
 * Look up the actual script string given the label
 */
static const LCHAR* lookUpScript(SR_SemanticGraphImpl* semgraph, const LCHAR* script_label);

/**
 * Recursively accumulate the scripts
 */
static ESR_ReturnCode accumulate_scripts(SR_SemanticGraphImpl* semgraph, script_list* scripts, sem_partial_path* path_root);

static ESR_ReturnCode interpretScripts(SR_SemanticProcessorImpl* semproc, LCHAR* scripts, SR_SemanticResult** result);


ESR_ReturnCode SR_SemanticProcessorCreate(SR_SemanticProcessor** self)
{
    SR_SemanticProcessorImpl* impl;
    ESR_ReturnCode rc;

    if (self == NULL)
    {
        PLogError(L("ESR_INVALID_ARGUMENT"));
        return ESR_INVALID_ARGUMENT;
    }
    impl = NEW(SR_SemanticProcessorImpl, MTAG);
    if (impl == NULL)
    {
        PLogError(L("ESR_OUT_OF_MEMORY"));
        return ESR_OUT_OF_MEMORY;
    }
    if ((rc = LA_Init(&impl->analyzer)) != ESR_SUCCESS)
        goto CLEANUP;
    if ((rc = EP_Init(&impl->parser)) != ESR_SUCCESS)
        goto CLEANUP;
    if ((rc = ST_Init(&impl->symtable)) != ESR_SUCCESS)
        goto CLEANUP;
    if ((rc = EE_Init(&impl->eval)) != ESR_SUCCESS)
        goto CLEANUP;
    impl->acc_scripts = MALLOC(sizeof(LCHAR) * MAX_SCRIPT_LEN, NULL);
    if (impl->acc_scripts == NULL)
    {
        rc = ESR_OUT_OF_MEMORY;
        PLogError(ESR_rc2str(rc));
        goto CLEANUP;
    }

    impl->Interface.destroy = &SR_SemanticProcessor_Destroy;
    impl->Interface.checkParse = &SR_SemanticProcessor_CheckParse;
    impl->Interface.checkParseByWordID = &SR_SemanticProcessor_CheckParseByWordID;
    impl->Interface.setParam = &SR_SemanticProcessor_SetParam;
    impl->Interface.flush = &SR_SemanticProcessor_Flush;


    *self = (SR_SemanticProcessor*) impl;
    return ESR_SUCCESS;
CLEANUP:
    impl->Interface.destroy(&impl->Interface);
    return rc;
}

ESR_ReturnCode SR_SemanticProcessor_Destroy(SR_SemanticProcessor* self)
{
    SR_SemanticProcessorImpl* impl = (SR_SemanticProcessorImpl*) self;

    if (self == NULL)
    {
        PLogError(L("ESR_INVALID_ARGUMENT"));
        return ESR_INVALID_ARGUMENT;
    }

    LA_Free(impl->analyzer);
    EP_Free(impl->parser);
    ST_Free(impl->symtable);
    EE_Free(impl->eval);
    if (impl->acc_scripts != NULL)
    {
        FREE(impl->acc_scripts);
        impl->acc_scripts = NULL;
    }
    FREE(impl);

    return ESR_SUCCESS;
}


ESR_ReturnCode append_with_check(LCHAR** dst, const LCHAR src, const LCHAR* end)
{
    if (*dst < end)
    {
        **dst = src;
        ++(*dst);
        return ESR_SUCCESS;
    }
    PLogError(L("ESR_BUFFER_OVERFLOW"));
    return ESR_BUFFER_OVERFLOW;
}

static const LCHAR* LSTRNCHR2(const LCHAR* text, LCHAR c, LCHAR c2, size_t len)
{
    for (; *text != c && *text != c2 && len > 0 && *text; text++, len--)
        ;
    if (len) return text;
    else return NULL;
}

static size_t get_next_token_len(const char* expr)
{
    const char *p;

    if (IS_OPERATOR(expr))
    {
        return 1;
    }
    else if (*expr == ';')
    {
        return 1;
    }
    else if (*expr == '\'')
    {
        /* a literal */
        for (p = expr; *p != '\0'; p++)
        {
            if (*p == '\\' && *(p + 1) == '\'')
            {
                ++p;
                continue;
            }
            if (p > expr && *p == '\'')
            {
                ++p;
                break;
            }
        }
        return p -expr;
    }
    else
    {
        for (p = expr; *p != '\0'; p++)
        {
            if (*p == '(')
            {
                ++p;
                break;
            }
            else if (IS_OPERATOR(p) || *p == ';')
            {
                break;
            }
        }
        return p -expr;
    }
}

#define firstWord(transcription) transcription
#define nextWord(transcription)  (transcription && *transcription ? &transcription[LSTRLEN(transcription)+1] : transcription)
/* assumption is that transcription has been prepared (word split by NULL,
   transcription ends with double NULL */

static ESR_ReturnCode checkpath_forwardByWordID(SR_SemanticGraphImpl* semgraph,
        sem_partial_path* heap,
        arc_token* atoken_start,
        sem_partial_path *pp,
        const wordID* wordIDs)
{
    arc_token* atok_use;
    sem_partial_path* pp_branch;
    arc_token* atok;
    const wordID* currentWord = wordIDs;

    /*****************
     * Recursive Part (operate on the next arc or the branch)
     *****************/
    for (atok = atoken_start; atok; atok = ARC_TOKEN_PTR(semgraph->arc_token_list, atok->next_token_index))
    {
#if DEBUG_CPF
        printf("%strying arc %d %p ilabel%d(%s) olabel %d\n", spaces(debug_depth), atok-debug_base_arc_token, atok,
               atok->ilabel, atok->ilabel!=MAXwordID?semgraph->ilabels->words[atok->ilabel]:"max",   atok->olabel);
#endif
        atok_use = NULL;
        currentWord = wordIDs;

        if (atok->ilabel < semgraph->ilabels->num_slots && atok->ilabel != WORD_EPSILON_LABEL &&
                wordmap_whether_in_rule(semgraph->ilabels, *currentWord, atok->ilabel))
        {
            /* atok->ilabel is the slotid */
            atok_use = arc_tokens_find_ilabel(semgraph->arc_token_list, semgraph->arcs_for_slot[atok->ilabel], *currentWord);
            if (!atok_use)
            {
                arc_token* a;
                PLogError(L("ESR_INVALID_STATE: finding wdid %d in slot %d"), *currentWord, atok->ilabel);
                for (a = semgraph->arcs_for_slot[atok->ilabel]; 0 && a; a = ARC_TOKEN_PTR(semgraph->arc_token_list, a->next_token_index))
                {
                    PLogError(L("a %x ilabel %d olabel %d"), a, a->ilabel, a->olabel);
                }
                return ESR_INVALID_STATE;
            }
            else
                ++currentWord;
        }
        else if (*currentWord != MAXwordID && atok->ilabel == *currentWord)
        {
            ++currentWord;
            atok_use = atok;
        }
        else if (atok->ilabel == WORD_EPSILON_LABEL) /* more eps transitions */
            atok_use = atok;

        if (atok_use == NULL)
            continue;
        else {
            arc_token* atokfna = ARC_TOKEN_PTR(semgraph->arc_token_list, atok->first_next_arc);
            pp_branch = sem_partial_path_create(heap);

#if DEBUG_CPF
            printf("%smatched arc %d %p ilabel%d(%s) olabel %d\n", spaces(debug_depth), atok-debug_base_arc_token, atok,
                   atok->ilabel, semgraph->ilabels->words[atok->ilabel],   atok->olabel);
#endif

            if (!pp_branch)
                return ESR_INVALID_STATE;
            pp->next = pp_branch;
            pp->arc_for_pp = atok_use;

            if (atok->first_next_arc == ARC_TOKEN_NULL && *currentWord == MAXwordID)
                return ESR_SUCCESS;
            else if (atokfna && atokfna->ilabel==MAXwordID && atokfna->olabel==MAXwordID && *currentWord==MAXwordID)
                return ESR_SUCCESS;
            else
            {
#if DEBUG_CPF
                sem_partial_path_print(pp_branch, &sem_partial_paths[0], MAX_SEM_PARTIAL_PATHS, semgraph->ilabels);
                debug_depth += 2;
#endif
                ESR_ReturnCode rc = checkpath_forwardByWordID(semgraph, heap, atokfna, pp_branch, currentWord);
#if DEBUG_CPF
                debug_depth -= 2;
#endif
                if (rc == ESR_SUCCESS)
                    return ESR_SUCCESS;
                else if (rc == ESR_INVALID_STATE)
                {
                    /* if out-of-memory of other problem, then just abort */
                    return ESR_INVALID_STATE;
                }
                else
                {
                    /* need to uncharge through epsilons, until pp->next==pp_branch */
                    // sem_partial_path* qq = pp->next;
                    sem_partial_path_free(heap, pp->next);
                    pp->arc_for_pp = NULL;
                    // for (qq = pp->next; qq != pp_branch; qq = qq->next)  sem_partial_path_free(qq);
                    pp->next = NULL;
                }
            }
        }
#if DEBUG_CPF
        printf("%sdone trying arc %d %p ilabel%d(%s) olabel %d\n", spaces(debug_depth), atok-debug_base_arc_token, atok,
               atok->ilabel, semgraph->ilabels->words[atok->ilabel],   atok->olabel);
#endif
    } /* end for atok .. */
    return ESR_NO_MATCH_ERROR;
}


static ESR_ReturnCode checkpath_forward(SR_SemanticGraphImpl* semgraph,
        sem_partial_path* heap,
        arc_token* atoken_start,
        sem_partial_path *pp,
        const LCHAR* transcription)
{
    arc_token* atok_use;
    wordID wdID;
    sem_partial_path* pp_branch;
    arc_token* atok;
    const LCHAR* transp;

    /*****************/
    /* Recursive Part (operate on the next arc or the branch)*/
    /*****************/
    for (atok = atoken_start; atok; atok = ARC_TOKEN_PTR(semgraph->arc_token_list, atok->next_token_index))
    {
#if DEBUG_CPF
        printf("%strying arc %d %p ilabel%d(%s) olabel %d\n", spaces(debug_depth), atok-debug_base_arc_token, atok,
               atok->ilabel, atok->ilabel!=MAXwordID?semgraph->ilabels->words[atok->ilabel]:"max",   atok->olabel);
#endif

        atok_use = NULL;
        transp = transcription;
        wdID = wordmap_find_index(semgraph->ilabels, firstWord(transp));

        if (atok->ilabel < semgraph->ilabels->num_slots && atok->ilabel != WORD_EPSILON_LABEL &&
                wordmap_whether_in_rule(semgraph->ilabels, wdID, atok->ilabel))
        {
            /* atok->ilabel is the slotid */
            atok_use = arc_tokens_find_ilabel(semgraph->arc_token_list, semgraph->arcs_for_slot[atok->ilabel], wdID);
            if (!atok_use)
            {
                arc_token* a;
                PLogError(L("ESR_INVALID_STATE: finding wdid %d in slot %d"), wdID, atok->ilabel);
                for (a = semgraph->arcs_for_slot[atok->ilabel]; 0 && a; a = ARC_TOKEN_PTR(semgraph->arc_token_list, a->next_token_index))
                {
                    PLogError(L("a %x ilabel %d olabel %d"), a, a->ilabel, a->olabel);
                }
                return ESR_INVALID_STATE;
            }
            else {
                transp = nextWord(transp);
                wdID = wordmap_find_index(semgraph->ilabels, firstWord(transp));
            }
        }
        else if (wdID != MAXwordID && atok->ilabel == wdID)
        {
            transp = nextWord(transp);
            wdID = wordmap_find_index(semgraph->ilabels, firstWord(transp));
            atok_use = atok;
        }
        else if (atok->ilabel == WORD_EPSILON_LABEL) /* more eps transitions */
            atok_use = atok;

        if (atok_use == NULL)
            continue;
        else {
            arc_token* atokfna = ARC_TOKEN_PTR(semgraph->arc_token_list, atok->first_next_arc);
            pp_branch = sem_partial_path_create(heap);

#if DEBUG_CPF
            printf("%smatched arc %d %p ilabel%d(%s) olabel %d\n", spaces(debug_depth), atok-debug_base_arc_token, atok,
                   atok->ilabel, semgraph->ilabels->words[atok->ilabel],   atok->olabel);
#endif

            if (!pp_branch)
                return ESR_INVALID_STATE;
            pp->next = pp_branch;
            pp->arc_for_pp = atok_use;
            if (atok->first_next_arc==ARC_TOKEN_NULL && *transp==0)
                return ESR_SUCCESS;
            else if (atokfna && atokfna->ilabel==MAXwordID && atokfna->olabel==MAXwordID && *transp==0)
                return ESR_SUCCESS;
            else
            {
#if DEBUG_CPF
                sem_partial_path_print(pp_branch, &sem_partial_paths[0], MAX_SEM_PARTIAL_PATHS, semgraph->ilabels);
                debug_depth += 2;
#endif
                ESR_ReturnCode rc = checkpath_forward(semgraph, heap, atokfna, pp_branch, transp);
#if DEBUG_CPF
                debug_depth -= 2;
#endif
                if (rc == ESR_SUCCESS)
                    return rc;
                else if (rc == ESR_INVALID_STATE)
                {
                    /* if out-of-memory of other problem, then just abort */
                    return ESR_INVALID_STATE;
                }
                else
                {
                    /* need to uncharge through epsilons, until pp->next==pp_branch */
                    // sem_partial_path* qq = pp->next;
                    sem_partial_path_free(heap, pp->next);
                    pp->arc_for_pp = NULL;
                    // for (qq = pp->next; qq != pp_branch; qq = qq->next)  sem_partial_path_free(qq);
                    pp->next = NULL;
                }
            }
        }
#if DEBUG_CPF
        printf("%sdone trying arc %d %p ilabel%d(%s) olabel %d\n", spaces(debug_depth), atok-debug_base_arc_token, atok,
               atok->ilabel, semgraph->ilabels->words[atok->ilabel],   atok->olabel);
#endif
    } /* end for atok .. */
    return ESR_NO_MATCH_ERROR;
}

/**
 * Parse the graph
 */
ESR_ReturnCode SR_SemanticProcessor_CheckParseByWordID(SR_SemanticProcessor* self,
                                                       SR_SemanticGraph* graph,
                                                       wordID* wordIDs,
                                                       SR_SemanticResult** results,
                                                       size_t* resultCount)
{
    sem_partial_path *path_root;
    script_list raw_scripts_buf;
    LCHAR lhs[MAX_STRING_LEN];
    LCHAR meaning[MAX_STRING_LEN];      /* special key */
    LCHAR ruleName[32];
    size_t i, j, size, resultIdx;
    LCHAR* dst = NULL;
    LCHAR* p;
    size_t tokenLen = 0;
    const LCHAR* src;
    HashMap* hashmap = NULL;
    ESR_ReturnCode rc;
    ESR_BOOL containsKey;
    sem_partial_path heap[MAX_SEM_PARTIAL_PATHS];
    SR_SemanticProcessorImpl* semproc  = (SR_SemanticProcessorImpl*) self;
    SR_SemanticGraphImpl* semgraph = (SR_SemanticGraphImpl*) graph;

    LSTRCPY(ruleName, L(""));
    CHKLOG(rc, sem_partial_path_list_init(heap, sizeof(heap)/sizeof(heap[0])));
    path_root = sem_partial_path_create(heap);
    if (!path_root)
    {
        rc = ESR_INVALID_STATE;
        goto CLEANUP;
    }

    /**
     * Parse the graph
     */
    rc = checkpath_forwardByWordID(semgraph, heap, &semgraph->arc_token_list[0], path_root,
                                   wordIDs);
    if (rc == ESR_NO_MATCH_ERROR)
    {
        *resultCount = 0;
        return ESR_SUCCESS; /* did not parse */
    }
    else if (rc == ESR_SUCCESS)
    {
        if (*resultCount > 0)
            *resultCount = 1;
        else
        {
            /**
             * If the array to hold the results is not big enough,
             * then tell the user right away by returning ESR_BUFFER_OVERFLOW
       with the size required returned in resultCount */
            rc = ESR_BUFFER_OVERFLOW;
            PLogError(ESR_rc2str(rc));
            goto CLEANUP;
        }
    }
    else if (rc == ESR_INVALID_STATE)
        goto CLEANUP;

#if DEBUG_CPF
    sem_partial_path_print(path_root, &sem_partial_paths[0], MAX_SEM_PARTIAL_PATHS,semgraph->ilabels);
#endif

    /* create the array of Semantic Result Pointers */
    for (resultIdx = 0; resultIdx < *resultCount; resultIdx++)
    {
        raw_scripts_buf.num_scripts = 0;
        for (i = 0; i < MAX_SCRIPTS; i++)
        {
            raw_scripts_buf.list[i].expression = 0;
            raw_scripts_buf.list[i].ruleName = 0;
        }

        /*
         * Go through the partial paths which were successful and accumulate the scripts
         * that you encountered (BUGGY)
         */
        CHKLOG(rc, accumulate_scripts(semgraph, &raw_scripts_buf, path_root));
        CHKLOG(rc, sem_partial_path_free(heap, path_root));

        /*pfprintf(PSTDOUT,"Accumulated scripts\n");*/

        /*
         * Prepare the scripts for processing, in other words, make them "nice".
         * What I mean by making them nice is to do stuff like:
         *
         * if ruleName is:   root}
         *    expression is: meaning='hello';meaning=meaning+' '+'world';
         *
         * what I want to accumulate is
         *    root.meaning='hello';root.meaning=root.meaning+' '+'world';
         *
         * I am basically replacing END_SCOPE_MARKER with '.'  and inserting 'root.'
         * before every lhs identifier.
         *
         */
        for (dst = &semproc->acc_scripts[0], semproc->acc_scripts[0] = '\0', i = 0; i < raw_scripts_buf.num_scripts; ++i)
        {
            if (raw_scripts_buf.list[i].ruleName && raw_scripts_buf.list[i].expression &&
                    raw_scripts_buf.list[i].ruleName != WORD_NOT_FOUND &&
                    raw_scripts_buf.list[i].expression != WORD_NOT_FOUND)
            {
                if (!LSTRCMP(raw_scripts_buf.list[i].expression, L(";")))
                    continue;
                /* set the rule name in a temporary buffer and in the dst */
                src = raw_scripts_buf.list[i].ruleName;
                p = ruleName;
                while (*src && *src != END_SCOPE_MARKER) /* trim off the trailing closing brace END_SCOPE_MARKER */
                {
                    CHKLOG(rc, append_with_check(&dst, *src, &semproc->acc_scripts[MAX_SCRIPT_LEN-1]));
                    CHKLOG(rc, append_with_check(&p, *src, &ruleName[31]));
                    ++src;
                }


                /* put a dot after the rule name, and before the lhs */
                CHKLOG(rc, append_with_check(&dst, L('.'), &semproc->acc_scripts[MAX_SCRIPT_LEN-1]));
                CHKLOG(rc, append_with_check(&p, L('.'), &ruleName[31]));

                /* terminate the ruleName string */
                CHKLOG(rc, append_with_check(&p, 0, &ruleName[31]));

                /* append the rest of the expression */
                src = raw_scripts_buf.list[i].expression;

                while (ESR_TRUE)
                {
                    /* get the LHS identifier, append to dst, and store temporarily
            in lhs buffer*/
                    p = lhs;
                    while (*src && *src != '=')
                    {
                        CHKLOG(rc, append_with_check(&dst, *src, &semproc->acc_scripts[MAX_SCRIPT_LEN-1]));
                        CHKLOG(rc, append_with_check(&p, *src, &lhs[MAX_STRING_LEN-1]));
                        ++src;
                    }
                    /* terminate the lhs string */
                    CHKLOG(rc, append_with_check(&p, 0, &lhs[MAX_STRING_LEN-1]));

                    /* prepend every occurrence of the LHS identifier with 'ruleName.'*/
                    for (; *src && *src != ';'; src += tokenLen)
                    {
                        const LCHAR* p2;

                        tokenLen = get_next_token_len(src);
                        if (IS_LOCAL_IDENTIFIER(src, tokenLen)  /* || !LSTRCMP(token, lhs) */)
                        {
                            /* use p to copy stuff now */
                            p = ruleName;
                            while (*p)
                            {
                                /* prepend the rule name to the identifier */
                                CHKLOG(rc, append_with_check(&dst, *p, &semproc->acc_scripts[MAX_SCRIPT_LEN-1]));
                                ++p;
                            }
                        }
                        for (p2 = src; p2 < src + tokenLen; ++p2)
                            CHKLOG(rc, append_with_check(&dst, *p2, &semproc->acc_scripts[MAX_SCRIPT_LEN-1]));

                    }

                    /*
                     * In an expression there may be several statements, each perhaps with a
                     * new LHS identifier
                     */

                    /* skip extra semicolons */
                    while (*src == ';')
                        ++src;
                    /* skip whitespace */
                    while (isspace(*src))
                        ++src;

                    if (!*src)
                    {
                        /* if end of the expression */
                        /* terminate the eScript expression properly */
                        CHKLOG(rc, append_with_check(&dst, L(';'), &semproc->acc_scripts[MAX_SCRIPT_LEN-1]));
                        *dst = '\0';/* terminate the string, DO NOT DO ++ !!! possibility of next loop iteration
                                       which will concatenate to the dst string */
                        break;
                    }
                    else
                    {
                        /* concat a single semi-colon */
                        CHKLOG(rc, append_with_check(&dst, L(';'), &semproc->acc_scripts[MAX_SCRIPT_LEN-1]));
                        p = ruleName;
                        while (*p)
                        {
                            /* prepend the rule name for the new statement */
                            CHKLOG(rc, append_with_check(&dst, *p, &semproc->acc_scripts[MAX_SCRIPT_LEN-1]));
                            ++p;
                        }
                    }
                }
            }
        }
        if (0) PLogMessage( L("Accumulated Scripts for:\n%s"), semproc->acc_scripts);
        if (&results[resultIdx] != NULL) /* SemanticResultImpl assumed to have been created externally */
            interpretScripts(semproc, semproc->acc_scripts, &results[resultIdx]);

        /**
         * Fill in the 'meaning', if it is not there
         *  map 'ROOT.meaning' to 'meaning'
         *
         * NOTE: I am reusing some vars even though the names are a little bit inappropriate.
         */
        hashmap = ((SR_SemanticResultImpl*)results[resultIdx])->results;

        LSTRCPY(meaning, L("meaning"));
        CHKLOG(rc, hashmap->containsKey(hashmap, meaning, &containsKey));
        if (!containsKey)
        {
            LSTRCPY(meaning, ruleName); /* the last rule name encountered is always the root */
            LSTRCAT(meaning, L("meaning"));
            CHKLOG(rc, hashmap->containsKey(hashmap, meaning, &containsKey));

            if (containsKey)
            {
                CHKLOG(rc, hashmap->get(hashmap, meaning, (void **)&p));
                /* create a new memory location to hold the meaning... not the same as the other cause
         I do not want memory destroy problems */
                /* add one more space */
                dst = MALLOC(sizeof(LCHAR) * (LSTRLEN(p) + 1), L("semproc.meaning"));
                if (dst == NULL)
                {
                    rc = ESR_OUT_OF_MEMORY;
                    PLogError(ESR_rc2str(rc));
                    goto CLEANUP;
                }
                LSTRCPY(dst, p);
                rc = hashmap->put(hashmap, L("meaning"), dst);
                if (rc != ESR_SUCCESS)
                {
                    FREE(dst);
                    PLogError(ESR_rc2str(rc));
                    goto CLEANUP;
                }
                dst = NULL;
            }
            else
            {
                /*
                 * No meaning was provided, so just concat all the values that are associated with the ROOT rule
                 * (key name begins with ROOT)
                 */
                meaning[0] = 0;
                CHKLOG(rc, hashmap->getSize(hashmap, &size));
                for (j = 0; j < size; j++)
                {
                    CHKLOG(rc, hashmap->getKeyAtIndex(hashmap, j, &p));
                    if (LSTRSTR(p, ruleName) == p) /* key name begins with root ruleName */
                    {
                        CHKLOG(rc, hashmap->get(hashmap, p, (void **)&dst));
                        if (meaning[0] != 0) /* separate vals with space */
                        {
                            if (LSTRLEN(meaning) + 1 < MAX_STRING_LEN)
                                LSTRCAT(meaning, L(" "));
                            /* chopping the meaning is harmless */
                        }
                        if (LSTRLEN(meaning) + LSTRLEN(dst) < MAX_STRING_LEN)
                        {
                            /* strcat a max of 32 chars */
                            LCHAR* p, *pp;
                            for (pp = &meaning[0]; *pp != 0; pp++) ; /* scan to the end */
                            for (p = dst; *p != 0 && p - dst < 32;) *pp++ = *p++; /* catenate up to 32 chars */
                            *pp++ = 0; /* null terminate */
                            /* LSTRCAT(meaning,dst); */
                        }
                        /* chopping the meaning is harmless */
                    }
                }
                if (meaning[0] != 0)
                {
                    dst = MALLOC(sizeof(LCHAR) * (LSTRLEN(meaning) + 1), L("semproc.meaning"));
                    if (dst == NULL)
                    {
                        rc = ESR_OUT_OF_MEMORY;
                        PLogError(ESR_rc2str(rc));
                        goto CLEANUP;
                    }
                    LSTRCPY(dst, meaning);
                    rc = hashmap->put(hashmap, L("meaning"), dst);
                    if (rc != ESR_SUCCESS)
                    {
                        FREE(dst);
                        PLogError(ESR_rc2str(rc));
                        goto CLEANUP;
                    }
                    dst = NULL;
                }
            }
        }
    }

    return ESR_SUCCESS;
CLEANUP:
    return rc;
}

/**
 * Parse the graph
 */
ESR_ReturnCode SR_SemanticProcessor_CheckParse(SR_SemanticProcessor* self,
                                               SR_SemanticGraph* graph,
                                               const LCHAR* transcription,
                                               SR_SemanticResult** results,
                                               size_t* resultCount)
{
    sem_partial_path *path_root;
    script_list raw_scripts_buf;
    LCHAR acc_scripts[MAX_SCRIPT_LEN];  /* the accumulated scripts */
    LCHAR lhs[MAX_STRING_LEN];
    LCHAR meaning[MAX_STRING_LEN];      /* special key */
    LCHAR ruleName[MAX_STRING_LEN];
    LCHAR prepared_transcription[MAX_STRING_LEN+1]; /*for final double null */
    size_t i, j, size, resultIdx;
    LCHAR* dst = NULL;
    LCHAR* p = NULL;
    size_t tokenLen = 0;
    const LCHAR* src;
    HashMap* hashmap = NULL;
    ESR_ReturnCode rc;
    ESR_BOOL containsKey;
    sem_partial_path heap[MAX_SEM_PARTIAL_PATHS];
    SR_SemanticProcessorImpl* semproc  = (SR_SemanticProcessorImpl*) self;
    SR_SemanticGraphImpl* semgraph = (SR_SemanticGraphImpl*) graph;

    LSTRCPY(ruleName, L(""));
    CHKLOG(rc, sem_partial_path_list_init(heap, sizeof(heap)/sizeof(heap[0])));
    path_root = sem_partial_path_create(heap);
    if (!path_root)
    {
        rc = ESR_INVALID_STATE;
        goto CLEANUP;
    }

    /**
     * prepare the transcription for processing
     * split words by inserting NULL
     * term by inserting double NULL at end
     */
    for (i = 0; transcription[i] && i < MAX_STRING_LEN - 2; i++)
    {
        if (transcription[i] == L(' '))
            prepared_transcription[i] = 0;
        else
            prepared_transcription[i] = transcription[i];
    }
    prepared_transcription[i] = prepared_transcription[i+1] = 0; /* double null */

    /**
     * Parse the graph
     */
#if DEBUG_CPF
    debug_base_arc_token = &semgraph->arc_token_list[0];
    debug_depth = 0;
#endif
    rc = checkpath_forward(semgraph, heap, &semgraph->arc_token_list[0], path_root, prepared_transcription);
    if (rc == ESR_NO_MATCH_ERROR)
    {
        *resultCount = 0;
        return ESR_SUCCESS; /* did not parse */
    }
    else if (rc == ESR_SUCCESS)
    {
        if (*resultCount > 0)
            *resultCount = 1;
        else
        {
            /**
             * If the array to hold the results is not big enough,
             * then tell the user right away by returning ESR_BUFFER_OVERFLOW
       with the size required returned in resultCount */
            rc = ESR_BUFFER_OVERFLOW;
            PLogError(ESR_rc2str(rc));
            goto CLEANUP;
        }
    }
    else if (rc == ESR_INVALID_STATE)
        goto CLEANUP;

    /* create the array of Semantic Result Pointers */
    for (resultIdx = 0; resultIdx < *resultCount; resultIdx++)
    {
        raw_scripts_buf.num_scripts = 0;
        for (i = 0; i < MAX_SCRIPTS; i++)
        {
            raw_scripts_buf.list[i].expression = 0;
            raw_scripts_buf.list[i].ruleName = 0;
        }

        /*
         * Go through the partial paths which were successful and accumulate the scripts
         * that you encountered (BUGGY)
         */
        CHKLOG(rc, accumulate_scripts(semgraph, &raw_scripts_buf, path_root));
        CHKLOG(rc, sem_partial_path_free(heap, path_root));

        /*pfprintf(PSTDOUT,"Accumulated scripts\n");*/

        /*
         * Prepare the scripts for processing, in other words, make them "nice".
         * What I mean by making them nice is to do stuff like:
         *
         * if ruleName is:   root}
         *    expression is: meaning='hello';meaning=meaning+' '+'world';
         *
         * what I want to accumulate is
         *    root.meaning='hello';root.meaning=root.meaning+' '+'world';
         *
         * I am basically replacing END_SCOPE_MARKER with '.'  and inserting 'root.'
         * before every lhs identifier.
         *
         */
        for (dst = &acc_scripts[0], acc_scripts[0] = '\0', i = 0; i < raw_scripts_buf.num_scripts; ++i)
        {
            if (raw_scripts_buf.list[i].ruleName && raw_scripts_buf.list[i].expression &&
                    raw_scripts_buf.list[i].ruleName != WORD_NOT_FOUND &&
                    raw_scripts_buf.list[i].expression != WORD_NOT_FOUND)
            {
                if (!LSTRCMP(raw_scripts_buf.list[i].expression, L(";")))
                    continue;
                /* set the rule name in a temporary buffer and in the dst */
                src = raw_scripts_buf.list[i].ruleName;
                p = ruleName;
                /* trim off the trailing closing brace END_SCOPE_MARKER */
                while (*src && *src != END_SCOPE_MARKER)
                {
                    CHKLOG(rc, append_with_check(&dst, *src, &acc_scripts[MAX_SCRIPT_LEN-1]));
                    CHKLOG(rc, append_with_check(&p, *src, &ruleName[MAX_STRING_LEN-1]));
                    ++src;
                }


                /* put a dot after the rule name, and before the lhs */
                CHKLOG(rc, append_with_check(&dst, L('.'), &acc_scripts[MAX_SCRIPT_LEN-1]));
                CHKLOG(rc, append_with_check(&p, L('.'), &ruleName[MAX_STRING_LEN-1]));

                /* terminate the ruleName string */
                CHKLOG(rc, append_with_check(&p, 0, &ruleName[MAX_STRING_LEN-1]));

                /* append the rest of the expression */
                src = raw_scripts_buf.list[i].expression;

                while (ESR_TRUE)
                {
                    /* get the LHS identifier, append to dst, and store temporarily in lhs buffer*/
                    p = lhs;
                    while (*src && *src != '=')
                    {
                        CHKLOG(rc, append_with_check(&dst, *src, &acc_scripts[MAX_SCRIPT_LEN-1]));
                        CHKLOG(rc, append_with_check(&p, *src, &lhs[MAX_STRING_LEN-1]));
                        ++src;
                    }
                    /* terminate the lhs string */
                    CHKLOG(rc, append_with_check(&p, 0, &lhs[MAX_STRING_LEN-1]));

                    /* prepend every occurrence of the LHS identifier with 'ruleName.'*/
                    for (; *src && *src != ';'; src += tokenLen)
                    {
                        const LCHAR* p2;

                        tokenLen = get_next_token_len(src);
                        if (IS_LOCAL_IDENTIFIER(src, tokenLen)  /* || !LSTRCMP(token, lhs) */)
                        {
                            /* use p to copy stuff now */
                            p = ruleName;
                            while (*p)
                            {
                                /* prepend the rule name to the identifier */
                                CHKLOG(rc, append_with_check(&dst, *p, &acc_scripts[MAX_SCRIPT_LEN-1]));
                                ++p;
                            }
                        }
                        for (p2 = src; p2 < src + tokenLen; ++p2)
                            CHKLOG(rc, append_with_check(&dst, *p2, &acc_scripts[MAX_SCRIPT_LEN-1]));
                    }

                    /*
                     * In an expression there may be several statements, each perhaps with a
                     * new LHS identifier
                     */

                    while (*src == ';')
                        ++src; /* skip the double triple... semi-colons*/

                    if (!*src)
                    {
                        /* if end of the expression */
                        /* terminate the eScript expression properly */
                        CHKLOG(rc, append_with_check(&dst, L(';'), &acc_scripts[MAX_SCRIPT_LEN-1]));
                        *dst = '\0';/* terminate the string, DO NOT DO ++ !!! possibility of next loop iteration
                                       which will concatenate to the dst string */
                        break;
                    }
                    else
                    {
                        /* concat a single semi-colon */
                        CHKLOG(rc, append_with_check(&dst, L(';'), &acc_scripts[MAX_SCRIPT_LEN-1]));
                        p = ruleName;
                        while (*p)
                        {
                            /* prepend the rule name for the new statement */
                            CHKLOG(rc, append_with_check(&dst, *p, &acc_scripts[MAX_SCRIPT_LEN-1]));
                            ++p;
                        }
                    }
                }
            }
        }
#if defined( SREC_ENGINE_VERBOSE_LOGGING)
        PLogMessage(L("Accumulated Scripts for (%s):\n%s"), transcription, acc_scripts);
#endif
        if (&results[resultIdx] != NULL) /* SemanticResultImpl assumed to have been created externally */
            interpretScripts(semproc, acc_scripts, &results[resultIdx]);

        /**
         * Fill in the 'meaning', if it is not there
         *  map 'ROOT.meaning' to 'meaning'
         *
         * NOTE: I am reusing some vars even though the names are a little bit inappropriate.
         */
        hashmap = ((SR_SemanticResultImpl*)results[resultIdx])->results;

        LSTRCPY(meaning, L("meaning"));
        CHKLOG(rc, hashmap->containsKey(hashmap, meaning, &containsKey));
        if (!containsKey)
        {
            LSTRCPY(meaning, ruleName); /* the last rule name encountered is always the root */
            LSTRCAT(meaning, L("meaning"));
            CHKLOG(rc, hashmap->containsKey(hashmap, meaning, &containsKey));

            if (containsKey)
            {
                CHKLOG(rc, hashmap->get(hashmap, meaning, (void **)&p));
                /* create a new memory location to hold the meaning... not the same as the other cause
         I do not want memory destroy problems */
                /* add one more space */
                dst = MALLOC(sizeof(LCHAR) * (LSTRLEN(p) + 1), L("semproc.meaning"));
                if (dst == NULL)
                {
                    rc = ESR_OUT_OF_MEMORY;
                    PLogError(ESR_rc2str(rc));
                    goto CLEANUP;
                }
                LSTRCPY(dst, p);
                CHKLOG(rc, hashmap->put(hashmap, L("meaning"), dst));
                dst = NULL;
            }
            else
                /* absolutely no meaning was provided, so just concat all the values that are associated
                 * with the ROOT rule (key name begins with ROOT) */
            {
                meaning[0] = 0;
                CHKLOG(rc, hashmap->getSize(hashmap, &size));
                for (j = 0; j < size; j++)
                {
                    CHKLOG(rc, hashmap->getKeyAtIndex(hashmap, j, &p));
                    if (LSTRSTR(p, ruleName) == p) /* key name begins with root ruleName */
                    {
                        CHKLOG(rc, hashmap->get(hashmap, p, (void **)&dst));
                        if (meaning[0] != 0) /* separate vals with space */
                            LSTRCAT(meaning, L(" "));
                        LSTRCAT(meaning, dst);
                    }
                }
                if (meaning[0] != 0)
                {
                    dst = MALLOC(sizeof(LCHAR) * (LSTRLEN(meaning) + 1), L("semproc.meaning"));
                    if (dst == NULL)
                    {
                        rc = ESR_OUT_OF_MEMORY;
                        PLogError(ESR_rc2str(rc));
                        goto CLEANUP;
                    }
                    LSTRCPY(dst, meaning);
                    CHKLOG(rc, hashmap->put(hashmap, L("meaning"), dst));
                    dst = NULL;
                }
            }
        }
    }

    return ESR_SUCCESS;
CLEANUP:
    if (dst != NULL) FREE(dst);
    return rc;
}

/**
 * After parsing, interpret the acumulated scripts
 */
static ESR_ReturnCode interpretScripts(SR_SemanticProcessorImpl* semproc,
        LCHAR* scripts, SR_SemanticResult** result)
{
    ESR_ReturnCode rc;
    SR_SemanticResultImpl** impl = (SR_SemanticResultImpl**) result;

    if ((rc = LA_Analyze(semproc->analyzer, scripts)) == ESR_SUCCESS)
    {
        /****************************
         * If all goes well, then the result
         * will be written to the HashMap provided
         ****************************/
        if ((rc = EP_parse(semproc->parser, semproc->analyzer, semproc->symtable, semproc->eval, &((*impl)->results))) != ESR_SUCCESS)
            pfprintf(PSTDOUT, "Semantic Result: Error (%s) could not interpret\n", ESR_rc2str(rc));
    }
    return rc;
}





/***************************************************************/
/* PartialPath stuff                                           */
/***************************************************************/

static ESR_ReturnCode sem_partial_path_list_init(sem_partial_path* heap, int nheap)
{
    int i;
    for (i = 0; i < MAX_SEM_PARTIAL_PATHS - 1; i++)
        heap[i].next = &heap[i+1];
    heap[i].next = 0;
    return ESR_SUCCESS;
}

static sem_partial_path* sem_partial_path_create(sem_partial_path* heap)
{
    sem_partial_path* path = heap->next;
    if (path == NULL)
    {
        /* PLogError() is dangerous here, because the stack is very deep */
        pfprintf(PSTDERR, "sem_partial_path_create() no more partial paths available (limit=%d)\n", MAX_SEM_PARTIAL_PATHS);
        return NULL;
    }

    heap->next = path->next;

    path->next = NULL;
    path->arc_for_pp = NULL;
    return path;
}

#if DEBUG_CPF
static void sem_partial_path_print(sem_partial_path* path,
        sem_partial_path* paths, int npaths, wordmap* ilabels)
{
    int i;
    sem_partial_path* frompath = 0;
    arc_token* a;

    if (!path)
    {
        printf("--- END ---\n");
        return;
    }
    printf("path %p arc %d %p ", path, (path->arc_for_pp-debug_base_arc_token),
           path->arc_for_pp);
    if ((a = path->arc_for_pp) != NULL)
    {
        printf(" ilabel %d(%s) olabel %d\n",
               a->ilabel, ilabels->words[a->ilabel],
               a->olabel);
    }
    else
    {
        printf("\n");
    }
    printf(" from ");
    for (i = 0; i < npaths; i++)
    {
        if (paths[i].next == path)
        {
            frompath = &paths[i];
            break;
        }
    }
    if (1)sem_partial_path_print(frompath, paths, npaths, ilabels);
}
#endif

static ESR_ReturnCode sem_partial_path_free(sem_partial_path* heap, sem_partial_path* path)
{
    path->next = heap->next;
    heap->next = path;
    return ESR_SUCCESS;
}

/***********************************************************************/


static const LCHAR* lookUpWord(SR_SemanticGraphImpl* semgraph, wordID wdid)
{
    int wdID = wdid;
    int mid_offset, upper_offset;
    wordmap* mid_words;
    wordmap* upper_words;

    if (wdID < 0 || wdID >= MAXwordID)
        return WORD_NOT_FOUND;

    if (semgraph->scopes_olabel_offset < semgraph->script_olabel_offset)
    {
        mid_offset   = semgraph->scopes_olabel_offset;
        mid_words    = semgraph->scopes_olabels;
        upper_offset = semgraph->script_olabel_offset;
        upper_words  = semgraph->scripts;
    }
    else
    {
        mid_offset   = semgraph->script_olabel_offset;
        mid_words    = semgraph->scripts;
        upper_offset = semgraph->scopes_olabel_offset;
        upper_words  = semgraph->scopes_olabels;
    }

    if (wdID < mid_offset && wdID < semgraph->ilabels->num_words)
    {
        return semgraph->ilabels->words[wdID];
    }
    else if (wdID >= mid_offset && wdID < upper_offset)
    {
        wdID -= mid_offset;
        if (wdID >= 0 && wdID < mid_words->num_words)
            return mid_words->words[wdID];
    }
    else if (wdID >= upper_offset && wdID < MAXwordID)
    {
        wdID -= upper_offset;
        if (wdID >= 0 && wdID < upper_words->num_words)
            return upper_words->words[wdID];
    }

    return WORD_NOT_FOUND;
}

static const LCHAR* lookUpScript(SR_SemanticGraphImpl* semgraph, const LCHAR* script_label)
{
    size_t index;

    index = atoi(&script_label[1]); /* skip the prepended '_' */

    if (index > semgraph->scripts->num_words)
        return WORD_NOT_FOUND;
    else
        return semgraph->scripts->words[index];
}

PINLINE ESR_BOOL isnum(const LCHAR* str)
{
    if (!str || !*str)
        return ESR_FALSE;

    while (*str)
    {
        if (!isdigit(*str))
            return ESR_FALSE;
        str++;
    }
    return ESR_TRUE;
}


static ESR_ReturnCode accumulate_scripts(SR_SemanticGraphImpl* semgraph,
        script_list* scripts, sem_partial_path* path)
{
    size_t scope = 0;
    arc_token* atok;
    sem_partial_path* p;
    const LCHAR* word;
    size_t j;
    ESR_ReturnCode rc;

    for (p = path; p != NULL; p = p->next)
    {
        atok = p->arc_for_pp;
        if (atok == NULL)
            continue;
        else if (atok->ilabel == WORD_EPSILON_LABEL && atok->olabel == WORD_EPSILON_LABEL)
            continue;
        else if (atok->olabel != WORD_EPSILON_LABEL)
        {
            LCHAR* _tMp;
            word = lookUpWord(semgraph, atok->olabel);

            if ( IS_BEGIN_SCOPE(word))
                ++scope;
            else if ( IS_END_SCOPE(word) )
            {
                j = scripts->num_scripts;
                do
                {
                    if (scripts->list[j].ruleName == (LCHAR*) scope) /* just an ID */
                        scripts->list[j].ruleName = word;
                    --j;
                }
                while (j != (size_t) - 1);
                if (scope > 0)
                    --scope;
                else
                {
                    rc = ESR_INVALID_STATE;
                    PLogError(L("ESR_INVALID_STATE: Tried popping scope when it was zero"));
                    goto CLEANUP;
                }
            }
            else
            {
                /* make sure it is actually a script */
                if (wordmap_find_index(semgraph->scripts, word) != MAXwordID)
                {
                    MEMCHK(rc, scripts->num_scripts, MAX_SCRIPTS);
                    scripts->list[scripts->num_scripts].expression = word;
                    scripts->list[scripts->num_scripts].ruleName = (LCHAR*) scope; /* just an ID */
                    ++scripts->num_scripts;
                }
                /* else ignore */
            }
        }
    }
    return ESR_SUCCESS;
CLEANUP:
    return rc;
}

ESR_ReturnCode SR_SemanticProcessor_SetParam(SR_SemanticProcessor* self,
        const LCHAR* key, const LCHAR* value)
{
    SR_SemanticProcessorImpl* impl = (SR_SemanticProcessorImpl*) self;

    if (self == NULL || key == NULL || value == NULL)
    {
        PLogError(L("ESR_INVALID_ARGUMENT"));
        return ESR_INVALID_ARGUMENT;
    }

    return ST_putSpecialKeyValue(impl->symtable, key, value);

}

ESR_ReturnCode SR_SemanticProcessor_Flush(SR_SemanticProcessor* self)
{
    SR_SemanticProcessorImpl* impl = (SR_SemanticProcessorImpl*) self;

    if (self == NULL)
    {
        PLogError(L("ESR_INVALID_ARGUMENT"));
        return ESR_INVALID_ARGUMENT;
    }
    return ST_reset_all(impl->symtable);
}
