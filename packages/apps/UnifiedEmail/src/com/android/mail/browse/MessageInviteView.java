/*
 * Copyright (C) 2012 Google Inc.
 * Licensed to The Android Open Source Project.
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

package com.android.mail.browse;

import android.content.AsyncQueryHandler;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.android.mail.R;
import com.android.mail.providers.Message;
import com.android.mail.providers.UIProvider;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.Utils;

public class MessageInviteView extends LinearLayout implements View.OnClickListener {

    private Message mMessage;
    private final Context mContext;
    private InviteCommandHandler mCommandHandler = new InviteCommandHandler();

    public MessageInviteView(Context c) {
        this(c, null);
    }

    public MessageInviteView(Context c, AttributeSet attrs) {
        super(c, attrs);
        mContext = c;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        findViewById(R.id.invite_calendar_view).setOnClickListener(this);
        findViewById(R.id.accept).setOnClickListener(this);
        findViewById(R.id.tentative).setOnClickListener(this);
        findViewById(R.id.decline).setOnClickListener(this);
    }

    public void bind(Message message) {
        mMessage = message;
    }

    @Override
    public void onClick(View v) {
        Integer command = null;

        final int id = v.getId();

        if (id == R.id.invite_calendar_view) {
            if (!Utils.isEmpty(mMessage.eventIntentUri)) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(mMessage.eventIntentUri);
                mContext.startActivity(intent);
            }
        } else if (id == R.id.accept) {
            command = UIProvider.MessageOperations.RESPOND_ACCEPT;
        } else if (id == R.id.tentative) {
            command = UIProvider.MessageOperations.RESPOND_TENTATIVE;
        } else if (id == R.id.decline) {
            command = UIProvider.MessageOperations.RESPOND_DECLINE;
        }

        if (command != null) {
            ContentValues params = new ContentValues();
            LogUtils.w("UnifiedEmail", "SENDING INVITE COMMAND, VALUE=%s", command);
            params.put(UIProvider.MessageOperations.RESPOND_COLUMN, command);
            mCommandHandler.sendCommand(params);
        }
    }

    private class InviteCommandHandler extends AsyncQueryHandler {

        public InviteCommandHandler() {
            super(getContext().getContentResolver());
        }

        public void sendCommand(ContentValues params) {
            startUpdate(0, null, mMessage.uri, params, null, null);
        }

    }
}
