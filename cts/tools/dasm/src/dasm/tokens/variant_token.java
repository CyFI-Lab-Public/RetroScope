/*
 * Copyright (C) 2008 The Android Open Source Project
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

package dasm.tokens;

/**
 * Holds different values as reference to Object
 */
public class variant_token extends java_cup.runtime.token {
    public Object variant_val;


    public variant_token(int term_num, Number v) {
        super(term_num);
        variant_val = v;
    }

    public variant_token(int term_num, String v) {
        super(term_num);
        variant_val = v;
    }

    public variant_token(int term_num) {
        this(term_num, new Integer(0));
    }

};
