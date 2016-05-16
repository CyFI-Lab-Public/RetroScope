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

import static signature.converter.dex.DexUtil.convertAnyWay;
import static signature.converter.dex.DexUtil.declaresExceptions;
import static signature.converter.dex.DexUtil.declaresMemberClasses;
import static signature.converter.dex.DexUtil.findPackageInfo;
import static signature.converter.dex.DexUtil.getClassModifiers;
import static signature.converter.dex.DexUtil.getClassName;
import static signature.converter.dex.DexUtil.getDefaultMappingsAnnotation;
import static signature.converter.dex.DexUtil.getDexName;
import static signature.converter.dex.DexUtil.getEnclosingClassName;
import static signature.converter.dex.DexUtil.getExceptionSignature;
import static signature.converter.dex.DexUtil.getGenericSignature;
import static signature.converter.dex.DexUtil.getKind;
import static signature.converter.dex.DexUtil.getMemberClassNames;
import static signature.converter.dex.DexUtil.getModifier;
import static signature.converter.dex.DexUtil.getPackageName;
import static signature.converter.dex.DexUtil.getQualifiedName;
import static signature.converter.dex.DexUtil.hasAnnotationDefaultSignature;
import static signature.converter.dex.DexUtil.hasGenericSignature;
import static signature.converter.dex.DexUtil.isAnnotation;
import static signature.converter.dex.DexUtil.isConstructor;
import static signature.converter.dex.DexUtil.isEnclosingClass;
import static signature.converter.dex.DexUtil.isEnum;
import static signature.converter.dex.DexUtil.isInternalAnnotation;
import static signature.converter.dex.DexUtil.isJavaLangObject;
import static signature.converter.dex.DexUtil.isMethod;
import static signature.converter.dex.DexUtil.isVisible;
import static signature.converter.dex.DexUtil.splitTypeList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import signature.converter.Visibility;
import signature.model.IAnnotation;
import signature.model.IAnnotationElement;
import signature.model.IAnnotationField;
import signature.model.IClassDefinition;
import signature.model.IClassReference;
import signature.model.IConstructor;
import signature.model.IEnumConstant;
import signature.model.IField;
import signature.model.IMethod;
import signature.model.IPackage;
import signature.model.IParameter;
import signature.model.ITypeReference;
import signature.model.ITypeVariableDefinition;
import signature.model.Kind;
import signature.model.Modifier;
import signature.model.impl.SigAnnotation;
import signature.model.impl.SigAnnotationElement;
import signature.model.impl.SigAnnotationField;
import signature.model.impl.SigApi;
import signature.model.impl.SigClassDefinition;
import signature.model.impl.SigClassReference;
import signature.model.impl.SigConstructor;
import signature.model.impl.SigEnumConstant;
import signature.model.impl.SigExecutableMember;
import signature.model.impl.SigField;
import signature.model.impl.SigMethod;
import signature.model.impl.SigPackage;
import signature.model.impl.SigParameter;
import signature.model.impl.Uninitialized;
import signature.model.util.TypePool;
import dex.structure.DexAnnotation;
import dex.structure.DexAnnotationAttribute;
import dex.structure.DexClass;
import dex.structure.DexEncodedAnnotation;
import dex.structure.DexEncodedValue;
import dex.structure.DexField;
import dex.structure.DexFile;
import dex.structure.DexMethod;
import dex.structure.DexParameter;

/**
 * Converts a set of dex files to the signature compare api.
 */
public final class DexToSigConverter implements IClassInitializer {

    private final FieldPool elementPool;
    private final TypePool factory;
    private static final Set<IField> EMPTY_FIELDS = Collections.emptySet();
    private static final Set<IEnumConstant> EMPTY_ENUM_CONSTANTS = Collections
            .emptySet();
    private static final Set<IAnnotationField> EMPTY_ANNOTATION_FIELDS =
            Collections.emptySet();
    private static final List<ITypeVariableDefinition> EMPTY_TYPE_VARIABLES =
            Collections.emptyList();
    private static final Set<IClassDefinition> EMPTY_INNER_CLASSES =
            Collections.emptySet();
    private static final Set<ITypeReference> EMPTY_EXCEPTIONS = Collections
            .emptySet();
    private Visibility visibility;
    private Map<String, DexClass> dexNameToDexClass;


