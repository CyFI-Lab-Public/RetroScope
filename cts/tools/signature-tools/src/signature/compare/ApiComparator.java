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

package signature.compare;

import signature.compare.model.IAnnotationDelta;
import signature.compare.model.IAnnotationElementDelta;
import signature.compare.model.IAnnotationFieldDelta;
import signature.compare.model.IApiDelta;
import signature.compare.model.IClassDefinitionDelta;
import signature.compare.model.IConstructorDelta;
import signature.compare.model.IDelta;
import signature.compare.model.IEnumConstantDelta;
import signature.compare.model.IFieldDelta;
import signature.compare.model.IGenericDeclarationDelta;
import signature.compare.model.IMethodDelta;
import signature.compare.model.IModifierDelta;
import signature.compare.model.IPackageDelta;
import signature.compare.model.IParameterDelta;
import signature.compare.model.IParameterizedTypeDelta;
import signature.compare.model.IPrimitiveTypeDelta;
import signature.compare.model.ITypeReferenceDelta;
import signature.compare.model.ITypeVariableDefinitionDelta;
import signature.compare.model.IUpperBoundsDelta;
import signature.compare.model.IValueDelta;
import signature.compare.model.IWildcardTypeDelta;
import signature.compare.model.impl.SigAnnotationDelta;
import signature.compare.model.impl.SigAnnotationElementDelta;
import signature.compare.model.impl.SigAnnotationFieldDelta;
import signature.compare.model.impl.SigApiDelta;
import signature.compare.model.impl.SigArrayTypeDelta;
import signature.compare.model.impl.SigClassDefinitionDelta;
import signature.compare.model.impl.SigClassReferenceDelta;
import signature.compare.model.impl.SigConstructorDelta;
import signature.compare.model.impl.SigEnumConstantDelta;
import signature.compare.model.impl.SigFieldDelta;
import signature.compare.model.impl.SigGenericDeclarationDelta;
import signature.compare.model.impl.SigMethodDelta;
import signature.compare.model.impl.SigModifierDelta;
import signature.compare.model.impl.SigPackageDelta;
import signature.compare.model.impl.SigParameterDelta;
import signature.compare.model.impl.SigParameterizedTypeDelta;
import signature.compare.model.impl.SigPrimitiveTypeDelta;
import signature.compare.model.impl.SigTypeDelta;
import signature.compare.model.impl.SigTypeVariableDefinitionDelta;
import signature.compare.model.impl.SigTypeVariableReferenceDelta;
import signature.compare.model.impl.SigUpperBoundsDelta;
import signature.compare.model.impl.SigValueDelta;
import signature.compare.model.impl.SigWildcardTypeDelta;
import signature.compare.model.subst.ClassProjection;
import signature.compare.model.subst.ViewpointAdapter;
import signature.model.IAnnotation;
import signature.model.IAnnotationElement;
import signature.model.IAnnotationField;
import signature.model.IApi;
import signature.model.IArrayType;
import signature.model.IClassDefinition;
import signature.model.IClassReference;
import signature.model.IConstructor;
import signature.model.IEnumConstant;
import signature.model.IExecutableMember;
import signature.model.IField;
import signature.model.IGenericDeclaration;
import signature.model.IMethod;
import signature.model.IPackage;
import signature.model.IParameter;
import signature.model.IParameterizedType;
import signature.model.IPrimitiveType;
import signature.model.ITypeReference;
import signature.model.ITypeVariableDefinition;
import signature.model.ITypeVariableReference;
import signature.model.IWildcardType;
import signature.model.Kind;
import signature.model.Modifier;
import signature.model.impl.SigAnnotationElement;
import signature.model.impl.SigArrayType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * {@code ApiComparator} takes two signature models as input and creates a delta
 * model describing the differences between those.
 */
public class ApiComparator implements IApiComparator {

    public IApiDelta compare(IApi from, IApi to) {
        assert from.getVisibility() == to.getVisibility();

        Set<IPackage> fromPackages = from.getPackages();
        Set<IPackage> toPackages = to.getPackages();

        Set<IPackageDelta> packageDeltas = compareSets(fromPackages,
                toPackages, new SigComparator<IPackage, IPackageDelta>() {
                    public IPackageDelta createChangedDelta(IPackage from,
                            IPackage to) {
                        return comparePackage(from, to);
                    }

                    public IPackageDelta createAddRemoveDelta(IPackage from,
                            IPackage to) {
                        return new SigPackageDelta(from, to);
                    }

                    public boolean considerEqualElement(IPackage from,
                            IPackage to) {
                        return from.getName().equals(to.getName());
                    }
                });

        SigApiDelta delta = null;
        if (packageDeltas != null) {
            delta = new SigApiDelta(from, to);
            delta.setPackageDeltas(packageDeltas);
        }
        return delta;
    }

    private IPackageDelta comparePackage(IPackage from, IPackage to) {
        assert from.getName().equals(to.getName());

        Set<IClassDefinition> fromClasses = from.getClasses();
        Set<IClassDefinition> toClasses = to.getClasses();

        Set<IClassDefinitionDelta> classDeltas = compareSets(fromClasses,
                toClasses,
                new SigComparator<IClassDefinition, IClassDefinitionDelta>() {
                    public boolean considerEqualElement(IClassDefinition from,
                            IClassDefinition to) {
                        return sameClassDefinition(from, to);
                    }

                    public IClassDefinitionDelta createChangedDelta(
                            IClassDefinition from, IClassDefinition to) {
                        return compareClass(from, to);
                    }

                    public IClassDefinitionDelta createAddRemoveDelta(
                            IClassDefinition from, IClassDefinition to) {
                        return new SigClassDefinitionDelta(from, to);
                    }
                });

        SigPackageDelta delta = null;
        if (classDeltas != null) {
            delta = new SigPackageDelta(from, to);
            delta.setClassDeltas(classDeltas);
        }

        // Annotations
        Set<IAnnotationDelta> annotationDeltas = compareAnnotations(from
                .getAnnotations(), to.getAnnotations());
        if (annotationDeltas != null) {
            if (delta != null) {
                delta = new SigPackageDelta(from, to);
            }
            delta.setAnnotationDeltas(annotationDeltas);
        }
        return delta;
    }

