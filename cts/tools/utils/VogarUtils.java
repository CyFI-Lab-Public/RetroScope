/*
 * Copyright (C) 2011 The Android Open Source Project
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

import vogar.Expectation;
import vogar.ExpectationStore;
import vogar.ModeId;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class VogarUtils {

    public static boolean isVogarKnownFailure(ExpectationStore[] expectationStores,
            final String testClassName,
            final String testMethodName) {
        for (ExpectationStore expectationStore : expectationStores) {
            if (isVogarKnownFailure(expectationStore, testClassName, testMethodName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isVogarKnownFailure(ExpectationStore expectationStore,
            final String testClassName,
            final String testMethodName) {
        String fullTestName = String.format("%s#%s", testClassName, testMethodName);
        return expectationStore != null
                && expectationStore.get(fullTestName) != Expectation.SUCCESS;
    }

    public static ExpectationStore provideExpectationStore(String dir) throws IOException {
        if (dir == null) {
            return null;
        }
        ExpectationStore result = ExpectationStore.parse(getExpectationFiles(dir), ModeId.DEVICE);
        return result;
    }

    private static Set<File> getExpectationFiles(String dir) {
        Set<File> expectSet = new HashSet<File>();
        File[] files = new File(dir).listFiles(new FilenameFilter() {
            // ignore obviously temporary files
            public boolean accept(File dir, String name) {
                return !name.endsWith("~") && !name.startsWith(".");
            }
        });
        if (files != null) {
            expectSet.addAll(Arrays.asList(files));
        }
        return expectSet;
    }
}
