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

import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.LinkedList;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.graphics.Rect;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.TextPaint;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.text.style.DynamicDrawableSpan;
import android.util.Log;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.AbsoluteLayout;
import android.widget.ImageView;

/**
 * The default candidates view manager class using {@link EditText}.
 *
 * @author Copyright (C) 2009-2011 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
public class TextCandidatesViewManager extends CandidatesViewManager implements GestureDetector.OnGestureListener {
    /** Number of lines to display (Portrait) */
    public static final int LINE_NUM_PORTRAIT       = 2;
    /** Number of lines to display (Landscape) */
    public static final int LINE_NUM_LANDSCAPE      = 1;

    /** Maximum lines */
    private static final int DISPLAY_LINE_MAX_COUNT = 1000;
    /** Maximum number of displaying candidates par one line (full view mode) */
    private static final int FULL_VIEW_DIV = 4;
    /** Maximum number of displaying candidates par one line (full view mode)(symbol)(portrait) */
    private static final int FULL_VIEW_SYMBOL_DIV_PORT = 6;
    /** Maximum number of displaying candidates par one line (full view mode)(symbol)(landscape) */
    private static final int FULL_VIEW_SYMBOL_DIV_LAND = 10;
    /** Delay of set candidate */
    private static final int SET_CANDIDATE_DELAY = 50;
    /** First line count */
    private static final int SET_CANDIDATE_FIRST_LINE_COUNT = 7;
    /** Delay line count */
    private static final int SET_CANDIDATE_DELAY_LINE_COUNT = 1;

    /** Focus is none now */
    private static final int FOCUS_NONE = -1;
    /** Handler for focus Candidate */
    private static final int MSG_MOVE_FOCUS = 0;
    /** Handler for set  Candidate */
    private static final int MSG_SET_CANDIDATES = 1;
    /** Handler for select Candidate */
    private static final int MSG_SELECT_CANDIDATES = 2;

    /** NUmber of Candidate display lines */
    private static final int SETTING_NUMBER_OF_LINEMAX = 5;

    /** Body view of the candidates list */
    private ViewGroup  mViewBody = null;

    /** The view of the Symbol Tab */
    private TextView mViewTabSymbol;
    /** The view of the Emoticon Tab */
    private TextView mViewTabEmoticon;
    /** Scroller of {@code mViewBodyText} */
    private ScrollView mViewBodyScroll;
    /** Base of {@code mViewCandidateList1st}, {@code mViewCandidateList2nd} */
    private ViewGroup mViewCandidateBase;
    /** Button displayed bottom of the view when there are more candidates. */
    private ImageView mReadMoreButton;
    /** Layout for the candidates list on normal view */
    private LinearLayout mViewCandidateList1st;
    /** Layout for the candidates list on full view */
    private AbsoluteLayout mViewCandidateList2nd;
    /** View for symbol tab */
    private LinearLayout mViewCandidateListTab;
    /** {@link OpenWnn} instance using this manager */
    private OpenWnn mWnn;
    /** View type (VIEW_TYPE_NORMAL or VIEW_TYPE_FULL or VIEW_TYPE_CLOSE) */
    private int mViewType;
    /** Portrait display({@code true}) or landscape({@code false}) */
    private boolean mPortrait;

    /** Width of the view */
    private int mViewWidth;
    /** Minimum width of a candidate (density support) */
    private int mCandidateMinimumWidth;
    /** Maximum width of a candidate (density support) */
    private int mCandidateMinimumHeight;
    /** Minimum height of the category candidate view */
    private int mCandidateCategoryMinimumHeight;
    /** Left align threshold of the candidate view */
    private int mCandidateLeftAlignThreshold;
    /** Height of keyboard */
    private int mKeyboardHeight;
    /** Height of symbol keyboard */
    private int mSymbolKeyboardHeight;
    /** Height of symbol keyboard tab */
    private int mSymbolKeyboardTabHeight;
    /** Whether being able to use Emoticon */
    private boolean mEnableEmoticon = false;

    /** Whether hide the view if there is no candidate */
    private boolean mAutoHideMode = true;
    /** The converter to get candidates from and notice the selected candidate to. */
    private WnnEngine mConverter;
    /** Limitation of displaying candidates */
    private int mDisplayLimit;

    /** Vibrator for touch vibration */
    private Vibrator mVibrator = null;
    /** AudioManager for click sound */
    private AudioManager mSound = null;

    /** Number of candidates displaying for 1st */
    private int mWordCount1st;
    /** Number of candidates displaying for 2nd */
    private int mWordCount2nd;
    /** List of candidates for 1st */
    private ArrayList<WnnWord> mWnnWordArray1st = new ArrayList<WnnWord>();
    /** List of candidates for 2nd */
    private ArrayList<WnnWord> mWnnWordArray2nd = new ArrayList<WnnWord>();
    /** List of select candidates */
    private LinkedList<WnnWord> mWnnWordSelectedList = new LinkedList<WnnWord>();

    /** Gesture detector */
    private GestureDetector mGestureDetector;
    /** Character width of the candidate area */
    private int mLineLength = 0;
    /** Number of lines displayed */
    private int mLineCount = 1;

    /** {@code true} if the full screen mode is selected */
    private boolean mIsFullView = false;

    /** The event object for "touch" */
    private MotionEvent mMotionEvent = null;

    /** The offset when the candidates is flowed out the candidate window */
    private int mDisplayEndOffset = 0;
    /** {@code true} if there are more candidates to display. */
    private boolean mCanReadMore = false;
    /** Color of the candidates */
    private int mTextColor = 0;
    /** Template object for each candidate and normal/full view change button */
    private TextView mViewCandidateTemplate;
    /** Number of candidates in full view */
    private int mFullViewWordCount;
    /** Number of candidates in the current line (in full view) */
    private int mFullViewOccupyCount;
    /** View of the previous candidate (in full view) */
    private TextView mFullViewPrevView;
    /** Id of the top line view (in full view) */
    private int mFullViewPrevLineTopId;
    /** Layout of the previous candidate (in full view) */
    private ViewGroup.LayoutParams mFullViewPrevParams;
    /** Whether all candidates are displayed */
    private boolean mCreateCandidateDone;
    /** Number of lines in normal view */
    private int mNormalViewWordCountOfLine;

    /** List of textView for CandiData List 1st for Symbol mode */
    private ArrayList<TextView> mTextViewArray1st = new ArrayList<TextView>();
    /** List of textView for CandiData List 2st for Symbol mode */
    private ArrayList<TextView> mTextViewArray2nd = new ArrayList<TextView>();
    /** Now focus textView index */
    private int mCurrentFocusIndex = FOCUS_NONE;
    /** Focused View */
    private View mFocusedView = null;
    /** Focused View Background */
    private Drawable mFocusedViewBackground = null;
    /** Axis to find next TextView for Up/Down */
    private int mFocusAxisX = 0;
    /** Now focused TextView in mTextViewArray1st */
    private boolean mHasFocusedArray1st = true;

    /** Portrait Number of Lines from Preference */
    private int mPortraitNumberOfLine = LINE_NUM_PORTRAIT;
    /** Landscape Number of Lines from Preference */
    private int mLandscapeNumberOfLine = LINE_NUM_LANDSCAPE;

    /** Coordinates of line */
    private int mLineY = 0;

    /** {@code true} if the candidate is selected */
    private boolean mIsSymbolSelected = false;

    /** Whether candidates is symbol */
    private boolean mIsSymbolMode = false;

    /** Symbol mode */
    private int mSymbolMode = OpenWnnJAJP.ENGINE_MODE_SYMBOL;

    /** Text size of candidates */
    private float mCandNormalTextSize;

    /** Text size of category */
    private float mCandCategoryTextSize;

    /** HardKeyboard hidden({@code true}) or disp({@code false}) */
    private boolean mHardKeyboardHidden = true;

    /** Minimum height of the candidate 1line view */
    private int mCandidateOneLineMinimumHeight;

    /** Whether candidates long click enable */
    private boolean mEnableCandidateLongClick = true;

    /** Keyboard vertical gap value */
    private static final float KEYBOARD_VERTICAL_GAP = 0.009f;

    /** Keyboard vertical gap count */
    private static final int KEYBOARD_VERTICAL_GAP_COUNT = 3;

    /** {@code Handler} Handler for focus Candidate wait delay */
    private Handler mHandler = new Handler() {
            @Override public void handleMessage(Message msg) {
                switch (msg.what) {
                case MSG_MOVE_FOCUS:
                    moveFocus(msg.arg1, msg.arg2 == 1);
                    break;

                case MSG_SET_CANDIDATES:
                    if (mViewType == CandidatesViewManager.VIEW_TYPE_FULL && mIsSymbolMode) {
                        displayCandidates(mConverter, false, SET_CANDIDATE_DELAY_LINE_COUNT);
                    }
                    break;

                case MSG_SELECT_CANDIDATES:
                    WnnWord word = null;
                    while ((word = mWnnWordSelectedList.poll()) != null) {
                        selectCandidate(word);
                    }
                    break;

                default:
                    break;
                }
            }
        };

    /** Event listener for touching a candidate for 1st */
    private OnClickListener mCandidateOnClick1st = new OnClickListener() {
        public void onClick(View v) {
            onClickCandidate(v, mWnnWordArray1st);
        }
    };

    /** Event listener for touching a candidate for 2nd */
    private OnClickListener mCandidateOnClick2nd = new OnClickListener() {
        public void onClick(View v) {
            onClickCandidate(v, mWnnWordArray2nd);
        }
    };

    /** Event listener for long-clicking a candidate for 1st */
    private OnLongClickListener mCandidateOnLongClick1st = new OnLongClickListener() {
        public boolean onLongClick(View v) {
            return onLongClickCandidate(v, mWnnWordArray1st);
        }
    };

    /** Event listener for long-clicking a candidate for for 2nd */
    private OnLongClickListener mCandidateOnLongClick2nd = new OnLongClickListener() {
        public boolean onLongClick(View v) {
            return onLongClickCandidate(v, mWnnWordArray2nd);
        }
    };

    /** Event listener for click a symbol tab */
    private OnClickListener mTabOnClick = new OnClickListener() {
        public void onClick(View v) {
            if (!v.isShown()) {
                return;
            }
            playSoundAndVibration();

            if (v instanceof TextView) {
                TextView text = (TextView)v;
                switch (text.getId()) {
                case R.id.candview_symbol:
                    if (mSymbolMode != OpenWnnJAJP.ENGINE_MODE_SYMBOL) {
                        mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.CHANGE_MODE,
                                                      OpenWnnJAJP.ENGINE_MODE_SYMBOL));
                    }
                    break;

                case R.id.candview_emoticon:
                    if (mSymbolMode != OpenWnnJAJP.ENGINE_MODE_SYMBOL_KAO_MOJI) {
                        mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.CHANGE_MODE,
                                OpenWnnJAJP.ENGINE_MODE_SYMBOL));
                    }
                    break;

                default:
                    break;
                }
            }
        }
    };

    /**
     * Constructor
     */
    public TextCandidatesViewManager() {
        this(-1);
    }

    /**
     * Constructor
     *
     * @param displayLimit      The limit of display
     */
    public TextCandidatesViewManager(int displayLimit) {
        mDisplayLimit = displayLimit;
    }

    /**
     * Handle a click event on the candidate.
     * @param v  View
     * @param list  List of candidates
     */
    private void onClickCandidate(View v, ArrayList<WnnWord> list) {
        if (!v.isShown()) {
            return;
        }
        playSoundAndVibration();

        if (v instanceof TextView) {
            TextView text = (TextView)v;
            int wordcount = text.getId();
            WnnWord word = list.get(wordcount);
            
            if (mHandler.hasMessages(MSG_SET_CANDIDATES)) {
                mWnnWordSelectedList.add(word);
                return;
            }
            clearFocusCandidate();
            selectCandidate(word);
        }
    }

    /**
     * Handle a long click event on the candidate.
     * @param v  View
     * @param list  List of candidates
     */
    public boolean onLongClickCandidate(View v, ArrayList<WnnWord> list) {
        if (mViewLongPressDialog == null) {
            return false;
        }

        if (mIsSymbolMode) {
            return false;
        }

        if (!mEnableCandidateLongClick) {
            return false;
        }

        if (!v.isShown()) {
            return true;
        }

        Drawable d = v.getBackground();
        if (d != null) {
            if(d.getState().length == 0){
                return true;
            }
        }

        int wordcount = ((TextView)v).getId();
        mWord = list.get(wordcount);
        clearFocusCandidate();
        displayDialog(v, mWord);

        return true;
    }

    /**
     * Set auto-hide mode.
     * @param hide      {@code true} if the view will hidden when no candidate exists;
     *                  {@code false} if the view is always shown.
     */
    public void setAutoHide(boolean hide) {
        mAutoHideMode = hide;
    }

    /** @see CandidatesViewManager#initView */
    public View initView(OpenWnn parent, int width, int height) {
        mWnn = parent;
        mViewWidth = width;
        Resources r = mWnn.getResources();
        mCandidateMinimumWidth = r.getDimensionPixelSize(R.dimen.cand_minimum_width);
        mCandidateMinimumHeight = r.getDimensionPixelSize(R.dimen.cand_minimum_height);
        if (OpenWnn.isXLarge()) {
            mCandidateOneLineMinimumHeight = r.getDimensionPixelSize(R.dimen.candidate_layout_height);
        }
        mCandidateCategoryMinimumHeight = r.getDimensionPixelSize(R.dimen.cand_category_minimum_height);
        mCandidateLeftAlignThreshold = r.getDimensionPixelSize(R.dimen.cand_left_align_threshold);
        mKeyboardHeight = r.getDimensionPixelSize(R.dimen.keyboard_height);
        if (OpenWnn.isXLarge()) {
            mKeyboardHeight += Math.round(height * KEYBOARD_VERTICAL_GAP)
                                * KEYBOARD_VERTICAL_GAP_COUNT;
        }
        mSymbolKeyboardHeight = r.getDimensionPixelSize(R.dimen.symbol_keyboard_height);
        Drawable d = r.getDrawable(R.drawable.tab_no_select);
        mSymbolKeyboardTabHeight = d.getMinimumHeight();

        mPortrait =
            (r.getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE);

        mCandNormalTextSize = r.getDimensionPixelSize(R.dimen.cand_normal_text_size);
        mCandCategoryTextSize = r.getDimensionPixelSize(R.dimen.cand_category_text_size);

        LayoutInflater inflater = parent.getLayoutInflater();
        mViewBody = (ViewGroup)inflater.inflate(R.layout.candidates, null);

        mViewTabSymbol = (TextView)mViewBody.findViewById(R.id.candview_symbol);
        mViewTabEmoticon = (TextView)mViewBody.findViewById(R.id.candview_emoticon);

        mViewBodyScroll = (ScrollView)mViewBody.findViewById(R.id.candview_scroll);

        mViewCandidateBase = (ViewGroup)mViewBody.findViewById(R.id.candview_base);

        setNumeberOfDisplayLines();
        createNormalCandidateView();
        mViewCandidateList2nd = (AbsoluteLayout)mViewBody.findViewById(R.id.candidates_2nd_view);

        mTextColor = r.getColor(R.color.candidate_text);

        mReadMoreButton = (ImageView)mViewBody.findViewById(R.id.read_more_button);
        mReadMoreButton.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    int resid = 0;
                    switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mIsFullView) {
                            resid = R.drawable.cand_up_press;
                        } else {
                            resid = R.drawable.cand_down_press;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mIsFullView) {
                            resid = R.drawable.cand_up;
                        } else {
                            resid = R.drawable.cand_down;
                        }
                        break;
                    default:
                        break;
                    }

                    if (resid != 0) {
                        mReadMoreButton.setImageResource(resid);
                    }
                    return false;
                }
            });
        mReadMoreButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (!v.isShown()) {
                        return;
                    }
                    playSoundAndVibration();

                    if (mIsFullView) {
                        mIsFullView = false;
                        mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.LIST_CANDIDATES_NORMAL));
                    } else {
                        mIsFullView = true;
                        mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.LIST_CANDIDATES_FULL));
                    }
                }
            });

        setViewType(CandidatesViewManager.VIEW_TYPE_CLOSE);

        mGestureDetector = new GestureDetector(this);

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

        return mViewBody;
    }

    /**
     * Create the normal candidate view
     */
    private void createNormalCandidateView() {
        mViewCandidateList1st = (LinearLayout)mViewBody.findViewById(R.id.candidates_1st_view);
        mViewCandidateList1st.setOnClickListener(mCandidateOnClick1st);

        mViewCandidateListTab = (LinearLayout)mViewBody.findViewById(R.id.candview_tab);
        TextView tSymbol = mViewTabSymbol;
        tSymbol.setOnClickListener(mTabOnClick);
        TextView tEmoticon = mViewTabEmoticon;
        tEmoticon.setOnClickListener(mTabOnClick);

        int line = SETTING_NUMBER_OF_LINEMAX;
        int width = mViewWidth;
        for (int i = 0; i < line; i++) {
            LinearLayout lineView = new LinearLayout(mViewBodyScroll.getContext());
            lineView.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams layoutParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                              ViewGroup.LayoutParams.WRAP_CONTENT);
            lineView.setLayoutParams(layoutParams);
            for (int j = 0; j < (width / getCandidateMinimumWidth()); j++) {
                TextView tv = createCandidateView();
                lineView.addView(tv);
            }

            if (i == 0) {
                TextView tv = createCandidateView();
                layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                                             ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.weight = 0;
                layoutParams.gravity = Gravity.RIGHT;
                tv.setLayoutParams(layoutParams);

                lineView.addView(tv);
                mViewCandidateTemplate = tv;
            }
            mViewCandidateList1st.addView(lineView);
        }
    }

    /** @see CandidatesViewManager#getCurrentView */
    public View getCurrentView() {
        return mViewBody;
    }

    /** @see CandidatesViewManager#setViewType */
    public void setViewType(int type) {
        boolean readMore = setViewLayout(type);

        if (readMore) {
            displayCandidates(this.mConverter, false, -1);
        } else {
            if (type == CandidatesViewManager.VIEW_TYPE_NORMAL) {
                mIsFullView = false;
                if (mDisplayEndOffset > 0) {
                    int maxLine = getMaxLine();
                    displayCandidates(this.mConverter, false, maxLine);
                } else {
                    setReadMore();
                }
            } else {
                if (mViewBody.isShown()) {
                    mWnn.setCandidatesViewShown(false);
                }
            }
        }
    }

    /**
     * Set the view layout
     *
     * @param type      View type
     * @return          {@code true} if display is updated; {@code false} if otherwise
     */
    private boolean setViewLayout(int type) {
        
        ViewGroup.LayoutParams params;
        int line = (mPortrait) ? mPortraitNumberOfLine : mLandscapeNumberOfLine;

        if ((mViewType == CandidatesViewManager.VIEW_TYPE_FULL)
                && (type == CandidatesViewManager.VIEW_TYPE_NORMAL)) {
            clearFocusCandidate();
        }

        mViewType = type;

        switch (type) {
        case CandidatesViewManager.VIEW_TYPE_CLOSE:
            params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                                                   getCandidateMinimumHeight() * line);
            mViewBodyScroll.setLayoutParams(params);
            mViewCandidateListTab.setVisibility(View.GONE);
            mViewCandidateBase.setMinimumHeight(-1);
            mHandler.removeMessages(MSG_SET_CANDIDATES);
            return false;

        case CandidatesViewManager.VIEW_TYPE_NORMAL:
            params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                                                   getCandidateMinimumHeight() * line);
            mViewBodyScroll.setLayoutParams(params);
            mViewBodyScroll.scrollTo(0, 0);
            mViewCandidateListTab.setVisibility(View.GONE);
            mViewCandidateList1st.setVisibility(View.VISIBLE);
            mViewCandidateList2nd.setVisibility(View.GONE);
            mViewCandidateBase.setMinimumHeight(-1);
            return false;

        case CandidatesViewManager.VIEW_TYPE_FULL:
        default:
            params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                                                   getCandidateViewHeight());
            mViewBodyScroll.setLayoutParams(params);
            if (mIsSymbolMode) {
                updateSymbolType();
                mViewCandidateListTab.setVisibility(View.VISIBLE);
            } else {
                mViewCandidateListTab.setVisibility(View.GONE);
            }
            mViewCandidateList2nd.setVisibility(View.VISIBLE);
            mViewCandidateBase.setMinimumHeight(-1);
            return true;
        }
    }

    /** @see CandidatesViewManager#getViewType */
    public int getViewType() {
        return mViewType;
    }

    /** @see CandidatesViewManager#displayCandidates */
    public void displayCandidates(WnnEngine converter) {

        mHandler.removeMessages(MSG_SET_CANDIDATES);

        if (mIsSymbolSelected) {
            mIsSymbolSelected = false;
            if (mSymbolMode == OpenWnnJAJP.ENGINE_MODE_SYMBOL_KAO_MOJI) {
                return;
            }

            int prevLineCount = mLineCount;
            int prevWordCount1st = mWordCount1st;
            clearNormalViewCandidate();
            mWordCount1st = 0;
            mLineCount = 1;
            mLineLength = 0;
            mNormalViewWordCountOfLine = 0;
            mWnnWordArray1st.clear();
            mTextViewArray1st.clear();
            if (((prevWordCount1st == 0) && (mWordCount1st == 1)) ||
                (prevLineCount < mLineCount)) {
                mViewBodyScroll.scrollTo(0, mViewBodyScroll.getScrollY() + getCandidateMinimumHeight());
            }
            if (isFocusCandidate() && mHasFocusedArray1st) {
                mCurrentFocusIndex = 0;
                Message m = mHandler.obtainMessage(MSG_MOVE_FOCUS, 0, 0);
                mHandler.sendMessage(m);
            }
            return;
        }

        mCanReadMore = false;
        mDisplayEndOffset = 0;
        mIsFullView = false;
        mFullViewWordCount = 0;
        mFullViewOccupyCount = 0;
        mFullViewPrevLineTopId = 0;
        mFullViewPrevView = null;
        mCreateCandidateDone = false;
        mNormalViewWordCountOfLine = 0;

        clearCandidates();
        mConverter = converter;
        mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.LIST_CANDIDATES_NORMAL));

        mViewCandidateTemplate.setVisibility(View.VISIBLE);
        mViewCandidateTemplate.setBackgroundResource(R.drawable.cand_back);

        displayCandidates(converter, true, getMaxLine());

        if (mIsSymbolMode) {
            mIsFullView = true;
            mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.LIST_CANDIDATES_FULL));
        }
    }

    /** @see CandidatesViewManager#getMaxLine */
    private int getMaxLine() {
        int maxLine = (mPortrait) ? mPortraitNumberOfLine : mLandscapeNumberOfLine;
        return maxLine;
    }

    /**
     * Get categories text.
     * @param String Source string replacement
     * @return String Categories text
     */
    private String getCategoriesText(String categoriesString) {
        String ret = null;

        Resources r = mWnn.getResources();
        if(categoriesString.equals(r.getString(R.string.half_symbol_categories_txt))) {
            ret = r.getString(R.string.half_symbol_txt);
        } else if (categoriesString.equals(r.getString(R.string.full_symbol_categories_txt))) {
            ret = r.getString(R.string.full_symbol_txt);
        } else {
            ret = new String("");
        }

        return ret;
    }

    /**
     * Display the candidates.
     * 
     * @param converter  {@link WnnEngine} which holds candidates.
     * @param dispFirst  Whether it is the first time displaying the candidates
     * @param maxLine    The maximum number of displaying lines
     */
    private void displayCandidates(WnnEngine converter, boolean dispFirst, int maxLine) {
        if (converter == null) {
            return;
        }

        /* Concatenate the candidates already got and the last one in dispFirst mode */
        int displayLimit = mDisplayLimit;

        boolean isDelay = false;
        boolean isBreak = false;

        if (converter instanceof SymbolList) {
            if (!dispFirst) {
                if (maxLine == -1) {
                    isDelay = true;
                    maxLine = mLineCount + SET_CANDIDATE_FIRST_LINE_COUNT;

                    mHandler.sendEmptyMessageDelayed(MSG_SET_CANDIDATES, SET_CANDIDATE_DELAY);

                } else if (maxLine == SET_CANDIDATE_DELAY_LINE_COUNT) {
                    isDelay = true;
                    maxLine = mLineCount + SET_CANDIDATE_DELAY_LINE_COUNT;

                    mHandler.sendEmptyMessageDelayed(MSG_SET_CANDIDATES, SET_CANDIDATE_DELAY);
                }
            }
            if (mSymbolMode != OpenWnnJAJP.ENGINE_MODE_SYMBOL_KAO_MOJI) {
                displayLimit = -1;
            }
        }

        /* Get candidates */
        WnnWord result = null;
        String prevCandidate = null;
        while ((displayLimit == -1 || getWordCount() < displayLimit)) {
            for (int i = 0; i < DISPLAY_LINE_MAX_COUNT; i++) {
                result = converter.getNextCandidate();
                if (result == null) {
                    break;
                }

                if (converter instanceof SymbolList) {
                    break;
                }

                if ((prevCandidate == null) || !prevCandidate.equals(result.candidate)) {
                    break;
                }
            }

            if (result == null) {
                break;
            } else {
                prevCandidate = result.candidate;
            }

            if (converter instanceof SymbolList) {
                if (isCategory(result)) {
                    if (getWordCount() != 0) {
                        createNextLine(false);
                    }
                    result.candidate = getCategoriesText(result.candidate);
                    setCandidate(true, result);
                    createNextLine(true);
                    continue;
                }
            }

            setCandidate(false, result);

            if ((dispFirst || isDelay) && (maxLine < mLineCount)) {
                mCanReadMore = true;
                isBreak = true;
                break;
            }
        }

        if (!isBreak && !mCreateCandidateDone) {
            /* align left if necessary */
            createNextLine(false);
            mCreateCandidateDone = true;
            mHandler.removeMessages(MSG_SET_CANDIDATES);
            mHandler.sendMessage(mHandler.obtainMessage(MSG_SELECT_CANDIDATES));
        }

        if (getWordCount() < 1) { /* no candidates */
            if (mAutoHideMode) {
                mWnn.setCandidatesViewShown(false);
                return;
            } else {
                mCanReadMore = false;
                mIsFullView = false;
                setViewLayout(CandidatesViewManager.VIEW_TYPE_NORMAL);
            }
        }

        setReadMore();

        if (!(mViewBody.isShown())) {
            mWnn.setCandidatesViewShown(true);
        }
        return;
    }

    /**
     * Add a candidate into the list.
     * @param isCategory  {@code true}:caption of category, {@code false}:normal word
     * @param word        A candidate word
     */
    private void setCandidate(boolean isCategory, WnnWord word) {
        int textLength = measureText(word.candidate, 0, word.candidate.length());
        TextView template = mViewCandidateTemplate;
        textLength += template.getPaddingLeft() + template.getPaddingRight();
        int maxWidth = mViewWidth;
        boolean isEmojiSymbol = false;
        if (mIsSymbolMode && (word.candidate.length() < 3)) {
            isEmojiSymbol = true;
        }
        TextView textView;

        boolean is2nd = isFirstListOver(mIsFullView, mLineCount, word);
        if (is2nd) {
            /* Full view */
            int viewDivison = getCandidateViewDivison();
            int indentWidth = mViewWidth / viewDivison;
            int occupyCount = Math.min((textLength + indentWidth + getCandidateSpaceWidth(isEmojiSymbol)) / indentWidth, viewDivison);
            if (isCategory) {
                occupyCount = viewDivison;
            }

            if (viewDivison < (mFullViewOccupyCount + occupyCount)) {
                if (viewDivison != mFullViewOccupyCount) {
                    mFullViewPrevParams.width += (viewDivison - mFullViewOccupyCount) * indentWidth;
                    if (mFullViewPrevView != null) {
                        mViewCandidateList2nd.updateViewLayout(mFullViewPrevView, mFullViewPrevParams);
                    }
                }
                mFullViewOccupyCount = 0;
                if (mFullViewPrevView != null) {
                    mFullViewPrevLineTopId = mFullViewPrevView.getId();
                }
                mLineCount++;
                if (isCategory) {
                    mLineY += mCandidateCategoryMinimumHeight;
                } else {
                    mLineY += getCandidateMinimumHeight();
                }
            }
            if (mFullViewWordCount == 0) {
                mLineY = 0;
            }

            ViewGroup layout = mViewCandidateList2nd;

            int width = indentWidth * occupyCount;
            int height = getCandidateMinimumHeight();
            if (isCategory) {
                height = mCandidateCategoryMinimumHeight;
            }

            ViewGroup.LayoutParams params = buildLayoutParams(mViewCandidateList2nd, width, height);

            textView = (TextView) layout.getChildAt(mFullViewWordCount);
            if (textView == null) {
                textView = createCandidateView();
                textView.setLayoutParams(params);

                mViewCandidateList2nd.addView(textView);
            } else {
                mViewCandidateList2nd.updateViewLayout(textView, params);
            }

            mFullViewOccupyCount += occupyCount;
            mFullViewWordCount++;
            mFullViewPrevView = textView;
            mFullViewPrevParams = params;

        } else {
            int viewDivison = getCandidateViewDivison();
            int indentWidth = mViewWidth / viewDivison;
            textLength = Math.max(textLength, indentWidth);

            /* Normal view */
            int nextEnd = mLineLength + textLength;
            nextEnd += getCandidateSpaceWidth(isEmojiSymbol);

            if (mLineCount == 1 && !mIsSymbolMode) {
                maxWidth -= getCandidateMinimumWidth();
            }

            if ((maxWidth < nextEnd) && (getWordCount() != 0)) {

                createNextLineFor1st();
                if (getMaxLine() < mLineCount) {
                    mLineLength = 0;
                    /* Call this method again to add the candidate in the full view */
                    if (!mIsSymbolSelected) {
                        setCandidate(isCategory, word);
                    }
                    return;
                }
                
                mLineLength = textLength;
                mLineLength += getCandidateSpaceWidth(isEmojiSymbol);
            } else {
                mLineLength = nextEnd;
            }

            LinearLayout lineView = (LinearLayout) mViewCandidateList1st.getChildAt(mLineCount - 1);
            textView = (TextView) lineView.getChildAt(mNormalViewWordCountOfLine);

            if (isCategory) {
                if (mLineCount == 1) {
                    mViewCandidateTemplate.setBackgroundDrawable(null);
                }
                mLineLength += mCandidateLeftAlignThreshold;
            } else {
                int CompareLength = textLength;
                CompareLength += getCandidateSpaceWidth(isEmojiSymbol);
            }

            mNormalViewWordCountOfLine++;
        }

        textView.setText(word.candidate);
        if (is2nd) {
            textView.setId(mWordCount2nd);
        } else {
            textView.setId(mWordCount1st);
        }
        textView.setVisibility(View.VISIBLE);
        textView.setPressed(false);
        textView.setFocusable(false);

        if (isCategory) {
            textView.setText("      " + word.candidate);

            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mCandCategoryTextSize);
            textView.setBackgroundDrawable(null);
            textView.setGravity(Gravity.CENTER_VERTICAL);
            textView.setMinHeight(mCandidateCategoryMinimumHeight);
            textView.setHeight(mCandidateCategoryMinimumHeight);

            textView.setOnClickListener(null);
            textView.setOnLongClickListener(null);
            textView.setTextColor(mTextColor);
        } else {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mCandNormalTextSize);
            textView.setGravity(Gravity.CENTER);
            textView.setMinHeight(getCandidateMinimumHeight());
            textView.setHeight(getCandidateMinimumHeight());

            if (is2nd) {
                textView.setOnClickListener(mCandidateOnClick2nd);
                textView.setOnLongClickListener(mCandidateOnLongClick2nd);
            } else {
                textView.setOnClickListener(mCandidateOnClick1st);
                textView.setOnLongClickListener(mCandidateOnLongClick1st);
            }

            textView.setBackgroundResource(R.drawable.cand_back);
 
            textView.setTextColor(mTextColor);
        }

        if (maxWidth < textLength) {
            textView.setEllipsize(TextUtils.TruncateAt.END);
        } else {
            textView.setEllipsize(null);
        }

        ImageSpan span = null;
        if (word.candidate.equals(" ")) {
            span = new ImageSpan(mWnn, R.drawable.word_half_space,
                                 DynamicDrawableSpan.ALIGN_BASELINE);
        } else if (word.candidate.equals("\u3000" /* full-width space */)) {
            span = new ImageSpan(mWnn, R.drawable.word_full_space,
                                 DynamicDrawableSpan.ALIGN_BASELINE);
        }

        if (span != null) {
            SpannableString spannable = new SpannableString("   ");
            spannable.setSpan(span, 1, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            textView.setText(spannable);
        }
        textView.setPadding(0, 0, 0, 0);

        if (is2nd) {
            mWnnWordArray2nd.add(mWordCount2nd, word);
            mWordCount2nd++;
            mTextViewArray2nd.add(textView);
        } else {
            mWnnWordArray1st.add(mWordCount1st, word);
            mWordCount1st++;
            mTextViewArray1st.add(textView);
        }
    }

    /**
     * Create AbsoluteLayout.LayoutParams
     * @param layout AbsoluteLayout
     * @param width  The width of the display
     * @param height The height of the display
     * @return Layout parameter
     */
    private ViewGroup.LayoutParams buildLayoutParams(AbsoluteLayout layout, int width, int height) {

        int viewDivison = getCandidateViewDivison();
        int indentWidth = mViewWidth / viewDivison;
        int x         = indentWidth * mFullViewOccupyCount;
        int y         = mLineY;
        ViewGroup.LayoutParams params
              = new AbsoluteLayout.LayoutParams(width, height, x, y);

        return params;
    }

    /**
     * Create a view for a candidate.
     * @return the view
     */
    private TextView createCandidateView() {
        TextView text = new CandidateTextView(mViewBodyScroll.getContext());
        text.setTextSize(TypedValue.COMPLEX_UNIT_PX, mCandNormalTextSize);
        text.setBackgroundResource(R.drawable.cand_back);
        text.setCompoundDrawablePadding(0);
        text.setGravity(Gravity.CENTER);
        text.setSingleLine();
        text.setPadding(0, 0, 0, 0);
        text.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                                           ViewGroup.LayoutParams.WRAP_CONTENT,
                                                           1.0f));
        text.setMinHeight(getCandidateMinimumHeight());
        text.setMinimumWidth(getCandidateMinimumWidth());
        text.setSoundEffectsEnabled(false);
        return text;
    }

    /**
     * Display {@code mReadMoreText} if there are more candidates.
     */
    private void setReadMore() {
        if (mIsSymbolMode) {
            mReadMoreButton.setVisibility(View.GONE);
            mViewCandidateTemplate.setVisibility(View.GONE);
            return;
        }

        int resid = 0;

        if (mIsFullView) {
            mReadMoreButton.setVisibility(View.VISIBLE);
            resid = R.drawable.cand_up;
        } else {
            if (mCanReadMore) {
                mReadMoreButton.setVisibility(View.VISIBLE);
                resid = R.drawable.cand_down;
            } else {
                mReadMoreButton.setVisibility(View.GONE);
                mViewCandidateTemplate.setVisibility(View.GONE);
            }
        }

        if (resid != 0) {
            mReadMoreButton.setImageResource(resid);
        }
    }

    /**
     * Clear the list of the normal candidate view.
     */
    private void clearNormalViewCandidate() {
        LinearLayout candidateList = mViewCandidateList1st;
        int lineNum = candidateList.getChildCount();
        for (int i = 0; i < lineNum; i++) {

            LinearLayout lineView = (LinearLayout)candidateList.getChildAt(i);
            int size = lineView.getChildCount();
            for (int j = 0; j < size; j++) {
                View v = lineView.getChildAt(j);
                v.setVisibility(View.GONE);
            }
        }
    }

    /** @see CandidatesViewManager#clearCandidates */
    public void clearCandidates() {
        closeDialog();
        clearFocusCandidate();
        clearNormalViewCandidate();

        ViewGroup layout = mViewCandidateList2nd;
        int size = layout.getChildCount();
        for (int i = 0; i < size; i++) {
            View v = layout.getChildAt(i);
            v.setVisibility(View.GONE);
        }

        mLineCount = 1;
        mWordCount1st = 0;
        mWordCount2nd = 0;
        mWnnWordArray1st.clear();
        mWnnWordArray2nd.clear();
        mTextViewArray1st.clear();
        mTextViewArray2nd.clear();
        mLineLength = 0;

        mLineY = 0;

        mIsFullView = false;
        mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.LIST_CANDIDATES_NORMAL));
        if (mAutoHideMode) {
            setViewLayout(CandidatesViewManager.VIEW_TYPE_CLOSE);
        }

        if (mAutoHideMode && mViewBody.isShown()) {
            mWnn.setCandidatesViewShown(false);
        }
        mCanReadMore = false;
        setReadMore();
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
            setNumeberOfDisplayLines();
        } catch (Exception ex) {
            Log.e("OpenWnn", "NO VIBRATOR");
        }
    }
    
    /**
     * Set normal mode.
     */
    public void setNormalMode() {
        setReadMore();
        mIsFullView = false;
        mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.LIST_CANDIDATES_NORMAL));
    }

    /**
     * Set full mode.
     */
    public void setFullMode() {
        mIsFullView = true;
        mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.LIST_CANDIDATES_FULL));
    }

    /**
     * Set symbol mode.
     */
    public void setSymbolMode(boolean enable, int mode) {
        if (mIsSymbolMode && !enable) {
            setViewType(CandidatesViewManager.VIEW_TYPE_CLOSE);
        }
        mSymbolMode = mode;
        mIsSymbolMode = enable;
    }

    /**
     * Set scroll up.
     */
    public void setScrollUp() {
        if (!mViewBodyScroll.pageScroll(ScrollView.FOCUS_UP)) {
            mViewBodyScroll.scrollTo(0, mViewBodyScroll.getChildAt(0).getHeight());
        }
    }

    /**
     * Set scroll down.
     */
    public void setScrollDown() {
        if (!mViewBodyScroll.pageScroll(ScrollView.FOCUS_DOWN)) {
            mViewBodyScroll.scrollTo(0, 0);
        }
    }

    /**
     * Set scroll full up.
     */
    public void setScrollFullUp() {
        if (!mViewBodyScroll.fullScroll(ScrollView.FOCUS_UP)) {
            mViewBodyScroll.scrollTo(0, mViewBodyScroll.getChildAt(0).getHeight());
        }
    }

    /**
     * Set scroll full down.
     */
    public void setScrollFullDown() {
        if (!mViewBodyScroll.fullScroll(ScrollView.FOCUS_DOWN)) {
            mViewBodyScroll.scrollTo(0, 0);
        }
    }

    /**
     * Process {@link OpenWnnEvent#CANDIDATE_VIEW_TOUCH} event.
     *
     * @return      {@code true} if event is processed; {@code false} if otherwise
     */
    public boolean onTouchSync() {
        return mGestureDetector.onTouchEvent(mMotionEvent);
    }

    /**
     * Select a candidate.
     * <br>
     * This method notices the selected word to {@link OpenWnn}.
     *
     * @param word  The selected word
     */
    private void selectCandidate(WnnWord word) {
        if (!mIsSymbolMode) {
            mIsFullView = false;
            mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.LIST_CANDIDATES_NORMAL));
        }
        mIsSymbolSelected = mIsSymbolMode;
        mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.SELECT_CANDIDATE, word));
    }

    private void playSoundAndVibration() {
        if (mVibrator != null) {
            try {
                mVibrator.vibrate(5);
            } catch (Exception ex) {
                Log.e("OpenWnn", "TextCandidatesViewManager::selectCandidate Vibrator " + ex.toString());
            }
        }
        if (mSound != null) {
            try {
                mSound.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, -1);
            } catch (Exception ex) {
                Log.e("OpenWnn", "TextCandidatesViewManager::selectCandidate Sound " + ex.toString());
            }
        }
    }

    /** @see android.view.GestureDetector.OnGestureListener#onDown */
    public boolean onDown(MotionEvent arg0) {
        return false;
    }

    /** @see android.view.GestureDetector.OnGestureListener#onFling */
    public boolean onFling(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) {
        boolean consumed = false;
        if (arg1 != null && arg0 != null && arg1.getY() < arg0.getY()) {
            if ((mViewType == CandidatesViewManager.VIEW_TYPE_NORMAL) && mCanReadMore) {
                if (mVibrator != null) {
                    try {
                        mVibrator.vibrate(5);
                    } catch (Exception ex) {
                        Log.e("iwnn", "TextCandidatesViewManager::onFling Vibrator " + ex.toString());
                    }
                }
                mIsFullView = true;
                mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.LIST_CANDIDATES_FULL));
                consumed = true;
            }
        } else {
            if (mViewBodyScroll.getScrollY() == 0) {
                if (mVibrator != null) {
                    try {
                        mVibrator.vibrate(5);
                    } catch (Exception ex) {
                        Log.e("iwnn", "TextCandidatesViewManager::onFling Sound " + ex.toString());
                    }
                }
                mIsFullView = false;
                mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.LIST_CANDIDATES_NORMAL));
                consumed = true;
            }
        }

        return consumed;
    }

    /** @see android.view.GestureDetector.OnGestureListener#onLongPress */
    public void onLongPress(MotionEvent arg0) {
        return;
    }

    /** @see android.view.GestureDetector.OnGestureListener#onScroll */
    public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) {
        return false;
    }

    /** @see android.view.GestureDetector.OnGestureListener#onShowPress */
    public void onShowPress(MotionEvent arg0) {
    }

    /** @see android.view.GestureDetector.OnGestureListener#onSingleTapUp */
    public boolean onSingleTapUp(MotionEvent arg0) {
        return false;
    }
    
    /**
     * Retrieve the width of string to draw.
     * 
     * @param text          The string
     * @param start         The start position (specified by the number of character)
     * @param end           The end position (specified by the number of character)
     * @return          The width of string to draw
     */ 
    public int measureText(CharSequence text, int start, int end) {
        if (end - start < 3) {
            return getCandidateMinimumWidth();
        }

        TextPaint paint = mViewCandidateTemplate.getPaint();
        return (int)paint.measureText(text, start, end);
    }

    /**
     * Create a layout for the next line.
     */
    private void createNextLine(boolean isCategory) {
        if (isFirstListOver(mIsFullView, mLineCount, null)) {
            /* Full view */
            mFullViewOccupyCount = 0;
            if (mFullViewPrevView != null) {
                mFullViewPrevLineTopId = mFullViewPrevView.getId();
            }
            if (isCategory) {
                mLineY += mCandidateCategoryMinimumHeight;
            } else {
                mLineY += getCandidateMinimumHeight();
            }
            mLineCount++;
        } else {
            createNextLineFor1st();
        }
    }

    /**
     * Create a layout for the next line.
     */
    private void createNextLineFor1st() {
        LinearLayout lineView = (LinearLayout) mViewCandidateList1st.getChildAt(mLineCount - 1);
        float weight = 0;
        if (mLineLength < mCandidateLeftAlignThreshold) {
            if (mLineCount == 1) {
                mViewCandidateTemplate.setVisibility(View.GONE);
            }
        } else {
            weight = 1.0f;
        }

        LinearLayout.LayoutParams params
            = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                            ViewGroup.LayoutParams.WRAP_CONTENT,
                                            weight);

        int child = lineView.getChildCount();
        for (int i = 0; i < child; i++) {
            View view = lineView.getChildAt(i);

            if (view != mViewCandidateTemplate) {
                view.setLayoutParams(params);
                view.setPadding(0, 0, 0, 0);
            }
        }

        mLineLength = 0;
        mNormalViewWordCountOfLine = 0;
        mLineCount++;
    }

    /**
     * Judge if it's a category.
     *
     * @return {@code true} if category
     */
    boolean isCategory(WnnWord word) {
        int length = word.candidate.length();
        return ((length > 3) && (word.candidate.charAt(0) == '['));
    }

    /**
     * Get a minimum width of a candidate view.
     *
     * @return the minimum width of a candidate view.
     */
    private int getCandidateMinimumWidth() {
        return mCandidateMinimumWidth;
    }

    /**
     * @return the minimum height of a candidate view.
     */
    private int getCandidateMinimumHeight() {
        return mCandidateMinimumHeight;
    }

    /**
     * Get a height of a candidate view.
     *
     * @return the height of a candidate view.
     */
    private int getCandidateViewHeight() {
        if (OpenWnn.isXLarge()) {
           return mKeyboardHeight + mCandidateOneLineMinimumHeight - mSymbolKeyboardHeight
                         - mSymbolKeyboardTabHeight;
        } else {
            int numberOfLine = (mPortrait) ? mPortraitNumberOfLine : mLandscapeNumberOfLine;
            Resources resource = mWnn.getResources();
            Drawable keyboardBackground = resource.getDrawable(R.drawable.keyboard_background);
            Rect keyboardPadding = new Rect(0 ,0 ,0 ,0);
            keyboardBackground.getPadding(keyboardPadding);
            int keyboardTotalPadding = keyboardPadding.top + keyboardPadding.bottom;
            if (mIsSymbolMode) {
                return mKeyboardHeight + numberOfLine * getCandidateMinimumHeight()
                       - mSymbolKeyboardHeight - mSymbolKeyboardTabHeight;
            } else if (!mHardKeyboardHidden) {
                return mKeyboardHeight + numberOfLine * getCandidateMinimumHeight()
                       - mSymbolKeyboardHeight;
            } else {
                return mKeyboardHeight + keyboardTotalPadding
                       + numberOfLine * getCandidateMinimumHeight();
            }
        }
    }

    /**
     * Update symbol type.
     */
    private void updateSymbolType() {
        switch (mSymbolMode) {
        case OpenWnnJAJP.ENGINE_MODE_SYMBOL:
            updateTabStatus(mViewTabSymbol, true, true);
            updateTabStatus(mViewTabEmoticon, mEnableEmoticon, false);
            break;

        case OpenWnnJAJP.ENGINE_MODE_SYMBOL_KAO_MOJI:
            updateTabStatus(mViewTabSymbol, true, false);
            updateTabStatus(mViewTabEmoticon, mEnableEmoticon, true);
            break;

        default:
            updateTabStatus(mViewTabSymbol, true, false);
            updateTabStatus(mViewTabEmoticon, mEnableEmoticon, false);
            break;
        }
    }

    /**
     * Update tab status.
     *
     * @param tab           The tab view.
     * @param enabled       The tab is enabled.
     * @param selected      The tab is selected.
     */
    private void updateTabStatus(TextView tab, boolean enabled, boolean selected) {
        tab.setVisibility(View.VISIBLE);
        tab.setEnabled(enabled);
        int backgroundId = 0;
        int colorId = 0;
        if (enabled) {
            if (selected) {
                backgroundId = R.drawable.cand_tab;
                colorId = R.color.tab_textcolor_select;
            } else {
                backgroundId = R.drawable.cand_tab_noselect;
                colorId = R.color.tab_textcolor_no_select;
            }
        } else {
            backgroundId = R.drawable.cand_tab_noselect;
            colorId = R.color.tab_textcolor_disable;
        }
        tab.setBackgroundResource(backgroundId);
        tab.setTextColor(mWnn.getResources().getColor(colorId));
    }

    /**
     * Get candidate number of division.
     * @return Number of division
     */
    private int getCandidateViewDivison() {
        int viewDivison;

        if (mIsSymbolMode) {
            int mode = mSymbolMode;
            switch (mode) {
            case OpenWnnJAJP.ENGINE_MODE_SYMBOL:
                viewDivison = (mPortrait) ? FULL_VIEW_SYMBOL_DIV_PORT : FULL_VIEW_SYMBOL_DIV_LAND;
                break;
            case OpenWnnJAJP.ENGINE_MODE_SYMBOL_KAO_MOJI:
            default:
                viewDivison = FULL_VIEW_DIV;
                break;
            }
        } else {
             viewDivison = FULL_VIEW_DIV;
        }
        return viewDivison;
    }

    /**
     * @return Word count
     */
    private int getWordCount() {
        return mWordCount1st + mWordCount2nd;
    }

    /**
     * @return Add second
     */
    private boolean isFirstListOver(boolean isFullView, int lineCount, WnnWord word) {

        if (mIsSymbolMode) {
            switch (mSymbolMode) {
            case OpenWnnJAJP.ENGINE_MODE_SYMBOL_KAO_MOJI:
                return true;
            case OpenWnnJAJP.ENGINE_MODE_SYMBOL:
				return true;
            default:
                return (isFullView || getMaxLine() < lineCount);
            }
        } else {
            return (isFullView || getMaxLine() < lineCount);
        }
    }

    /**
     * @return Candidate space width
     */
    private int getCandidateSpaceWidth(boolean isEmojiSymbol) {
        Resources r = mWnn.getResources();
        if (mPortrait) {
            if (isEmojiSymbol) {
                return 0;
            } else {
                return r.getDimensionPixelSize(R.dimen.cand_space_width);
            }
        } else {
            if (isEmojiSymbol) {
                return r.getDimensionPixelSize(R.dimen.cand_space_width_emoji_symbol);
            } else {
                return r.getDimensionPixelSize(R.dimen.cand_space_width);
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
        case KeyEvent.KEYCODE_DPAD_UP:
            moveFocus(-1, true);
            break;

        case KeyEvent.KEYCODE_DPAD_DOWN:
            moveFocus(1, true);
            break;

        case KeyEvent.KEYCODE_DPAD_LEFT:
            moveFocus(-1, false);
            break;

        case KeyEvent.KEYCODE_DPAD_RIGHT:
            moveFocus(1, false);
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
    public void setViewStatusOfFocusedCandidate(){
        View view = mFocusedView;
        if (view != null) {
            view.setBackgroundDrawable(mFocusedViewBackground);
            view.setPadding(0, 0, 0, 0);
        }

        TextView v = getFocusedView();
        mFocusedView = v;
        if (v != null) {
            mFocusedViewBackground = v.getBackground();
            v.setBackgroundResource(R.drawable.cand_back_focuse);
            v.setPadding(0, 0, 0, 0);

            int viewBodyTop = getViewTopOnScreen(mViewBodyScroll);
            int viewBodyBottom = viewBodyTop + mViewBodyScroll.getHeight();
            int focusedViewTop = getViewTopOnScreen(v);
            int focusedViewBottom = focusedViewTop + v.getHeight();

            if (focusedViewBottom > viewBodyBottom) {
                mViewBodyScroll.scrollBy(0, (focusedViewBottom - viewBodyBottom));
            } else if (focusedViewTop < viewBodyTop) {
                mViewBodyScroll.scrollBy(0, (focusedViewTop - viewBodyTop));
            }
        }
    }

    /**
     * Clear focus to selected candidate.
     */
    public void clearFocusCandidate(){
        View view = mFocusedView;
        if (view != null) {
            view.setBackgroundDrawable(mFocusedViewBackground);
            view.setPadding(0, 0, 0, 0);
            mFocusedView = null;
        }

        mFocusAxisX = 0;
        mHasFocusedArray1st = true;
        mCurrentFocusIndex = FOCUS_NONE;
        mHandler.removeMessages(MSG_MOVE_FOCUS);
        mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.FOCUS_CANDIDATE_END));
    }

    /**
     * @see CandidatesViewManager#selectFocusCandidate
     */
    public void selectFocusCandidate(){
        if (mCurrentFocusIndex != FOCUS_NONE) {
            WnnWord word = getFocusedWnnWord();

            if (mHandler.hasMessages(MSG_SET_CANDIDATES)) {
                mWnnWordSelectedList.add(word);
            } else {
                selectCandidate(word);
            }
        }
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
        WnnWord word = null;
        if (index < 0) {
            index = 0;
            mHandler.removeMessages(MSG_MOVE_FOCUS);
            Log.i("iwnn", "TextCandidatesViewManager::getWnnWord  index < 0 ");
        } else {
            int size = mHasFocusedArray1st ? mWnnWordArray1st.size() : mWnnWordArray2nd.size();
            if (index >= size) {
                index = size - 1;
                mHandler.removeMessages(MSG_MOVE_FOCUS);
                Log.i("iwnn", "TextCandidatesViewManager::getWnnWord  index > candidate max ");
            }
        }
     
        if (mHasFocusedArray1st) {
            word = mWnnWordArray1st.get(index);
        } else {
            word = mWnnWordArray2nd.get(index);
        }
        return word;
    }

    /**
     * Set display candidate line from SharedPreferences.
     */
    private void setNumeberOfDisplayLines(){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mWnn);
        mPortraitNumberOfLine = Integer.parseInt(pref.getString("setting_portrait", "2"));
        mLandscapeNumberOfLine = Integer.parseInt(pref.getString("setting_landscape", "1"));
    }

    /**
     * Set emoticon enabled.
     */
    public void setEnableEmoticon(boolean enableEmoticon) {
        mEnableEmoticon = enableEmoticon;
    }

    /**
     * Get View of focus candidate.
     */
    public TextView getFocusedView() {
        if (mCurrentFocusIndex == FOCUS_NONE) {
            return null;
        }
        TextView t;
        if (mHasFocusedArray1st) {
            t = mTextViewArray1st.get(mCurrentFocusIndex);
        } else {
            t = mTextViewArray2nd.get(mCurrentFocusIndex);
        }
        return t;
    }

    /**
     * Move the focus to next candidate.
     *
     * @param direction  The direction of increment or decrement.
     * @param updown     {@code true} if move is up or down.
     */
    public void moveFocus(int direction, boolean updown) {
        boolean isStart = (mCurrentFocusIndex == FOCUS_NONE);
        if (direction == 0) {
            setViewStatusOfFocusedCandidate();
        }

        int size1st = mTextViewArray1st.size();
        if (mHasFocusedArray1st && (size1st == 0)) {
            mHasFocusedArray1st = false;
        }
        ArrayList<TextView> list = mHasFocusedArray1st ? mTextViewArray1st : mTextViewArray2nd;
        int size = list.size();
        int start = (mCurrentFocusIndex == FOCUS_NONE) ? 0 : (mCurrentFocusIndex + direction);

        int index = -1;
        boolean hasChangedLine = false;
        for (int i = start; (0 <= i) && (i < size); i += direction) {
            TextView view = list.get(i);
            if (!view.isShown()) {
                break;
            }

            if (mIsSymbolMode && (view.getBackground() == null)) {
                continue;
            }

            if (updown) {
                int left = view.getLeft();
                if ((left <= mFocusAxisX)
                        && (mFocusAxisX < view.getRight())) {
                    index = i;
                    break;
                }

                if (left == 0) {
                    hasChangedLine = true;
                }
            } else {
                index = i;
                break;
            }
        }

        if ((index < 0) && hasChangedLine && !mHasFocusedArray1st && (0 < direction)) {
            index = mTextViewArray2nd.size() - 1;
        }

        if (0 <= index) {
            mCurrentFocusIndex = index;
            setViewStatusOfFocusedCandidate();
            if (!updown) {
                mFocusAxisX = getFocusedView().getLeft();
            }
        } else {
            if (mCanReadMore && (0 < size1st)) {

                if ((mHasFocusedArray1st && (direction < 0))
                        || (!mHasFocusedArray1st && (0 < direction))) {
                    updown = false;
                }

                mHasFocusedArray1st = !mHasFocusedArray1st;

                if (!mHasFocusedArray1st && !mIsFullView) {
                    setFullMode();
                }
            }

            if (size1st == 0) {
                updown = false;
            }

            if (0 < direction) {
                mCurrentFocusIndex = -1;
            } else {
                mCurrentFocusIndex = (mHasFocusedArray1st ? size1st : mTextViewArray2nd.size());
            }
            Message m = mHandler.obtainMessage(MSG_MOVE_FOCUS, direction, updown ? 1 : 0);
            mHandler.sendMessage(m);
        }

        if (isStart) {
            mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.FOCUS_CANDIDATE_START));
        }
    }

    /**
     * Set hardkeyboard hidden.
     *
     * @param hardKeyboardHidden hardkeyaboard hidden.
     */
    public void setHardKeyboardHidden(boolean hardKeyboardHidden) {
        mHardKeyboardHidden = hardKeyboardHidden;
    }

    /**
     * Get view top position on screen.
     *
     * @param view target view.
     * @return int view top position on screen
     */
    public int getViewTopOnScreen(View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        return location[1];
    }


    /** @see CandidatesViewManager#setCandidateMsgRemove */
    public void setCandidateMsgRemove() {
        mHandler.removeMessages(MSG_SET_CANDIDATES);
    }
}
