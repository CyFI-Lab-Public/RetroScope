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

/**
 * Different string manipulations utility methods
 */
public class Utils {

    /**
     * Replace '.' characters with '/' in a string
     */
    public static String convertDotsToSlashes(String name) {
        if (name == null) return null;

        return name.replace('.', '/');
    }

    /**
     * Splits string like "v1, v2, v3" or "v1..v3" into list of registers
     */
    public static String[] splitRegList(String list) {
        String[] result = null;
        if (list.length() > 0)
            result = list.split("[\\s]*,[\\s]*|[\\s]*\\.\\.[\\s]*");
        return result;
    }

    /**
     * Converts string to a smallest possible number (int, long, float or
     * double)
     */
    public static Number stringToNumber(String str)
            throws NumberFormatException {
        if (str.startsWith("+")) {
            return new Integer(str.substring(1));
        }
        if (str.startsWith("0x")) {
            return (Utils.stringToSmallestInteger(str.substring(2), 16));
        } else if (str.indexOf('.') != -1) {

            double x = Double.parseDouble(str);
            if (x <= (double) Float.MAX_VALUE && x >= (float) Float.MIN_VALUE) {
                return new Float((float) x);
            }

            return new Double(x);
        } else {
            return (Utils.stringToSmallestInteger(str, 10));
        }
    }

    /**
     * Converts string to a smallest possible integer (int or long)
     */
    private static Number stringToSmallestInteger(String str, int radix)
            throws NumberFormatException {
        long x = Long.parseLong(str, radix);
        if (x <= (long) Integer.MAX_VALUE && x >= (long) Integer.MIN_VALUE) {
            return new Integer((int) x);
        }
        return new Long(x);
    }

    /**
     * Splits string "package/class/method(param)return_type" into
     * "package/class", "method", "(param)return_type"
     */
    public static String[] getClassMethodSignatureFromString(String name) {

        String signature = convertDotsToSlashes(name);

        String result[] = new String[3];
        int mpos = 0, sigpos = 0;
        int i;

        // find first '('
        sigpos = signature.indexOf('(');
        if (sigpos == -1) {
            sigpos = 0;
            i = signature.length() - 1;
        } else {
            i = sigpos - 1;
        }

        // find last '/' before '('
        for (; i >= 0; i--) {
            if (signature.charAt(i) == '/') {
                mpos = i;
                break;
            }
        }
        try {
            result[0] = signature.substring(0, mpos);
            result[1] = signature.substring(mpos + 1, sigpos);
            result[2] = signature.substring(sigpos);
        } catch (StringIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("malformed method signature : "
                    + name);
        }
        return result;
    }

    /**
     * Splits string "package/class/field" into "package/class" and "field"
     */
    public static String[] getClassFieldFromString(String name) {
        name = convertDotsToSlashes(name);

        String result[] = new String[2];
        int pos = name.lastIndexOf('/');

        if (pos == -1) {
            result[0] = null;
            result[1] = name;
        } else {
            result[0] = name.substring(0, pos);
            result[1] = name.substring(pos + 1);
        }

        return result;
    }

    /**
     * Splits string "method(param)return_type" into "method" and
     * "(param)return_type"
     */
    public static String[] getMethodSignatureFromString(String name) {
        int sigpos = name.indexOf('(');
        if (sigpos == -1) sigpos = 0;

        String result[] = new String[2];
        result[0] = name.substring(0, sigpos);
        result[1] = convertDotsToSlashes(name.substring(sigpos));

        return result;
    }

}
