/*
 * Copyright (C) 2008 The Android Open Source Project
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

package util.build;

import sun.tools.jar.Main;

import java.io.File;
import java.io.IOException;


public class JarBuildStep extends BuildStep {

    String destFileName;
    private final boolean deleteInputFileAfterBuild;

    public JarBuildStep(BuildFile inputFile, String destFileName,
            BuildFile outputFile, boolean deleteInputFileAfterBuild) {
        super(inputFile, outputFile);
        this.destFileName = destFileName;
        this.deleteInputFileAfterBuild = deleteInputFileAfterBuild;
    }

    @Override
    boolean build() {
        if (super.build()) {
            File tempFile = new File(inputFile.folder, destFileName);
            try {
                if (!inputFile.fileName.equals(tempFile)) {
                    copyFile(inputFile.fileName, tempFile);
                } else {
                    tempFile = null;
                }
            } catch (IOException e) {
                System.err.println("io exception:"+e.getMessage());
                e.printStackTrace();
                return false;
            }

            File outDir = outputFile.fileName.getParentFile();
            if (!outDir.exists() && !outDir.mkdirs()) {
                System.err.println("failed to create output dir: "
                        + outDir.getAbsolutePath());
                return false;
            }
            String[] arguments = new String[] {
                    "-cMf", outputFile.fileName.getAbsolutePath(), "-C",
                    inputFile.folder.getAbsolutePath(), destFileName};
            Main main = new Main(System.out, System.err, "jar");
            boolean success = main.run(arguments);

            if (success) {
                if (tempFile != null) {
                    tempFile.delete();
                }
                if (deleteInputFileAfterBuild) {
                    inputFile.fileName.delete();
                }
            } else {
                System.err.println("exception in JarBuildStep while calling jar with args:" +
                        " \"-cMf\", "+outputFile.fileName.getAbsolutePath()+", \"-C\"," + 
                        inputFile.folder.getAbsolutePath()+", "+ destFileName);
            }
            return success;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return inputFile.hashCode() ^ outputFile.hashCode()
                ^ destFileName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            JarBuildStep other = (JarBuildStep) obj;
            return inputFile.equals(other.inputFile)
                    && outputFile.equals(other.outputFile)
                    && destFileName.equals(other.destFileName);

        }
        return false;
    }

}
