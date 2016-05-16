/*
 * Copyright (C) 2010 The Android Open Source Project
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

import java.util.List;

public interface LogOutput {

    void verbose(String s);

    void warn(String message);

    /**
     * Warns, and also puts a list of strings afterwards.
     */
    void warn(String message, List<String> list);

    void info(String s);

    void info(String message, Throwable throwable);

    void nativeOutput(String outputLine);

}
