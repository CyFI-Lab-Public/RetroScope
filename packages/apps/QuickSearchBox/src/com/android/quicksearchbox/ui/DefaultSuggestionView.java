/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.quicksearchbox.ui;

import com.android.quicksearchbox.R;
import com.android.quicksearchbox.Source;
import com.android.quicksearchbox.Suggestion;
import com.android.quicksearchbox.util.Consumer;
import com.android.quicksearchbox.util.NowOrLater;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * View for the items in the suggestions list. This includes promoted suggestions,
 * sources, and suggestions under each source.
 */
public class DefaultSuggestionView extends BaseSuggestionView {

    private static final boolean DBG = false;

    private static final String VIEW_ID = "default";

    private final String TAG = "QSB.DefaultSuggestionView";

    private AsyncIcon mAsyncIcon1;
    private AsyncIcon mAsyncIcon2;

    public DefaultSuggestionView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public DefaultSuggestionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DefaultSuggestionView(Context context) {
        super(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mText1 = (TextView) findViewById(R.id.text1);
        mText2 = (TextView) findViewById(R.id.text2);
        mAsyncIcon1 = new AsyncIcon(mIcon1) {
            // override default icon (when no other available) with default source icon
            @Override
            protected String getFallbackIconId(Source source) {
                return source.getSourceIconUri().toString();
            }
            @Override
            protected Drawable getFallbackIcon(Source source) {
                return source.getSourceIcon();
            }
        };
        mAsyncIcon2 = new AsyncIcon(mIcon2);
    }

    @Override
    public void bindAsSuggestion(Suggestion suggestion, String userQuery) {
        super.bindAsSuggestion(suggestion, userQuery);

        CharSequence text1 = formatText(suggestion.getSuggestionText1(), suggestion);
        CharSequence text2 = suggestion.getSuggestionText2Url();
        if (text2 != null) {
            text2 = formatUrl(text2);
        } else {
            text2 = formatText(suggestion.getSuggestionText2(), suggestion);
        }
        // If there is no text for the second line, allow the first line to be up to two lines
        if (TextUtils.isEmpty(text2)) {
            mText1.setSingleLine(false);
            mText1.setMaxLines(2);
            mText1.setEllipsize(TextUtils.TruncateAt.START);
        } else {
            mText1.setSingleLine(true);
            mText1.setMaxLines(1);
            mText1.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        }
        setText1(text1);
        setText2(text2);
        mAsyncIcon1.set(suggestion.getSuggestionSource(), suggestion.getSuggestionIcon1());
        mAsyncIcon2.set(suggestion.getSuggestionSource(), suggestion.getSuggestionIcon2());

        if (DBG) {
            Log.d(TAG, "bindAsSuggestion(), text1=" + text1 + ",text2=" + text2 + ",q='" +
                    userQuery + ",fromHistory=" + isFromHistory(suggestion));
        }
    }

    private CharSequence formatUrl(CharSequence url) {
        SpannableString text = new SpannableString(url);
        ColorStateList colors = getResources().getColorStateList(R.color.url_text);
        text.setSpan(new TextAppearanceSpan(null, 0, 0, colors, null),
                0, url.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return text;
    }

    private CharSequence formatText(String str, Suggestion suggestion) {
        boolean isHtml = "html".equals(suggestion.getSuggestionFormat());
        if (isHtml && looksLikeHtml(str)) {
            return Html.fromHtml(str);
        } else {
            return str;
        }
    }

    private boolean looksLikeHtml(String str) {
        if (TextUtils.isEmpty(str)) return false;
        for (int i = str.length() - 1; i >= 0; i--) {
            char c = str.charAt(i);
            if (c == '>' || c == '&') return true;
        }
        return false;
    }

    /**
     * Sets the drawable in an image view, makes sure the view is only visible if there
     * is a drawable.
     */
    private static void setViewDrawable(ImageView v, Drawable drawable) {
        // Set the icon even if the drawable is null, since we need to clear any
        // previous icon.
        v.setImageDrawable(drawable);

        if (drawable == null) {
            v.setVisibility(View.GONE);
        } else {
            v.setVisibility(View.VISIBLE);

            // This is a hack to get any animated drawables (like a 'working' spinner)
            // to animate. You have to setVisible true on an AnimationDrawable to get
            // it to start animating, but it must first have been false or else the
            // call to setVisible will be ineffective. We need to clear up the story
            // about animated drawables in the future, see http://b/1878430.
            drawable.setVisible(false, false);
            drawable.setVisible(true, false);
        }
    }

    private class AsyncIcon {
        private final ImageView mView;
        private String mCurrentId;
        private String mWantedId;

        public AsyncIcon(ImageView view) {
            mView = view;
        }

        public void set(final Source source, final String sourceIconId) {
            if (sourceIconId != null) {
                // The iconId can just be a package-relative resource ID, which may overlap with
                // other packages. Make sure it's globally unique.
                Uri iconUri = source.getIconUri(sourceIconId);
                final String uniqueIconId = iconUri == null ? null : iconUri.toString();
                mWantedId = uniqueIconId;
                if (!TextUtils.equals(mWantedId, mCurrentId)) {
                    if (DBG) Log.d(TAG, "getting icon Id=" + uniqueIconId);
                    NowOrLater<Drawable> icon = source.getIcon(sourceIconId);
                    if (icon.haveNow()) {
                        if (DBG) Log.d(TAG, "getIcon ready now");
                        handleNewDrawable(icon.getNow(), uniqueIconId, source);
                    } else {
                        // make sure old icon is not visible while new one is loaded
                        if (DBG) Log.d(TAG , "getIcon getting later");
                        clearDrawable();
                        icon.getLater(new Consumer<Drawable>(){
                            @Override
                            public boolean consume(Drawable icon) {
                                if (DBG) {
                                    Log.d(TAG, "IconConsumer.consume got id " + uniqueIconId +
                                            " want id " + mWantedId);
                                }
                                // ensure we have not been re-bound since the request was made.
                                if (TextUtils.equals(uniqueIconId, mWantedId)) {
                                    handleNewDrawable(icon, uniqueIconId, source);
                                    return true;
                                }
                                return false;
                            }});
                    }
                }
            } else {
                mWantedId = null;
                handleNewDrawable(null, null, source);
            }
        }

        private void handleNewDrawable(Drawable icon, String id, Source source) {
            if (icon == null) {
                mWantedId = getFallbackIconId(source);
                if (TextUtils.equals(mWantedId, mCurrentId)) {
                    return;
                }
                icon = getFallbackIcon(source);
            }
            setDrawable(icon, id);
        }

        private void setDrawable(Drawable icon, String id) {
            mCurrentId = id;
            setViewDrawable(mView, icon);
        }

        private void clearDrawable() {
            mCurrentId = null;
            mView.setImageDrawable(null);
        }

        protected String getFallbackIconId(Source source) {
            return null;
        }

        protected Drawable getFallbackIcon(Source source) {
            return null;
        }

    }

    public static class Factory extends SuggestionViewInflater {
        public Factory(Context context) {
            super(VIEW_ID, DefaultSuggestionView.class, R.layout.suggestion, context);
        }
    }

}
