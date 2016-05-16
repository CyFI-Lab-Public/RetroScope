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

import com.android.tradefed.build.FolderBuildInfo;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.build.IBuildProvider;
import com.android.tradefed.build.IFolderBuildInfo;
import com.android.tradefed.config.Option;

import java.io.File;

/**
 * A simple {@link IBuildProvider} that uses a pre-existing CTS install.
 */
public class CtsBuildProvider implements IBuildProvider {

    @Option(name="cts-install-path", description="the path to the cts installation to use")
    private String mCtsRootDirPath = System.getProperty("CTS_ROOT");

    public static final String CTS_BUILD_VERSION = "4.4_r1.9";

    /**
     * {@inheritDoc}
     */
    @Override
    public IBuildInfo getBuild() {
        if (mCtsRootDirPath == null) {
            throw new IllegalArgumentException("Missing --cts-install-path");
        }
        IFolderBuildInfo ctsBuild = new FolderBuildInfo(CTS_BUILD_VERSION, "cts", "cts");
        ctsBuild.setRootDir(new File(mCtsRootDirPath));
        return ctsBuild;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void buildNotTested(IBuildInfo info) {
        // ignore
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cleanUp(IBuildInfo info) {
        // ignore
    }
}
