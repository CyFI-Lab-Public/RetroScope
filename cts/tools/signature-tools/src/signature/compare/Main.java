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

package signature.compare;

import signature.UsageException;
import signature.compare.model.IApiDelta;
import signature.compare.model.IPackageDelta;
import signature.compare.model.impl.SigDelta;
import signature.converter.Visibility;
import signature.converter.dex.DexFactory;
import signature.converter.doclet.DocletFactory;
import signature.io.IApiDeltaExternalizer;
import signature.io.IApiLoader;
import signature.io.html.HtmlDeltaExternalizer;
import signature.io.impl.BinaryApi;
import signature.model.IApi;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Driver class for the --compare option.
 */
public class Main {

    /**
     * <pre>
     * --from=(doclet | dex | sig) <sourcefiles>
     * --name <name>
     * --to=(doclet | dex | sig) <sourcefiles>
     * --name <name>
     * --out directory
     * --packages packageName{ packageName}
     * </pre>
     */
    public static void main(String[] args) throws IOException {
        int at = 0;

        if (!"--from".equals(args[at])) {
            throw new UsageException();
        }
        String fromType = args[++at];

        boolean hasName = false;
        Set<String> fromFiles = new HashSet<String>();
        ++at;
        for (/* at */; at < args.length; at++) {
            if ("--name".equals(args[at])) {
                hasName = true;
                break;
            }
            if ("--to".equals(args[at])) {
                break;
            }
            fromFiles.add(args[at]);
        }

        String nameFrom = null;
        if (hasName) {
            nameFrom = "";
            if (!"--name".equals(args[at])) {
                throw new UsageException();
            }
            ++at;
            for (/* at */; at < args.length; at++) {
                if ("--to".equals(args[at])) {
                    break;
                }
                nameFrom += args[at];
                nameFrom += " ";
            }
            nameFrom = nameFrom.trim();
        }

        if (!"--to".equals(args[at])) {
            throw new UsageException();
        }
        String toType = args[++at];

        hasName = false;
        Set<String> toFiles = new HashSet<String>();
        ++at;
        for (/* at */; at < args.length; at++) {
            if ("--name".equals(args[at])) {
                hasName = true;
                break;
            }
            if ("--out".equals(args[at])) {
                break;
            }
            toFiles.add(args[at]);
        }

        String nameTo = null;
        if (hasName) {
            nameTo = "";
            if (!"--name".equals(args[at])) {
                throw new UsageException();
            }
            ++at;
            for (/* at */; at < args.length; at++) {
                if ("--out".equals(args[at])) {
                    break;
                }
                nameTo += args[at];
                nameTo += " ";
            }
            nameTo = nameTo.trim();
        }

        if (!"--out".equals(args[at])) {
            throw new UsageException();
        }
        String output = args[++at];

        if (!"--packages".equals(args[++at])) {
            throw new UsageException();
        }
        Set<String> packages = new HashSet<String>();
        ++at;
        for (/* at */; at < args.length; at++) {
            packages.add(args[at]);
        }

        IApiComparator comparator = new ApiComparator();
        IApi fromApi = getApi(fromType, nameFrom, fromFiles, packages);
        IApi toApi = getApi(toType, nameTo, toFiles, packages);

        IApiDeltaExternalizer externalizer = new HtmlDeltaExternalizer();
        System.out.println("Writing delta report to " + output);
        IApiDelta delta = comparator.compare(fromApi, toApi);
        if (delta == null) {
            delta = new EmptyDelta(fromApi, toApi);
        }

        externalizer.externalize(output, delta);
    }

    private static class EmptyDelta extends SigDelta<IApi> implements
            IApiDelta {
        public EmptyDelta(IApi from, IApi to) {
            super(from, to);
        }

        public Set<IPackageDelta> getPackageDeltas() {
            return Collections.emptySet();
        }
    }

    private static IApi getApi(String specType, String name,
            Set<String> fileNames, Set<String> packageNames) throws
            IOException {
        System.out.println("Loading " + name + " of type " + specType
                + " from " + fileNames);
        IApiLoader factory = null;
        if ("doclet".equals(specType)) {
            checkName(name);
            factory = new DocletFactory();
        } else if ("dex".equals(specType)) {
            checkName(name);
            factory = new DexFactory();
        } else if ("sig".equals(specType)) {
            factory = new BinaryApi();
        } else {
            throw new UsageException();
        }
        return factory.loadApi(name, Visibility.PROTECTED, fileNames,
                packageNames);
    }

    private static void checkName(String name) {
        if (name == null) {
            throw new UsageException();
        }
    }
}
