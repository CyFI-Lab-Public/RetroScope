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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.AlignmentSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * The default candidates view manager using {@link android.widget.EditText}.
 *
 * @author Copyright (C) 2011 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
public class TextCandidates1LineViewManager extends CandidatesViewManager {

    /** displayCandidates() normal display */
    private static final int IS_NEXTCANDIDATE_NORMAL = 1;
    /** displayCandidates() delay display */
    private static final int IS_NEXTCANDIDATE_DELAY = 2;
    /** displayCandidates() display end */
    private static final int IS_NEXTCANDIDATE_END = 3;

    /** Delay of set candidate */
    private static final int SET_CANDIDATE_DELAY = 50;
    /** Delay Millis */
    private static final int CANDIDATE_DELAY_MILLIS = 500;

    /** Scroll distance */
    private static final float SCROLL_DISTANCE = 0.9f;

    /** Body view of the candidates list */
    private ViewGroup  mViewBody;
    /** Scroller */
    private HorizontalScrollView mViewBodyScroll;
    /** Left more button */
    private ImageView mLeftMoreButton;
    /** Right more button */
    private ImageView mRightMoreButton;
    /** Candidate view */
    private LinearLayout mViewCandidateList;

    /** {@link OpenWnn} instance using this manager */
    private OpenWnn mWnn;
    /** View type (VIEW_TYPE_NORMAL or VIEW_TYPE_FULL or VIEW_TYPE_CLOSE) */
    private int mViewType;

    /** view width */
    private int mViewWidth;
    /** Minimum width of candidate view */
    private int mCandidateMinimumWidth;
    /** Minimum height of candidate view */
    private int mCandidateMinimumHeight;

    /** Minimum width of candidate view */
    private static final int CANDIDATE_MINIMUM_WIDTH = 48;

    /** Whether hide the view if there is no candidate */
    private boolean mAutoHideMode;
    /** The converter to get candidates from and notice the selected candidate to. */
    private WnnEngine mConverter;
    /** Limitation of displaying candidates */
    private int mDisplayLimit;

    /** Vibrator for touch vibration */
    private Vibrator mVibrator = null;
    /** AudioManager for click sound */
    private AudioManager mSound = null;

    /** Number of candidates displaying */
    private int mWordCount;
    /** List of candidates */
    private ArrayList<WnnWord> mWnnWordArray;

    /** Character width of the candidate area */
    private int mLineLength = 0;
    /** Maximum width of candidate view */
    private int mCandidateMaxWidth = 0;
    /** general information about a display */
    private final DisplayMetrics mMetrics = new DisplayMetrics();
    /** Focus is none now */
    private static final int FOCUS_NONE = -1;
    /** Handler for set  Candidate */
    private static final int MSG_SET_CANDIDATES = 1;
    /** List of textView for CandiData List */
    private ArrayList<TextView> mTextViewArray = new ArrayList<TextView>();
    /** Now focus textView index */
    private int mCurrentFocusIndex = FOCUS_NONE;
    /** Focused View */
    private View mFocusedView = null;
    /** Focused View Background */
    private Drawable mFocusedViewBackground = null;
    /** Scale up text size */
    private AbsoluteSizeSpan mSizeSpan;
    /** Scale up text alignment */
    private AlignmentSpan.Standard mCenterSpan;
    /** Whether candidates long click enable */
    private boolean mEnableCandidateLongClick = true;

   /** {@code Handler} Handler for focus Candidate wait delay */
    private Handler mHandler = new Handler() {
            @Override public void handleMessage(Message msg) {

            switch (msg.what) {
                case MSG_SET_CANDIDATES:
                    displayCandidatesDelay(mConverter);
                    break;

                default:
                    break;
                }
            }
        };

    /** Event listener for touching a candidate */
    private OnClickListener mCandidateOnClick = new OnClickListener() {
        public void onClick(View v) {
            if (!v.isShown()) {
                return;
            }
            playSoundAndVibration();

            if (v instanceof CandidateTextView) {
                CandidateTextView text = (CandidateTextView)v;
                int wordcount = text.getId();
                WnnWord word = getWnnWord(wordcount);
                clearFocusCandidate();
                selectCandidate(word);
            }
        }
    };

