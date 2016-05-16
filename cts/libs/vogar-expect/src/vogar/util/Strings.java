/*
 * Copyright (C) 2009 The Android Open Source Project
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


package vogar.util;

//import com.google.common.collect.Lists;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for strings.
 */
public class Strings {

    private static final Pattern XML_INVALID_CHARS
            = Pattern.compile("[^\\u0009\\u000A\\u000D\\u0020-\\uD7FF\\uE000-\\uFFFD]+");

    public static String readStream(Reader reader) throws IOException {
        StringBuilder result = new StringBuilder();
        BufferedReader in = new BufferedReader(reader);
        String line;
        while ((line = in.readLine()) != null) {
            result.append(line);
            result.append('\n');
        }
        in.close();
        return result.toString();
    }

    public static String readFile(File f) throws IOException {
        return readStream(new InputStreamReader(new FileInputStream(f), "UTF-8"));
    }

    public static List<String> readFileLines(File f) throws IOException {
        BufferedReader in =
                new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
        List<String> list = new ArrayList<String>();
        String line;
        while ((line = in.readLine()) != null) {
            list.add(line);
        }
        in.close();
        return list;
    }

    public static String join(String delimiter, Object... objects) {
        return join(Arrays.asList(objects), delimiter);
    }

    public static String join(Iterable<?> objects, String delimiter) {
        Iterator<?> i = objects.iterator();
        if (!i.hasNext()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        result.append(i.next());
        while(i.hasNext()) {
            result.append(delimiter).append(i.next());
        }
        return result.toString();
    }

    public static String[] objectsToStrings(Object[] objects) {
        String[] result = new String[objects.length];
        int i = 0;
        for (Object o : objects) {
            result[i++] = o.toString();
        }
        return result;
    }

    public static String[] objectsToStrings(Collection<?> objects) {
        return objectsToStrings(objects.toArray());
    }

    /**
     * Replaces XML-invalid characters with the corresponding U+XXXX code point escapes.
     */
    public static String xmlSanitize(String text) {
        StringBuffer result = new StringBuffer();
        Matcher matcher = XML_INVALID_CHARS.matcher(text);
        while (matcher.find()) {
            matcher.appendReplacement(result, "");
            result.append(escapeCodePoint(matcher.group()));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String escapeCodePoint(CharSequence cs) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < cs.length(); ++i) {
            result.append(String.format("U+%04X", (int) cs.charAt(i)));
        }
        return result.toString();
    }
}
