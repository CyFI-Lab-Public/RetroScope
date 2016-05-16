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

package signature.io.impl;

import signature.converter.Visibility;
import signature.io.IApiExternalizer;
import signature.io.IApiLoader;
import signature.model.IApi;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Set;

public class BinaryApi implements IApiExternalizer, IApiLoader {

    public void externalizeApi(String fileName, IApi api) throws IOException {

        File directory = new File(fileName);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File file = new File(directory, getFileName(api));
        file.createNewFile();

        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
                file));
        oos.writeObject(api);

        oos.flush();
        oos.close();
    }

    private String getFileName(IApi api) {
        return api.getName().replaceAll(" ", "_").concat(".sig");
    }

    public IApi loadApi(String name, Visibility visibility,
            Set<String> fileNames, Set<String> packageNames) throws
            IOException {
        System.err
                .println("Binary signature loader ignores visibility and " +
                        "package names.");
        if (fileNames.size() != 1) {
            throw new IllegalArgumentException(
                    "Only one file can be processed by the binary signature " +
                    "loader.");
        }
        String fileName = fileNames.iterator().next();
        File file = new File(fileName);
        ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(file));
        IApi sig = null;
        try {
            sig = (IApi) ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
        if (name != null) {
            sig.setName(name);
        }

        ois.close();
        return sig;
    }
}