    private IClassDefinitionDelta compareClass(IClassDefinition from,
            IClassDefinition to) {
        assert from.getKind() == to.getKind();
        assert from.getName().equals(to.getName());
        assert from.getPackageName().equals(to.getPackageName());

        SigClassDefinitionDelta classDelta = null;

        // modifiers
        Set<IModifierDelta> modifierDeltas = compareModifiers(from
                .getModifiers(), to.getModifiers());
        if (modifierDeltas != null) {
            if (classDelta == null) {
                classDelta = new SigClassDefinitionDelta(from, to);
            }
            classDelta.setModifierDeltas(modifierDeltas);
        }

        // super class
        ITypeReferenceDelta<?> superTypeDelta = compareType(from
                .getSuperClass(), to.getSuperClass(), false);
        if (superTypeDelta != null) {
            if (classDelta == null) {
                classDelta = new SigClassDefinitionDelta(from, to);
            }
            classDelta.setSuperClassDelta(superTypeDelta);
        }

        // interfaces
        Set<ITypeReferenceDelta<?>> interfaceDeltas = compareInterfaces(from,
                to);
        if (interfaceDeltas != null) {
            if (classDelta == null) {
                classDelta = new SigClassDefinitionDelta(from, to);
            }
            classDelta.setInterfaceDeltas(interfaceDeltas);
        }

        // type variables
        Set<ITypeVariableDefinitionDelta> typeVariableDeltas =
                compareTypeVariableSequence(from.getTypeParameters(),
                        to.getTypeParameters());
        if (typeVariableDeltas != null) {
            if (classDelta == null) {
                classDelta = new SigClassDefinitionDelta(from, to);
            }
            classDelta.setTypeVariableDeltas(typeVariableDeltas);
        }

        // constructors
        Set<IConstructorDelta> constructorDeltas = compareConstructors(from
                .getConstructors(), to.getConstructors());
        if (constructorDeltas != null) {
            if (classDelta == null) {
                classDelta = new SigClassDefinitionDelta(from, to);
            }
            classDelta.setConstructorDeltas(constructorDeltas);
        }

        // methods
        Set<IMethodDelta> methodDeltas = compareMethods(from, to);
        if (methodDeltas != null) {
            if (classDelta == null) {
                classDelta = new SigClassDefinitionDelta(from, to);
            }
            classDelta.setMethodDeltas(methodDeltas);
        }

        // fields
        Set<IFieldDelta> fieldDeltas = compareFields(from.getFields(), to
                .getFields());
        if (fieldDeltas != null) {
            if (classDelta == null) {
                classDelta = new SigClassDefinitionDelta(from, to);
            }
            classDelta.setFieldDeltas(fieldDeltas);
        }

        // enum constants
        if (from.getKind() == Kind.ENUM) {
            Set<IEnumConstantDelta> enumDeltas = compareEnumConstants(from
                    .getEnumConstants(), to.getEnumConstants());
            if (enumDeltas != null) {
                if (classDelta == null) {
                    classDelta = new SigClassDefinitionDelta(from, to);
                }
                classDelta.setEnumConstantDeltas(enumDeltas);
            }
        } else if (from.getKind() == Kind.ANNOTATION) {
            Set<IAnnotationFieldDelta> annotationFieldDeltas =
                    compareAnnotationFields(from.getAnnotationFields(),
                            to.getAnnotationFields());
            if (annotationFieldDeltas != null) {
                if (classDelta == null) {
                    classDelta = new SigClassDefinitionDelta(from, to);
                }
                classDelta.setAnnotationFieldDeltas(annotationFieldDeltas);
            }
        }

        Set<IAnnotationDelta> annotationDeltas = compareAnnotations(from
                .getAnnotations(), to.getAnnotations());
        if (annotationDeltas != null) {
            if (classDelta == null) {
                classDelta = new SigClassDefinitionDelta(from, to);
            }
            classDelta.setAnnotationDeltas(annotationDeltas);
        }
        return classDelta;
    }

    private Set<ITypeReferenceDelta<?>> compareInterfaces(
            IClassDefinition from, IClassDefinition to) {
        Set<ITypeReference> fromClosure = getInterfaceClosure(from);
        Set<ITypeReference> toClosure = getInterfaceClosure(to);

        Set<ITypeReference> fromInterfaces = from.getInterfaces();
        Set<ITypeReference> toInterfaces = to.getInterfaces();

        Set<ITypeReferenceDelta<?>> deltas =
                new HashSet<ITypeReferenceDelta<?>>();

        // check whether all from interfaces are directly or indirectly
        // implemented by the to method
        for (ITypeReference type : fromInterfaces) {
            if (!containsType(type, toInterfaces)) {
                if (!(containsType(type, toClosure) /*
                                                     * && !containsType(type,
                                                     * toInterfaces)
                                                     */)) {
                    deltas.add(new SigTypeDelta<ITypeReference>(type, null));
                }
            }
        }

        // check whether all interfaces to are directly or indirectly
        // implemented by the from method
        for (ITypeReference type : toInterfaces) {
            if (!containsType(type, fromInterfaces)) {
                if (!(containsType(type, fromClosure) /*
                                                       * && !containsType(type,
                                                       * fromInterfaces)
                                                       */)) {
                    deltas.add(new SigTypeDelta<ITypeReference>(null, type));
                }
            }
        }
        return deltas.isEmpty() ? null : deltas;
    }


    private boolean containsType(ITypeReference type,
            Set<ITypeReference> setOfTypes) {
        for (ITypeReference other : setOfTypes) {
            if (compareType(type, other, false) == null) {
                return true;
            }
        }
        return false;
    }

    private Set<ITypeReference> getInterfaceClosure(IClassDefinition clazz) {
        Set<ITypeReference> closure = new HashSet<ITypeReference>();
        collectInterfaceClosure(ViewpointAdapter.getReferenceTo(clazz),
                closure);
        return closure;
    }

    private void collectInterfaceClosure(ITypeReference clazz,
            Set<ITypeReference> closure) {

        IClassDefinition classDefinition = getClassDefinition(clazz);
        Set<ITypeReference> interfaces = classDefinition.getInterfaces();
        if (interfaces == null) {
            return;
        }
        for (ITypeReference interfaze : interfaces) {
            closure.add(interfaze);
        }

        ITypeReference superclass = classDefinition.getSuperClass();
        if (superclass != null) {
            if (superclass instanceof IParameterizedType) {
                collectInterfaceClosure(((IParameterizedType) superclass)
                        .getRawType(), closure);
            } else {
                collectInterfaceClosure(superclass, closure);
            }
        }
        for (ITypeReference interfaze : interfaces) {
            if (interfaze instanceof IParameterizedType) {
                collectInterfaceClosure(((IParameterizedType) interfaze)
                        .getRawType(), closure);
            } else {
                collectInterfaceClosure(interfaze, closure);
            }
        }
    }

    private Set<IAnnotationDelta> compareAnnotations(Set<IAnnotation> from,
            Set<IAnnotation> to) {
        return compareSets(from, to,
                new SigComparator<IAnnotation, IAnnotationDelta>() {
                    public IAnnotationDelta createAddRemoveDelta(
                            IAnnotation from, IAnnotation to) {
                        return new SigAnnotationDelta(from, to);
                    }

                    public boolean considerEqualElement(IAnnotation from,
                            IAnnotation to) {
                        return sameClassDefinition(from.getType()
                                .getClassDefinition(), to.getType()
                                .getClassDefinition());
                    }

                    public IAnnotationDelta createChangedDelta(
                            IAnnotation from, IAnnotation to) {
                        return compareAnnotation(from, to);
                    }
                });
    }

    private Set<IAnnotationFieldDelta> compareAnnotationFields(
            Set<IAnnotationField> from, Set<IAnnotationField> to) {
        return compareSets(from, to,
                new SigComparator<IAnnotationField, IAnnotationFieldDelta>() {
                    public boolean considerEqualElement(IAnnotationField from,
                            IAnnotationField to) {
                        return from.getName().equals(to.getName());
                    }

                    public IAnnotationFieldDelta createAddRemoveDelta(
                            IAnnotationField from, IAnnotationField to) {
                        return new SigAnnotationFieldDelta(from, to);
                    }

                    public IAnnotationFieldDelta createChangedDelta(
                            IAnnotationField from, IAnnotationField to) {
                        return compareAnnotationField(from, to);
                    }
                });
    }

