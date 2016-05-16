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

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import signature.converter.Visibility;
import signature.model.IClassDefinition;
import signature.model.Kind;
import signature.model.Modifier;
import signature.model.impl.SigPackage;
import signature.model.util.ModelUtil;
import dex.reader.DexBuffer;
import dex.reader.DexFileReader;
import dex.structure.DexAnnotatedElement;
import dex.structure.DexAnnotation;
import dex.structure.DexAnnotationAttribute;
import dex.structure.DexClass;
import dex.structure.DexEncodedValue;
import dex.structure.DexField;
import dex.structure.DexFile;
import dex.structure.DexMethod;


public class DexUtil {

    private static final String PACKAGE_INFO = "package-info";
    private static final String THROWS_ANNOTATION =
            "Ldalvik/annotation/Throws;";
    private static final String SIGNATURE_ANNOTATION =
            "Ldalvik/annotation/Signature;";
    private static final String ANNOTATION_DEFAULT_ANNOTATION =
            "Ldalvik/annotation/AnnotationDefault;";
    private static final String ENCLOSING_CLASS_ANNOTATION =
            "Ldalvik/annotation/EnclosingClass;";
    private static final String ENCLOSING_METHOD_ANNOTATION =
            "Ldalvik/annotation/EnclosingMethod;";
    private static final String INNER_CLASS_ANNOTATION =
            "Ldalvik/annotation/InnerClass;";
    private static final String MEMBER_CLASS_ANNOTATION =
            "Ldalvik/annotation/MemberClasses;";
    private static final String JAVA_LANG_OBJECT = "Ljava/lang/Object;";

    private static final Set<String> INTERNAL_ANNOTATION_NAMES;

    static {
        Set<String> tmp = new HashSet<String>();
        tmp.add(THROWS_ANNOTATION);
        tmp.add(SIGNATURE_ANNOTATION);
        tmp.add(ANNOTATION_DEFAULT_ANNOTATION);
        tmp.add(ENCLOSING_CLASS_ANNOTATION);
        tmp.add(ENCLOSING_METHOD_ANNOTATION);
        tmp.add(INNER_CLASS_ANNOTATION);
        tmp.add(MEMBER_CLASS_ANNOTATION);
        INTERNAL_ANNOTATION_NAMES = Collections.unmodifiableSet(tmp);
    }

    private DexUtil() {
        // not constructable from outside
    }

    /**
     * "La/b/c/A;" -> "a.b.c" "LA;" -> "" empty string
     * 
     * @param classIdentifier
     * @return the package name
     */
    public static String getPackageName(String classIdentifier) {
        String name = removeTrailingSemicolon(removeHeadingL(classIdentifier));
        return ModelUtil.getPackageName(name.replace("/", "."));
    }

    /**
     * "La/b/c/A;" -> "A" "LA;" -> "A"
     * 
     * @param classIdentifier
     *            the dalvik internal identifier
     * @return the class name
     */
    public static String getClassName(String classIdentifier) {
        String name = removeTrailingSemicolon(removeHeadingL(classIdentifier));
        return ModelUtil.getClassName(name.replace("/", ".")).replace('$', '.');
    }

    public static String getQualifiedName(String classIdentifier) {
        String name = removeTrailingSemicolon(removeHeadingL(classIdentifier));
        return name.replace('/', '.');
    }

    private static String removeHeadingL(String className) {
        assert className.startsWith("L");
        return className.substring(1);
    }

    private static String removeTrailingSemicolon(String className) {
        assert className.endsWith(";");
        return className.substring(0, className.length() - 1);
    }

    public static String getDexName(String packageName, String className) {
        return "L" + packageName.replace('.', '/') + "/"
                + className.replace('.', '$') + ";";
    }

    public static String getDexName(IClassDefinition sigClass) {
        return getDexName(sigClass.getPackageName(), sigClass.getName());
    }

    /**
     * Returns correct modifiers for inner classes
     */
    public static int getClassModifiers(DexClass clazz) {
        int modifiers = 0;
        if (isInnerClass(clazz)) {
            Integer accessFlags = (Integer) getAnnotationAttributeValue(
                    getAnnotation(clazz, INNER_CLASS_ANNOTATION),
                            "accessFlags");
            modifiers = accessFlags.intValue();
        } else {
            modifiers = clazz.getModifiers();
        }
        return modifiers;
    }

