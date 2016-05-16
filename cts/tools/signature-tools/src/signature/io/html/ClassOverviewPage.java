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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import org.antlr.stringtemplate.StringTemplate;

import signature.compare.model.IAnnotationFieldDelta;
import signature.compare.model.IClassDefinitionDelta;
import signature.compare.model.IConstructorDelta;
import signature.compare.model.IEnumConstantDelta;
import signature.compare.model.IFieldDelta;
import signature.compare.model.IMethodDelta;
import signature.compare.model.impl.SigDelta;

public class ClassOverviewPage implements IEmitter {

    private static final String PAGE = "ClassOverviewPage";

    private final IClassDefinitionDelta classDelta;

    private FieldByNameComparator<IFieldDelta> fieldComparator =
            new FieldByNameComparator<IFieldDelta>();
    private ArrayList<IFieldDelta> removedFields;
    private ArrayList<IFieldDelta> addedFields;
    private ArrayList<IFieldDelta> changedFields;

    private FieldByNameComparator<IAnnotationFieldDelta> annotationfieldComparator =
            new FieldByNameComparator<IAnnotationFieldDelta>();
    private ArrayList<IAnnotationFieldDelta> removedAnnotationFields;
    private ArrayList<IAnnotationFieldDelta> addedAnnotationFields;
    private ArrayList<IAnnotationFieldDelta> changedAnnotationFields;

    private FieldByNameComparator<IEnumConstantDelta> enumConstantComparator =
            new FieldByNameComparator<IEnumConstantDelta>();
    private ArrayList<IEnumConstantDelta> removedEnumConstants;
    private ArrayList<IEnumConstantDelta> addedEnumConstants;
    private ArrayList<IEnumConstantDelta> changedEnumConstants;

    private ExecutableMemberComparator constructorComparator =
            new ExecutableMemberComparator();
    private ArrayList<IConstructorDelta> removedConstructors;
    private ArrayList<IConstructorDelta> addedConstructors;
    private ArrayList<IConstructorDelta> changedConstructors;

    private ExecutableMemberComparator methodComparator =
            new ExecutableMemberComparator();
    private ArrayList<IMethodDelta> removedMethods;
    private ArrayList<IMethodDelta> addedMethods;
    private ArrayList<IMethodDelta> changedMethods;

    private final Map<String, String> commonInfos;

    public ClassOverviewPage(IClassDefinitionDelta classDelta,
            Map<String, String> commonInfos) {
        this.classDelta = classDelta;
        this.commonInfos = commonInfos;
        prepareData();
    }

    private void prepareData() {
        if (classDelta.getFieldDeltas() != null) {
            prepareFieldDeltas();
        }
        if (classDelta.getAnnotationFieldDeltas() != null) {
            prepareAnnotationFieldDeltas();
        }
        if (classDelta.getEnumConstantDeltas() != null) {
            prepareEnumConstantDeltas();
        }
        if (classDelta.getConstructorDeltas() != null) {
            prepareConstructorDeltas();
        }
        if (classDelta.getMethodDeltas() != null) {
            prepareMethodDeltas();
        }
    }

    private void prepareFieldDeltas() {
        removedFields = new ArrayList<IFieldDelta>(SigDelta
                .getRemoved(classDelta.getFieldDeltas()));
        Collections.sort(removedFields, fieldComparator);

        addedFields = new ArrayList<IFieldDelta>(SigDelta.getAdded(classDelta
                .getFieldDeltas()));
        Collections.sort(addedFields, fieldComparator);

        changedFields = new ArrayList<IFieldDelta>(SigDelta
                .getChanged(classDelta.getFieldDeltas()));
        Collections.sort(changedFields, fieldComparator);
    }

    private void prepareAnnotationFieldDeltas() {
        removedAnnotationFields = new ArrayList<IAnnotationFieldDelta>(SigDelta
                .getRemoved(classDelta.getAnnotationFieldDeltas()));
        Collections.sort(removedAnnotationFields, annotationfieldComparator);

        addedAnnotationFields = new ArrayList<IAnnotationFieldDelta>(SigDelta
                .getAdded(classDelta.getAnnotationFieldDeltas()));
        Collections.sort(addedAnnotationFields, annotationfieldComparator);

        changedAnnotationFields = new ArrayList<IAnnotationFieldDelta>(SigDelta
                .getChanged(classDelta.getAnnotationFieldDeltas()));
        Collections.sort(changedAnnotationFields, annotationfieldComparator);
    }

