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

import jp.co.omronsoft.openwnn.*;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import java.util.Comparator;

/**
 * The user dictionary tool class for English IME.
 *
 * @author Copyright (C) 2009 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
public class UserDictionaryToolsListEN extends UserDictionaryToolsList {
    /**
     * Constructor
     */
    public UserDictionaryToolsListEN() {
        if (OpenWnnEN.getInstance() == null) {
            new OpenWnnEN(this);
        }
        mListViewName = "jp.co.omronsoft.openwnn.EN.UserDictionaryToolsListEN";
        mEditViewName = "jp.co.omronsoft.openwnn.EN.UserDictionaryToolsEditEN";
        mPackageName  = "jp.co.omronsoft.openwnn";
    }

    /** @see jp.co.omronsoft.iwnnime.ml.UserDictionaryToolsList#onCreate */
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.user_dictionary_list_words_en);
    }

    /** @see jp.co.omronsoft.openwnn.UserDictionaryToolsList#createUserDictionaryToolsEdit */
    @Override protected UserDictionaryToolsEdit createUserDictionaryToolsEdit(View view1, View view2) {
        return new UserDictionaryToolsEditEN(view1, view2);
    }

    /** @see jp.co.omronsoft.openwnn.UserDictionaryToolsList#sendEventToIME */
    @Override protected boolean sendEventToIME(OpenWnnEvent ev) {
        try {
            return OpenWnnEN.getInstance().onEvent(ev);
        } catch (Exception ex) {
            /* do nothing if an error occurs */
        }
        return false;
    }

    /** @see jp.co.omronsoft.openwnn.UserDictionaryToolsList#getComparator */
    @Override protected Comparator<WnnWord> getComparator() {
    	return new ListComparatorEN();
    }

    /** Comparator class for sorting the list of English user dictionary */
    protected class ListComparatorEN implements Comparator<WnnWord>{
        public int compare(WnnWord word1, WnnWord word2) {
            return word1.stroke.compareTo(word2.stroke);
        };
    }
}
