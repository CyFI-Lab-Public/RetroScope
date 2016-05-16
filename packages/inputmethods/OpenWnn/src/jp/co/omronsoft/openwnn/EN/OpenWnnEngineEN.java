/*
 * Copyright (C) 2008-2012  OMRON SOFTWARE Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.co.omronsoft.openwnn.EN;

import java.util.HashMap;
import java.util.ArrayList;

import jp.co.omronsoft.openwnn.*;
import android.content.SharedPreferences;

/**
 * The OpenWnn engine class for English IME.
 * 
 * @author Copyright (C) 2009 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
public class OpenWnnEngineEN implements WnnEngine {
    /** Normal dictionary */
    public static final int DICT_DEFAULT              = 0;
    /** Dictionary for mistype correction */
    public static final int DICT_FOR_CORRECT_MISTYPE  = 1;
    /** Score(frequency value) of word in the learning dictionary */
    public static final int FREQ_LEARN = 600;
    /** Score(frequency value) of word in the user dictionary */
    public static final int FREQ_USER = 500;
    /** Limitation of predicted candidates */
    public static final int PREDICT_LIMIT = 300;

    /** OpenWnn dictionary */
	private   WnnDictionary mDictionary;
    /** Word list */
    private ArrayList<WnnWord> mConvResult;
    /** HashMap for checking duplicate word */
    private HashMap<String, WnnWord> mCandTable;
    /** Input string */
    private String        mInputString;
    /** Searching string */
    private String        mSearchKey;
    /** Number of output candidates */
    private int           mOutputNum;
    /** The candidate filter */
    private CandidateFilter mFilter = null;
    
    /**
     * Candidate's case
     * <br>
     * CASE_LOWER: all letters are lower.<br>
     * CASE_HEAD_UPPER: the first letter is upper; others are lower.<br>
     * CASE_UPPER: all letters are upper.<br>
     */
    private int           mCandidateCase;
    private static final int CASE_LOWER = 0;
    private static final int CASE_UPPER = 1;
    private static final int CASE_HEAD_UPPER = 3;

    /**
     * Constructor
     * 
     * @param writableDictionaryName		Writable dictionary file name(null if not use)
     */
    public OpenWnnEngineEN(String writableDictionaryName) {
        mConvResult = new ArrayList<WnnWord>();
        mCandTable = new HashMap<String, WnnWord>();
        mSearchKey = null;
        mOutputNum = 0;

        mDictionary = new OpenWnnDictionaryImpl( 
        		"/data/data/jp.co.omronsoft.openwnn/lib/libWnnEngDic.so",
        		writableDictionaryName);
        if (!mDictionary.isActive()) {
        	mDictionary = new OpenWnnDictionaryImpl(
        			"/system/lib/libWnnEngDic.so",
        			writableDictionaryName);
        }
        mDictionary.clearDictionary( );
        
        mDictionary.setDictionary(0, 400, 550);
        mDictionary.setDictionary(1, 400, 550);
        mDictionary.setDictionary(2, 400, 550);
        mDictionary.setDictionary(WnnDictionary.INDEX_USER_DICTIONARY, FREQ_USER, FREQ_USER);
        mDictionary.setDictionary(WnnDictionary.INDEX_LEARN_DICTIONARY, FREQ_LEARN, FREQ_LEARN);

        mDictionary.setApproxPattern(WnnDictionary.APPROX_PATTERN_EN_QWERTY_NEAR);

        mDictionary.setInUseState( false );
    }

    /**
     * Get a candidate.
     *
     * @param index		Index of candidate
     * @return			A candidate; {@code null} if no candidate for the index.
     */
    private WnnWord getCandidate(int index) {
        WnnWord word;
        /* search the candidate from the dictionaries */
        while (mConvResult.size() < PREDICT_LIMIT && index >= mConvResult.size()) {
            while ((word = mDictionary.getNextWord()) != null) {
                /* adjust the case of letter */
                char c = word.candidate.charAt(0);
                if (mCandidateCase == CASE_LOWER) {
                    if (Character.isLowerCase(c)) {
                        break;
                    }
                } else if (mCandidateCase == CASE_HEAD_UPPER) {
                    if (Character.isLowerCase(c)) {
                        word.candidate = Character.toString(Character.toUpperCase(c)) + word.candidate.substring(1);
                    }
                    break;
                } else {
                    word.candidate = word.candidate.toUpperCase();
                    break;
                }
            }
            if (word == null) {
                break;
            }
            /* check duplication */
            addCandidate(word);
        }

        /* get the default candidates */
        if (index >= mConvResult.size()) {
            /* input string itself */
            addCandidate(new WnnWord(mInputString, mSearchKey));

            /* Capitalize the head of input */
            if (mSearchKey.length() > 1) {
                addCandidate(new WnnWord(mSearchKey.substring(0,1).toUpperCase() + mSearchKey.substring(1),
                                         mSearchKey));
            }

            /* Capitalize all */
            addCandidate(new WnnWord(mSearchKey.toUpperCase(), mSearchKey));
        }

        if (index >= mConvResult.size()) {
            return null;
        }
        return mConvResult.get(index);
    }

    /**
     * Add a word to the candidates list if there is no duplication.
     * 
     * @param word		A word
     * @return			{@code true} if the word is added to the list; {@code false} if not.
     */
    private boolean addCandidate(WnnWord word) {
        if (word.candidate == null || mCandTable.containsKey(word.candidate)) {
            return false;
        }
        if (mFilter != null && !mFilter.isAllowed(word)) {
        	return false;
        }
        mCandTable.put(word.candidate, word);
        mConvResult.add(word);
        return true;
    }

    private void clearCandidates() {
        mConvResult.clear();
        mCandTable.clear();
        mOutputNum = 0;
        mSearchKey = null;
    }

    /**
     * Set dictionary.
     *
     * @param type		Type of dictionary (DIC_DEFAULT or DIC_FOR_CORRECT_MISTYPE)
     * @return			{@code true} if the dictionary is changed; {@code false} if not.
     */
    public boolean setDictionary(int type) {
        if (type == DICT_FOR_CORRECT_MISTYPE) {
            mDictionary.clearApproxPattern();
            mDictionary.setApproxPattern(WnnDictionary.APPROX_PATTERN_EN_QWERTY_NEAR);
        } else {
            mDictionary.clearApproxPattern();
        }
        return true;
    }

    /**
     * Set search key for the dictionary.
     * <br>
     * To search the dictionary, this method set the lower case of
     * input string to the search key. And hold the input string's
     * capitalization information to adjust the candidates
     * capitalization later.
     *
     * @param input		Input string
     * @return			{@code true} if the search key is set; {@code false} if not.
     */
    private boolean setSearchKey(String input) {
        if (input.length() == 0) {
            return false;
        }

        /* set mInputString */
        mInputString = input;

        /* set mSearchKey */
        mSearchKey = input.toLowerCase();

        /* set mCandidateCase */
        if (Character.isUpperCase(input.charAt(0))) {
            if (input.length() > 1 && Character.isUpperCase(input.charAt(1))) {
                mCandidateCase = CASE_UPPER;
            } else {
                mCandidateCase = CASE_HEAD_UPPER;
            }
        } else {
            mCandidateCase = CASE_LOWER;
        }

        return true;
    }
    
    /**
     * Set the candidate filter
     * 
     * @param filter	The candidate filter
     */
    public void setFilter(CandidateFilter filter) {
    	mFilter = filter;
    }
    
    /***********************************************************************
     * WnnEngine's interface
     **********************************************************************/
    /** @see jp.co.omronsoft.openwnn.WnnEngine#init */
    public void init() {}

    /** @see jp.co.omronsoft.openwnn.WnnEngine#close */
    public void close() {}

    /** @see jp.co.omronsoft.openwnn.WnnEngine#predict */
    public int predict(ComposingText text, int minLen, int maxLen) {
        clearCandidates();

        if (text == null) { return 0; }
        
        String input = text.toString(2);
        if (!setSearchKey(input)) {
            return 0;
        }

        /* set dictionaries by the length of input */
        WnnDictionary dict = mDictionary;
        dict.setInUseState( true );

        dict.clearDictionary();
        dict.setDictionary(0, 400, 550);
        if (input.length() > 1) {
            dict.setDictionary(1, 400, 550);
        }
        if (input.length() > 2) {
            dict.setDictionary(2, 400, 550);
        }
        dict.setDictionary(WnnDictionary.INDEX_USER_DICTIONARY, FREQ_USER, FREQ_USER);
        dict.setDictionary(WnnDictionary.INDEX_LEARN_DICTIONARY, FREQ_LEARN, FREQ_LEARN);
        
        /* search dictionaries */
        dict.searchWord(WnnDictionary.SEARCH_PREFIX, WnnDictionary.ORDER_BY_FREQUENCY, mSearchKey);
        return 1;
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine#convert */
    public int convert(ComposingText text) {
        clearCandidates();
        return 0;
    }
    
    /** @see jp.co.omronsoft.openwnn.WnnEngine#searchWords */
    public int searchWords(String key) {
        clearCandidates();
        return 0;
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine#searchWords */
    public int searchWords(WnnWord word) {
        clearCandidates();
        return 0;
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine#getNextCandidate */
    public WnnWord getNextCandidate() {
        if (mSearchKey == null) {
            return null;
        }
        WnnWord word = getCandidate(mOutputNum);
        if (word != null) {
            mOutputNum++;
        }
        return word;
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine#learn */
    public boolean learn(WnnWord word) {
        return ( mDictionary.learnWord(word) == 0 );
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine#addWord */
    public int addWord(WnnWord word) {
        WnnDictionary dict = mDictionary;
        dict.setInUseState( true );
        dict.addWordToUserDictionary(word);
        dict.setInUseState( false );
        return 0;
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine#deleteWord */
    public boolean deleteWord(WnnWord word) {
        WnnDictionary dict = mDictionary;
        dict.setInUseState( true );
        dict.removeWordFromUserDictionary(word);
        dict.setInUseState( false );
        return false;
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine#setPreferences */
    public void setPreferences(SharedPreferences pref) {}

    /** @see jp.co.omronsoft.openwnn.WnnEngine#breakSequence */
    public void breakSequence()  {}

    /** @see jp.co.omronsoft.openwnn.WnnEngine#makeCandidateListOf */
    public int makeCandidateListOf(int clausePosition)  {return 0;}

    /** @see jp.co.omronsoft.openwnn.WnnEngine#initializeDictionary */
    public boolean initializeDictionary(int dictionary)  {
        WnnDictionary dict = mDictionary;

        switch( dictionary ) {
        case WnnEngine.DICTIONARY_TYPE_LEARN:
            dict.setInUseState( true );
            dict.clearLearnDictionary();
            dict.setInUseState( false );
            return true;

        case WnnEngine.DICTIONARY_TYPE_USER:
            dict.setInUseState( true );
            dict.clearUserDictionary();
            dict.setInUseState( false );
            return true;
        }
        return false;
    }
    
    /** @see jp.co.omronsoft.openwnn.WnnEngine#initializeDictionary */
    public boolean initializeDictionary(int dictionary, int type) {
    	return initializeDictionary(dictionary);
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine#getUserDictionaryWords */
    public WnnWord[] getUserDictionaryWords( ) {
        WnnDictionary dict = mDictionary;
        dict.setInUseState( true );
        WnnWord[] result = dict.getUserDictionaryWords( );
        dict.setInUseState( false );
        return result;
    }

}
