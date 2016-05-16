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

import org.antlr.stringtemplate.StringTemplate;

import signature.compare.model.IApiDelta;
import signature.compare.model.IPackageDelta;
import signature.compare.model.impl.SigDelta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ApiOverviewPage implements IEmitter {

    private final IApiDelta apiDelta;

    private List<IPackageDelta> removedPackages;
    private List<IPackageDelta> addedPackages;
    private List<IPackageDelta> changedPackages;
    private Map<String, String> commonInfos;

    public ApiOverviewPage(IApiDelta apiDelta,
            Map<String, String> commonInfos) {
        this.apiDelta = apiDelta;
        this.commonInfos = commonInfos;
        prepareData();
    }

    private void prepareData() {
        if (apiDelta.getPackageDeltas().isEmpty()) {
            commonInfos.put("no_delta", "no_delta");
        }
        removedPackages = new ArrayList<IPackageDelta>(SigDelta
                .getRemoved(apiDelta.getPackageDeltas()));
        Collections.sort(removedPackages, new PackageByNameComparator());

        addedPackages = new ArrayList<IPackageDelta>(SigDelta.getAdded(apiDelta
                .getPackageDeltas()));
        Collections.sort(addedPackages, new PackageByNameComparator());

        changedPackages = new ArrayList<IPackageDelta>(SigDelta
                .getChanged(apiDelta.getPackageDeltas()));
        Collections.sort(changedPackages, new PackageByNameComparator());
    }

    public void writeTo(StringBuilder b) {
        StringTemplate template = TemplateStore
                .getStringTemplate("ApiOverviewPage");
        template.setArgumentContext(commonInfos);
        template.setAttribute("removed_packages", removedPackages);
        template.setAttribute("added_packages", addedPackages);
        template.setAttribute("changed_packages", changedPackages);
        b.append(template.toString());
    }
}