    /**
     * Creates a new instance of {@link DexToSigConverter}.
     */
    public DexToSigConverter() {
        factory = new TypePool();
        elementPool = new FieldPool();
    }


    public SigApi convertApi(String apiName, Set<DexFile> dexFiles,
            Visibility visibility) {
        this.visibility = visibility;
        SigApi api = new SigApi(apiName, visibility);
        api.setPackages(convertPackages(dexFiles));
        factory.replaceAllUninitialiezWithNull();
        return api;
    }

    /**
     * Converts the given {@link DexFile}s into the corresponding (packages
     * including their (classes and their members, etc.))E
     * 
     * @param parsedFiles
     *            the dex files to convert
     * @return the converted packages
     */
    /* package */Set<IPackage> convertPackages(Set<DexFile> parsedFiles) {
        Map<String, SigPackage> packageNameToPackage =
                new HashMap<String, SigPackage>();
        Map<SigPackage, Set<DexClass>> packageToDexClasses =
                new HashMap<SigPackage, Set<DexClass>>();

        dexNameToDexClass = new HashMap<String, DexClass>();

        for (DexFile dexFile : parsedFiles) {
            List<DexClass> definedClasses = dexFile.getDefinedClasses();
            for (DexClass dexClass : definedClasses) {

                dexNameToDexClass.put(dexClass.getName(), dexClass);

                String dexName = dexClass.getName();
                String packageName = getPackageName(dexName);
                SigPackage aPackage = packageNameToPackage.get(packageName);
                if (aPackage == null) {
                    aPackage = convertPackage(packageName);
                    packageNameToPackage.put(packageName, aPackage);

                    Set<DexClass> classes = new HashSet<DexClass>();
                    packageToDexClasses.put(aPackage, classes);
                }
                Set<DexClass> classes = packageToDexClasses.get(aPackage);
                classes.add(dexClass);
            }
        }

        Set<SigClassDefinition> allClasses = new HashSet<SigClassDefinition>();

        for (SigPackage aPackage : packageToDexClasses.keySet()) {
            Set<SigClassDefinition> classes = convertClasses(packageToDexClasses
                    .get(aPackage));
            allClasses.addAll(classes);
            aPackage.setClasses(new HashSet<IClassDefinition>(classes));
        }

        // remove package info
        for (SigPackage aPackage : packageToDexClasses.keySet()) {
            IClassDefinition packageInfo = findPackageInfo(aPackage);
            if (packageInfo != null) {
                aPackage.setAnnotations(packageInfo.getAnnotations());
                aPackage.getClasses().remove(packageInfo);
            }
        }

        // link enclosed classes only if they are part of visible api
        for (SigClassDefinition sigClass : allClasses) {
            String dexName = getDexName(sigClass);
            DexClass dexClass = dexNameToDexClass.get(dexName);

            if (declaresMemberClasses(dexClass)) {
                Set<String> enclosedClassesNames =
                        getMemberClassNames(dexClass);
                Set<IClassDefinition> memberClasses =
                        new HashSet<IClassDefinition>();
                for (String enclosedClassName : enclosedClassesNames) {
                    SigClassDefinition memberClass = factory.getClass(
                            getPackageName(enclosedClassName),
                            getClassName(enclosedClassName));
                    // add inner class only if parsed
                    if (allClasses.contains(memberClass)) {
                        memberClasses.add(memberClass);
                    }
                }
                sigClass.setInnerClasses(memberClasses);
            } else {
                sigClass.setInnerClasses(EMPTY_INNER_CLASSES);
            }
        }

        // remove inner classes, is outer class is not visible
        for (SigClassDefinition sigClass : allClasses) {
            if (hasInvisibleParent(sigClass, dexNameToDexClass)) {
                SigPackage sigPackage = packageNameToPackage.get(sigClass
                        .getPackageName());
                sigPackage.getClasses().remove(sigClass);
            }
        }
        return new HashSet<IPackage>(packageToDexClasses.keySet());
    }

