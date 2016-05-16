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

package android.example;

/**
 * Example class being tested. In a real test, the classes to test would
 * live somewhere other than in the test package, but for the sake of
 * brevity, we include this one here instead.
 */
public class Example {
    /**
     * Return the standard string indication of a successfuly blorting.
     *
     * @returns {@code "blort"}, always
     */
    public static String blort() {
        return "blort";
    }

    /**
     * Return the standard string indication of a successfuly zorching.
     *
     * @returns {@code "zorch"}, always
     */
    public static String zorch() {
        return "zorch";
    }
}
