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

package signature.converter;

import signature.UsageException;
import signature.converter.dex.DexFactory;
import signature.converter.doclet.DocletFactory;
import signature.io.IApiLoader;
import signature.io.IApiExternalizer;
import signature.io.impl.BinaryApi;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class Main {
    // (doclet | dex) sourcefiles --out file --name name --packages packageName{
    // packageName}" +
    public static void main(String[] args) throws IOException {
        String type = args[0];
        Set<String> sources = new HashSet<String>();
        int at = 1;
        for (/* at */; at < args.length; at++) {
            if ("--out".equals(args[at])) {
                break;
            }
            sources.add(args[at]);
        }

        if (!"--out".equals(args[at])) {
            throw new UsageException();
        }
        String targetFile = args[++at];

        if (!"--name".equals(args[++at])) {
            throw new UsageException();
        }
        String name = args[++at];

        if (!"--packages".equals(args[++at])) {
            throw new UsageException();
        }
        Set<String> packages = new HashSet<String>();
        ++at;
        for (/* at */; at < args.length; at++) {
            packages.add(args[at]);
        }

        IApiExternalizer externalizer = new BinaryApi();
        IApiLoader factory = null;

        if ("doclet".equals(type)) {
            factory = new DocletFactory();
        } else if ("dex".equals(type)) {
            factory = new DexFactory();
        } else {
            throw new UsageException();
        }

        externalizer.externalizeApi(targetFile, factory.loadApi(name,
                Visibility.PROTECTED, sources, packages));
    }
}