    private boolean hasInvisibleParent(IClassDefinition sigClass,
            Map<String, DexClass> dexNameToDexClass) {

        do {
            String dexName = getDexName(sigClass);
            DexClass dexClass = dexNameToDexClass.get(dexName);
            if (isEnclosingClass(dexClass)) {
                IClassDefinition declaringClass = sigClass.getDeclaringClass();
                DexClass declaringDexClass = dexNameToDexClass
                        .get(getDexName(declaringClass));
                if (!isVisible(declaringDexClass, visibility)) {
                    return true;
                }
            }
        } while ((sigClass = sigClass.getDeclaringClass()) != null);
        return false;
    }

    /**
     * Converts a simple string to the corresponding {@link SigPackage}.<br>
     * Format: "a.b.c"
     * 
     * @param packageName
     *            the name of the package
     * @return the package
     */
    protected SigPackage convertPackage(String packageName) {
        SigPackage sigPackage = new SigPackage(packageName);
        return sigPackage;
    }

    /**
     * Converts a set of {@link DexClass} objects to a set of the corresponding
     * {@link SigClassDefinition} objects.
     * 
     * @param dexClasses
     *            the {@link DexClass} objects
     * @return a set of {@link DexClass} objects
     */
    protected Set<SigClassDefinition> convertClasses(Set<DexClass> dexClasses) {
        Set<SigClassDefinition> classes = new HashSet<SigClassDefinition>();
        for (DexClass dexClass : dexClasses) {
            // convert all classes but synthetic, return only initialized
            if (convertAnyWay(dexClass)) {
                SigClassDefinition sigCLass = convertClass(dexClass);
                if (isVisible(dexClass, visibility)) {
                    classes.add(sigCLass);
                }
            }
        }
        return classes;
    }

