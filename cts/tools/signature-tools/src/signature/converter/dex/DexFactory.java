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

import dex.structure.DexFile;
import signature.converter.Visibility;
import signature.io.IApiLoader;
import signature.model.IApi;
import signature.model.IPackage;
import signature.model.impl.SigApi;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

public class DexFactory implements IApiLoader {

    public IApi loadApi(String name, Visibility visibility,
            Set<String> fileNames, Set<String> packageNames) throws
            IOException {
        DexToSigConverter converter = new DexToSigConverter();
        Set<DexFile> files = DexUtil.getDexFiles(fileNames);
        SigApi api = converter.convertApi(name, files, visibility);

        Iterator<IPackage> it = api.getPackages().iterator();
        while (it.hasNext()) {
            IPackage aPackage = it.next();
            boolean found = false;
            for (String packageName : packageNames) {
                if (aPackage.getName().equals(packageName)) {
                    found = true;
                }
            }
            if (!found) {
                it.remove();
            }
        }
        return api;
    }
}