    /** Event listener for long-clicking a candidate */
    private OnLongClickListener mCandidateOnLongClick = new OnLongClickListener() {
        public boolean onLongClick(View v) {
            if (!v.isShown()) {
                return true;
            }

            if (!mEnableCandidateLongClick) {
                return false;
            }

            clearFocusCandidate();

            int wordcount = ((TextView)v).getId();
            mWord = mWnnWordArray.get(wordcount);

            displayDialog(v, mWord);
            return true;
        }
    };

    /**
     * Constructor
     */
    public TextCandidates1LineViewManager() {
        this(300);
    }

    /**
     * Constructor
     *
     * @param displayLimit      The limit of display
     */
    public TextCandidates1LineViewManager(int displayLimit) {
        mDisplayLimit = displayLimit;
        mWnnWordArray = new ArrayList<WnnWord>();
        mAutoHideMode = true;
        mMetrics.setToDefaults();
    }

    /**
     * Set auto-hide mode.
     * @param hide      {@code true} if the view will hidden when no candidate exists;
     *                  {@code false} if the view is always shown.
     */
    public void setAutoHide(boolean hide) {
        mAutoHideMode = hide;
    }

    /** @see CandidatesViewManager */
    public View initView(OpenWnn parent, int width, int height) {
        mWnn = parent;
        mViewWidth = width;

        Resources r = mWnn.getResources();

        mCandidateMinimumWidth = (int)(CANDIDATE_MINIMUM_WIDTH * mMetrics.density);
        mCandidateMinimumHeight = r.getDimensionPixelSize(R.dimen.candidate_layout_height);

        LayoutInflater inflater = parent.getLayoutInflater();
        mViewBody = (ViewGroup)inflater.inflate(R.layout.candidates_1line, null);
        mViewBodyScroll = (HorizontalScrollView)mViewBody.findViewById(R.id.candview_scroll_1line);
        mViewBodyScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mViewBodyScroll.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    if (mHandler.hasMessages(MSG_SET_CANDIDATES)) {
                        mHandler.removeMessages(MSG_SET_CANDIDATES);
                        mHandler.sendEmptyMessageDelayed(MSG_SET_CANDIDATES, CANDIDATE_DELAY_MILLIS);
                    }
                    break;

                default:
                    break;

                }
                return false;
            }
        });

        mLeftMoreButton = (ImageView)mViewBody.findViewById(R.id.left_more_imageview);
        mLeftMoreButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!v.isShown()) {
                    return;
                }
                playSoundAndVibration();
                if (mViewBodyScroll.getScrollX() > 0) {
                    mViewBodyScroll.smoothScrollBy(
                                        (int)(mViewBodyScroll.getWidth() * -SCROLL_DISTANCE), 0);
                }
            }
        });
        mLeftMoreButton.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                if (!v.isShown()) {
                    return false;
                }
                if (!mViewBodyScroll.fullScroll(View.FOCUS_LEFT)) {
                    mViewBodyScroll.scrollTo(mViewBodyScroll.getChildAt(0).getWidth(), 0);
                }
                return true;
            }
        });

        mRightMoreButton = (ImageView)mViewBody.findViewById(R.id.right_more_imageview);
        mRightMoreButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!v.isShown()) {
                    return;
                }
                int width = mViewBodyScroll.getWidth();
                int scrollMax = mViewBodyScroll.getChildAt(0).getRight();

                if ((mViewBodyScroll.getScrollX() + width) < scrollMax) {
                    mViewBodyScroll.smoothScrollBy((int)(width * SCROLL_DISTANCE), 0);
                }
            }
        });
        mRightMoreButton.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                if (!v.isShown()) {
                    return false;
                }
                if (!mViewBodyScroll.fullScroll(View.FOCUS_RIGHT)) {
                    mViewBodyScroll.scrollTo(0, 0);
                }
                return true;
            }
        });

        mViewLongPressDialog = (View)inflater.inflate(R.layout.candidate_longpress_dialog, null);

        /* select button */
        Button longPressDialogButton = (Button)mViewLongPressDialog.findViewById(R.id.candidate_longpress_dialog_select);
        longPressDialogButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    playSoundAndVibration();
                    clearFocusCandidate();
                    selectCandidate(mWord);
                    closeDialog();
                }
            });

        /* cancel button */
        longPressDialogButton = (Button)mViewLongPressDialog.findViewById(R.id.candidate_longpress_dialog_cancel);
        longPressDialogButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    playSoundAndVibration();
                    mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.LIST_CANDIDATES_NORMAL));
                    mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.UPDATE_CANDIDATE));
                    closeDialog();
                }
            });

        int buttonWidth = r.getDimensionPixelSize(R.dimen.candidate_layout_width);
        mCandidateMaxWidth = (mViewWidth - buttonWidth * 2) / 2;

        mSizeSpan = new AbsoluteSizeSpan(r.getDimensionPixelSize(R.dimen.candidate_delete_word_size));
        mCenterSpan = new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER);

        createNormalCandidateView();

        setViewType(CandidatesViewManager.VIEW_TYPE_CLOSE);

        return mViewBody;
    }

    /**
     * Create normal candidate view
     */
    private void createNormalCandidateView() {
        mViewCandidateList = (LinearLayout)mViewBody.findViewById(R.id.candidates_view_1line);

        Context context = mViewBodyScroll.getContext();
        for (int i = 0; i < mDisplayLimit; i++) {
            mViewCandidateList.addView(new CandidateTextView(context,
                                                            mCandidateMinimumHeight,
                                                            mCandidateMinimumWidth,
                                                            mCandidateMaxWidth));
        }
    }

    /** @see CandidatesViewManager#getCurrentView */
    public View getCurrentView() {
        return mViewBody;
    }

    /** @see CandidatesViewManager#setViewType */
    public void setViewType(int type) {
        mViewType = type;

        if (type == CandidatesViewManager.VIEW_TYPE_NORMAL) {
            mViewCandidateList.setMinimumHeight(mCandidateMinimumHeight);
        } else {
            mViewCandidateList.setMinimumHeight(-1);
            mHandler.removeMessages(MSG_SET_CANDIDATES);

            if (mViewBody.isShown()) {
                mWnn.setCandidatesViewShown(false);
            }
        }
    }

    /** @see CandidatesViewManager#getViewType */
    public int getViewType() {
        return mViewType;
    }

    /** @see CandidatesViewManager#displayCandidates */
    public void displayCandidates(WnnEngine converter) {

        mHandler.removeMessages(MSG_SET_CANDIDATES);

        closeDialog();
        clearCandidates();
        mConverter = converter;

        int isNextCandidate = IS_NEXTCANDIDATE_NORMAL;
        while(isNextCandidate == IS_NEXTCANDIDATE_NORMAL) {
            isNextCandidate = displayCandidatesNormal(converter);
        }

        if (isNextCandidate == IS_NEXTCANDIDATE_DELAY) {
            isNextCandidate = displayCandidatesDelay(converter);
        }

        mViewBodyScroll.scrollTo(0,0);
    }


    /**
     * Display the candidates.
     * @param converter  {@link WnnEngine} which holds candidates.
     */
    private int displayCandidatesNormal(WnnEngine converter) {
        int isNextCandidate = IS_NEXTCANDIDATE_NORMAL;

        if (converter == null) {
            return IS_NEXTCANDIDATE_END;
        }

        /* Get candidates */
        WnnWord result = converter.getNextCandidate();
        if (result == null) {
            return IS_NEXTCANDIDATE_END;
        }

        mLineLength += setCandidate(result);
        if (mLineLength >= mViewWidth) {
            isNextCandidate = IS_NEXTCANDIDATE_DELAY;
        }

        if (mWordCount < 1) { /* no candidates */
            if (mAutoHideMode) {
                mWnn.setCandidatesViewShown(false);
                return IS_NEXTCANDIDATE_END;
            }
        }

        if (mWordCount > mDisplayLimit) {
            return IS_NEXTCANDIDATE_END;
        }

        if (!(mViewBody.isShown())) {
            mWnn.setCandidatesViewShown(true);
        }
        return isNextCandidate;
    }

    /**
     * Display the candidates.
     * @param converter  {@link WnnEngine} which holds candidates.
     */
    private int displayCandidatesDelay(WnnEngine converter) {
        int isNextCandidate = IS_NEXTCANDIDATE_DELAY;

        if (converter == null) {
            return IS_NEXTCANDIDATE_END;
        }

        /* Get candidates */
        WnnWord result = converter.getNextCandidate();
        if (result == null) {
            return IS_NEXTCANDIDATE_END;
        }

        setCandidate(result);

        if (mWordCount > mDisplayLimit) {
            return IS_NEXTCANDIDATE_END;
        }

        mHandler.sendEmptyMessageDelayed(MSG_SET_CANDIDATES, SET_CANDIDATE_DELAY);

        return isNextCandidate;
    }

    /**
     * Set the candidate for candidate view
     * @param word set word
     * @return int Set width
     */
    private int setCandidate(WnnWord word) {
        CandidateTextView candidateTextView =
                (CandidateTextView) mViewCandidateList.getChildAt(mWordCount);
        candidateTextView.setCandidateTextView(word, mWordCount, mCandidateOnClick,
                                                    mCandidateOnLongClick);
        mWnnWordArray.add(mWordCount, word);
        mWordCount++;
        mTextViewArray.add(candidateTextView);

        return candidateTextView.getWidth();
    }

    /**
     * Clear the candidate view
     */
    private void clearNormalViewCandidate() {
        int candidateNum = mViewCandidateList.getChildCount();
        for (int i = 0; i < candidateNum; i++) {
            View v = mViewCandidateList.getChildAt(i);
            v.setVisibility(View.GONE);
        }
    }

    /** @see CandidatesViewManager#clearCandidates */
    public void clearCandidates() {
        clearFocusCandidate();
        clearNormalViewCandidate();

        mLineLength = 0;

        mWordCount = 0;
        mWnnWordArray.clear();
        mTextViewArray.clear();

        if (mAutoHideMode && mViewBody.isShown()) {
            mWnn.setCandidatesViewShown(false);
        }
    }

    /** @see CandidatesViewManager#setPreferences */
    public void setPreferences(SharedPreferences pref) {
        try {
            if (pref.getBoolean("key_vibration", false)) {
                mVibrator = (Vibrator)mWnn.getSystemService(Context.VIBRATOR_SERVICE);
            } else {
                mVibrator = null;
            }
            if (pref.getBoolean("key_sound", false)) {
                mSound = (AudioManager)mWnn.getSystemService(Context.AUDIO_SERVICE);
            } else {
                mSound = null;
            }
        } catch (Exception ex) {
            Log.d("OpenWnn", "NO VIBRATOR");
        }
    }

    /**
     * Select a candidate.
     * <br>
     * This method notices the selected word to {@link OpenWnn}.
     *
     * @param word  The selected word
     */
    private void selectCandidate(WnnWord word) {
        mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.SELECT_CANDIDATE, word));
    }

    private void playSoundAndVibration() {
        if (mVibrator != null) {
            try {
                mVibrator.vibrate(5);
            } catch (Exception ex) {
                Log.e("OpenWnn", "TextCandidates1LineViewManager::selectCandidate Vibrator " + ex.toString());
            }
        }
        if (mSound != null) {
            try {
                mSound.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 1.0f);
            } catch (Exception ex) {
                Log.e("OpenWnn", "TextCandidates1LineViewManager::selectCandidate Sound " + ex.toString());
            }
        }
    }

    /**
     * KeyEvent action for the candidate view.
     *
     * @param key    Key event
     */
    public void processMoveKeyEvent(int key) {
        if (!mViewBody.isShown()) {
            return;
        }

        switch (key) {
        case KeyEvent.KEYCODE_DPAD_LEFT:
            moveFocus(-1);
            break;

        case KeyEvent.KEYCODE_DPAD_RIGHT:
            moveFocus(1);
            break;

        case KeyEvent.KEYCODE_DPAD_UP:
            moveFocus(-1);
            break;

        case KeyEvent.KEYCODE_DPAD_DOWN:
            moveFocus(1);
            break;

        default:
            break;

        }
    }

    /**
     * Get a flag candidate is focused now.
     *
     * @return the Candidate is focused of a flag.
     */
    public boolean isFocusCandidate(){
        if (mCurrentFocusIndex != FOCUS_NONE) {
            return true;
        }
        return false;
    }

    /**
     * Give focus to View of candidate.
     */
    private void setViewStatusOfFocusedCandidate() {
        View view = mFocusedView;
        if (view != null) {
            view.setBackgroundDrawable(mFocusedViewBackground);
        }

        TextView v = getFocusedView();
        mFocusedView = v;
        if (v != null) {
            mFocusedViewBackground = v.getBackground();
            v.setBackgroundResource(R.drawable.cand_back_focuse);

            int viewBodyLeft = getViewLeftOnScreen(mViewBodyScroll);
            int viewBodyRight = viewBodyLeft + mViewBodyScroll.getWidth();
            int focusedViewLeft = getViewLeftOnScreen(v);
            int focusedViewRight = focusedViewLeft + v.getWidth();

            if (focusedViewRight > viewBodyRight) {
                mViewBodyScroll.scrollBy((focusedViewRight - viewBodyRight), 0);
            } else if (focusedViewLeft < viewBodyLeft) {
                mViewBodyScroll.scrollBy((focusedViewLeft - viewBodyLeft), 0);
            }
        }
    }

    /**
     * Clear focus to selected candidate.
     */
    private void clearFocusCandidate(){
        View view = mFocusedView;
        if (view != null) {
            view.setBackgroundDrawable(mFocusedViewBackground);
            mFocusedView = null;
        }

        mCurrentFocusIndex = FOCUS_NONE;

        mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.FOCUS_CANDIDATE_END));
    }

    /**
     * Select candidate that has focus.
     */
    public void selectFocusCandidate(){
        if (mCurrentFocusIndex != FOCUS_NONE) {
            selectCandidate(getFocusedWnnWord());
        }
    }

    /**
     * Get View of focus candidate.
     */
    private TextView getFocusedView() {
        if (mCurrentFocusIndex == FOCUS_NONE) {
            return null;
        }
        return mTextViewArray.get(mCurrentFocusIndex);
    }

    /**
     * Move the focus to next candidate.
     *
     * @param direction  The direction of increment or decrement.
     */
    private void moveFocus(int direction) {
        boolean isStart = (mCurrentFocusIndex == FOCUS_NONE);
        int size = mTextViewArray.size();
        int index = isStart ? 0 : (mCurrentFocusIndex + direction);

        if (index < 0) {
            index = size - 1;
        } else {
            if (index >= size) {
                index = 0;
            }
        }

        mCurrentFocusIndex = index;
        setViewStatusOfFocusedCandidate();

        if (isStart) {
            mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.FOCUS_CANDIDATE_START));
        }
    }

    /**
     * Get view top position on screen.
     *
     * @param view target view.
     * @return int view top position on screen
     */
    private int getViewLeftOnScreen(View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        return location[0];
    }

    /** @see CandidatesViewManager#getFocusedWnnWord */
    public WnnWord getFocusedWnnWord() {
        return getWnnWord(mCurrentFocusIndex);
    }

    /**
     * Get WnnWord.
     *
     * @return WnnWord word
     */
    public WnnWord getWnnWord(int index) {
        return mWnnWordArray.get(index);
    }

    /** @see CandidatesViewManager#setCandidateMsgRemove */
    public void setCandidateMsgRemove() {
        mHandler.removeMessages(MSG_SET_CANDIDATES);
    }
}
