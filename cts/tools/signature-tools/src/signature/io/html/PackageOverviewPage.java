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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import signature.compare.model.IClassDefinitionDelta;
import signature.compare.model.IPackageDelta;
import signature.compare.model.impl.SigDelta;

public class PackageOverviewPage implements IEmitter {

    private static final String PACGE = "PackageOverviewPage";

    private final IPackageDelta delta;

    private List<IClassDefinitionDelta> removedClasses;
    private List<IClassDefinitionDelta> addedClasses;
    private List<IClassDefinitionDelta> changedClasses;
    private final Map<String, String> commonInfos;


    public PackageOverviewPage(IPackageDelta delta,
            Map<String, String> commonInfos) {
        this.delta = delta;
        this.commonInfos = commonInfos;
        prepareData();
    }

    private void prepareData() {
        removedClasses = new ArrayList<IClassDefinitionDelta>(SigDelta
                .getRemoved(delta.getClassDeltas()));
        Collections.sort(removedClasses, new ClassByNameComparator());

        addedClasses = new ArrayList<IClassDefinitionDelta>(SigDelta
                .getAdded(delta.getClassDeltas()));
        Collections.sort(addedClasses, new ClassByNameComparator());

        changedClasses = new ArrayList<IClassDefinitionDelta>(SigDelta
                .getChanged(delta.getClassDeltas()));
        Collections.sort(changedClasses, new ClassByNameComparator());
    }

    public void writeTo(StringBuilder b) {
        StringTemplate template = TemplateStore.getStringTemplate(PACGE);
        template.setArgumentContext(commonInfos);
        template.setAttribute("package_delta", delta);
        template.setAttribute("removed_classes", removedClasses);
        template.setAttribute("added_classes", addedClasses);
        template.setAttribute("changed_classes", changedClasses);
        b.append(template.toString());
    }



}
