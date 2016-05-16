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

package signature.model.util;

import signature.model.IAnnotatableElement;
import signature.model.IAnnotation;
import signature.model.IAnnotationElement;
import signature.model.IAnnotationField;
import signature.model.IApi;
import signature.model.IClassDefinition;
import signature.model.IField;
import signature.model.IPackage;
import signature.model.ITypeReference;

import java.util.Collection;

public class ModelUtil {
    private ModelUtil() {
    }

    /**
     * Returns the IClass for the given className.<br>
     * Format: a.b.C
     * 
     * @param qualifiedClassName
     *            the fully qualified class name
     * @return the IClass instance or null;
     */
    public static IClassDefinition getClass(IPackage aPackage,
            String qualifiedClassName) {
        for (IClassDefinition clazz : aPackage.getClasses()) {
            if (qualifiedClassName.equals(clazz.getName())) {
                return clazz;
            }
        }
        return null;
    }

    public static IAnnotation getAnnotation(IAnnotatableElement element,
            String qualifiedTypeName) {
        for (IAnnotation annotation : element.getAnnotations()) {
            if (qualifiedTypeName.equals(annotation.getType()
                    .getClassDefinition().getQualifiedName())) {
                return annotation;
            }
        }
        return null;
    }

    public static IAnnotationElement getAnnotationElement(
            IAnnotation annotation, String elementName) {
        for (IAnnotationElement element : annotation.getElements()) {
            if (elementName.equals(element.getDeclaringField().getName())) {
                return element;
            }
        }
        return null;
    }

    public static IField getField(IClassDefinition clazz, String fieldName) {
        for (IField field : clazz.getFields()) {
            if (fieldName.equals(field.getName())) {
                return field;
            }
        }
        return null;
    }

    public static IAnnotationField getAnnotationField(
            IClassDefinition annotation, String fieldName) {
        for (IAnnotationField field : annotation.getAnnotationFields()) {
            if (fieldName.equals(field.getName())) {
                return field;
            }
        }
        return null;
    }

    /**
     * Returns the IPackage for the given className.<br>
     * Format: a.b
     * 
     * @param api
     *            the api
     * @param packageName
     *            the name of the package
     * @return the IClass instance or null;
     */
    public static IPackage getPackage(IApi api, String packageName) {
        for (IPackage aPackage : api.getPackages()) {
            if (packageName.equals(aPackage.getName())) {
                return aPackage;
            }
        }
        return null;
    }

    /**
     * "a.b.c.A;" -> "a.b.c" "A" -> "" empty string
     * 
     * @param classIdentifier
     * @return the package name
     */
    public static String getPackageName(String classIdentifier) {
        int lastIndexOfSlash = classIdentifier.lastIndexOf('.');
        String packageName = null;
        if (lastIndexOfSlash == -1) {
            packageName = "";
        } else {
            packageName = classIdentifier.substring(0, lastIndexOfSlash);
        }
        return packageName;
    }

    /**
     * "a.b.c.A;" -> "A" "A" -> "A"
     * 
     * @param classIdentifier
     *            fully qualified class name
     * @return the class name
     */
    public static String getClassName(String classIdentifier) {
        int lastIndexOfDot = classIdentifier.lastIndexOf('.');
        String className = null;
        if (lastIndexOfDot == -1) {
            className = classIdentifier;
        } else {
            className = classIdentifier.substring(lastIndexOfDot + 1);
        }
        return className;
    }


    public static String separate(Collection<? extends Object> elements,
            String separator) {
        StringBuilder s = new StringBuilder();
        boolean first = true;
        for (Object object : elements) {
            if (!first) {
                s.append(separator);
            }
            s.append(object.toString());
            first = false;
        }
        return s.toString();
    }

    public static boolean isJavaLangObject(ITypeReference type) {
        if (type instanceof IClassDefinition) {
            IClassDefinition clazz = (IClassDefinition) type;
            if ("java.lang".equals(clazz.getPackageName())) {
                return "Object".equals(clazz.getName());
            }
        }
        return false;
    }

}
