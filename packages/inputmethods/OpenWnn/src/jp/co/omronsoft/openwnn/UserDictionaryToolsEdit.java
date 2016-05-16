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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The abstract class for user dictionary's word editor.
 * 
 * @author Copyright (C) 2009, OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
public abstract class UserDictionaryToolsEdit extends Activity implements View.OnClickListener {
    /** The class information for intent(Set this informations in the extend class) */
    protected String  mListViewName;
    /** The class information for intent(Set this informations in the extend class) */
    protected String  mPackageName;

    /** The operation mode (Unknown) */
    private static final int STATE_UNKNOWN = 0;
    /** The operation mode (Add the word) */
    private static final int STATE_INSERT = 1;
    /** The operation mode (Edit the word) */
    private static final int STATE_EDIT = 2;

    /** Maximum length of a word's string */
    private static final int MAX_TEXT_SIZE = 20;

    /** The error code (Already registered the same word) */
    private static final int RETURN_SAME_WORD = -11;

    /** The focus view and pair view */
    private static View sFocusingView = null;
    private static View sFocusingPairView = null;

    /** Widgets which constitute this screen of activity */
    private EditText mReadEditText;
    private EditText mCandidateEditText;
    private Button mEntryButton;
    private Button mCancelButton;

    /** The word information which contains the previous information */
    private WnnWord mBeforeEditWnnWord;
    /** The instance of word list activity */
    private UserDictionaryToolsList mListInstance;

    /** The constant for notifying dialog (Already exists the specified word) */
    private static final int DIALOG_CONTROL_WORDS_DUPLICATE = 0;
    /** The constant for notifying dialog (The length of specified stroke or candidate exceeds the limit) */
    private static final int DIALOG_CONTROL_OVER_MAX_TEXT_SIZE = 1;

    /** The operation mode of this activity */
    private int mRequestState;

    /**
     * Constructor
     */
    public UserDictionaryToolsEdit() {
        super();
    }

    /**
     * Constructor
     *
     * @param  focusView      The information of view
     * @param  focusPairView  The information of pair of view
     */
    public UserDictionaryToolsEdit(View focusView, View focusPairView) {
        super();
        sFocusingView = focusView;
        sFocusingPairView = focusPairView;
    }
    
    /**
     * Send the specified event to IME
     *
     * @param ev    The event object
     * @return      {@code true} if this event is processed.
     */
    protected abstract boolean sendEventToIME(OpenWnnEvent ev);

    /** @see android.app.Activity#onCreate */
    @Override protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        /* create view from XML layout */
        setContentView(R.layout.user_dictionary_tools_edit);

        /* get widgets */
        mEntryButton = (Button)findViewById(R.id.addButton);
        mCancelButton = (Button)findViewById(R.id.cancelButton);
        mReadEditText = (EditText)findViewById(R.id.editRead);
        mCandidateEditText = (EditText)findViewById(R.id.editCandidate);

        /* set the listener */
        mEntryButton.setOnClickListener(this);
        mCancelButton.setOnClickListener(this);

        /* initialize */
        mRequestState = STATE_UNKNOWN;
        mReadEditText.setSingleLine();
        mCandidateEditText.setSingleLine();
        
