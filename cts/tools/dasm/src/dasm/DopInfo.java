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

import com.android.dx.dex.code.Dop;
import com.android.dx.dex.code.Dops;
import com.android.dx.dex.code.InsnFormat;
import com.android.dx.dex.code.form.Form10t;
import com.android.dx.dex.code.form.Form10x;
import com.android.dx.dex.code.form.Form11n;
import com.android.dx.dex.code.form.Form11x;
import com.android.dx.dex.code.form.Form12x;
import com.android.dx.dex.code.form.Form20t;
import com.android.dx.dex.code.form.Form21c;
import com.android.dx.dex.code.form.Form21h;
import com.android.dx.dex.code.form.Form21s;
import com.android.dx.dex.code.form.Form21t;
import com.android.dx.dex.code.form.Form22b;
import com.android.dx.dex.code.form.Form22c;
import com.android.dx.dex.code.form.Form22s;
import com.android.dx.dex.code.form.Form22t;
import com.android.dx.dex.code.form.Form22x;
import com.android.dx.dex.code.form.Form23x;
import com.android.dx.dex.code.form.Form30t;
import com.android.dx.dex.code.form.Form31c;
import com.android.dx.dex.code.form.Form31i;
import com.android.dx.dex.code.form.Form31t;
import com.android.dx.dex.code.form.Form32x;
import com.android.dx.dex.code.form.Form35c;
import com.android.dx.dex.code.form.Form3rc;
import com.android.dx.dex.code.form.Form51l;
import com.android.dx.io.Opcodes;

import java.util.Hashtable;

/**
 * Holds information about Dalvik opcodes and its parameters.
 */
class DopInfo {

    // maps instruction name -> DopInfo object
    static Hashtable<String, DopInfo> dopsTable;

    // information maintained about each instruction:
    public String name; // opcode name
    public Dop opcode; // its opcode
    public String args; // the argument code

    public final static String ARG_REGISTER = "R";
    public final static String ARG_TYPE = "T";
    public final static String ARG_LITERAL = "I";
    public final static String ARG_ADDRESS = "A";
    public final static String ARG_FIELD = "F";
    public final static String ARG_METHOD = "M";
    public final static String ARG_INTFMETHOD = "Y";
    public final static String ARG_STRING = "S";
    public final static String ARG_REGLIST = "Z";
    public final static String ARG_REGRANGE = "G";

    /**
     * define type of parameters each instruction has
     */
    public final static String ARG_REG_REG = ARG_REGISTER + ARG_REGISTER;
    public final static String ARG_REG_REG_REG = ARG_REGISTER + ARG_REGISTER
            + ARG_REGISTER;
    public final static String ARG_REG_LITERAL = ARG_REGISTER + ARG_LITERAL;
    public final static String ARG_REG_REG_LITERAL = ARG_REGISTER
            + ARG_REGISTER + ARG_LITERAL;
    public final static String ARG_REG_ADDRESS = ARG_REGISTER + ARG_ADDRESS;
    public final static String ARG_REG_REG_ADDRESS = ARG_REGISTER
            + ARG_REGISTER + ARG_ADDRESS;
    public final static String ARG_REG_TYPE = ARG_REGISTER + ARG_TYPE;
    public final static String ARG_REG_REG_TYPE = ARG_REGISTER + ARG_REGISTER
            + ARG_TYPE;
    public final static String ARG_REG_FIELD = ARG_REGISTER + ARG_FIELD;
    public final static String ARG_REG_REG_FIELD = ARG_REGISTER + ARG_REGISTER
            + ARG_FIELD;
    public final static String ARG_REG_STRING = ARG_REGISTER + ARG_STRING;
    public final static String ARG_REG_REG_STRING = ARG_REGISTER + ARG_REGISTER
            + ARG_STRING;
    public final static String ARG_REGLIST_TYPE = ARG_REGLIST + ARG_TYPE;
    public final static String ARG_REGLIST_METHOD = ARG_REGLIST + ARG_METHOD;
    public final static String ARG_REGLIST_INTFMETHOD = ARG_REGLIST
            + ARG_INTFMETHOD;
    public final static String ARG_REGRANGE_TYPE = ARG_REGRANGE + ARG_TYPE;
    public final static String ARG_REGRANGE_METHOD = ARG_REGRANGE + ARG_METHOD;
    public final static String ARG_REGRANGE_INTFMETHOD = ARG_REGRANGE
            + ARG_INTFMETHOD;

    public static DopInfo get(String name) {
        return (DopInfo) dopsTable.get(name);
    }

    public static boolean contains(String name) {
        return dopsTable.get(name) != null;
    }

    /**
     * Adds new opcode to table
     */
    static private void add(String name, Dop opcode, String args) {
        DopInfo info = new DopInfo();
        info.name = name;
        info.opcode = opcode;
        info.args = args;
        dopsTable.put(name, info);
    }

