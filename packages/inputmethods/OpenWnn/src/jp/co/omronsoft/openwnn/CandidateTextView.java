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
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.text.TextPaint;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The default candidates view manager using {@link TextView}.
 *
 * @author Copyright (C) 2011 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
public class CandidateTextView extends TextView {

    /** Width of fontsize change */
    private static final int WIDTH_SIZE[] = {0, 50, 100};
    /** Fontsize corresponding to width */
    private static final float CUSTOM_FONTSIZE[] = {20.0f, 17.0f, 15.0f};
    /** Width of fontsize change beginning */
    private static final int CHANGE_FONTSIZE_WIDTH = 120;

    /** Maximum width of candidate view */
    private int mMaxWidth;
    /** Width of fontsize change beginning */
    private int mChangeFontSize;
    /** Minimum width of candidate view */
    private int mCandidateMinimumWidth;

    /** Alert dialog */
    private Dialog mCandidateDialog = null;

   /**
    * Constructor
    * @param context    context
    */
    public CandidateTextView(Context context) {
        super(context);
        setSoundEffectsEnabled(false);
    }

   /**
    * Constructor
    * @param context    context
    * @param candidateMinimumHeight Minimum height of candidate view
    * @param candidateMinimumWidth  Minimum width of candidate view
    * @param maxWidth  Maximum width of candidate view
    */
    public CandidateTextView(Context context, int candidateMinimumHeight, int candidateMinimumWidth, int maxWidth) {
        super(context);
        setSoundEffectsEnabled(false);
        setTextColor(getResources().getColor(R.color.candidate_text_1line));
        setBackgroundResource(R.drawable.cand_back_1line);
        setGravity(Gravity.CENTER);
        setSingleLine();
        setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                                           ViewGroup.LayoutParams.MATCH_PARENT,
                                                           1.0f));
        setMinHeight(candidateMinimumHeight);
        setMinimumWidth(candidateMinimumWidth);
        mCandidateMinimumWidth = candidateMinimumWidth;
        mMaxWidth = maxWidth;
        mChangeFontSize = maxWidth - CHANGE_FONTSIZE_WIDTH;
    }

   /**
    * Textview is set to the best content for the display of candidate.
    * @param WnnWord    candidate
    * @param wordCount  candidate id
    * @param OnClickListener Operation when clicking
    * @param OnClickListener Operation when longclicking
    * @return Set completion textview
    */
    public CandidateTextView setCandidateTextView(WnnWord word, int wordCount,
                                                        OnClickListener candidateOnClick,
                                                        OnLongClickListener candidateOnLongClick) {
        setTextSize(CUSTOM_FONTSIZE[0]);
        setText(word.candidate);
        setId(wordCount);
        setVisibility(View.VISIBLE);
        setPressed(false);
        setWidth(0);
        setEllipsize(null);
        setOnClickListener(candidateOnClick);
        setOnLongClickListener(candidateOnLongClick);
        setCustomCandidate(word);
        return this;
    }

   /**
    * If the text view is set to the best width for the display,
    * and it is necessary, the character is shortened.
    * @param WnnWord candidate word
    * @return int    textview width
    */
    public int setCustomCandidate(WnnWord word) {
        TextPaint paint = getPaint();
        int width = (int)paint.measureText(word.candidate, 0, word.candidate.length());
        width += getPaddingLeft() + getPaddingRight();

        if (width > mCandidateMinimumWidth) {
            int i;
            for (i = 0; i < WIDTH_SIZE.length; i++) {
                if (width > mChangeFontSize + WIDTH_SIZE[i]) {
                    setTextSize(CUSTOM_FONTSIZE[i]);
                }
            }

            width = (int)paint.measureText(word.candidate, 0, word.candidate.length());
            width += getPaddingLeft() + getPaddingRight();

            if (width >= mMaxWidth) {
                setWidth(mMaxWidth);
                width = mMaxWidth;
                setEllipsize(TextUtils.TruncateAt.START);
            } else {
                setWidth(width);
            }
        } else {
            setWidth(mCandidateMinimumWidth);
            width = mCandidateMinimumWidth;
        }
        return width;
    }

    /** @see View#setBackgroundDrawable */
    @Override public void setBackgroundDrawable(Drawable d) {
        super.setBackgroundDrawable(d);
        setPadding(20, 0, 20, 0);
    }

    /**
     * Display Dialog.
     *
     * @param builder  The Dialog builder,
     */
    public void displayCandidateDialog(Dialog builder) {
        if (mCandidateDialog != null) {
            mCandidateDialog.dismiss();
            mCandidateDialog = null;
        }
        mCandidateDialog = builder;
        Window window = mCandidateDialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.token = getWindowToken();
        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
        window.setAttributes(lp);
        window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        mCandidateDialog.show();
    }

    /** @see android.view.View#onWindowVisibilityChanged */
    @Override protected void onWindowVisibilityChanged(int visibility) {
       super.onWindowVisibilityChanged(visibility);
       if ((visibility != VISIBLE) && (mCandidateDialog != null)) {
           mCandidateDialog.dismiss();
       }
    }
}
