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

package dasm;

import java.util.Hashtable;

import java_cup.runtime.token;

class ReservedWords {
    static Hashtable<String, token> reserved_words;

    public static token get(String name) {
        return (token) reserved_words.get(name);
    }

    public static boolean contains(String name) {
        return reserved_words.get(name) != null;
    }

    static {
        reserved_words = new Hashtable<String, token>();

        // Dasm directives
        reserved_words.put(".annotation", new token(sym.DANNOTATION));
        reserved_words.put(".attribute", new token(sym.DATTRIBUTE));
        reserved_words.put(".bytecode", new token(sym.DBYTECODE));
        reserved_words.put(".catch", new token(sym.DCATCH));
        reserved_words.put(".class", new token(sym.DCLASS));
        reserved_words.put(".deprecated", new token(sym.DDEPRECATED));
        reserved_words.put(".end", new token(sym.DEND));
        reserved_words.put(".field", new token(sym.DFIELD));
        reserved_words.put(".implements", new token(sym.DIMPLEMENTS));
        reserved_words.put(".inner", new token(sym.DINNER));
        reserved_words.put(".interface", new token(sym.DINTERFACE));
        reserved_words.put(".limit", new token(sym.DLIMIT));
        reserved_words.put(".line", new token(sym.DLINE));
        reserved_words.put(".method", new token(sym.DMETHOD));
        reserved_words.put(".set", new token(sym.DSET));
        reserved_words.put(".source", new token(sym.DSOURCE));
        reserved_words.put(".super", new token(sym.DSUPER));
        reserved_words.put(".throws", new token(sym.DTHROWS));
        reserved_words.put(".var", new token(sym.DVAR));
        reserved_words.put(".enclosing", new token(sym.DENCLOSING));
        reserved_words.put(".signature", new token(sym.DSIGNATURE));

        // reserved_words used in Dasm directives
        reserved_words.put("field", new token(sym.FIELD));
        reserved_words.put("from", new token(sym.FROM));
        reserved_words.put("method", new token(sym.METHOD));
        reserved_words.put("to", new token(sym.TO));
        reserved_words.put("is", new token(sym.IS));
        reserved_words.put("using", new token(sym.USING));
        reserved_words.put("signature", new token(sym.SIGNATURE));
        reserved_words.put("regs", new token(sym.REGS));
        reserved_words.put("inner", new token(sym.INNER));
        reserved_words.put("outer", new token(sym.OUTER));
        reserved_words.put("class", new token(sym.CLASS));
        reserved_words.put("visible", new token(sym.VISIBLE));
        reserved_words.put("invisible", new token(sym.INVISIBLE));
        reserved_words.put("visibleparam", new token(sym.VISIBLEPARAM));
        reserved_words.put("invisibleparam", new token(sym.INVISIBLEPARAM));

        // Special-case instructions
        reserved_words.put("fill-array-data", new token(sym.FILL_ARRAY_DATA));
        reserved_words.put("fill-array-data-end", new token(
                sym.FILL_ARRAY_DATA_END));
        reserved_words.put("packed-switch", new token(sym.PACKED_SWITCH));
        reserved_words.put("packed-switch-end",
                new token(sym.PACKED_SWITCH_END));
        reserved_words.put("sparse-switch", new token(sym.SPARSE_SWITCH));
        reserved_words.put("sparse-switch-end",
                new token(sym.SPARSE_SWITCH_END));
        reserved_words.put("default", new token(sym.DEFAULT));

        // Access flags
        reserved_words.put("public", new token(sym.PUBLIC));
        reserved_words.put("private", new token(sym.PRIVATE));
        reserved_words.put("protected", new token(sym.PROTECTED));
        reserved_words.put("static", new token(sym.STATIC));
        reserved_words.put("final", new token(sym.FINAL));
        reserved_words.put("synchronized", new token(sym.SYNCHRONIZED));
        reserved_words.put("declared_synchronized", new token(
                sym.DECLARED_SYNCHRONIZED));
        reserved_words.put("volatile", new token(sym.VOLATILE));
        reserved_words.put("transient", new token(sym.TRANSIENT));
        reserved_words.put("native", new token(sym.NATIVE));
        reserved_words.put("interface", new token(sym.INTERFACE));
        reserved_words.put("abstract", new token(sym.ABSTRACT));

        reserved_words.put("annotation", new token(sym.ANNOTATION));
        reserved_words.put("enum", new token(sym.ENUM));
        reserved_words.put("bridge", new token(sym.BRIDGE));
        reserved_words.put("varargs", new token(sym.VARARGS));
        reserved_words.put("fpstrict", new token(sym.STRICT));
        reserved_words.put("synthetic", new token(sym.SYNTHETIC));
    }
}