    private void prepareEnumConstantDeltas() {
        removedEnumConstants = new ArrayList<IEnumConstantDelta>(SigDelta
                .getRemoved(classDelta.getEnumConstantDeltas()));
        Collections.sort(removedEnumConstants, enumConstantComparator);

        addedEnumConstants = new ArrayList<IEnumConstantDelta>(SigDelta
                .getAdded(classDelta.getEnumConstantDeltas()));
        Collections.sort(addedEnumConstants, enumConstantComparator);

        changedEnumConstants = new ArrayList<IEnumConstantDelta>(SigDelta
                .getChanged(classDelta.getEnumConstantDeltas()));
        Collections.sort(changedEnumConstants, enumConstantComparator);
    }

    private void prepareConstructorDeltas() {
        removedConstructors = new ArrayList<IConstructorDelta>(SigDelta
                .getRemoved(classDelta.getConstructorDeltas()));
        Collections.sort(removedConstructors, constructorComparator);

        addedConstructors = new ArrayList<IConstructorDelta>(SigDelta
                .getAdded(classDelta.getConstructorDeltas()));
        Collections.sort(addedConstructors, constructorComparator);

        changedConstructors = new ArrayList<IConstructorDelta>(SigDelta
                .getChanged(classDelta.getConstructorDeltas()));
        Collections.sort(changedConstructors, constructorComparator);
    }

    private void prepareMethodDeltas() {
        removedMethods = new ArrayList<IMethodDelta>(SigDelta
                .getRemoved(classDelta.getMethodDeltas()));
        Collections.sort(removedMethods, methodComparator);

        addedMethods = new ArrayList<IMethodDelta>(SigDelta.getAdded(classDelta
                .getMethodDeltas()));
        Collections.sort(addedMethods, methodComparator);

        changedMethods = new ArrayList<IMethodDelta>(SigDelta
                .getChanged(classDelta.getMethodDeltas()));
        Collections.sort(changedMethods, methodComparator);
    }

    public void writeTo(StringBuilder b) {
        StringTemplate template = TemplateStore.getStringTemplate(PAGE);

        template.setAttribute("class_delta", classDelta);
        boolean annotationDelta = classDelta.getAnnotationDeltas() != null;
        boolean modifierDelta = classDelta.getModifierDeltas() != null;
        boolean typeVariableDelta = classDelta.getTypeVariableDeltas() != null;
        boolean superClassDelta = classDelta.getSuperClassDelta() != null;
        boolean interfaceDelta = classDelta.getInterfaceDeltas() != null;
        boolean hasSignatureDelta = annotationDelta || modifierDelta
                || typeVariableDelta || superClassDelta || interfaceDelta;

        template.setAttribute("has_class_signature_delta", hasSignatureDelta);

        template.setAttribute("removed_fields", removedFields);
        template.setAttribute("added_fields", addedFields);
        template.setAttribute("changed_fields", changedFields);

        template.setAttribute("removed_annotation_fields",
                removedAnnotationFields);
        template.setAttribute("added_annotation_fields", addedAnnotationFields);
        template.setAttribute("changed_annotation_fields",
                changedAnnotationFields);

        template.setAttribute("removed_enum_constants", removedEnumConstants);
        template.setAttribute("added_enum_constants", addedEnumConstants);
        template.setAttribute("changed_enum_constants", changedEnumConstants);

        template.setAttribute("removed_constructors", removedConstructors);
        template.setAttribute("added_constructors", addedConstructors);
        template.setAttribute("changed_constructors", changedConstructors);

        template.setAttribute("removed_methods", removedMethods);
        template.setAttribute("added_methods", addedMethods);
        template.setAttribute("changed_methods", changedMethods);

        template.setArgumentContext(commonInfos);
        b.append(template.toString());
    }
}
