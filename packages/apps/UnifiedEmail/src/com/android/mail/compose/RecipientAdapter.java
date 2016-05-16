/**
 * Copyright (c) 2007, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.mail.compose;

import com.android.ex.chips.BaseRecipientAdapter;
import com.android.mail.providers.Account;

import android.content.Context;

public class RecipientAdapter extends BaseRecipientAdapter {
    public RecipientAdapter(Context context, Account account) {
        super(context);
        setAccount(account.getAccountManagerAccount());
    }
}