    private Set<IEnumConstantDelta> compareEnumConstants(
            Set<IEnumConstant> from, Set<IEnumConstant> to) {
        return compareSets(from, to,
                new SigComparator<IEnumConstant, IEnumConstantDelta>() {
                    public boolean considerEqualElement(IEnumConstant from,
                            IEnumConstant to) {
                        return from.getName().equals(to.getName());
                    }

                    public IEnumConstantDelta createAddRemoveDelta(
                            IEnumConstant from, IEnumConstant to) {
                        return new SigEnumConstantDelta(from, to);
                    }

                    public IEnumConstantDelta createChangedDelta(
                            IEnumConstant from, IEnumConstant to) {
                        return compareEnumConstant(from, to);
                    }
                });
    }

    private Set<IFieldDelta> compareFields(Set<IField> from, Set<IField> to) {
        return compareSets(from, to, new SigComparator<IField, IFieldDelta>() {
            public boolean considerEqualElement(IField from, IField to) {
                return from.getName().equals(to.getName());
            }

            public IFieldDelta createAddRemoveDelta(IField from, IField to) {
                return new SigFieldDelta(from, to);
            }

            public IFieldDelta createChangedDelta(IField from, IField to) {
                return compareField(from, to);
            }
        });
    }

    private Set<IMethodDelta> compareMethods(IClassDefinition from,
            IClassDefinition to) {
        assert from != null;
        assert to != null;

        Set<IMethod> toMethods = new HashSet<IMethod>(to.getMethods());
        Set<IMethod> toClosure = getMethodClosure(to);
        Set<IMethod> fromMethods = new HashSet<IMethod>(from.getMethods());
        Set<IMethod> fromClosure = getMethodClosure(from);

        Set<IMethodDelta> deltas = new HashSet<IMethodDelta>();

        for (IMethod method : fromMethods) {
            IMethod compatibleMethod = findCompatibleMethod(method, toMethods);
            if (compatibleMethod == null) {
                compatibleMethod = findCompatibleMethod(method, toClosure);
                if (compatibleMethod == null) {
                    deltas.add(new SigMethodDelta(method, null));
                }
            }

            if (compatibleMethod != null) {
                IMethodDelta delta = compareMethod(method, compatibleMethod);
                if (delta != null) {
                    deltas.add(delta);
                }
            }
        }

        for (IMethod method : toMethods) {
            IMethod compatibleMethod = findCompatibleMethod(method, fromMethods);
            if (compatibleMethod == null) {
                compatibleMethod = findCompatibleMethod(method, fromClosure);
                if (compatibleMethod == null) {
                    deltas.add(new SigMethodDelta(null, method));
                }
            }
        }
        return deltas.isEmpty() ? null : deltas;
    }

    private IMethod findCompatibleMethod(IMethod method, Set<IMethod> set) {
        for (IMethod methodFromSet : set) {
            if (equalsSignature(method, methodFromSet)) {
                return methodFromSet;
            }
        }
        return null;
    }


    private Set<IMethod> getMethodClosure(IClassDefinition clazz) {
        Set<IMethod> closure = new HashSet<IMethod>();
        collectMethods(new ClassProjection(clazz,
                new HashMap<ITypeVariableDefinition, ITypeReference>()),
                closure);
        return closure;
    }

    private void collectMethods(IClassDefinition clazz, Set<IMethod> closure) {
        if (clazz == null) {
            return;
        }
        if (clazz.getMethods() != null) {
            closure.addAll(clazz.getMethods());
        }
        if (clazz.getSuperClass() != null) {
            collectMethods(getClassDefinition(clazz.getSuperClass()), closure);
        }
        if (clazz.getInterfaces() != null) {
            for (ITypeReference interfaze : clazz.getInterfaces()) {
                collectMethods(getClassDefinition(interfaze), closure);
            }
        }
    }

    private Set<IConstructorDelta> compareConstructors(Set<IConstructor> from,
            Set<IConstructor> to) {
        return compareSets(from, to,
                new SigComparator<IConstructor, IConstructorDelta>() {
                    public boolean considerEqualElement(IConstructor from,
                            IConstructor to) {
                        return equalsSignature(from, to);
                    }

                    public IConstructorDelta createAddRemoveDelta(
                            IConstructor from, IConstructor to) {
                        return new SigConstructorDelta(from, to);
                    }

                    public IConstructorDelta createChangedDelta(
                            IConstructor from, IConstructor to) {
                        return compareConstructor(from, to);
                    }
                });
    }

    // compares names and parameter types
    private boolean equalsSignature(IExecutableMember from,
            IExecutableMember to) {
        if (from.getName().equals(to.getName())) {
            return compareTypeSequence(getParameterList(from.getParameters()),
                    getParameterList(to.getParameters()), true) == null;
        }
        return false;
    }

    private List<ITypeReference> getParameterList(List<IParameter> parameters) {
        List<ITypeReference> parameterTypes = new LinkedList<ITypeReference>();
        for (IParameter parameter : parameters) {
            parameterTypes.add(parameter.getType());
        }
        return parameterTypes;
    }

    private IAnnotationDelta compareAnnotation(IAnnotation from,
            IAnnotation to) {
        assert sameClassDefinition(from.getType().getClassDefinition(), to
                .getType().getClassDefinition());

        Set<IAnnotationElement> fromAnnotationElement =
                getNormalizedAnnotationElements(from);
        Set<IAnnotationElement> toAnnotationElement =
                getNormalizedAnnotationElements(to);

        Set<IAnnotationElementDelta> annotationElementDeltas =
                compareAnnotationElements(
                        fromAnnotationElement, toAnnotationElement);
        SigAnnotationDelta delta = null;

        if (annotationElementDeltas != null) {
            delta = new SigAnnotationDelta(from, to);
            delta.setAnnotationElementDeltas(annotationElementDeltas);
        }
        return delta;
    }

    /**
     * Returns the annotation elements for the given annotation. The returned
     * set contains all declared elements plus all elements with default values.
     * 
     * @param annotation
     *            the annotation to return the elements for
     * @return the default enriched annotation elements
     */
    private Set<IAnnotationElement> getNormalizedAnnotationElements(
            IAnnotation annotation) {
        Set<IAnnotationElement> elements = new HashSet<IAnnotationElement>(
                annotation.getElements());

        Set<String> names = new HashSet<String>();
        for (IAnnotationElement annotationElement : elements) {
            names.add(annotationElement.getDeclaringField().getName());
        }

        for (IAnnotationField field : annotation.getType().getClassDefinition()
                .getAnnotationFields()) {
            if (!names.contains(field.getName())) {
                SigAnnotationElement sigAnnotationElement =
                        new SigAnnotationElement();
                sigAnnotationElement.setDeclaringField(field);
                sigAnnotationElement.setValue(field.getDefaultValue());
                elements.add(sigAnnotationElement);
            }
        }
        return elements;
    }

    private Set<IAnnotationElementDelta> compareAnnotationElements(
            Set<IAnnotationElement> from, Set<IAnnotationElement> to) {
        return compareSets(from, to,
                new SigComparator<IAnnotationElement, IAnnotationElementDelta>() {
                    public boolean considerEqualElement(
                            IAnnotationElement from, IAnnotationElement to) {
                        return from.getDeclaringField().getName().equals(
                                to.getDeclaringField().getName());
                    }

                    public IAnnotationElementDelta createAddRemoveDelta(
                            IAnnotationElement from, IAnnotationElement to) {
                        return new SigAnnotationElementDelta(from, to);
                    }

                    public IAnnotationElementDelta createChangedDelta(
                            IAnnotationElement from, IAnnotationElement to) {
                        return compareAnnotationElement(from, to);
                    }
                });
    }