    /**
     * Converts a {@link DexClass} to the corresponding
     * {@link SigClassDefinition}.
     * 
     * @param dexClass
     *            the {@link DexClass} to convert
     * @return the corresponding {@link SigClassDefinition}
     */
    protected SigClassDefinition convertClass(DexClass dexClass) {
        assert dexClass != null;

        String packageName = getPackageName(dexClass.getName());
        String className = getClassName(dexClass.getName());
        SigClassDefinition sigClass = factory.getClass(packageName, className);
        // Kind
        sigClass.setKind(getKind(dexClass));
        // modifiers
        Set<Modifier> modifiers = getModifier(getClassModifiers(dexClass));
        sigClass.setModifiers(modifiers);

        if (isEnclosingClass(dexClass)) {
            String declaringClassDexName = getEnclosingClassName(dexClass);
            declaringClassDexName = getClassName(declaringClassDexName);
            // declaring class is in same package
            sigClass.setDeclaringClass(factory.getClass(sigClass
                    .getPackageName(), declaringClassDexName));
        } else {
            sigClass.setDeclaringClass(null);
        }

        if (hasGenericSignature(dexClass)) {
            GenericSignatureParser parser = new GenericSignatureParser(factory,
                    this);
            parser.parseForClass(sigClass, getGenericSignature(dexClass));
            sigClass.setTypeParameters(parser.formalTypeParameters);

            if (Kind.INTERFACE.equals(sigClass.getKind())) {
                sigClass.setSuperClass(null);
            } else {
                sigClass.setSuperClass(parser.superclassType);
            }

            sigClass.setInterfaces(new HashSet<ITypeReference>(
                    parser.interfaceTypes));
        } else {

            // Type parameters
            sigClass.setTypeParameters(EMPTY_TYPE_VARIABLES);

            // java.lang.Object has no super class
            if (isJavaLangObject(dexClass)) {
                sigClass.setSuperClass(null);
            } else {

                if (Kind.INTERFACE.equals(sigClass.getKind())
                        || Kind.ANNOTATION.equals(sigClass.getKind())) {
                    sigClass.setSuperClass(null);
                } else {
                    String superClassPackageName = getPackageName(dexClass
                            .getSuperClass());
                    String superClassName = getClassName(dexClass
                            .getSuperClass());
                    sigClass.setSuperClass(factory.getClassReference(
                            superClassPackageName, superClassName));
                }
            }

            List<String> interfaceDexNames = dexClass.getInterfaces();
            Set<ITypeReference> interfaces = new HashSet<ITypeReference>();
            for (String interfaceDexName : interfaceDexNames) {
                String interfacePackageName = getPackageName(interfaceDexName);
                String interfaceName = getClassName(interfaceDexName);
                SigClassDefinition interfaze = factory.getClass(
                        interfacePackageName, interfaceName);
                interfaze.setKind(Kind.INTERFACE);
                interfaces.add(new SigClassReference(interfaze));
            }
            sigClass.setInterfaces(interfaces);
        }

        // constructors
        Set<SigConstructor> constructors = convertConstructors(dexClass
                .getMethods());
        for (SigConstructor constructor : constructors) {
            constructor.setDeclaringClass(sigClass);
        }
        sigClass.setConstructors(new HashSet<IConstructor>(constructors));

        // methods
        Set<SigMethod> methods = Collections.emptySet();


        if (isAnnotation(dexClass)) {
            Map<String, Object> mappings = getDefaultValueMapping(dexClass);
            Set<SigAnnotationField> annotationFields = convertAnnotationFields(
                    dexClass.getMethods(), mappings);
            sigClass.setAnnotationFields(new HashSet<IAnnotationField>(
                    annotationFields));
            addAnnotationsToAnnotationFields(dexClass.getMethods(),
                    annotationFields);

            sigClass.setEnumConstants(EMPTY_ENUM_CONSTANTS);
            sigClass.setFields(EMPTY_FIELDS);

            // sigClass.setAnnotationFields(new
            // HashSet<IAnnotationField>(convertAnnotationFields(dexClass)));
        } else if (isEnum(dexClass)) {
            Set<IField> fields = new HashSet<IField>();
            Set<IEnumConstant> enumConstants = new HashSet<IEnumConstant>();

            for (DexField dexField : dexClass.getFields()) {
                if (isVisible(dexField, visibility)) {
                    if (dexField.isEnumConstant()) {
                        enumConstants.add(convertEnumConstant(dexField));
                    } else {
                        fields.add(convertField(dexField));
                    }
                }
            }

            sigClass.setFields(fields);
            sigClass.setEnumConstants(enumConstants);
            sigClass.setAnnotationFields(EMPTY_ANNOTATION_FIELDS);
            methods = convertMethods(dexClass.getMethods());
        } else {
            // fields
            sigClass.setFields(new HashSet<IField>(convertFields(dexClass
                    .getFields())));
            sigClass.setEnumConstants(EMPTY_ENUM_CONSTANTS);
            sigClass.setAnnotationFields(EMPTY_ANNOTATION_FIELDS);
            methods = convertMethods(dexClass.getMethods());
        }

        for (SigMethod method : methods) {
            method.setDeclaringClass(sigClass);
        }
        sigClass.setMethods(new HashSet<IMethod>(methods));

        // Annotations
        sigClass.setAnnotations(convertAnnotations(dexClass.getAnnotations()));

        return sigClass;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getDefaultValueMapping(DexClass dexClass) {
        HashMap<String, Object> mappings = new HashMap<String, Object>();
        if (hasAnnotationDefaultSignature(dexClass)) {
            // read mapping to defaults from annotation
            DexAnnotation annotation = getDefaultMappingsAnnotation(dexClass);
            DexAnnotationAttribute dexAnnotationAttribute = annotation
                    .getAttributes().get(0);
            if ("value".equals(dexAnnotationAttribute.getName())) {
                DexEncodedValue encodedValue = dexAnnotationAttribute
                        .getEncodedValue();
                DexEncodedValue value = (DexEncodedValue) encodedValue
                        .getValue();
                List<DexAnnotationAttribute> defaults = 
                        (List<DexAnnotationAttribute>) value.getValue();
                for (DexAnnotationAttribute defaultAttribute : defaults) {
                    mappings.put(defaultAttribute.getName(),
                            convertEncodedValue(defaultAttribute
                                    .getEncodedValue()));
                }
            }
        }
        return mappings;
    }


    private void addAnnotationsToAnnotationFields(List<DexMethod> methods,
            Set<SigAnnotationField> annotationFields) {
        Map<String, SigAnnotationField> nameToAnnotationField =
                new HashMap<String, SigAnnotationField>();

        for (SigAnnotationField annotationField : annotationFields) {
            nameToAnnotationField.put(annotationField.getName(),
                    annotationField);
        }

        for (DexMethod method : methods) {
            SigAnnotationField annotationField = nameToAnnotationField
                    .get(method.getName());
            annotationField.setAnnotations(convertAnnotations(method
                    .getAnnotations()));
        }
    }


    private Set<SigAnnotationField> convertAnnotationFields(
            List<DexMethod> list, Map<String, Object> mappings) {
        Set<SigAnnotationField> annotationfields =
                new HashSet<SigAnnotationField>();
        for (DexMethod dexMethod : list) {
            if (isVisible(dexMethod, visibility)) {
                annotationfields.add(convertAnnotationField(dexMethod, mappings
                        .get(dexMethod.getName())));
            }
        }
        return annotationfields;
    }

    private SigAnnotationField convertAnnotationField(DexMethod dexMethod,
            Object defaultValue) {
        SigAnnotationField annotationField = new SigAnnotationField(dexMethod
                .getName());
        annotationField.setDefaultValue(defaultValue);
        annotationField.setModifiers(getModifier(dexMethod.getModifiers()));
        GenericSignatureParser parser = new GenericSignatureParser(factory,
                this);
        annotationField.setType(parser.parseNonGenericType(dexMethod
                .getReturnType()));
        return annotationField;
    }

    private IEnumConstant convertEnumConstant(DexField dexField) {
        String qualifiedTypeName = getQualifiedName(dexField
                .getDeclaringClass().getName());
        SigEnumConstant enumConstant = elementPool.getEnumConstant(
                qualifiedTypeName, dexField.getName());
        Set<Modifier> modifiers = getModifier(dexField.getModifiers());
        modifiers.add(Modifier.STATIC);
        enumConstant.setModifiers(modifiers);

        String typePackageName = getPackageName(dexField.getType());
        String typeName = getClassName(dexField.getType());
        enumConstant.setType(factory.getClassReference(typePackageName,
                typeName));
        enumConstant.setAnnotations(convertAnnotations(dexField
                .getAnnotations()));
        return enumConstant;
    }

    private Set<SigField> convertFields(List<DexField> dexFields) {
        Set<SigField> fields = new HashSet<SigField>();
        for (DexField dexField : dexFields) {
            if (isVisible(dexField, visibility)) {
                fields.add(convertField(dexField));
            }
        }
        return fields;
    }

    private SigField convertField(DexField dexField) {
        String qualTypeName = getQualifiedName(dexField.getDeclaringClass()
                .getName());
        SigField field = elementPool.getField(qualTypeName, dexField.getName());

        field.setModifiers(getModifier(dexField.getModifiers()));

        field.setAnnotations(convertAnnotations(dexField.getAnnotations()));

        if (hasGenericSignature(dexField)) {
            GenericSignatureParser parser = new GenericSignatureParser(factory,
                    this);
            String declaringClassPackageName = getPackageName(dexField
                    .getDeclaringClass().getName());
            String declaringClassName = getClassName(dexField
                    .getDeclaringClass().getName());

            parser.parseForField(factory.getClass(declaringClassPackageName,
                    declaringClassName), getGenericSignature(dexField));
            field.setType(parser.fieldType);
        } else {
            GenericSignatureParser parser = new GenericSignatureParser(factory,
                    this);
            field.setType(parser.parseNonGenericType(dexField.getType()));
        }

        return field;
    }

    /**
     * Converts a set of {@link DexMethod} to a set of corresponding
     * {@link IConstructor}. This method ignores methods which are not
     * constructors.
     * 
     * @param methods
     *            the {@link DexMethod}s to convert
     * @return the corresponding {@link IConstructor}s
     */
    private Set<SigConstructor> convertConstructors(List<DexMethod> methods) {
        Set<SigConstructor> constructors = new HashSet<SigConstructor>();
        for (DexMethod method : methods) {
            if (isConstructor(method) && isVisible(method, visibility)) {
                constructors.add(convertConstructor(method));
            }
        }
        return constructors;
    }

    /**
     * Converts a set of {@link DexMethod} to a set of corresponding
     * {@link DexMethod}. This method ignores methods which are constructors.
     * 
     * @param methods
     *            the {@link DexMethod}s to convert
     * @return the corresponding {@link IConstructor}s
     */
    private Set<SigMethod> convertMethods(List<DexMethod> methods) {
        Set<SigMethod> sigMethods = new HashSet<SigMethod>();
        for (DexMethod method : methods) {
            if (isMethod(method) && isVisible(method, visibility)) {
                sigMethods.add(convertMethod(method));
            }
        }
        return sigMethods;
    }

    /**
     * Converts a dexMethod which must be a constructor to the corresponding
     * {@link SigConstructor} instance.
     * 
     * @param dexMethod
     *            the dex constructor to convert
     * @return the corresponding {@link SigConstructor}
     */
    public SigConstructor convertConstructor(DexMethod dexMethod) {
        String declaringClassName = getClassName(dexMethod.getDeclaringClass()
                .getName());

        SigConstructor constructor = new SigConstructor(declaringClassName);
        constructor.setModifiers(getModifier(dexMethod.getModifiers()));
        String declaringClassPackageName = getPackageName(dexMethod
                .getDeclaringClass().getName());


        SigClassDefinition declaringClass = factory.getClass(
                declaringClassPackageName, declaringClassName);
        constructor.setDeclaringClass(declaringClass);

        // Annotations
        constructor.setAnnotations(convertAnnotations(dexMethod
                .getAnnotations()));

        if (hasGenericSignature(dexMethod)) {
            GenericSignatureParser parser = new GenericSignatureParser(factory,
                    this);
            parser.parseForConstructor(constructor,
                    getGenericSignature(dexMethod));

            // type parameters
            constructor.setTypeParameters(parser.formalTypeParameters);

            // parameters
            // generic parameter types parameters
            List<DexParameter> dexParameters = dexMethod.getParameters();
            List<IParameter> parameters = new ArrayList<IParameter>(
                    parser.parameterTypes.size());
            Iterator<DexParameter> iterator = dexParameters.iterator();
            for (ITypeReference parameterType : parser.parameterTypes) {
                SigParameter parameter = new SigParameter(parameterType);
                iterator.hasNext();
                DexParameter dexParam = iterator.next();
                parameter.setAnnotations(convertAnnotations(dexParam
                        .getAnnotations()));
                parameters.add(parameter);
            }

            constructor.setParameters(parameters);

            // exceptions
            constructor.setExceptions(new HashSet<ITypeReference>(
                    parser.exceptionTypes));

        } else {
            convertNonGenericExecutableMember(constructor, dexMethod);

            // remove first parameter of non static inner class constructors
            // implicit outer.this reference
            if (declaringClass.getDeclaringClass() != null) {
                if (!declaringClass.getModifiers().contains(Modifier.STATIC)) {
                    if (constructor.getParameters().isEmpty()) {
                        throw new IllegalStateException(
                                "Expected at least one parameter!");
                    }
                    IParameter first = constructor.getParameters().remove(0);
                    String enclosingName = declaringClass.getDeclaringClass()
                            .getName();
                    String firstParameterTypeName = ((IClassReference) first
                            .getType()).getClassDefinition().getName();
                    if (!enclosingName.equals(firstParameterTypeName))
                        throw new IllegalStateException(
                                "Expected first constructor parameter of type "
                                        + enclosingName);
                }
            }
        }

        addExceptions(constructor, dexMethod);
        return constructor;
    }

    public SigMethod convertMethod(DexMethod dexMethod) {
        SigMethod method = new SigMethod(dexMethod.getName());
        method.setModifiers(getModifier(dexMethod.getModifiers()));

        String declaringClassPackageName = getPackageName(dexMethod
                .getDeclaringClass().getName());
        String declaringClassName = getClassName(dexMethod.getDeclaringClass()
                .getName());

        method.setDeclaringClass(factory.getClass(declaringClassPackageName,
                declaringClassName));

        // Annotations
        method.setAnnotations(convertAnnotations(dexMethod.getAnnotations()));

        if (hasGenericSignature(dexMethod)) {
            GenericSignatureParser parser = new GenericSignatureParser(factory,
                    this);
            parser.parseForMethod(method, getGenericSignature(dexMethod));

            // type parameters
            method.setTypeParameters(parser.formalTypeParameters);

            // generic parameter types parameters
            List<DexParameter> dexParameters = dexMethod.getParameters();
            List<IParameter> parameters = new ArrayList<IParameter>(
                    parser.parameterTypes.size());
            Iterator<DexParameter> iterator = dexParameters.iterator();
            for (ITypeReference parameterType : parser.parameterTypes) {
                SigParameter parameter = new SigParameter(parameterType);
                iterator.hasNext();
                DexParameter dexParam = iterator.next();
                parameter.setAnnotations(convertAnnotations(dexParam
                        .getAnnotations()));
                parameters.add(parameter);
            }
            method.setParameters(parameters);

            // exceptions
            method.setExceptions(new HashSet<ITypeReference>(
                    parser.exceptionTypes));
            method.setReturnType(parser.returnType);

        } else {
            convertNonGenericExecutableMember(method, dexMethod);
            GenericSignatureParser parser = new GenericSignatureParser(factory,
                    this);
            ITypeReference type = parser.parseNonGenericReturnType(dexMethod
                    .getReturnType());
            method.setReturnType(type);
        }
        addExceptions(method, dexMethod);
        return method;
    }

    private void addExceptions(SigExecutableMember member,
            DexMethod dexMethod) {
        if (declaresExceptions(dexMethod)) {
            String exceptionSignature = getExceptionSignature(dexMethod);
            Set<String> exceptionTypeNames = splitTypeList(exceptionSignature);
            Set<ITypeReference> exceptions = new HashSet<ITypeReference>();

            for (String exTypeName : exceptionTypeNames) {
                String packageName = getPackageName(exTypeName);
                String className = getClassName(exTypeName);
                exceptions.add(factory
                        .getClassReference(packageName, className));
            }
            member.setExceptions(exceptions);
        } else {
            member.setExceptions(EMPTY_EXCEPTIONS);
        }
    }

    private void convertNonGenericExecutableMember(SigExecutableMember member,
            DexMethod dexMethod) {
        List<DexParameter> dexParameters = dexMethod.getParameters();
        List<IParameter> parameters = new ArrayList<IParameter>(dexParameters
                .size());

        for (DexParameter dexParameter : dexParameters) {
            GenericSignatureParser parser = new GenericSignatureParser(factory,
                    this);
            ITypeReference type = parser.parseNonGenericType(dexParameter
                    .getTypeName());
            SigParameter parameter = new SigParameter(type);
            parameters.add(parameter);
            // Annotations
            parameter.setAnnotations(convertAnnotations(dexParameter
                    .getAnnotations()));
        }
        member.setParameters(parameters);

        member.setTypeParameters(EMPTY_TYPE_VARIABLES);

        // if (declaresExceptions(dexMethod)) {
        // String exceptionSignature = getExceptionSignature(dexMethod);
        // Set<String> exceptionTypeNames = splitTypeList(exceptionSignature);
        // Set<IType> exceptions = new HashSet<IType>();
        //
        // for (String exTypeName : exceptionTypeNames) {
        // String packageName = getPackageName(exTypeName);
        // String className = getClassName(exTypeName);
        // exceptions.add(factory.getClass(packageName, className));
        // }
        // member.setExceptions(exceptions);
        // } else {
        // member.setExceptions(EMPTY_EXCEPTIONS);
        // }
    }

    /**
     * Converts a set of {@link DexAnnotation} to a set of corresponding
     * {@link SigAnnotation}.
     * 
     * @param dexAnnotations
     *            the {@link DexAnnotation}s to convert
     * @return the corresponding {@link SigAnnotation}s
     */
    private Set<IAnnotation> convertAnnotations(
            Set<DexAnnotation> dexAnnotations) {
        Set<IAnnotation> annotations = new HashSet<IAnnotation>();
        for (DexAnnotation dexAnnotation : dexAnnotations) {
            if (!isInternalAnnotation(dexAnnotation)) {
                annotations.add(convertAnnotation(dexAnnotation));
            }
        }
        return annotations;
    }

    /**
     * Converts a {@link DexAnnotation} to the corresponding
     * {@link SigAnnotation}.
     * 
     * @param dexAnnotation
     *            the {@link DexAnnotation} to convert
     * @return the corresponding {@link SigAnnotation}
     */
    protected SigAnnotation convertAnnotation(DexAnnotation dexAnnotation) {
        SigAnnotation sigAnnotation = new SigAnnotation();
        String packageName = getPackageName(dexAnnotation.getTypeName());
        String className = getClassName(dexAnnotation.getTypeName());
        sigAnnotation
                .setType(factory.getClassReference(packageName, className));
        sigAnnotation.setElements(convertAnnotationElements(dexAnnotation
                .getAttributes()));
        return sigAnnotation;
    }

    private Set<IAnnotationElement> convertAnnotationElements(
            List<DexAnnotationAttribute> attributes) {
        Set<IAnnotationElement> annotationAttributes =
                new HashSet<IAnnotationElement>();
        for (DexAnnotationAttribute dexAnnotationAttribute : attributes) {
            annotationAttributes
                    .add(convertAnnotationAttribute(dexAnnotationAttribute));
        }
        return annotationAttributes;
    }

    private IAnnotationElement convertAnnotationAttribute(
            DexAnnotationAttribute dexAnnotationAttribute) {

        SigAnnotationElement sigElement = new SigAnnotationElement();
        String nameOfField = dexAnnotationAttribute.getName();


        String typeName = dexAnnotationAttribute.getAnnotation().getTypeName();
        SigClassDefinition annotationClass = factory.getClass(
                getPackageName(typeName), getClassName(typeName));
        if (!Uninitialized.isInitialized(
                annotationClass.getAnnotationFields())) {
            initializeClass(getPackageName(typeName), getClassName(typeName));
        }
        for (IAnnotationField field : annotationClass.getAnnotationFields()) {
            if (nameOfField.equals(field.getName())) {
                sigElement.setDeclaringField(field);
            }
        }

        sigElement.setValue(convertEncodedValue(dexAnnotationAttribute
                .getEncodedValue()));
        return sigElement;
    }

    @SuppressWarnings("unchecked")
    private Object convertEncodedValue(DexEncodedValue dexEnodedValue) {
        Object value = null;
        switch (dexEnodedValue.getType()) {
        case VALUE_INT:
        case VALUE_BOOLEAN:
        case VALUE_BYTE:
        case VALUE_CHAR:
        case VALUE_DOUBLE:
        case VALUE_FLOAT:
        case VALUE_LONG:
        case VALUE_NULL:
        case VALUE_STRING:
        case VALUE_SHORT:
            value = dexEnodedValue.getValue();
            break;
        case VALUE_ARRAY: {
            List<DexEncodedValue> dexValues =
                    (List<DexEncodedValue>) dexEnodedValue.getValue();
            Object[] arrayValues = new Object[dexValues.size()];
            int i = 0;
            for (DexEncodedValue dexValue : dexValues) {
                arrayValues[i++] = convertEncodedValue(dexValue);
            }
            value = arrayValues;
            break;
        }
        case VALUE_ANNOTATION: {
            DexEncodedAnnotation annotation =
                    (DexEncodedAnnotation) dexEnodedValue.getValue();
            SigAnnotation sigAnnotation = new SigAnnotation();
            String packageName = getPackageName(annotation.getTypeName());
            String className = getClassName(annotation.getTypeName());
            sigAnnotation.setType(factory.getClassReference(packageName,
                    className));

            sigAnnotation.setElements(convertAnnotationElements(annotation
                    .getValue()));
            value = sigAnnotation;
            break;
        }
        case VALUE_FIELD: {
            String fieldDesc = (String) dexEnodedValue.getValue();
            // FORMAT La/b/E;!CONSTANT
            String[] typeAndFieldName = fieldDesc.split("!");
            String typeName = typeAndFieldName[0];
            String fieldName = typeAndFieldName[1];
            value = elementPool.getField(getQualifiedName(typeName), fieldName);
            break;
        }
        case VALUE_ENUM: {
            String fieldDesc = (String) dexEnodedValue.getValue();
            // FORMAT La/b/E;!CONSTANT
            String[] typeAndFieldName = fieldDesc.split("!");
            String typeName = typeAndFieldName[0];
            String fieldName = typeAndFieldName[1];
            value = elementPool.getEnumConstant(getQualifiedName(typeName),
                    fieldName);
            break;
        }
        case VALUE_TYPE: {
            String typeName = (String) dexEnodedValue.getValue();
            GenericSignatureParser parser = new GenericSignatureParser(factory,
                    this);
            value = parser.parseNonGenericReturnType(typeName);
            break;
        }
        default:
            throw new IllegalStateException();
        }
        return value;
    }

    public IClassDefinition initializeClass(String packageName,
            String className) {
        String dexName = getDexName(packageName, className);
        DexClass dexClass = dexNameToDexClass.get(dexName);
        return convertClass(dexClass);
    }
}
