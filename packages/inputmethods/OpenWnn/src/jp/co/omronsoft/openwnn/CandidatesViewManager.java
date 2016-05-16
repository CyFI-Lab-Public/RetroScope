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

package jp.co.omronsoft.openwnn;

import android.app.Dialog;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.TypedValue;

import java.util.ArrayList;

/**
 * The interface of candidates view manager used by {@link OpenWnn}.
 *
 * @author Copyright (C) 2008-2011 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
public abstract class CandidatesViewManager {
    /** Size of candidates view (normal) */
    public static final int VIEW_TYPE_NORMAL = 0;
    /** Size of candidates view (full) */
    public static final int VIEW_TYPE_FULL   = 1;
    /** Size of candidates view (close/non-display) */
    public static final int VIEW_TYPE_CLOSE  = 2;

    /**
     * Attribute of a word (no attribute)
     * @see jp.co.omronsoft.openwnn.WnnWord
     */
    public static final int ATTRIBUTE_NONE    = 0;
    /**
     * Attribute of a word (a candidate in the history list)
     * @see jp.co.omronsoft.openwnn.WnnWord
     */
    public static final int ATTRIBUTE_HISTORY = 1;
    /**
     * Attribute of a word (the best candidate)
     * @see jp.co.omronsoft.openwnn.WnnWord
     */
    public static final int ATTRIBUTE_BEST    = 2;
    /**
     * Attribute of a word (auto generated/not in the dictionary)
     * @see jp.co.omronsoft.openwnn.WnnWord
     */
    public static final int ATTRIBUTE_AUTO_GENERATED  = 4;

    /** The view of the LongPressDialog */
    protected View mViewLongPressDialog = null;

    /** Whether candidates long click enable */
    protected Dialog mDialog = null;

    /** The word pressed */
    protected WnnWord mWord;

    /**
     * Initialize the candidates view.
     *
     * @param parent    The OpenWnn object
     * @param width     The width of the display
     * @param height    The height of the display
     *
     * @return The candidates view created in the initialize process; {@code null} if cannot create a candidates view.
     */
    public abstract View initView(OpenWnn parent, int width, int height);

    /**
     * Get the candidates view being used currently.
     *
     * @return The candidates view; {@code null} if no candidates view is used currently.
     */
    public abstract View getCurrentView();

    /**
     * Set the candidates view type.
     *
     * @param type  The candidate view type,
     * from {@link CandidatesViewManager#VIEW_TYPE_NORMAL} to
     * {@link CandidatesViewManager#VIEW_TYPE_CLOSE}
     */
    public abstract void setViewType(int type);

    /**
     * Get the candidates view type.
     *
     * @return      The view type,
     * from {@link CandidatesViewManager#VIEW_TYPE_NORMAL} to
     * {@link CandidatesViewManager#VIEW_TYPE_CLOSE}
     */
    public abstract int getViewType();

    /**
     * Display candidates.
     *
     * @param converter  The {@link WnnEngine} from which {@link CandidatesViewManager} gets the candidates
     *
     * @see jp.co.omronsoft.openwnn.WnnEngine#getNextCandidate
     */
    public abstract void displayCandidates(WnnEngine converter);

    /**
     * Clear and hide the candidates view.
     */
    public abstract void clearCandidates();

    /**
     * Replace the preferences in the candidates view.
     *
     * @param pref    The preferences
     */
    public abstract void setPreferences(SharedPreferences pref);

    /**
     * KeyEvent action for soft key board.
     *
     * @param key    Key event
     */
    public abstract void processMoveKeyEvent(int key);

    /**
     * Get candidate is focused now.
     *
     * @return the Candidate is focused of a flag.
     */
    public abstract boolean isFocusCandidate();

    /**
     * Select candidate that has focus.
     */
    public abstract void selectFocusCandidate();

    /**
     * MSG_SET_CANDIDATES removeMessages.
     */
    public abstract void setCandidateMsgRemove();

    /**
     * Display Dialog.
     *
     * @param view  View,
     * @param word  Display word,
     */
    protected void displayDialog(View view, final WnnWord word) {
        if ((view instanceof CandidateTextView) && (null != mViewLongPressDialog)) {
            closeDialog();
            mDialog = new Dialog(view.getContext(), R.style.Dialog);

            TextView text = (TextView)mViewLongPressDialog.findViewById(R.id.candidate_longpress_dialog_text);
            text.setText(word.candidate);

            mDialog.setContentView(mViewLongPressDialog);
            ((CandidateTextView) view).displayCandidateDialog(mDialog);
        }
    }

    /**
     * Close Dialog.
     *
     */
    public void closeDialog() {
        if (null != mDialog) {
            mDialog.dismiss();
            mDialog = null;
            if (null != mViewLongPressDialog) {
                ViewGroup parent = (ViewGroup)mViewLongPressDialog.getParent();
                if (null != parent) {
                    parent.removeView(mViewLongPressDialog);
                }
            }
        }
    }
}