        /* get the request and do it */
        Intent intent = getIntent();
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_INSERT)) {
            /* add a word */
            mEntryButton.setEnabled(false);
            mRequestState = STATE_INSERT;
        } else if (action.equals(Intent.ACTION_EDIT)) {
            /* edit a word */
            mEntryButton.setEnabled(true);
            mReadEditText.setText(((TextView)sFocusingView).getText());
            mCandidateEditText.setText(((TextView)sFocusingPairView).getText());
            mRequestState = STATE_EDIT;

            /* save the word's information before this edit */
            mBeforeEditWnnWord = new WnnWord();
            mBeforeEditWnnWord.stroke = ((TextView)sFocusingView).getText().toString();
            mBeforeEditWnnWord.candidate = ((TextView)sFocusingPairView).getText().toString();
        } else {
            /* finish if it is unknown request */
            Log.e("OpenWnn", "onCreate() : Invaled Get Intent. ID=" + intent);
            finish();
            return;
        }

        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
                                  R.layout.user_dictionary_tools_edit_header);

        /* set control buttons */
        setAddButtonControl();

    }

    /** @see android.app.Activity#onKeyDown */
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            /* go back to the word list view */
            screenTransition();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Change the state of the "Add" button into the depending state of input area.
     */
    public void setAddButtonControl() {

        /* Text changed listener for the reading text */
        mReadEditText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                /* Enable/disable the "Add" button */
                if ((mReadEditText.getText().toString().length() != 0) && 
                    (mCandidateEditText.getText().toString().length() != 0)) {
                    mEntryButton.setEnabled(true);
                } else {
                    mEntryButton.setEnabled(false);
                }
            }
        });
        /* Text changed listener for the candidate text */
        mCandidateEditText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            	/* Enable/disable the "Add" button */
                if ((mReadEditText.getText().toString().length() != 0) && 
                    (mCandidateEditText.getText().toString().length() != 0)) {
                    mEntryButton.setEnabled(true);
                } else {
                    mEntryButton.setEnabled(false);
                }
            }
        });

    }

    /** @see android.view.View.OnClickListener */
    public void onClick(View v) {

        mEntryButton.setEnabled(false);
        mCancelButton.setEnabled(false);

        switch (v.getId()) {
            case R.id.addButton:
                /* save the word */
                doSaveAction();
                break;
 
            case R.id.cancelButton:
                /* cancel the edit */
                doRevertAction();
                break;

            default:
                Log.e("OpenWnn", "onClick: Get Invalid ButtonID. ID=" + v.getId());
                finish();
                return;
        }
    }

    /**
     * Process the adding or editing action
     */
    private void doSaveAction() {

        switch (mRequestState) {
        case STATE_INSERT:
            /* register a word */
            if (inputDataCheck(mReadEditText) && inputDataCheck(mCandidateEditText)) {
                    String stroke = mReadEditText.getText().toString();
                    String candidate = mCandidateEditText.getText().toString();
                    if (addDictionary(stroke, candidate)) {
                        screenTransition();
                    }
                }
            break;
            
        case STATE_EDIT:
            /* edit a word (=delete the word selected & add the word edited) */
            if (inputDataCheck(mReadEditText) && inputDataCheck(mCandidateEditText)) {
                deleteDictionary(mBeforeEditWnnWord);
                    String stroke = mReadEditText.getText().toString();
                    String candidate = mCandidateEditText.getText().toString();
                    if (addDictionary(stroke, candidate)) {
                        screenTransition();
                    } else {
                        addDictionary(mBeforeEditWnnWord.stroke, mBeforeEditWnnWord.candidate);
                    }
                }
            break;

        default:
            Log.e("OpenWnn", "doSaveAction: Invalid Add Status. Status=" + mRequestState);
            break;
        }
    }
    
    /**
     * Process the cancel action
     */
    private void doRevertAction() {
        /* go back to the words list */
        screenTransition();
    }

    /**
     * Create the alert dialog for notifying the error
     *
     * @param  id        The dialog ID
     * @return           The information of the dialog
     */
    @Override protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_CONTROL_WORDS_DUPLICATE:
                /* there is the same word in the dictionary */
                return new AlertDialog.Builder(UserDictionaryToolsEdit.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(R.string.user_dictionary_words_duplication_message)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                mEntryButton.setEnabled(true);
                                mCancelButton.setEnabled(true);
                            }
                        })
                        .setCancelable(true)
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            public void onCancel(DialogInterface dialog) {
                                mEntryButton.setEnabled(true);
                                mCancelButton.setEnabled(true);
                            }
                        })
                        .create();

            case DIALOG_CONTROL_OVER_MAX_TEXT_SIZE:
                /* the length of the word exceeds the limit */
                return new AlertDialog.Builder(UserDictionaryToolsEdit.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(R.string.user_dictionary_over_max_text_size_message)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int witchButton) {
                                mEntryButton.setEnabled(true);
                                mCancelButton.setEnabled(true);
                            }
                        })
                        .setCancelable(true)
                        .create();
        }
        return super.onCreateDialog(id);
    }

    /**
     * Add the word
     *
     * @param  stroke       The stroke of the word
     * @param  candidate    The string of the word
     * @return              {@code true} if success; {@code false} if fail.
     */
    private boolean addDictionary(String stroke, String candidate) {
        boolean ret;

        /* create WnnWord from the strings */
        WnnWord wnnWordAdd = new WnnWord();
        wnnWordAdd.stroke = stroke;
        wnnWordAdd.candidate = candidate;
        /* add word event */
        OpenWnnEvent event = new OpenWnnEvent(OpenWnnEvent.ADD_WORD,
                                  WnnEngine.DICTIONARY_TYPE_USER,
                                  wnnWordAdd);
        /* notify the event to IME */
        ret = sendEventToIME(event);
        if (ret == false) {
            /* get error code if the process in IME is failed */
            int ret_code = event.errorCode;
            if (ret_code == RETURN_SAME_WORD) {
                showDialog(DIALOG_CONTROL_WORDS_DUPLICATE);
            }
        } else {
            /* update the dictionary */
            mListInstance = createUserDictionaryToolsList();
        }
        return ret;
    }

    /**
     * Delete the word
     *
     * @param  word     The information of word
     */
    private void deleteDictionary(WnnWord word) {
        /* delete the word from the dictionary */
        mListInstance = createUserDictionaryToolsList();
        boolean deleted = mListInstance.deleteWord(word);
        if (!deleted) {
            Toast.makeText(getApplicationContext(),
                           R.string.user_dictionary_delete_fail,
                           Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Create the instance of UserDictionaryToolList object
     */
    protected abstract UserDictionaryToolsList createUserDictionaryToolsList();

    /**
     * Check the input string
     *
     * @param   v       The information of view
     * @return          {@code true} if success; {@code false} if fail.
     */
    private boolean inputDataCheck(View v) {

        /* return false if the length of the string exceeds the limit. */
        if ((((TextView)v).getText().length()) > MAX_TEXT_SIZE) {
            showDialog(DIALOG_CONTROL_OVER_MAX_TEXT_SIZE);
            Log.e("OpenWnn", "inputDataCheck() : over max string length.");
            return false;
        }

        return true;
    }

    /**
     * Transit the new state
     */
    private void screenTransition() {
        finish();

        /* change to the word listing window */
        Intent intent = new Intent();
        intent.setClassName(mPackageName, mListViewName);
        startActivity(intent);
        
    }

}
