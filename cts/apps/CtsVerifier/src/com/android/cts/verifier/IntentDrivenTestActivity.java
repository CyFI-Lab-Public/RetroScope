// Copyright 2013 Google Inc. All Rights Reserved.

package com.android.cts.verifier;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 *  A generic activity for intent based tests.
 *
 *  This activity can be reused for various tests that are intent based. The activity consists of a
 *  text view containing instructions and one or more buttons that trigger intents.
 *  Each button can send one or more intenrs as startActivity() calls.
 *
 *  The intents can either be generated statically and passed as an extra to the intent that started
 *  this activity or in some cases where the intent needs to be based on dynamic data (for example
 *  time of day), an {@link IntentFactory} class name can be passed instead. This intent factory
 *  will dynamically create the intent when the button is clicked based on the test id and the
 *  button that was clicked.
 */
public class IntentDrivenTestActivity extends PassFailButtons.Activity implements OnClickListener {
    public static final String EXTRA_ID = "id";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_INFO = "info";
    public static final String EXTRA_BUTTONS = "buttons";

    private String mTestId;
    private ButtonInfo[] mButtonInfos;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.intent_driven_test);
        setPassFailButtonClickListeners();

        final Intent intent = getIntent();
        if (!intent.hasExtra(EXTRA_ID)
                || !intent.hasExtra(EXTRA_TITLE)
                || !intent.hasExtra(EXTRA_INFO)
                || !intent.hasExtra(EXTRA_BUTTONS)) {
            throw new IllegalArgumentException(
                    "Intent must have EXTRA_ID, EXTRA_TITLE, EXTRA_INFO & EXTRA_BUTTONS");
        }

        mTestId = intent.getStringExtra(EXTRA_ID);
        setTitle(intent.getIntExtra(EXTRA_TITLE, -1));

        final TextView info = (TextView) findViewById(R.id.info);
        info.setText(intent.getIntExtra(EXTRA_INFO, -1));

        final ViewGroup buttons = (ViewGroup) findViewById(R.id.buttons);
        final Parcelable[] parcelables = intent.getParcelableArrayExtra(EXTRA_BUTTONS);
        mButtonInfos = new ButtonInfo[parcelables.length];
        for (int i = 0; i < parcelables.length; i++) {
            final ButtonInfo buttonInfo = (ButtonInfo) parcelables[i];
            mButtonInfos[i] = buttonInfo;
            final Button button = new Button(this);
            buttons.addView(button);
            button.setText(buttonInfo.mButtonText);
            button.setTag(i);
            button.setOnClickListener(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setResult(RESULT_CANCELED);
    }

    @Override
    public void onClick(View v) {
        final ButtonInfo buttonInfo = mButtonInfos[(Integer) v.getTag()];
        final Intent[] intents = buttonInfo.getIntents();
        if (intents != null) {
            for (Parcelable intent : intents) {
                startActivity((Intent) intent);
            }
        } else {
            final Class<?> factoryClass;
            final String className = buttonInfo.getIntentFactoryClassName();
            try {
                factoryClass = Thread.currentThread().getContextClassLoader().loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Factory not found: " + className, e);
            }
            final IntentFactory factory;
            try {
                factory = (IntentFactory) factoryClass.newInstance();
            } catch (InstantiationException e) {
                throw new IllegalArgumentException("Can't create factory: " + className, e);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Can't create factory: " + className, e);
            }

            for (Intent intent : factory.createIntents(mTestId, buttonInfo.getButtonText())) {
                startActivity(intent);
            }
        }
    }

    @Override
    public String getTestId() {
        return mTestId;
    }

    public static class TestInfo {
        private final String mTestId;
        private final int mTitle;
        private final int mInfoText;
        private final ButtonInfo[] mButtons;

        public TestInfo(String testId,  int title, int infoText, ButtonInfo... buttons) {
            mTestId = testId;
            mTitle = title;
            mInfoText = infoText;
            mButtons = buttons;
        }

        public String getTestId() {
            return mTestId;
        }

        public int getTitle() {
            return mTitle;
        }

        public int getInfoText() {
            return mInfoText;
        }

        public ButtonInfo[] getButtons() {
            return mButtons;
        }
    }

    public static class ButtonInfo implements Parcelable {
        private final int mButtonText;
        private final Intent[] mIntents;
        private final String mIntentFactoryClassName;

        public ButtonInfo(int buttonText, Intent... intents) {
            mButtonText = buttonText;
            mIntents = intents;
            mIntentFactoryClassName = null;
        }

        public ButtonInfo(int buttonText, String intentFactoryClassName) {
            mButtonText = buttonText;
            mIntents = null;
            mIntentFactoryClassName = intentFactoryClassName;
        }

        public ButtonInfo(Parcel source) {
            mButtonText = source.readInt();
            final Parcelable[] parcelables = source.readParcelableArray(null);
            if (parcelables != null) {
                mIntents = new Intent[parcelables.length];
                for (int i = 0, parcelablesLength = parcelables.length; i < parcelablesLength; i++) {
                    mIntents[i] = (Intent) parcelables[i];
                }
            } else {
                mIntents = null;
            }
            mIntentFactoryClassName = source.readString();
        }

        public int getButtonText() {
            return mButtonText;
        }

        public Intent[] getIntents() {
            return mIntents;
        }

        public String getIntentFactoryClassName() {
            return mIntentFactoryClassName;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(mButtonText);
            dest.writeParcelableArray(mIntents, 0);
            dest.writeString(mIntentFactoryClassName);
        }

        public static final Creator<ButtonInfo> CREATOR = new Creator<ButtonInfo>() {
            public ButtonInfo createFromParcel(Parcel source) {
                return new ButtonInfo(source);
            }

            public ButtonInfo[] newArray(int size) {
                return new ButtonInfo[size];
            }
        };

    }

    public interface IntentFactory {
        Intent[] createIntents(String testId, int buttonText);
    }
}