    private IAnnotationElementDelta compareAnnotationElement(
            IAnnotationElement from, IAnnotationElement to) {
        SigAnnotationElementDelta delta = null;
        SigValueDelta valueDelta = compareValue(from.getValue(), to.getValue());

        if (valueDelta != null) {
            delta = new SigAnnotationElementDelta(from, to);
            delta.setValueDelta(valueDelta);
        }
        return delta;
    }

    /**
     * Removes the {@link Modifier#ABSTRACT} modifier.
     */
    private Set<Modifier> prepareMethodModifiers(IMethod method) {
        Set<Modifier> modifierCopy = new HashSet<Modifier>(method
                .getModifiers());
        modifierCopy.remove(Modifier.ABSTRACT);
        return modifierCopy;
    }

    private IMethodDelta compareMethod(IMethod from, IMethod to) {
        assert from != null && to != null;

        SigMethodDelta methodDelta = null;
        Set<IModifierDelta> modiferDeltas = compareModifiers(
                prepareMethodModifiers(from), prepareMethodModifiers(to));
        if (modiferDeltas != null) {
            methodDelta = new SigMethodDelta(from, to);
            methodDelta.setModifierDeltas(modiferDeltas);
        }

        Set<IParameterDelta> parameterDeltas = compareParameterSequence(from
                .getParameters(), to.getParameters());
        if (parameterDeltas != null) {
            if (methodDelta == null) {
                methodDelta = new SigMethodDelta(from, to);
            }
            methodDelta.setParameterDeltas(parameterDeltas);
        }

        Set<IAnnotationDelta> annotationDeltas = compareAnnotations(from
                .getAnnotations(), to.getAnnotations());
        if (annotationDeltas != null) {
            if (methodDelta == null) {
                methodDelta = new SigMethodDelta(from, to);
            }
            methodDelta.setAnnotationDeltas(annotationDeltas);
        }

        Set<ITypeVariableDefinitionDelta> typeParameterDeltas =
                compareTypeVariableSequence(from.getTypeParameters(),
                        to.getTypeParameters());
        if (typeParameterDeltas != null) {
            if (methodDelta == null) {
                methodDelta = new SigMethodDelta(from, to);
            }
            methodDelta.setTypeVariableDeltas(typeParameterDeltas);
        }

        Set<ITypeReferenceDelta<?>> exceptionDeltas = compareTypes(
                normalizeExceptions(from.getExceptions()),
                normalizeExceptions(to.getExceptions()));
        if (exceptionDeltas != null) {
            if (methodDelta == null) {
                methodDelta = new SigMethodDelta(from, to);
            }
            methodDelta.setExceptionDeltas(exceptionDeltas);
        }

        ITypeReferenceDelta<?> returnTypeDelta = compareType(from
                .getReturnType(), to.getReturnType(), false);
        if (returnTypeDelta != null) {
            if (methodDelta == null) {
                methodDelta = new SigMethodDelta(from, to);
            }
            methodDelta.setReturnTypeDelta(returnTypeDelta);
        }

        return methodDelta;
    }

    // remove runtime exceptions,
    // remove sub types of containing exception
    private Set<ITypeReference> normalizeExceptions(
            Set<ITypeReference> exceptions) {
        Set<ITypeReference> exceptionCopy = new HashSet<ITypeReference>(
                exceptions);

        Iterator<ITypeReference> iterator = exceptionCopy.iterator();
        while (iterator.hasNext()) {
            ITypeReference exception = iterator.next();
            if (isRuntimeExceptionOrErrorSubtype(exception)) {
                iterator.remove();
            }
        }
        exceptionCopy = removeSpecializations(exceptionCopy);
        return exceptionCopy;
    }

    private Set<ITypeReference> removeSpecializations(
            Set<ITypeReference> exceptions) {
        Set<ITypeReference> exceptionCopy = new HashSet<ITypeReference>(
                exceptions);
        for (ITypeReference type : exceptions) {
            Iterator<ITypeReference> it = exceptionCopy.iterator();
            while (it.hasNext()) {
                ITypeReference subType = it.next();
                if (isSuperClass(getClassDefinition(type),
                        getClassDefinition(subType))) {
                    it.remove();
                }
            }
        }
        return exceptionCopy;
    }

    /**
     * Returns true if superC is a super class of subC.
     */
    private boolean isSuperClass(IClassDefinition superC,
            IClassDefinition subC) {
        if (superC == null || subC == null) {
            return false;
        }

        if (subC.getSuperClass() == null) {
            return false;
        } else {
            if (getClassDefinition(subC.getSuperClass()).equals(superC)) {
                return true;
            } else {
                return isSuperClass(superC, getClassDefinition(subC
                        .getSuperClass()));
            }
        }
    }

