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

import java.io.Serializable;
import java.util.Set;

import signature.converter.Visibility;
import signature.model.IApi;
import signature.model.IPackage;

@SuppressWarnings("serial")
public class SigApi implements IApi, Serializable {

    private Set<IPackage> packages = Uninitialized.unset();
    private String description;
    private Visibility visibility;

    public SigApi(String description, Visibility visibility) {
        this.description = description;
        this.visibility = visibility;
    }

    public String getName() {
        return description;
    }

    public void setName(String description) {
        this.description = description;
    }

    public Set<IPackage> getPackages() {
        return packages;
    }

    public void setPackages(Set<IPackage> packages) {
        this.packages = packages;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getName());
        return builder.toString();
    }
}
