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

package dex.reader.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

/**
 * {@code SpecialJavaFileManager} is a file manager which returns
 * {@link MemoryByteCode} objects for its output and keeps track of them.
 */
/* package */ class SpecialJavaFileManager extends
        ForwardingJavaFileManager<StandardJavaFileManager> {

    private Map<String, MemoryByteCode> store;

    public SpecialJavaFileManager(StandardJavaFileManager sjfm) {
        super(sjfm);
        store = new HashMap<String, MemoryByteCode>();
    }

    public JavaFileObject getJavaFileForOutput(Location location, String name,
            JavaFileObject.Kind kind, FileObject sibling) {
        MemoryByteCode mbc = new MemoryByteCode(name);
        store.put(name, mbc);
        return mbc;
    }

    public Set<MemoryByteCode> getAllMemoryByteCodes() {
        return new HashSet<MemoryByteCode>(store.values());
    }

    public MemoryByteCode getMemoryByteCode(String className) {
        return store.get(className);
    }
}
