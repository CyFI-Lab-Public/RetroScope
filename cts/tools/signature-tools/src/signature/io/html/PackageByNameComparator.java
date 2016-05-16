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

package signature.io.html;

import signature.compare.model.IPackageDelta;
import signature.model.IPackage;

import java.util.Comparator;

public class PackageByNameComparator implements Comparator<IPackageDelta> {

    public int compare(IPackageDelta a, IPackageDelta b) {
        assert a.getType() == b.getType();
        IPackage aPackage = null;
        IPackage bPackage = null;

        if (a.getFrom() != null) {
            aPackage = a.getFrom();
            bPackage = b.getFrom();
        } else {
            aPackage = a.getTo();
            bPackage = b.getTo();
        }
        return aPackage.getName().compareTo(bPackage.getName());
    }
}
