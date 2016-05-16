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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.HashSet;
import java.util.Set;

abstract class BuildStep implements Comparable<BuildStep> {

    BuildFile inputFile;
    BuildFile outputFile;

    static class BuildFile {
        final File folder;
        final File fileName;

        BuildFile(String folder, String fileName) {
            this.folder = new File(folder);
            this.fileName = new File(this.folder, fileName);
        }

        String getPath() {
            return fileName.getAbsolutePath();
        }

        @Override
        public int hashCode() {
            return fileName.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (this == obj) return true;
            if (getClass() == obj.getClass()) {
                BuildFile other = (BuildFile) obj;
                return fileName.equals(other.fileName);
            }
            return false;
        }
    }

    BuildStep(BuildFile inputFile, BuildFile outputFile) {
        if (inputFile == null) {
            throw new NullPointerException("inputFile is null");
        }
        if (outputFile == null) {
            throw new NullPointerException("outputFile is null");
        }
        this.inputFile = inputFile;
        this.outputFile = outputFile;
    }

    BuildStep() {
    }

    private Set<BuildStep> children;

    boolean build() {
        if (children != null) {
            for (BuildStep child : children) {
                if (!child.build()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        return this.getClass() == obj.getClass();
    }

    @Override
    public abstract int hashCode();

    public void addChild(BuildStep child) {
        if (children == null) {
            children = new HashSet<BuildStep>();
        }
        children.add(child);
    }

    public static void copyFile(File in, File out) throws IOException {
        FileChannel inChannel = new FileInputStream(in).getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            throw e;
        } finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
    }

    public int compareTo(BuildStep o) {
        return (inputFile == o.inputFile ? 0 : (inputFile != null
                ? (o.inputFile != null ? inputFile.getPath().compareTo(
                        o.inputFile.getPath()) : 1) : -1));
    }
}
