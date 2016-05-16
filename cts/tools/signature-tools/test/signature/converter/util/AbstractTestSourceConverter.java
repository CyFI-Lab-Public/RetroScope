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

package signature.converter.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import signature.converter.Visibility;
import signature.model.IApi;

public abstract class AbstractTestSourceConverter implements ITestSourceConverter {

    public IApi convert(Set<CompilationUnit> units) throws IOException {
        return convert(Visibility.PROTECTED, units);
    }
    public IApi convert(CompilationUnit... units) throws IOException {
        return convert(Visibility.PROTECTED, new HashSet<CompilationUnit>(Arrays.asList(units)));
    }
    public IApi convert(Visibility visibility, CompilationUnit... units) throws IOException {
        return convert(visibility, new HashSet<CompilationUnit>(Arrays.asList(units)));
    }
}

