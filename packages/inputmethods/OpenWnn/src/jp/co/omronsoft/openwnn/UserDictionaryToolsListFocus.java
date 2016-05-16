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

import android.view.View;
import android.content.Context;
import android.widget.TextView;

/**
 * The view class of the stroke and the candidate.
 *
 * @author Copyright (C) 2008, OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
public class UserDictionaryToolsListFocus extends TextView {
    /** the information of the pair of view */
    private TextView mPairView = null;

    /**
     * Constructor
     *
     * @param  context       The context
     */
    public UserDictionaryToolsListFocus(Context context) {
        super(context);
    }

    /**
     * Get the pair of view
     *
     * @return               The information of the pair of view
     */
    public View getPairView() {
        return mPairView;
    }

    /**
     * Set the pair of view
     *
     * @param  pairView      The information of the pair of view
     */
    public void setPairView(TextView pairView) {
        mPairView = pairView;
    }
}
