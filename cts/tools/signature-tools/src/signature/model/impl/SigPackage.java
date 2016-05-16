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

package signature.model.impl;

import signature.model.IClassDefinition;
import signature.model.IPackage;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@SuppressWarnings("serial")
public class SigPackage extends SigAnnotatableElement implements IPackage,
        Serializable {

    private String name;
    private Set<IClassDefinition> classes = Uninitialized.unset();

    public SigPackage(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<String> getPackageFragments() {
        return Arrays.asList(name.split("\\."));
    }

    public Set<IClassDefinition> getClasses() {
        return classes;
    }

    public void setClasses(Set<IClassDefinition> classes) {
        this.classes = classes;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("package: ");
        builder.append(getName());
        return builder.toString();
    }
}