    private boolean isSuperInterface(IClassDefinition superClass,
            IClassDefinition subClass) {
        if (superClass == null || subClass == null) {
            return false;
        }

        if (subClass.getInterfaces() == null) {
            return false;
        } else {
            if (getClassDefinitions(subClass.getInterfaces()).contains(
                    superClass)) {
                return true;
            } else {
                for (ITypeReference subType : subClass.getInterfaces()) {
                    if (isSuperInterface(superClass,
                            getClassDefinition(subType))) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    private Set<IClassDefinition> getClassDefinitions(
            Set<ITypeReference> references) {
        Set<IClassDefinition> definitions = new HashSet<IClassDefinition>();
        for (ITypeReference ref : references) {
            definitions.add(getClassDefinition(ref));
        }
        return definitions;
    }

    /**
     * Returns null if type is not one of:
     * <ul>
     * <li>IClassReference</li>
     * <li>IParameterizedType</li>
     * </ul>
     */
    private IClassDefinition getClassDefinition(ITypeReference type) {
        assert type != null;

        IClassDefinition returnValue = null;
        if (type instanceof IClassReference) {
            returnValue = ((IClassReference) type).getClassDefinition();
        } else if (type instanceof IParameterizedType) {
            returnValue = ((IParameterizedType) type).getRawType()
                    .getClassDefinition();
        }
        return returnValue;
    }

    private boolean isRuntimeExceptionOrErrorSubtype(ITypeReference exception) {

        IClassDefinition clazz = getClassDefinition(exception);
        if (clazz != null) {
            if (isRuntimeExceptionOrError(clazz)) {
                return true;
            } else if (clazz.getSuperClass() != null) {
                return isRuntimeExceptionOrErrorSubtype(clazz.getSuperClass());
            } else {
                return false;
            }
        }
        return false;
    }

    private boolean isRuntimeExceptionOrError(IClassDefinition exception) {
        if (exception == null) {
            return false;
        }
        String packageName = exception.getPackageName();
        String className = exception.getName();

        if (packageName != null && className != null
                && "java.lang".equals(packageName)) {
            return "RuntimeException".equals(className)
                    || "Error".equals(className);
        }
        return false;
    }

    private IConstructorDelta compareConstructor(IConstructor from,
            IConstructor to) {
        SigConstructorDelta constructorDelta = null;
        Set<IModifierDelta> modiferDeltas = compareModifiers(from
                .getModifiers(), to.getModifiers());
        if (modiferDeltas != null) {
            constructorDelta = new SigConstructorDelta(from, to);
            constructorDelta.setModifierDeltas(modiferDeltas);
        }

        Set<IParameterDelta> parameterDeltas = compareParameterSequence(from
                .getParameters(), to.getParameters());
        if (parameterDeltas != null) {
            if (constructorDelta == null) {
                constructorDelta = new SigConstructorDelta(from, to);
            }
            constructorDelta.setParameterDeltas(parameterDeltas);
        }

        Set<IAnnotationDelta> annotationDeltas = compareAnnotations(from
                .getAnnotations(), to.getAnnotations());
        if (annotationDeltas != null) {
            if (constructorDelta == null) {
                constructorDelta = new SigConstructorDelta(from, to);
            }
            constructorDelta.setAnnotationDeltas(annotationDeltas);
        }

        Set<ITypeVariableDefinitionDelta> typeParameterDeltas =
                compareTypeVariableSequence(from.getTypeParameters(),
                        to.getTypeParameters());
        if (typeParameterDeltas != null) {
            if (constructorDelta == null) {
                constructorDelta = new SigConstructorDelta(from, to);
            }
            constructorDelta.setTypeVariableDeltas(typeParameterDeltas);
        }

        Set<ITypeReferenceDelta<?>> exceptionDeltas = compareTypes(
                normalizeExceptions(from.getExceptions()),
                normalizeExceptions(to.getExceptions()));
        if (exceptionDeltas != null) {
            if (constructorDelta == null) {
                constructorDelta = new SigConstructorDelta(from, to);
            }
            constructorDelta.setExceptionDeltas(exceptionDeltas);
        }
        return constructorDelta;
    }

    private Set<IParameterDelta> compareParameterSequence(
            List<IParameter> from, List<IParameter> to) {
        assert from.size() == to.size();
        Set<IParameterDelta> deltas = new HashSet<IParameterDelta>();
        Iterator<IParameter> fromIterator = from.iterator();
        Iterator<IParameter> toIterator = to.iterator();
        while (fromIterator.hasNext() && toIterator.hasNext()) {
            IParameterDelta delta = compareParameter(fromIterator.next(),
                    toIterator.next());
            if (delta != null) {
                deltas.add(delta);
            }
        }
        return deltas.isEmpty() ? null : deltas;
    }

    private IParameterDelta compareParameter(IParameter from, IParameter to) {
        SigParameterDelta delta = null;
        ITypeReferenceDelta<?> typeDelta = compareType(from.getType(), to
                .getType(), false);
        if (typeDelta != null) {
            if (delta == null) {
                delta = new SigParameterDelta(from, to);
            }
            delta.setTypeDelta(typeDelta);
        }

        Set<IAnnotationDelta> annotationDeltas = compareAnnotations(from
                .getAnnotations(), to.getAnnotations());
        if (annotationDeltas != null) {
            if (delta == null) {
                delta = new SigParameterDelta(from, to);
            }
            delta.setAnnotationDeltas(annotationDeltas);
        }
        return delta;
    }

    private Set<ITypeVariableDefinitionDelta> compareTypeVariableSequence(
            List<ITypeVariableDefinition> from,
            List<ITypeVariableDefinition> to) {
        Set<ITypeVariableDefinitionDelta> deltas =
                new HashSet<ITypeVariableDefinitionDelta>();
        if (from.size() != to.size()) {
            for (ITypeVariableDefinition fromVariable : from) {
                deltas.add(new SigTypeVariableDefinitionDelta(fromVariable,
                        null));
            }
            for (ITypeVariableDefinition toVariable : to) {
                deltas
                        .add(new SigTypeVariableDefinitionDelta(null,
                                toVariable));
            }
        }

        Iterator<ITypeVariableDefinition> fromIterator = from.iterator();
        Iterator<ITypeVariableDefinition> toIterator = to.iterator();
        while (fromIterator.hasNext() && toIterator.hasNext()) {
            ITypeVariableDefinitionDelta delta = compareTypeVariableDefinition(
                    fromIterator.next(), toIterator.next());
            if (delta != null) {
                deltas.add(delta);
            }
        }
        return deltas.isEmpty() ? null : deltas;
    }

    private ITypeVariableDefinitionDelta compareTypeVariableDefinition(
            ITypeVariableDefinition from, ITypeVariableDefinition to) {
        IGenericDeclarationDelta declarationDelta = compareGenericDeclaration(
                from, to);

        if (declarationDelta != null) {
            SigTypeVariableDefinitionDelta delta =
                    new SigTypeVariableDefinitionDelta(from, to);
            delta.setGenericDeclarationDelta(declarationDelta);
            return delta;
        }
        IUpperBoundsDelta upperBoundDelta = compareUpperBounds(from
                .getUpperBounds(), to.getUpperBounds());

        if (upperBoundDelta != null) {
            SigTypeVariableDefinitionDelta delta =
                    new SigTypeVariableDefinitionDelta(from, to);
            delta.setUpperBoundsDelta(upperBoundDelta);
            return delta;
        }
        return null;
    }

    private ITypeReferenceDelta<ITypeVariableReference> compareTypeVariableReference(
            ITypeVariableReference from, ITypeVariableReference to) {
        IGenericDeclarationDelta declarationDelta = compareGenericDeclaration(
                from.getTypeVariableDefinition(), to
                        .getTypeVariableDefinition());
        if (declarationDelta != null) {
            SigTypeVariableReferenceDelta delta =
                    new SigTypeVariableReferenceDelta(from, to);
            delta.setGenericDeclarationDelta(declarationDelta);
            return delta;
        }
        return null;
    }

    private Set<IModifierDelta> compareModifiers(Set<Modifier> from,
            Set<Modifier> to) {
        return compareSets(from, to,
                new SigComparator<Modifier, IModifierDelta>() {
                    public boolean considerEqualElement(Modifier from,
                            Modifier to) {
                        return from.equals(to);
                    }

                    public IModifierDelta createAddRemoveDelta(Modifier from,
                            Modifier to) {
                        return new SigModifierDelta(from, to);
                    }

                    public IModifierDelta createChangedDelta(Modifier from,
                            Modifier to) {
                        return null;
                    }
                });
    }


    private IFieldDelta compareField(IField from, IField to) {
        SigFieldDelta fieldDelta = null;

        Set<IModifierDelta> modiferDeltas = compareModifiers(from
                .getModifiers(), to.getModifiers());
        if (modiferDeltas != null) {
            fieldDelta = new SigFieldDelta(from, to);
            fieldDelta.setModifierDeltas(modiferDeltas);
        }

        Set<IAnnotationDelta> annotationDeltas = compareAnnotations(from
                .getAnnotations(), to.getAnnotations());
        if (annotationDeltas != null) {
            if (fieldDelta == null) {
                fieldDelta = new SigFieldDelta(from, to);
            }
            fieldDelta.setAnnotationDeltas(annotationDeltas);
        }

        ITypeReferenceDelta<?> typeDelta = compareType(from.getType(), to
                .getType(), false);
        if (typeDelta != null) {
            if (fieldDelta == null) {
                fieldDelta = new SigFieldDelta(from, to);
            }
            fieldDelta.setTypeDelta(typeDelta);
        }
        return fieldDelta;
    }

    private IEnumConstantDelta compareEnumConstant(IEnumConstant from,
            IEnumConstant to) {
        SigEnumConstantDelta enumConstantDelta = null;

        Set<IModifierDelta> modiferDeltas = compareModifiers(from
                .getModifiers(), to.getModifiers());
        if (modiferDeltas != null) {
            enumConstantDelta = new SigEnumConstantDelta(from, to);
            enumConstantDelta.setModifierDeltas(modiferDeltas);
        }

        Set<IAnnotationDelta> annotationDeltas = compareAnnotations(from
                .getAnnotations(), to.getAnnotations());
        if (annotationDeltas != null) {
            if (enumConstantDelta == null) {
                enumConstantDelta = new SigEnumConstantDelta(from, to);
            }
            enumConstantDelta.setAnnotationDeltas(annotationDeltas);
        }

        ITypeReferenceDelta<?> typeDelta = compareType(from.getType(), to
                .getType(), false);
        if (typeDelta != null) {
            if (enumConstantDelta == null) {
                enumConstantDelta = new SigEnumConstantDelta(from, to);
            }
            enumConstantDelta.setTypeDelta(typeDelta);
        }

        // FIXME ordinal not supported in dex
        // ValueDelta ordinalDelta = compareValue(from.getOrdinal(),
        // to.getOrdinal());
        // if (ordinalDelta != null) {
        // if (enumConstantDelta == null) {
        // enumConstantDelta = new SigEnumConstantDelta(from, to);
        // }
        // enumConstantDelta.setOrdinalDelta(ordinalDelta);
        // }

        return enumConstantDelta;
    }

    private IAnnotationFieldDelta compareAnnotationField(IAnnotationField from,
            IAnnotationField to) {
        SigAnnotationFieldDelta annotationFieldDelta = null;

        Set<IModifierDelta> modiferDeltas = compareModifiers(from
                .getModifiers(), to.getModifiers());
        if (modiferDeltas != null) {
            annotationFieldDelta = new SigAnnotationFieldDelta(from, to);
            annotationFieldDelta.setModifierDeltas(modiferDeltas);
        }

        Set<IAnnotationDelta> annotationDeltas = compareAnnotations(from
                .getAnnotations(), to.getAnnotations());
        if (annotationDeltas != null) {
            if (annotationFieldDelta == null) {
                annotationFieldDelta = new SigAnnotationFieldDelta(from, to);
            }
            annotationFieldDelta.setAnnotationDeltas(annotationDeltas);
        }

        ITypeReferenceDelta<?> typeDelta = compareType(from.getType(), to
                .getType(), false);
        if (typeDelta != null) {
            if (annotationFieldDelta == null) {
                annotationFieldDelta = new SigAnnotationFieldDelta(from, to);
            }
            annotationFieldDelta.setTypeDelta(typeDelta);
        }

        IValueDelta defaultValueDelta = compareValue(from.getDefaultValue(), to
                .getDefaultValue());
        if (defaultValueDelta != null) {
            if (annotationFieldDelta == null) {
                annotationFieldDelta = new SigAnnotationFieldDelta(from, to);
            }
            annotationFieldDelta.setDefaultValueDelta(defaultValueDelta);
        }

        return annotationFieldDelta;
    }

    private SigValueDelta compareValue(Object from, Object to) {
        // same value
        if (from == null && to == null) {
            return null;
        }

        // one of both is null and other is not
        if (from == null || to == null) {
            return new SigValueDelta(from, to);
        }

        SigValueDelta delta = null;
        // different types
        if (from.getClass() == to.getClass()) {
            if (from.getClass().isArray()) {
                Object[] fromArray = (Object[]) from;
                Object[] toArray = (Object[]) from;
                if (!Arrays.equals(fromArray, toArray)) {
                    delta = new SigValueDelta(from, to);
                }
            } else if (from instanceof IEnumConstant) {
                IEnumConstantDelta enumConstantDelta = compareEnumConstant(
                        (IEnumConstant) from, (IEnumConstant) to);
                if (enumConstantDelta != null) {
                    delta = new SigValueDelta(from, to);
                }
            } else if (from instanceof IAnnotation) {
                IAnnotationDelta annotationDelta = compareAnnotation(
                        (IAnnotation) from, (IAnnotation) to);
                if (annotationDelta != null) {
                    delta = new SigValueDelta(from, to);
                }
            } else if (from instanceof IField) {
                IFieldDelta fieldDelta = compareField((IField) from,
                        (IField) to);
                if (fieldDelta != null) {
                    delta = new SigValueDelta(from, to);
                }
            } else if (from instanceof ITypeReference) {
                ITypeReferenceDelta<? extends ITypeReference> typeDelta =
                        compareType((ITypeReference) from, (ITypeReference) to,
                                false);
                if (typeDelta != null) {
                    delta = new SigValueDelta(from, to);
                }
            } else if (!from.equals(to)) {
                delta = new SigValueDelta(from, to);
            }

        } else if (!(from == null && to == null)) {
            delta = new SigValueDelta(from, to);
        }
        return delta;
    }

    private boolean considerEqualTypes(ITypeReference from, ITypeReference to) {
        assert from != null && to != null;

        if (implementInterface(from, to, IPrimitiveType.class)) {
            return comparePrimitiveType((IPrimitiveType) from,
                    (IPrimitiveType) to) == null;
        }
        if (implementInterface(from, to, IClassReference.class)) {
            return sameClassDefinition(((IClassReference) from)
                    .getClassDefinition(), ((IClassReference) to)
                    .getClassDefinition());
        }
        if (implementInterface(from, to, IArrayType.class)) {
            return considerEqualTypes(((IArrayType) from).getComponentType(),
                    ((IArrayType) to).getComponentType());
        }
        if (implementInterface(from, to, IParameterizedType.class)) {
            return compareClassReference(((IParameterizedType) from)
                    .getRawType(), ((IParameterizedType) to)
                    .getRawType()) == null;
        }
        if (implementInterface(from, to, ITypeVariableReference.class)) {
            return compareTypeVariableReference((ITypeVariableReference) from,
                    (ITypeVariableReference) to) == null;
        }

        return false;
    }

    private Set<ITypeReference> fromComparison = new HashSet<ITypeReference>();
    private Set<ITypeReference> toComparison = new HashSet<ITypeReference>();


    private boolean areInComparison(ITypeReference from, ITypeReference to) {
        return fromComparison.contains(from) && toComparison.contains(to);
    }

    private void markInComparison(ITypeReference from, ITypeReference to) {
        fromComparison.add(from);
        toComparison.add(to);
    }

    private void markFinishedComparison(ITypeReference from,
            ITypeReference to) {
        fromComparison.remove(from);
        toComparison.remove(to);
    }

    private ITypeReferenceDelta<? extends ITypeReference> compareType(
            ITypeReference from, ITypeReference to, boolean acceptErasedTypes) {

        if (from == null && to == null) {
            return null;
        }
        if ((from == null && to != null) || (from != null && to == null)) {
            return new SigTypeDelta<ITypeReference>(from, to);
        }
        if (areInComparison(from, to)) {
            return null;
        }
        try {
            markInComparison(from, to);

            if (implementInterface(from, to, IPrimitiveType.class)) {
                return comparePrimitiveType((IPrimitiveType) from,
                        (IPrimitiveType) to);
            }
            if (implementInterface(from, to, IClassReference.class)) {
                return compareClassReference((IClassReference) from,
                        (IClassReference) to);
            }
            if (implementInterface(from, to, IArrayType.class)) {
                return compareArrayType((IArrayType) from, (IArrayType) to);
            }
            if (implementInterface(from, to, IParameterizedType.class)) {
                return compareParameterizedType((IParameterizedType) from,
                        (IParameterizedType) to, acceptErasedTypes);
            }
            if (implementInterface(from, to, ITypeVariableReference.class)) {
                return compareTypeVariableReference(
                        (ITypeVariableReference) from,
                        (ITypeVariableReference) to);
            }
            if (implementInterface(from, to, IWildcardType.class)) {
                return compareWildcardType((IWildcardType) from,
                        (IWildcardType) to);
            }

            if (acceptErasedTypes) {
                if (isGeneric(from) && !isGeneric(to)) {
                    return compareType(getErasedType(from), to, false);
                }

                if (!isGeneric(from) && isGeneric(to)) {
                    return compareType(from, getErasedType(to), false);
                }
            }
            return new SigTypeDelta<ITypeReference>(from, to);
        } finally {
            markFinishedComparison(from, to);
        }
    }

    private boolean isGeneric(ITypeReference reference) {
        if (reference instanceof IParameterizedType
                || reference instanceof ITypeVariableReference
                || reference instanceof IWildcardType) {
            return true;
        }
        if (reference instanceof IArrayType) {
            return isGeneric(((IArrayType) reference).getComponentType());
        }
        return false;
    }

    private ITypeReference getErasedType(ITypeReference reference) {

        if (reference instanceof IParameterizedType) {
            return ((IParameterizedType) reference).getRawType();
        }
        if (reference instanceof ITypeVariableReference) {
            ITypeVariableDefinition typeVariableDefinition =
                    ((ITypeVariableReference) reference)
                            .getTypeVariableDefinition();
            return getErasedType(
                    typeVariableDefinition.getUpperBounds().get(0));
        }
        if (reference instanceof IWildcardType) {
            return getErasedType(((IWildcardType) reference).getUpperBounds()
                    .get(0));
        }
        if (reference instanceof IArrayType) {
            // FIXME implement with erasure projection?
            return new SigArrayType(getErasedType(((IArrayType) reference)
                    .getComponentType()));
        }
        if (reference instanceof IPrimitiveType) {
            return reference;
        }
        if (reference instanceof IClassReference) {
            return reference;
        }
        throw new IllegalArgumentException("Unexpected type: " + reference);
    }

    private boolean implementInterface(ITypeReference from, ITypeReference to,
            Class<?> check) {
        return check.isAssignableFrom(from.getClass())
                && check.isAssignableFrom(to.getClass());
    }

    private IWildcardTypeDelta compareWildcardType(IWildcardType from,
            IWildcardType to) {
        SigWildcardTypeDelta delta = null;

        ITypeReference fromLowerBound = from.getLowerBound();
        ITypeReference toLowerBound = to.getLowerBound();

        ITypeReferenceDelta<?> lowerBoundDelta = compareType(fromLowerBound,
                toLowerBound, false);
        if (lowerBoundDelta != null) {
            delta = new SigWildcardTypeDelta(from, to);
            delta.setLowerBoundDelta(lowerBoundDelta);
        }

        IUpperBoundsDelta upperBoundsDelta = compareUpperBounds(from
                .getUpperBounds(), to.getUpperBounds());
        if (upperBoundsDelta != null) {
            if (delta == null) {
                delta = new SigWildcardTypeDelta(from, to);
            }
            delta.setUpperBoundDelta(upperBoundsDelta);
        }
        return delta;
    }

    private IGenericDeclarationDelta compareGenericDeclaration(
            ITypeVariableDefinition fromVariable,
            ITypeVariableDefinition toVariable) {
        IGenericDeclarationDelta delta = null;

        IGenericDeclaration from = fromVariable.getGenericDeclaration();
        IGenericDeclaration to = toVariable.getGenericDeclaration();

        if (from != null && to != null) {

            if (from.getClass() != to.getClass()) {
                delta = new SigGenericDeclarationDelta(from, to);
            } else if (from instanceof IClassDefinition) {
                IClassDefinition fromDeclaringClass = (IClassDefinition) from;
                IClassDefinition toDeclaringClass = (IClassDefinition) to;

                if (!sameClassDefinition(fromDeclaringClass,
                        toDeclaringClass)) {
                    delta = new SigGenericDeclarationDelta(from, to);
                }

            } else if (from instanceof IConstructor) {
                IConstructor fromConstructor = (IConstructor) from;
                IConstructor toConstructor = (IConstructor) from;

                String fromConstructorName = fromConstructor.getName();
                String fromClassName = fromConstructor.getDeclaringClass()
                        .getQualifiedName();

                String toConstructorName = toConstructor.getName();
                String toClassName = toConstructor.getDeclaringClass()
                        .getQualifiedName();

                if ((!fromConstructorName.equals(toConstructorName))
                        || (!fromClassName.equals(toClassName))) {
                    delta = new SigGenericDeclarationDelta(from, to);
                }

            } else if (from instanceof IMethod) {
                IMethod fromMethod = (IMethod) from;
                IMethod toMethod = (IMethod) from;

                String fromConstructorName = fromMethod.getName();
                String fromClassName = fromMethod.getDeclaringClass()
                        .getQualifiedName();

                String toConstructorName = toMethod.getName();
                String toClassName = toMethod.getDeclaringClass()
                        .getQualifiedName();

                if ((!fromConstructorName.equals(toConstructorName))
                        || (!fromClassName.equals(toClassName))) {
                    delta = new SigGenericDeclarationDelta(from, to);
                }
            } else {
                throw new IllegalStateException("Invlaid eclaration site: "
                        + from);
            }

            // check position
            int fromPosition = getPositionOf(fromVariable, from);
            int toPosition = getPositionOf(toVariable, to);

            if (fromPosition != toPosition) {
                delta = new SigGenericDeclarationDelta(from, to);
            }


        } else {
            // one of both is null
            delta = new SigGenericDeclarationDelta(from, to);
        }
        return delta;
    }

    private int getPositionOf(ITypeVariableDefinition variable,
            IGenericDeclaration declaration) {
        return declaration.getTypeParameters().indexOf(variable);
    }

    private IUpperBoundsDelta compareUpperBounds(List<ITypeReference> from,
            List<ITypeReference> to) {
        if (from.isEmpty() && to.isEmpty()) {
            return null;
        }
        SigUpperBoundsDelta delta = null;

        ITypeReference fromFirstUpperBound = from.get(0);
        ITypeReference toFirstUpperBound = to.get(0);

        ITypeReferenceDelta<?> firstUpperBoundDelta = compareType(
                fromFirstUpperBound, toFirstUpperBound, false);
        if (firstUpperBoundDelta != null) {
            delta = new SigUpperBoundsDelta(from, to);
            delta.setFirstUpperBoundDelta(firstUpperBoundDelta);
        } else {
            // normalize
            Set<ITypeReference> normalizedfrom = removeGeneralizations(
                    new HashSet<ITypeReference>(from));
            Set<ITypeReference> normalizedto = removeGeneralizations(
                    new HashSet<ITypeReference>(to));

            Set<ITypeReferenceDelta<?>> remainingUpperBoundsDelta =
                    compareTypes(normalizedfrom, normalizedto);
            if (remainingUpperBoundsDelta != null) {
                delta = new SigUpperBoundsDelta(from, to);
                delta.setRemainingUpperBoundDeltas(remainingUpperBoundsDelta);
            }
        }
        return delta;
    }

    private Set<ITypeReference> removeGeneralizations(
            Set<ITypeReference> bounds) {
        Set<ITypeReference> boundsCopy = new HashSet<ITypeReference>(bounds);
        for (ITypeReference type : bounds) {
            Iterator<ITypeReference> it = boundsCopy.iterator();
            while (it.hasNext()) {
                ITypeReference superType = it.next();
                if (isSuperClass(getClassDefinition(superType),
                        getClassDefinition(type))
                        || isSuperInterface(getClassDefinition(superType),
                                getClassDefinition(type))) {
                    it.remove();
                }
            }
        }
        return boundsCopy;
    }

    private IParameterizedTypeDelta compareParameterizedType(
            IParameterizedType from, IParameterizedType to,
            boolean ignoreTypeArguments) {

        SigParameterizedTypeDelta delta = null;
        // check raw type
        ITypeReferenceDelta<?> rawTypeDelta = compareType(from.getRawType(), to
                .getRawType(), false);
        if (rawTypeDelta != null) {
            delta = new SigParameterizedTypeDelta(from, to);
            delta.setRawTypeDelta(rawTypeDelta);
        } else {
            // check owner type
            ITypeReferenceDelta<?> ownerTypeDelta = compareType(from
                    .getOwnerType(), to.getOwnerType(), false);
            if (ownerTypeDelta != null) {
                delta = new SigParameterizedTypeDelta(from, to);
                delta.setOwnerTypeDelta(ownerTypeDelta);
            } else {
                // check argument type
                if (!ignoreTypeArguments) {
                    Set<ITypeReferenceDelta<?>> argumentTypeDeltas =
                            compareTypeSequence(from.getTypeArguments(),
                                    to.getTypeArguments(), false);
                    if (argumentTypeDeltas != null) {
                        delta = new SigParameterizedTypeDelta(from, to);
                        delta.setArgumentTypeDeltas(argumentTypeDeltas);
                    }
                }
            }
        }
        return delta;
    }

    private Set<ITypeReferenceDelta<? extends ITypeReference>> compareTypeSequence(
            List<ITypeReference> from, List<ITypeReference> to,
            boolean ignoreTypeArguments) {
        Set<ITypeReferenceDelta<?>> deltas =
                new HashSet<ITypeReferenceDelta<?>>();
        if (from.size() != to.size()) {

            for (ITypeReference type : from) {
                deltas.add(new SigTypeDelta<ITypeReference>(type, null));
            }
            for (ITypeReference type : to) {
                deltas.add(new SigTypeDelta<ITypeReference>(null, type));
            }
            return deltas;
        }

        Iterator<? extends ITypeReference> fromIterator = from.iterator();
        Iterator<? extends ITypeReference> toIterator = to.iterator();
        while (fromIterator.hasNext() && toIterator.hasNext()) {
            ITypeReferenceDelta<?> delta = compareType(fromIterator.next(),
                    toIterator.next(), ignoreTypeArguments);
            if (delta != null) {
                deltas.add(delta);
            }
        }
        return deltas.isEmpty() ? null : deltas;
    }

    private Set<ITypeReferenceDelta<? extends ITypeReference>> compareTypes(
            Set<ITypeReference> from, Set<ITypeReference> to) {
        return compareSets(from, to,
                new SigComparator<ITypeReference, ITypeReferenceDelta<? extends ITypeReference>>() {
                    public ITypeReferenceDelta<? extends ITypeReference> createAddRemoveDelta(
                            ITypeReference from, ITypeReference to) {
                        return new SigTypeDelta<ITypeReference>(from, to);
                    }

                    public boolean considerEqualElement(ITypeReference from,
                            ITypeReference to) {
                        return considerEqualTypes(from, to);
                    }

                    public ITypeReferenceDelta<? extends ITypeReference> createChangedDelta(
                            ITypeReference from, ITypeReference to) {
                        return compareType(from, to, false);
                    }
                });
    }

    private static interface SigComparator<T, S extends IDelta<? extends T>> {
        boolean considerEqualElement(T from, T to);

        S createChangedDelta(T from, T to);

        /**
         * If null is returned, it will be ignored.
         */
        S createAddRemoveDelta(T from, T to);
    }


    private <T, S extends IDelta<? extends T>> Set<S> compareSets(Set<T> from,
            Set<T> to, SigComparator<T, S> comparator) {

        Set<T> toCopy = new HashSet<T>(to);
        Set<S> deltas = new HashSet<S>();

        for (T fromType : from) {
            Iterator<T> toIterator = toCopy.iterator();
            boolean equals = false;
            boolean hasNext = toIterator.hasNext();

            while (hasNext && !equals) {
                T toElement = toIterator.next();
                equals = comparator.considerEqualElement(fromType, toElement);
                if (equals) {
                    S compare = comparator.createChangedDelta(fromType,
                            toElement);
                    if (compare != null) {
                        deltas.add(compare);
                    }
                }
                hasNext = toIterator.hasNext();
            }

            if (equals) {
                toIterator.remove();
            } else {
                S delta = comparator.createAddRemoveDelta(fromType, null);
                if (delta != null) {
                    deltas.add(delta);
                }
            }
        }

        for (T type : toCopy) {
            S delta = comparator.createAddRemoveDelta(null, type);
            if (delta != null) {
                deltas.add(delta);
            }
        }
        return deltas.isEmpty() ? null : deltas;
    }


    private ITypeReferenceDelta<?> compareArrayType(IArrayType from,
            IArrayType to) {
        ITypeReferenceDelta<?> componentTypeDelta = compareType(from
                .getComponentType(), to.getComponentType(), false);
        if (componentTypeDelta != null) {
            SigArrayTypeDelta delta = new SigArrayTypeDelta(from, to);
            delta.setComponentTypeDelta(componentTypeDelta);
            return delta;
        }
        return null;
    }

    private ITypeReferenceDelta<IClassReference> compareClassReference(
            IClassReference fromRef, IClassReference toRef) {
        IClassDefinition from = fromRef.getClassDefinition();
        IClassDefinition to = toRef.getClassDefinition();

        if (!sameClassDefinition(from, to)) {
            return new SigClassReferenceDelta(fromRef, toRef);
        }
        return null;
    }


    private boolean sameClassDefinition(IClassDefinition from,
            IClassDefinition to) {
        boolean sameName = from.getName().equals(to.getName());
        boolean samePackage = from.getPackageName().equals(to.getPackageName());

        Kind fromKind = from.getKind();
        Kind toKind = to.getKind();
        boolean sameKind = (fromKind == null || toKind == null)
                || fromKind.equals(toKind);

        return sameName && samePackage && sameKind;
    }

    private IPrimitiveTypeDelta comparePrimitiveType(IPrimitiveType from,
            IPrimitiveType to) {
        if (!from.equals(to)) {
            return new SigPrimitiveTypeDelta(from, to);
        }
        return null;
    }
}
