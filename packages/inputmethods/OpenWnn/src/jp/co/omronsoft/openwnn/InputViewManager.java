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
import android.content.SharedPreferences;
import android.view.inputmethod.EditorInfo;

/**
 * The interface of input view manager used by OpenWnn.
 *
 * @author Copyright (C) 2009-2011 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
public interface InputViewManager {
    /**
     * Initialize the input view.
     *
     * @param parent    The OpenWnn object
     * @param width     The width of the display
     * @param height    The height of the display
     *
     * @return      The input view created in the initialize process; {@code null} if cannot create a input view.
     */
    public View initView(OpenWnn parent, int width, int height);

    /**
     * Get the input view being used currently.
     *
     * @return  The input view; {@code null} if no input view is used currently.
     */
    public View getCurrentView();

    /**
     * Notification of updating parent's state.
     *
     * @param parent    The OpenWnn object using this manager
     */
    public void onUpdateState(OpenWnn parent);

    /**
     * Reflect the preferences in the input view.
     *
     * @param pref    The preferences
     * @param editor  The information about the editor
     */
    public void setPreferences(SharedPreferences pref, EditorInfo editor);

    /**
     * Close the input view.
     */
    public void closing();
 
    /**
     * Show the input view.
     */
    public void showInputView();

    /**
     * Hide the input view.
     */
    public void hideInputView();

}