    /**
     * Returns a set containing all modifiers for the given int.
     * 
     * @param mod
     *            the original bit coded modifiers as specified by
     *            {@link java.lang.reflect.Modifier}
     * @return a set containing {@link signature.model.Modifier} elements
     */
    public static Set<Modifier> getModifier(int mod) {
        Set<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
        if (java.lang.reflect.Modifier.isAbstract(mod))
            modifiers.add(Modifier.ABSTRACT);
        if (java.lang.reflect.Modifier.isFinal(mod))
            modifiers.add(Modifier.FINAL);
        // if (java.lang.reflect.Modifier.isNative(mod))
        // modifiers.add(Modifier.NATIVE);
        if (java.lang.reflect.Modifier.isPrivate(mod))
            modifiers.add(Modifier.PRIVATE);
        if (java.lang.reflect.Modifier.isProtected(mod))
            modifiers.add(Modifier.PROTECTED);
        if (java.lang.reflect.Modifier.isPublic(mod))
            modifiers.add(Modifier.PUBLIC);
        if (java.lang.reflect.Modifier.isStatic(mod))
            modifiers.add(Modifier.STATIC);
        // if (java.lang.reflect.Modifier.isStrict(mod))
        // modifiers.add(Modifier.STRICT);
        // if (java.lang.reflect.Modifier.isSynchronized(mod))
        // modifiers.add(Modifier.SYNCHRONIZED);
        // if (java.lang.reflect.Modifier.isTransient(mod))
        // modifiers.add(Modifier.TRANSIENT);
        if (java.lang.reflect.Modifier.isVolatile(mod))
            modifiers.add(Modifier.VOLATILE);

        return modifiers;
    }

    /**
     * Returns true if the given class is an enumeration, false otherwise.
     * 
     * @param dexClass
     *            the DexClass under test
     * @return true if the given class is an enumeration, false otherwise
     */
    public static boolean isEnum(DexClass dexClass) {
        return (getClassModifiers(dexClass) & 0x4000) > 0;
    }

    /**
     * Returns true if the given class is an interface, false otherwise.
     * 
     * @param dexClass
     *            the DexClass under test
     * @return true if the given class is an interface, false otherwise
     */
    public static boolean isInterface(DexClass dexClass) {
        int modifiers = getClassModifiers(dexClass);
        return java.lang.reflect.Modifier.isInterface(modifiers);
    }

    /**
     * Returns true if the given class is an annotation, false otherwise.
     * 
     * @param dexClass
     *            the DexClass under test
     * @return true if the given class is an annotation, false otherwise
     */
    public static boolean isAnnotation(DexClass dexClass) {
        return (getClassModifiers(dexClass) & 0x2000) > 0;
    }

    public static boolean isSynthetic(int modifier) {
        return (modifier & 0x1000) > 0;
    }

    /**
     * Returns the Kind of the given DexClass.
     * 
     * @param dexClass
     *            the DexClass under test
     * @return the Kind of the given class
     */
    public static Kind getKind(DexClass dexClass) {
        // order of branches is crucial since a annotation is also an interface
        if (isEnum(dexClass)) {
            return Kind.ENUM;
        } else if (isAnnotation(dexClass)) {
            return Kind.ANNOTATION;
        } else if (isInterface(dexClass)) {
            return Kind.INTERFACE;
        } else {
            return Kind.CLASS;
        }
    }

    /**
     * Returns whether the specified annotated element has an annotation with
     * type "Ldalvik/annotation/Throws;".
     * 
     * @param annotatedElement
     *            the annotated element to check
     * @return <code>true</code> if the given annotated element has the
     *         mentioned annotation, false otherwise
     */
    public static boolean declaresExceptions(
            DexAnnotatedElement annotatedElement) {
        return getAnnotation(annotatedElement, THROWS_ANNOTATION) != null;
    }

    /**
     * Returns the throws signature if the given element has such an annotation,
     * null otherwise.
     * 
     * @param annotatedElement
     *            the annotated element
     * @return he generic signature if the given element has such an annotation,
     *         null otherwise
     */
    @SuppressWarnings("unchecked")
    public static String getExceptionSignature(
            DexAnnotatedElement annotatedElement) {
        DexAnnotation annotation = getAnnotation(annotatedElement,
                THROWS_ANNOTATION);
        if (annotation != null) {
            List<DexEncodedValue> value =
                    (List<DexEncodedValue>) getAnnotationAttributeValue(
                            annotation, "value");
            return concatEncodedValues(value);
        }
        return null;
    }

    /**
     * Splits a list of types:
     * "Ljava/io/IOException;Ljava/lang/IllegalStateException;" <br>
     * into separate type designators: <br>
     * "Ljava/io/IOException;" , "Ljava/lang/IllegalStateException;"
     * 
     * @param typeList
     *            the type list
     * @return a set of type designators
     */
    public static Set<String> splitTypeList(String typeList) {
        String[] split = typeList.split(";");
        Set<String> separateTypes = new HashSet<String>();
        for (String string : split) {
            separateTypes.add(string + ";");// add semicolon again
        }
        return separateTypes;
    }

