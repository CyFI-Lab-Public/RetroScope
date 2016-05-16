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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

/**
 * {@code MemoryByteCode} represents an in-memory java byte code.
 */
/* package */ class MemoryByteCode extends SimpleJavaFileObject {
    private ByteArrayOutputStream baos;
    private final String name;

    public MemoryByteCode(String name) {
        super(URI.create("byte:///" + name.replace(".", "/") + ".class"),
                Kind.CLASS);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        throw new IllegalStateException();
    }

    public OutputStream openOutputStream() {
        baos = new ByteArrayOutputStream();
        return baos;
    }

    public InputStream openInputStream() {
        throw new IllegalStateException();
    }

    public byte[] getBytes() {
        return baos.toByteArray();
    }
}