    static private String getArgsFormat(Dop dop) {
        InsnFormat format = dop.getFormat();

        if (format instanceof Form10x) return "";
        if (format instanceof Form12x) return ARG_REG_REG;
        if (format instanceof Form11n) return ARG_REG_LITERAL;
        if (format instanceof Form11x) return ARG_REGISTER;
        if (format instanceof Form10t || format instanceof Form20t)
            return ARG_ADDRESS;
        if (format instanceof Form22x) return ARG_REG_REG;
        if (format instanceof Form21t) return ARG_REG_ADDRESS;
        if (format instanceof Form21s) return ARG_REG_LITERAL;
        if (format instanceof Form21h) return ARG_REG_LITERAL;
        if (format instanceof Form21c) {
            switch (dop.getOpcode()) {
            case Opcodes.CONST_CLASS:
            case Opcodes.CHECK_CAST:
            case Opcodes.NEW_INSTANCE:
                return ARG_REG_TYPE;
            case Opcodes.SGET:
            case Opcodes.SGET_WIDE:
            case Opcodes.SGET_OBJECT:
            case Opcodes.SGET_BOOLEAN:
            case Opcodes.SGET_BYTE:
            case Opcodes.SGET_CHAR:
            case Opcodes.SGET_SHORT:
            case Opcodes.SPUT:
            case Opcodes.SPUT_WIDE:
            case Opcodes.SPUT_OBJECT:
            case Opcodes.SPUT_BOOLEAN:
            case Opcodes.SPUT_BYTE:
            case Opcodes.SPUT_CHAR:
            case Opcodes.SPUT_SHORT:
                return ARG_REG_FIELD;
            default:
                return ARG_REG_STRING;
            }
        }
        if (format instanceof Form23x) return ARG_REG_REG_REG;
        if (format instanceof Form22b) return ARG_REG_REG_LITERAL;
        if (format instanceof Form22t) return ARG_REG_REG_ADDRESS;
        if (format instanceof Form22s) return ARG_REG_REG_LITERAL;
        if (format instanceof Form22c) {
            switch (dop.getOpcode()) {
            case Opcodes.INSTANCE_OF:
            case Opcodes.NEW_ARRAY:
                return ARG_REG_REG_TYPE;
            case Opcodes.IGET:
            case Opcodes.IGET_WIDE:
            case Opcodes.IGET_OBJECT:
            case Opcodes.IGET_BOOLEAN:
            case Opcodes.IGET_BYTE:
            case Opcodes.IGET_CHAR:
            case Opcodes.IGET_SHORT:
            case Opcodes.IPUT:
            case Opcodes.IPUT_WIDE:
            case Opcodes.IPUT_OBJECT:
            case Opcodes.IPUT_BOOLEAN:
            case Opcodes.IPUT_BYTE:
            case Opcodes.IPUT_CHAR:
            case Opcodes.IPUT_SHORT:
                return ARG_REG_REG_FIELD;
            default:
                return ARG_REG_REG_STRING;
            }
        }
        if (format instanceof Form30t) return ARG_ADDRESS;
        if (format instanceof Form32x) return ARG_REG_REG;
        if (format instanceof Form31i) return ARG_REG_LITERAL;
        if (format instanceof Form31t) return ARG_REG_ADDRESS;
        if (format instanceof Form31c) return ARG_REG_STRING;
        if (format instanceof Form35c) {
            switch (dop.getOpcode()) {
            case Opcodes.INVOKE_VIRTUAL:
            case Opcodes.INVOKE_SUPER:
            case Opcodes.INVOKE_DIRECT:
            case Opcodes.INVOKE_STATIC:
                return ARG_REGLIST_METHOD;
            case Opcodes.INVOKE_INTERFACE:
                return ARG_REGLIST_INTFMETHOD;
            default:
                return ARG_REGLIST_TYPE;
            }
        }
        if (format instanceof Form3rc) {
            switch (dop.getOpcode()) {
            case Opcodes.FILLED_NEW_ARRAY_RANGE:
                return ARG_REGRANGE_TYPE;
            case Opcodes.INVOKE_INTERFACE_RANGE:
                return ARG_REGRANGE_INTFMETHOD;
            default:
                return ARG_REGRANGE_METHOD;
            }
        }
        if (format instanceof Form51l) return ARG_REG_LITERAL; // not "RL" since
                                                               // it is
                                                               // processed as
                                                               // Number

        return "";
    }

    /**
     * dopsTable initialization
     */
    static {
        dopsTable = new Hashtable<String, DopInfo>();

        for (int i = 0; i < Opcodes.MAX_VALUE - Opcodes.MIN_VALUE + 1; i++) {
            try {
                Dop dop = Dops.get(i);
                add(dop.getName(), dop, getArgsFormat(dop));
            } catch (Exception e) {

            }
        }
    }

};
