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

import java.lang.Enum;
import java.util.EnumSet; //import com.ti.jbtl.core.*;

import com.ti.jfm.core.*;

public final class JFmUtils {

    private static final String TAG = "JFmUtils";

    public static <V, E extends Enum<E> & IJFmEnum<V>> E getEnumConst(Class<E> enumType,
          V constValue) {
       EnumSet<E> es = EnumSet.allOf(enumType);

       E matchingConst = null;

       for (E enumConst : es) {
          if (enumConst.getValue().equals(constValue)) {
             matchingConst = enumConst;
             break;
          }
       }

       if (matchingConst == null) {
          JFmLog.e(TAG, "getEnumConst: Invalid const (" + constValue + ") for "
                + enumType.toString());
       }
       return matchingConst;
    }
}
