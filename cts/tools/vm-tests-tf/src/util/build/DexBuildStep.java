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

import com.android.dx.command.dexer.Main;
import java.io.IOException;

public class DexBuildStep extends BuildStep {

    private final boolean deleteInputFileAfterBuild;

    DexBuildStep(BuildFile inputFile, BuildFile outputFile,
            boolean deleteInputFileAfterBuild) {
        super(inputFile, outputFile);
        this.deleteInputFileAfterBuild = deleteInputFileAfterBuild;
    }

    @Override
    boolean build() {

        if (super.build()) {
            Main.Arguments args = new Main.Arguments();

            args.jarOutput = true;
            args.fileNames = new String[] {inputFile.fileName.getAbsolutePath()};

            args.outName = outputFile.fileName.getAbsolutePath();

            int result = 0;
            try {
                result = Main.run(args);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            if (result == 0) {
                if (deleteInputFileAfterBuild) {
                    inputFile.fileName.delete();
                }
                return true;
            } else {
                System.err.println("exception while dexing "
                        + inputFile.fileName.getAbsolutePath() + " to "
                        + args.outName);
                return false;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return inputFile.hashCode() ^ outputFile.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            DexBuildStep other = (DexBuildStep) obj;

            return inputFile.equals(other.inputFile)
                    && outputFile.equals(other.outputFile);
        }
        return false;
    }


}
