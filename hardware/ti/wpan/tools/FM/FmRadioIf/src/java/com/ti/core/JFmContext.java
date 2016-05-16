/*
 *
 * Copyright 2001-2011 Texas Instruments, Inc. - http://www.ti.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ti.jfm.core;

import android.util.Log;

public class JFmContext {
    private long value = 0;

    public static final long INVALID_CONTEXT_VALUE = -1;

    public JFmContext() {
       value = INVALID_CONTEXT_VALUE;
    }

    public JFmContext(long contextValue) {
       value = contextValue;
    }

    @Override
    public boolean equals(Object otherContextAsObject) {
       if (otherContextAsObject instanceof JFmContext) {
          return (value == ((JFmContext) otherContextAsObject).getValue());
       } else {
          return false;
       }
    }

    @Override
    public int hashCode() {
       return (int) value;
    }

    public final long getValue() {
       return value;
    }

    public final void setValue(int value) {
       Log.d("JFmContext()", "setValue: setValue called, value:" + value);
       this.value = value;
    }

}
