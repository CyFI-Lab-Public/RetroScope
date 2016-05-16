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

package signature.converter.dex;

import dex.reader.util.JavaSource;
import dex.reader.util.JavaSourceToDexUtil;
import dex.structure.DexFile;
import signature.converter.Visibility;
import signature.converter.dex.DexToSigConverter;
import signature.converter.util.AbstractTestSourceConverter;
import signature.converter.util.CompilationUnit;
import signature.model.IApi;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DexTestConverter extends AbstractTestSourceConverter {

    public IApi convert(Visibility visibility, Set<CompilationUnit> units) throws IOException {
        JavaSourceToDexUtil toDexUtil = new JavaSourceToDexUtil();
        DexToSigConverter converter = new DexToSigConverter();
        Set<JavaSource> sources = new HashSet<JavaSource>();
        for (CompilationUnit unit : units) {
            sources.add(new JavaSource(unit.getName(), unit.getSource()));
        }
        DexFile dexFile = toDexUtil.getAllFrom(sources);
        return converter.convertApi("Dex Tests", Collections.singleton(dexFile), visibility);
    }

}
