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
package com.android.cts.tradefed.build;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Stub implementation of CtsBuildHelper that returns empty files for all methods
 */
public class StubCtsBuildHelper extends CtsBuildHelper {

    public StubCtsBuildHelper()  {
        super(new File("tmp"));
    }

    @Override
    public void validateStructure() {
        // ignore
    }

    @Override
    public File getTestApp(String appFileName) throws FileNotFoundException {
        return new File("tmp");
    }
}
