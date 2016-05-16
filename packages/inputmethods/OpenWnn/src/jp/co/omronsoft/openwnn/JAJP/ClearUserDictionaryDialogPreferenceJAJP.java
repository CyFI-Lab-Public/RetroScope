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

package jp.co.omronsoft.openwnn.JAJP;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.widget.Toast;
import jp.co.omronsoft.openwnn.*;

/**
 * The preference class to clear user dictionary for Japanese IME.
 *
 * @author Copyright (C) 2009 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
public class ClearUserDictionaryDialogPreferenceJAJP extends DialogPreference {
    /** The context */
    protected Context mContext = null;

    /**
     * Constructor
     *
     * @param context   The context
     * @param attrs     The set of attributes
     */
    public ClearUserDictionaryDialogPreferenceJAJP(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    /**
     * Constructor
     *
     * @param context   The context
     */
    public ClearUserDictionaryDialogPreferenceJAJP(Context context) {
        this(context, null);
    }

    /** @see android.preference.DialogPreference#onDialogClosed */
    @Override protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            /* clear the user dictionary */
            OpenWnnEvent ev = new OpenWnnEvent(OpenWnnEvent.INITIALIZE_USER_DICTIONARY, new WnnWord());
            OpenWnnJAJP.getInstance().onEvent(ev);

            /* show the message */
            Toast.makeText(mContext.getApplicationContext(), R.string.dialog_clear_user_dictionary_done,
                           Toast.LENGTH_LONG).show();
        }
    }

}
