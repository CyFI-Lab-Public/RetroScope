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

import dxconvext.ClassFileAssembler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;
import java.io.Reader;

public class DFHBuildStep extends BuildStep {

    public DFHBuildStep(BuildFile inputFile, BuildFile outputFile) {
        super(inputFile, outputFile);
    }

    @Override
    boolean build() {
        if (super.build()) {
            File out_dir = outputFile.fileName.getParentFile();
            if (!out_dir.exists() && !out_dir.mkdirs()) {
                System.err.println("failed to create dir: "
                        + out_dir.getAbsolutePath());
                return false;
            }

            ClassFileAssembler cfAssembler = new ClassFileAssembler();
            Reader r;
            OutputStream os;
            try {
                r = new FileReader(inputFile.fileName);
                os = new FileOutputStream(outputFile.fileName);
            } catch (FileNotFoundException e) {
                System.err.println(e);
                return false;
            }
            try {
                // cfAssembler throws a runtime exception
                cfAssembler.writeClassFile(r, os, true);
            } catch (RuntimeException e) {
                System.err.println("error in DFHBuildStep for inputfile "+inputFile.fileName+", outputfile "+outputFile.fileName);
                throw e;
            }
            
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {

        if (super.equals(obj)) {
            return inputFile.equals(((DFHBuildStep) obj).inputFile)
                    && outputFile.equals(((DFHBuildStep) obj).outputFile);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (inputFile == null ? 31 : inputFile.hashCode())
                ^ (outputFile == null ? 37 : outputFile.hashCode());
    }
}