    /**
     * Returns whether the specified annotated element has an annotation with
     * type "Ldalvik/annotation/Signature;".
     * 
     * @param annotatedElement
     *            the annotated element to check
     * @return <code>true</code> if the given annotated element has the
     *         mentioned annotation, false otherwise
     */
    public static boolean hasGenericSignature(
            DexAnnotatedElement annotatedElement) {
        return getAnnotation(annotatedElement, SIGNATURE_ANNOTATION) != null;
    }

    /**
     * Returns the generic signature if the given element has such an
     * annotation, null otherwise.
     * 
     * @param annotatedElement
     *            the annotated element
     * @return he generic signature if the given element has such an annotation,
     *         null otherwise
     */
    @SuppressWarnings("unchecked")
    public static String getGenericSignature(
            DexAnnotatedElement annotatedElement) {
        DexAnnotation annotation = getAnnotation(annotatedElement,
                SIGNATURE_ANNOTATION);
        if (annotation != null) {
            List<DexEncodedValue> value =
                    (List<DexEncodedValue>) getAnnotationAttributeValue(
                            annotation, "value");
            return concatEncodedValues(value);
        }
        return null;
    }

    /**
     * Returns whether the specified annotated element has an annotation with
     * type "Ldalvik/annotation/AnnotationDefault;".
     * 
     * @param annotatedElement
     *            the annotated element to check
     * @return <code>true</code> if the given annotated element has the
     *         mentioned annotation, false otherwise
     */
    public static boolean hasAnnotationDefaultSignature(
            DexAnnotatedElement annotatedElement) {
        return getAnnotation(
                annotatedElement, ANNOTATION_DEFAULT_ANNOTATION)!= null;
    }

    /**
     * Returns a mapping form annotation attribute name to its default value.
     * 
     * @param dexClass
     *            the class defining a annotation
     * @return a mapping form annotation attribute name to its default value
     */
    public static DexAnnotation getDefaultMappingsAnnotation(
            DexClass dexClass) {
        return getAnnotation(dexClass, ANNOTATION_DEFAULT_ANNOTATION);
    }

    /**
     * Returns the annotation with the specified type from the given element or
     * null if no such annotation is available.
     * 
     * @param element
     *            the annotated element
     * @param annotationType
     *            the dex internal name of the annotation type
     * @return the annotation with the specified type or null if not present
     */
    public static DexAnnotation getAnnotation(DexAnnotatedElement element,
            String annotationType) {
        assert element != null;
        assert annotationType != null;

        for (DexAnnotation anno : element.getAnnotations()) {
            if (annotationType.equals(anno.getTypeName())) {
                return anno;
            }
        }
        return null;
    }

    /**
     * Returns the value for the specified attribute name of the given
     * annotation or null if not present.
     * 
     * @param annotation
     *            the annotation
     * @param attributeName
     *            the name of the attribute
     * @return the value for the specified attribute
     */
    public static Object getAnnotationAttributeValue(DexAnnotation annotation,
            String attributeName) {
        for (DexAnnotationAttribute dexAnnotationAttribute : annotation
                .getAttributes()) {
            if (attributeName.equals(dexAnnotationAttribute.getName())) {
                return dexAnnotationAttribute.getEncodedValue().getValue();
            }
        }
        return null;
    }

    private static String concatEncodedValues(List<DexEncodedValue> values) {
        StringBuilder builder = new StringBuilder();
        for (DexEncodedValue string : values) {
            builder.append(string.getValue());
        }
        return builder.toString();
    }

    /**
     * Returns true if the given method is a constructor, false otherwise.
     * 
     * @param method
     *            the method to test
     * @return true if the given method is a constructor, false otherwise
     */
    public static boolean isConstructor(DexMethod method) {
        return "<init>".equals(method.getName());
    }

    /**
     * Returns true if the given method is a static constructor, false
     * otherwise.
     * 
     * @param method
     *            the method to test
     * @return true if the given method is a static constructor, false otherwise
     */
    public static boolean isStaticConstructor(DexMethod method) {
        return "<clinit>".equals(method.getName());
    }

    public static boolean isMethod(DexMethod method) {
        return !isConstructor(method) && !isStaticConstructor(method);
    }

    /**
     * Returns the package-info class for the given package.
     * 
     * @param aPackage
     *            the package
     * @return the class called "package-info" or null, if not available
     */
    public static IClassDefinition findPackageInfo(SigPackage aPackage) {
        for (IClassDefinition clazz : aPackage.getClasses()) {
            if (PACKAGE_INFO.equals(clazz.getName())) {
                return clazz;
            }
        }
        return null;
    }

    public static boolean isPackageInfo(DexClass clazz) {
        return PACKAGE_INFO.equals(getClassName(clazz.getName()));
    }

    public static boolean isInternalAnnotation(DexAnnotation dexAnnotation) {
        return INTERNAL_ANNOTATION_NAMES.contains(dexAnnotation.getTypeName());
    }

    /**
     * An InnerClass annotation is attached to each class which is defined in
     * the lexical scope of another class's definition. Any class which has this
     * annotation must also have either an EnclosingClass annotation or an
     * EnclosingMethod annotation.
     */
    public static boolean isInnerClass(DexClass clazz) {
        return getAnnotation(clazz, INNER_CLASS_ANNOTATION) != null;
    }

    /**
     * An EnclosingClass annotation is attached to each class which is either
     * defined as a member of another class, per se, or is anonymous but not
     * defined within a method body (e.g., a synthetic inner class). Every class
     * that has this annotation must also have an InnerClass annotation.
     * Additionally, a class may not have both an EnclosingClass and an
     * EnclosingMethod annotation.
     */
    public static boolean isEnclosingClass(DexClass clazz) {
        return getAnnotation(clazz, ENCLOSING_CLASS_ANNOTATION) != null;
    }

    public static boolean declaresMemberClasses(DexClass dexClass) {
        return getAnnotation(dexClass, MEMBER_CLASS_ANNOTATION) != null;
    }

    @SuppressWarnings("unchecked")
    public static Set<String> getMemberClassNames(DexClass dexClass) {
        DexAnnotation annotation = getAnnotation(dexClass,
                MEMBER_CLASS_ANNOTATION);
        List<DexEncodedValue> enclosedClasses =
                (List<DexEncodedValue>) getAnnotationAttributeValue(
                        annotation, "value");
        Set<String> enclosedClassesNames = new HashSet<String>();
        for (DexEncodedValue string : enclosedClasses) {
            enclosedClassesNames.add((String) string.getValue());
        }
        return enclosedClassesNames;
    }


    public static String getEnclosingClassName(DexClass dexClass) {
        DexAnnotation annotation = getAnnotation(dexClass,
                ENCLOSING_CLASS_ANNOTATION);
        String value = (String) getAnnotationAttributeValue(annotation,
                "value");
        return value;
    }

    public static boolean convertAnyWay(DexClass dexClass) {
        return !isSynthetic(getClassModifiers(dexClass))
                && !isAnonymousClassName(dexClass.getName())
                || isPackageInfo(dexClass);
    }

    public static boolean isVisible(DexClass dexClass, Visibility visibility) {
        // package info is always visible
        if (isPackageInfo(dexClass)) {
            return true;
        }

        if (isDeclaredInMethod(dexClass)) {
            return false;
        }

        if (isAnonymousClassName(dexClass.getName())) {
            return false;
        }

        int modifiers = getClassModifiers(dexClass);

        return isVisible(modifiers, visibility);
    }

    private static boolean isDeclaredInMethod(DexClass dexClass) {
        return getAnnotation(dexClass, ENCLOSING_METHOD_ANNOTATION) != null;
    }

    /**
     * Returns whether the given dex identifier is an anonymous class name.
     * Format: La/b/C$1;
     * 
     * @param dexName
     *            the name to analyze
     * @return whether the given dex identifier is an anonymous class name
     */
    public static boolean isAnonymousClassName(String dexName) {
        int index = dexName.lastIndexOf('$');
        return (index != 0) ? Character.isDigit(dexName.charAt(index + 1))
                : false;
    }

    public static boolean isVisible(DexField dexField, Visibility visibility) {
        return isVisible(dexField.getModifiers(), visibility);
    }

    public static boolean isVisible(DexMethod dexMethod,
            Visibility visibility) {
        return isVisible(dexMethod.getModifiers(), visibility);
    }

    private static boolean isVisible(int modifiers, Visibility visibility) {

        if (isSynthetic(modifiers)) {
            return false;
        }

        Set<Modifier> elementModifiers = getModifier(modifiers);
        if (elementModifiers.contains(Modifier.PUBLIC)) {
            return true;
        } else if (elementModifiers.contains(Modifier.PROTECTED)) {
            return visibility == Visibility.PROTECTED
                    || visibility == Visibility.PACKAGE
                    || visibility == Visibility.PRIVATE;
        } else if (elementModifiers.contains(Modifier.PRIVATE)) {
            return visibility == Visibility.PRIVATE;
        } else {
            return visibility == Visibility.PACKAGE
                    || visibility == Visibility.PRIVATE;
        }
    }

    public static Set<DexFile> getDexFiles(Set<String> fileNames)
            throws IOException {
        Set<DexFile> parsedFiles = new HashSet<DexFile>();

        for (String dexFile : fileNames) {
            DexFileReader reader = new DexFileReader();
            DexBuffer dexBuffer = new DexBuffer(dexFile);
            parsedFiles.add(reader.read(dexBuffer));
        }
        return parsedFiles;
    }


    public static boolean isJavaLangObject(DexClass dexClass) {
        assert dexClass != null;
        return JAVA_LANG_OBJECT.equals(dexClass.getName());
    }
}
