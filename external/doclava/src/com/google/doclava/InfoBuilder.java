/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.doclava;

import com.google.doclava.parser.JavaLexer;
import com.google.doclava.parser.JavaParser;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.debug.ParseTreeBuilder;
import org.antlr.runtime.tree.ParseTree;
import org.antlr.runtime.tree.Tree;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * InfoBuilder parses an individual file and builds Doclava
 * objects out of the data within the file. This data is
 * stored within a global cache for later use.
 */
public class InfoBuilder {
    private PackageInfo mPackage;
    private ArrayList<String> mImports;
    private HashSet<String> mClassNames;
    private String mFilename; // TODO - remove this eventually
    private ClassInfo mRootClass;

    public InfoBuilder(String filename) {
        mImports = new ArrayList<String>();
        mImports.add("java.lang.*"); // should allow us to resolve this properly, eventually
                                     // alternatively, we could add everything from java.lang.*
                                     // but that would probably be too brittle
        mClassNames = new HashSet<String>();
        mFilename = filename;
    }

    @Override
    public String toString() {
        return mFilename;
    }

    public void parseFile() {
        JavaLexer lex;
        try {
            lex = new JavaLexer(new ANTLRFileStream(mFilename, "UTF8"));

            CommonTokenStream tokens = new CommonTokenStream(lex);

            // create the ParseTreeBuilder to build a parse tree
            // much easier to parse than ASTs
            ParseTreeBuilder builder = new ParseTreeBuilder("compilationUnit");
            JavaParser g = new JavaParser(tokens, builder);

            g.compilationUnit();
            ParseTree tree = builder.getTree();

            lex = null;
            tokens = null;
            builder = null;
            g = null;

            parseFile(tree);

        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (RecognitionException e) {
            e.printStackTrace();
        }
    }

    public static void resolve() {
        Caches.resolve();
    }

    // All of the print functions exist for debugging alone.
    public void printStuff() {
        System.out.println(mPackage.name() + "\n");

        printList(mImports);

        Caches.printResolutions();
    }

    private void printList(ArrayList<String> list) {
        for (String value : list) {
            System.out.println(value);
        }

        System.out.println();
    }

    public static void printClassInfo(ClassInfo cl) {
        System.out.print("Class: " + cl.toString());

        printTypeVariables(cl.type());

        System.out.println();

        System.out.println(cl.comment().mText);

        if (!cl.annotations().isEmpty()) {
            System.out.println("\nAnnotations:");
            printAnnotations(cl.annotations());
        }

        if (cl.superclass() != null) {
            System.out.print("Superclass: " + cl.superclass().qualifiedName());
            printTypeVariables(cl.superclassType());
            System.out.println();
        }

        if (!cl.realInterfaces().isEmpty()) {
            System.out.println("\nInterfaces Implemented:");
            Iterator<TypeInfo> it = cl.realInterfaceTypes().iterator();
            for (ClassInfo cls : cl.realInterfaces()) {
                TypeInfo outerType = it.next();
                if (cls == null) {
                    System.out.print(outerType.simpleTypeName());
                } else {
                    System.out.print(cls.qualifiedName());
                }

                printTypeVariables(outerType);

                System.out.println();
            }

            System.out.println();
        }

        if (!cl.allSelfFields().isEmpty()) {
            System.out.println("\nFields:");
            for (FieldInfo f : cl.allSelfFields()) {
                if (f != cl.allSelfFields().get(0)) {
                    System.out.println();
                }
                System.out.println(f.comment().mText);

                printAnnotations(f.annotations());
                printTypeName(f.type());

                System.out.print(" " + f.name());

                if (f.constantValue() != null) {
                    System.out.println(": " + f.constantValue());
                } else if (f.hasValue()) {
                    System.out.println(": has some value");
                } else {
                    System.out.println();
                }
            }

            System.out.println();
        }

        if (cl.enumConstants() != null && !cl.enumConstants().isEmpty()) {
            System.out.println("\nEnum Constants:");
            for (FieldInfo f : cl.enumConstants()) {
                if (f != cl.enumConstants().get(0)) {
                    System.out.println();
                }
                System.out.println(f.comment().mText);
                printAnnotations(f.annotations());
                System.out.print(f.type().simpleTypeName() + " " + f.name());

                if (f.constantValue() != null) {
                    System.out.println(": " + f.constantValue());
                } else {
                    System.out.println();
                }
            }

            System.out.println();
        }

        if (!cl.allConstructors().isEmpty()) {
            System.out.println("\nConstructors:");
            for (MethodInfo m : cl.allConstructors()) {
                if (m != cl.allConstructors().get(0)) {
                    System.out.println();
                }

                System.out.println(m.comment().mText);

                printAnnotations(m.annotations());
                if (m.getTypeParameters() != null) {
                    printTypeVariableList(m.getTypeParameters());
                    System.out.print(" ");
                }

                System.out.println(m.name() + m.flatSignature());
            }

            System.out.println();
        }

        if (!cl.allSelfMethods().isEmpty()) {
            System.out.println("\nMethods:");
            for (MethodInfo m : cl.allSelfMethods()) {
                if (m != cl.allSelfMethods().get(0)) {
                    System.out.println();
                }

                System.out.println(m.comment().mText);
                printAnnotations(m.annotations());
                if (m.getTypeParameters() != null) {
                    printTypeVariableList(m.getTypeParameters());
                    System.out.print(" ");
                }

                printTypeName(m.returnType());

                System.out.print(" " + m.name() + m.flatSignature());

                if (m.thrownExceptions() != null && !m.thrownExceptions().isEmpty()) {
                    System.out.print(" throws ");
                    for (ClassInfo c : m.thrownExceptions()) {
                        if (c != m.thrownExceptions().get(0)) {
                            System.out.print(", ");
                        }

                        System.out.print(c.name());
                    }
                }

                System.out.println();
            }

            System.out.println();
        }

        if (!cl.annotationElements().isEmpty()) {
            System.out.println("\nAnnotation Elements:");

            for (MethodInfo m : cl.annotationElements()) {
                if (m != cl.annotationElements().get(0)) {
                    System.out.println();
                }

                System.out.println(m.comment().mText);
                printAnnotations(m.annotations());
                printTypeName(m.returnType());

                System.out.print(" " + m.name() + m.flatSignature());

                if (m.defaultAnnotationElementValue() != null) {
                    System.out.print(" default " +
                            m.defaultAnnotationElementValue().valueString());
                }

                System.out.println();
            }

            System.out.println();
        }

        if (cl.innerClasses() != null && !cl.innerClasses().isEmpty()) {
            System.out.println("\nInner Classes:");
            for (ClassInfo c : cl.innerClasses()) {
                printClassInfo(c);
            }
        }
    }

    private static void printTypeName(TypeInfo type) {
        System.out.print(type.simpleTypeName());

        if (type.extendsBounds() != null && !type.extendsBounds().isEmpty()) {
            System.out.print(" extends ");
            for (TypeInfo t : type.extendsBounds()) {
                if (t != type.extendsBounds().get(0)) {
                    System.out.print(" & ");
                }
                printTypeName(t);
            }
        }

        if (type.superBounds() != null && !type.superBounds().isEmpty()) {
            System.out.print(" super ");
            for (TypeInfo t : type.superBounds()) {
                if (t != type.superBounds().get(0)) {
                    System.out.print(" & ");
                }
                printTypeName(t);
            }
        }

        printTypeVariables(type);

        if (type.dimension() != null) {
            System.out.print(type.dimension());
        }
    }

    private static void printAnnotations(ArrayList<AnnotationInstanceInfo> annotations) {
        for (AnnotationInstanceInfo i : annotations) {
            System.out.println(i);
        }
    }

    private static void printTypeVariables(TypeInfo type) {
        printTypeVariableList(type.typeArguments());
    }

    private static void printTypeVariableList(ArrayList<TypeInfo> typeList) {
        if (typeList != null && !typeList.isEmpty()) {
            System.out.print("<");
            for (TypeInfo type : typeList) {
                if (type != typeList.get(0)) {
                    System.out.print(", ");
                }
                printTypeName(type);
            }
            System.out.print(">");
        }
    }

    /**
     * Parses the file represented by the ParseTree.
     * @param tree A ParseTree of the file to parse.
     */
    private void parseFile(ParseTree tree) {
        if (tree.payload != null) {
            String payload = tree.payload.toString();

            // first pass at ignore method blocks
            if ("block".equals(payload) ||
                    "blockStatement".equals(payload) ||
                    "explicitConstructorInvocation".equals(payload)) {
                tree = null;
                return;
            }

            // parse package of file
            if ("packageDeclaration".equals(payload)) {
                mPackage = buildPackage(tree);
                return;
            // parse imports
            } else if ("importDeclaration".equals(payload)) {
                mImports.add(buildImport(tree));
                return;
            // classes
            } else if ("normalClassDeclaration".equals(payload)) {
                buildClass(tree, null);
                return;
            // enums
            }  else if ("enumDeclaration".equals(payload)) {
                buildEnum(tree, null);
                return;
            // interfaces
            } else if ("normalInterfaceDeclaration".equals(payload)) {
                buildInterface(tree, null);
                return;
            // annotations
            } else if ("annotationTypeDeclaration".equals(payload)) {
                buildAnnotationDeclaration(tree, null);
                return;
            }
        }

        // if we're not at the end, recurse down the tree
        for (int i = 0; i < tree.getChildCount(); i++) {
            parseFile((ParseTree) tree.getChild(i));
        }
    }

    /**
     * Parses a packageDeclaration in the tree. This function should only be called once per file.
     * @param tree The tree to parse. packageDeclaration should be the root value.
     * @return a PackageInfo representing the package in which this file exists.
     */
    private PackageInfo buildPackage(ParseTree tree) {
        for (int i = 0; i < tree.getChildCount(); i++) {
            ParseTree child = (ParseTree) tree.getChild(i);

            if (child.payload != null && "qualifiedName".equals(child.payload.toString())) {
                String packageName = buildQualifiedName(child);

                // return package because we might be creating packages for other classes
                return Caches.obtainPackage(packageName);
            }
        }

        return null;
    }

    /**
     * Parses a qualifiedName, returning it as a String.
     * @param tree The tree to parse. qualifiedName should be the root value.
     * @return
     */
    private static String buildQualifiedName(ParseTree tree) {
        StringBuilder packageName = new StringBuilder();

        for (int j = 0; j < tree.getChildCount(); j++) {
            packageName.append(tree.getChild(j).toString());
        }

        return packageName.toString();
    }

    /**
     * Builds a string representing an import declaration.
     * @param tree The tree to parse. importDeclaration should be the root value.
     * @return a String version of the import.
     */
    private String buildImport(ParseTree tree) {
        StringBuilder theImport = new StringBuilder();
        for (int i = 1; i < tree.getChildCount(); i++) {
            String part = tree.getChild(i).toString();

            if ((i == 1 && "static".equals(part))
                    || (i == tree.getChildCount()-1 && ";".equals(part))) {
                continue;
            }

            theImport.append(part);
        }

        return theImport.toString();
    }

    /**
     * Builds a ClassInfo for a normalClassDeclaration.
     * @param tree The tree to parse. normalClassDeclaration should be the root value.
     * @param containingClass The class that contains the class that will be built.
     * This value should be null if this class is a root class in the file.
     * @return A ClassInfo that contains all of the information about the class.
     */
    private ClassInfo buildClass(ParseTree tree, ClassInfo containingClass) {
        CommentAndPosition commentAndPosition = parseCommentAndPosition(tree);
        Modifiers modifiers = new Modifiers(this);
        ClassInfo cls = null;

        @SuppressWarnings("unchecked")
        Iterator<ParseTree> it = (Iterator<ParseTree>) tree.getChildren().iterator();
        ParseTree child = it.next();

        // parse modifiers
        modifiers.parseModifiers(child);

        it.next();
        child = it.next();

        // parse class name
        cls = buildClassName(child, containingClass, modifiers,
                commentAndPosition.getCommentText(),
                commentAndPosition.getPosition(),
                ClassType.ORDINARY);

        child = it.next();

        // handle generics
        if ("typeParameters".equals(child.toString())) {
            cls.type().setTypeArguments(buildTypeVariables(child));
            child = it.next();

        }

        // handle extends
        if ("extends".equals(child.toString())) {
            child = it.next();

            TypeInfo type = buildType(child);
            cls.setSuperclassType(type);

            // if ClassInfo is null, we need to add a resolution
            if (type.asClassInfo() == null) {
                addFutureResolution(cls, "superclassQualifiedName", type.simpleTypeName(), this);
            }

            cls.setSuperClass(type.asClassInfo());

            child = it.next();
        }

        // TODO - do I have to make java.lang.Object the superclass if there is none otherwise?

        // handle implements
        if ("implements".equals(child.toString())) {
            child = it.next();

            parseInterfaces(child, cls);

            child = it.next();
        }

        // finally, parse the body
        buildClassBody(child, cls);

        return cls;
    }

    /**
     * Parses the list of interfaces that the class implements.
     * Should only be called if the implements keyword is found.
     * @param tree The tree to parse. typeList should be the root element.
     * @param cls The class that implements these interfaces.
     */
    private void parseInterfaces(ParseTree tree, ClassInfo cls) {
        for (Object o : tree.getChildren()) {
            if ("type".equals(o.toString())) {
                TypeInfo type = buildType((ParseTree) o);
                cls.addInterfaceType(type);

                // if ClassInfo is null, we need to add a resolution
                if (type.asClassInfo() == null) {
                    addFutureResolution(cls, "interfaceQualifiedName", type.simpleTypeName(), this);
                }

                cls.addInterface(type.asClassInfo());
            }
        }
    }

    /**
     * ClassType exists solely to tell buildClassName which type of ClassInfo is being built.
     */
    private enum ClassType {
        ENUM, INTERFACE, ANNOTATION, ORDINARY
    }

    /**
     * Parses the class name from the declaration. Also initializes the class.
     * @param tree Position of the tree where the name of the class resides.
     * @param containingClass Class that this class is contained within.
     * <tt>null</tt> if this class is the root class.
     * @param modifiers Contains all the modifiers of this class.
     * @param commentText Javadoc comment of this class.
     * @param position Position of the class.
     * @param classType Type of class being instantiated.
     * @return the ClassInfo being initialized.
     */
    private ClassInfo buildClassName(ParseTree tree, ClassInfo containingClass, Modifiers modifiers,
            String commentText, SourcePositionInfo position, ClassType classType) {
        String qualifiedClassName = null;
        boolean isOrdinaryClass = true;
        boolean isException = false;
        boolean isError = false;
        boolean isIncluded = false;
        boolean isPrimitive = false;
        boolean isEnum = false;
        boolean isInterface = false;
        boolean isAnnotation = false;

        // set appropriate flags based on ClassType
        switch (classType) {
            case ENUM:
                isEnum = true;
                break;
            case INTERFACE:
                isInterface = true;
                break;
            case ANNOTATION:
                isAnnotation = true;
                break;
        }

        String qualifiedTypeName = null;
        ClassInfo cls = null;

        // changes the name based upon whether this is the root class or an inner class
        if (containingClass == null) {
            qualifiedClassName = mPackage.name() + "." + tree.toString();
        } else {
            qualifiedClassName = containingClass.qualifiedName() + "." + tree.toString();
        }

        qualifiedTypeName = new String(qualifiedClassName);

        // add the name to mClassNames so that we can use it to resolve usages of this class
        mClassNames.add(qualifiedClassName);

        // get the class from the cache and initialize it
        cls = Caches.obtainClass(qualifiedClassName);
        cls.initialize(commentText, position,
                modifiers.isPublic(), modifiers.isProtected(),
                modifiers.isPackagePrivate(), modifiers.isPrivate(),
                modifiers.isStatic(), isInterface, modifiers.isAbstract(),
                isOrdinaryClass, isException, isError, isEnum, isAnnotation,
                modifiers.isFinal(), isIncluded, qualifiedTypeName, isPrimitive,
                modifiers.getAnnotations());

        cls.setContainingClass(containingClass);
        cls.setContainingPackage(mPackage);

        if (containingClass == null) {
            mRootClass = cls;
        }

        // create an set a TypeInfo for this class
        TypeInfo type = new TypeInfo(false, null, cls.name(), qualifiedTypeName, cls);
        cls.setTypeInfo(type);

        return cls;
    }

    /**
     * Parses the body of a class.
     * @param tree The tree to parse. classBody should be the root value.
     * @param cls
     */
    private void buildClassBody(ParseTree tree, ClassInfo cls) {
        for (Object o : tree.getChildren()) {
            ParseTree child = (ParseTree) o;

            // skip all of the cruft that isn't a declaration
            if (!"classBodyDeclaration".equals(child.toString())) {
                continue;
            }

            // get to an actual definition
            ParseTree member = (ParseTree) child.getChild(0).getChild(0);

            // ignores static initializers
            if (member == null) {
                continue;
            }

            // field
            if ("fieldDeclaration".equals(member.toString())) {
                for (FieldInfo f : buildFields(member, cls)) {
                    cls.addField(f);
                }
            // method and constructor
            } else if ("methodDeclaration".equals(member.toString())) {
                MethodInfo method = buildMethod(member, cls, false);

                if (method.kind().equals("constructor")) {
                    cls.addConstructor(method);
                } else {
                    cls.addMethod(method);
                }
            // classes and enums
            } else if ("classDeclaration".equals(member.toString())) {
                Object tmp = member.getChild(0);

                if ("normalClassDeclaration".equals(tmp.toString())) {
                    cls.addInnerClass(buildClass((ParseTree) tmp, cls));
                } else if ("enumDeclaration".equals(tmp.toString())) {
                    cls.addInnerClass(buildEnum((ParseTree) tmp, cls));
                }
            // interfaces and annotations
            } else if ("interfaceDeclaration".equals(member.toString())) {
                Object tmp = member.getChild(0);

                if ("normalInterfaceDeclaration".equals(tmp.toString())) {
                    cls.addInnerClass(buildInterface((ParseTree) tmp, cls));
                } else if ("annotationTypeDeclaration".equals(tmp.toString())) {
                    cls.addInnerClass(buildAnnotationDeclaration((ParseTree) tmp, cls));
                }
            }
        }
    }

    /**
     * Builds one or more FieldInfos for the field declared in this class.
     * @param tree The tree to parse. fieldDeclaration should be the root value.
     * @param containingClass The ClassInfo in which this field is contained.
     * @return A list of FieldInfos for this field declaration.
     */
    private ArrayList<FieldInfo> buildFields(ParseTree tree, ClassInfo containingClass) {
        ArrayList<FieldInfo> fields = new ArrayList<FieldInfo>();
        Modifiers modifiers = new Modifiers(this);
        CommentAndPosition commentAndPosition = parseCommentAndPosition(tree);
        String name = null;
        Object constantValue = null;
        TypeInfo type = null;
        boolean hasValue = false;

        @SuppressWarnings("unchecked")
        Iterator<ParseTree> it = (Iterator<ParseTree>) tree.getChildren().iterator();
        ParseTree child = it.next();

        // modifiers
        modifiers.parseModifiers(child);
        child = it.next();

        // parse the type of this field
        type = buildType(child);

        child = it.next();

        // parse the variable declarators
        boolean firstType = true;
        while (!";".equals(child.toString())) {
            if ("variableDeclarator".equals(child.toString())) {
                TypeInfo newType;
                if (firstType) {
                    firstType = false;
                    newType = type;
                } else {
                    newType = new TypeInfo(type.isPrimitive(), type.dimension(),
                            type.simpleTypeName(), type.qualifiedTypeName(), type.asClassInfo());
                    newType.setBounds(type.superBounds(), type.extendsBounds());
                    newType.setIsWildcard(type.isWildcard());
                    newType.setIsTypeVariable(type.isTypeVariable());
                    newType.setTypeArguments(type.typeArguments());
                }
                name = child.getChild(0).toString();

                // if we have a value for the field and/or dimensions
                if (child.getChildCount() > 1) {
                    int j = 1;
                    ParseTree tmp = (ParseTree) child.getChild(j++);

                    // if we have dimensions in the wrong place
                    if ("[".equals(tmp.toString())) {
                        StringBuilder builder = new StringBuilder();

                        do {
                            builder.append(tmp.toString());
                            tmp = (ParseTree) child.getChild(j++);
                        } while (j < child.getChildCount() && !"=".equals(tmp.toString()));

                        newType.setDimension(builder.toString());
                    }

                    // get value if it exists
                    if (j < child.getChildCount()) {
                        // get to variableInitializer
                        do {
                            tmp = (ParseTree) child.getChild(j++);
                        } while (!"variableInitializer".equals(tmp.toString()));

                        // get the constantValue
                        constantValue = parseExpression(tmp);
                    }

                    hasValue = true;
                }

                FieldInfo field = new FieldInfo(name, containingClass, containingClass,
                        modifiers.isPublic(), modifiers.isProtected(),
                        modifiers.isPackagePrivate(), modifiers.isPrivate(),
                        modifiers.isFinal(), modifiers.isStatic(), modifiers.isTransient(),
                        modifiers.isVolatile(), modifiers.isSynthetic(),
                        newType, commentAndPosition.getCommentText(), constantValue,
                        commentAndPosition.getPosition(), modifiers.getAnnotations());
                field.setHasValue(hasValue);
                fields.add(field);
            }

            child = it.next();
        }

        return fields;
    }

    /**
     * Parses an expression in the ParseTree to get a constant value.
     * @param tree the place in the tree to get the constant value.
     * @return the constant value.
     */
    private static Object parseExpression(ParseTree tree) {
        Object constantValue = null;
        StringBuilder builder = new StringBuilder();

        while (!"primary".equals(tree.toString())) {
            if (tree.getChildCount() > 1) {
                if ("unaryExpression".equals(tree.toString()) ||
                        "unaryExpressionNotPlusMinus".equals(tree.toString())) {
                    if ("selector".equals(tree.getChild(1).toString())) {
                        return constantValue;
                    }

                    builder.append(tree.getChild(0));
                    tree = (ParseTree) tree.getChild(1);
                } else if ("arrayInitializer".equals(tree.toString())) {
                    // TODO - do we wanna parse arrays or just skip it
                    return constantValue;
                } else {
                    return constantValue;
                }
            } else if ("castExpression".equals(tree.toString())) {
                tree = (ParseTree) tree.getChild(tree.getChildCount()-1);
            } else {
                tree = (ParseTree) tree.getChild(0);
            }
        }

        if ("literal".equals(tree.getChild(0).toString())) {
            constantValue = builder.append(tree.getChild(0).getChild(0).toString()).toString();
        } else if (tree.getChildCount() > 1) {
            for (Object o : tree.getChildren()) {
                builder.append(o.toString());
            }

            constantValue = builder.toString();
        }

        return constantValue;
    }

    /**
     * Builds  TypeInfo. Requires that tree points to "type" in the ParseTree.
     * @param tree The tree to parse. type should be the root value.
     * @return A TypeInfo for this type.
     */
    private TypeInfo buildType(ParseTree tree) {
        boolean isPrimitive = false;
        String dimension = null;
        String simpleTypeName = null;
        String qualifiedTypeName = null;
        ClassInfo cl = null;
        boolean addResolution = false;
        ArrayList<TypeInfo> typeArguments = null;

        // parse primitive types - very easy
        if ("primitiveType".equals(tree.getChild(0).toString())) {
            isPrimitive = true;

            simpleTypeName = tree.getChild(0).getChild(0).toString();
            qualifiedTypeName = simpleTypeName;
        // any non-primitives
        } else {
            StringBuilder builder = new StringBuilder();

            // get the full name of the type
            for (Object namePart : ((ParseTree) tree.getChild(0)).getChildren()) {
                // if we get to typeArguments, aka generics, parse that and bale out
                // of building the name
                if ("typeArguments".equals(namePart.toString())) {
                    typeArguments = buildTypeVariables((ParseTree) namePart);
                    break;
                }

                builder.append(namePart.toString());
            }

            // get simple and qualified name
            simpleTypeName = builder.toString();
            StringBuilder qualifiedTypeNameBuilder = new StringBuilder();
            boolean isGeneric = resolveQualifiedName(simpleTypeName,
                    qualifiedTypeNameBuilder, this);
            qualifiedTypeName = qualifiedTypeNameBuilder.toString();

            // if we couldn't figure out the qualified name
            // tell us we need to resolve this
            // can't add the resolution until the TypeInfo has been created
            if ("".equals(qualifiedTypeName)) {
                addResolution = true;
            // otherwise, if the name is not a generic, get the class that this Type refers to
            } else if (!isGeneric) {
                cl = Caches.obtainClass(qualifiedTypeName);
            }
        }

        // get the dimensions of this type
        dimension = getDimensions(tree);

        TypeInfo type = new TypeInfo(isPrimitive, dimension, simpleTypeName, qualifiedTypeName, cl);
        type.setTypeArguments(typeArguments);

        if (addResolution) {
            addFutureResolution(type, "class", simpleTypeName, this);
        }

        return type;
    }

    /**
     * Processes the type variables of a class that contains generics.
     * @param tree Root of the type parameters.
     * @param cls Class in which these type variables are contained.
     */
    private ArrayList<TypeInfo> buildTypeVariables(ParseTree tree) {
        ArrayList<TypeInfo> typeVariables = new ArrayList<TypeInfo>();
        ArrayList<TypeInfo> superBounds = new ArrayList<TypeInfo>();
        ArrayList<TypeInfo> extendsBounds = new ArrayList<TypeInfo>();

        for (Object o : tree.getChildren()) {
            // if we're not dealing with a type, skip
            // basically gets rid of commas and lessthan and greater than signs
            if (!o.toString().equals("typeParameter") &&
                    !o.toString().equals("typeArgument")) {
                continue;
            }

            ParseTree typeParameter = (ParseTree) o;

            TypeInfo type;
            // if we have a typeArgument and it is not a wildcard
            if ("typeArgument".equals(typeParameter.toString()) &&
                    !"?".equals(typeParameter.getChild(0).toString())) {
                type = buildType((ParseTree) typeParameter.getChild(0));
            } else {
                // otherwise, we have a wildcard or parameter
                // which can be more vague because of generics
                String name = typeParameter.getChild(0).toString();

                type = new TypeInfo(false, null, name, name, null);
                if ("?".equals(name)) {
                    type.setIsWildcard(true);
                } else {
                    // add generic
                    mClassNames.add(name);
                }
            }

            // if we have an extends or super on our type variable
            if (typeParameter.getChildCount() > 1) {
                ParseTree value = (ParseTree) typeParameter.getChild(1);

                if ("extends".equals(value.toString())) {
                    value = (ParseTree) typeParameter.getChild(2);

                    // wildcard extends
                    if ("type".equals(value.toString())) {
                        extendsBounds.add(buildType(value));
                    // all other extends
                    } else {
                        // will have to handle stuff with typeBound - multiple types
                        for (Object obj : value.getChildren()) {
                            if ("type".equals(obj.toString())) {
                                extendsBounds.add(buildType((ParseTree) obj));
                            }
                        }
                    }
                } else if ("super".equals(value.toString())) {
                    superBounds.add(buildType((ParseTree) typeParameter.getChild(2)));
                }
            }

            type.setIsTypeVariable(true);
            type.setBounds(superBounds, extendsBounds);
            typeVariables.add(type);
        }

        return typeVariables;
    }

    /**
     * Builds a MethodInfo for methods, constructors and annotation elements.
     * @param tree The tree to parse. methodDeclaration, interfaceMethodDeclaration
     * or annotationMethodDeclaration should be the root value.
     * @param containingClass the class in which this method exists.
     * @param isAnnotation true if the class is an annotation element
     * @return the MethodInfo
     */
    private MethodInfo buildMethod(ParseTree tree, ClassInfo containingClass,
            boolean isAnnotation) {
        Modifiers modifiers = new Modifiers(this);
        CommentAndPosition commentAndPosition = parseCommentAndPosition(tree);

        String name = null;
        StringBuilder flatSignature = new StringBuilder().append('(');
        ArrayList<TypeInfo> typeParameters = null;
        ArrayList<ParameterInfo> parameters = new ArrayList<ParameterInfo>();
        ArrayList<ClassInfo> thrownExceptions = new ArrayList<ClassInfo>();
        TypeInfo returnType = null;
        boolean isAnnotationElement = false;
        boolean isVarArg = false;
        String kind = "method"; // annotationElement, method, or constructor
        AnnotationValueInfo elementValue = null;
        ArrayList<Resolution> pendingResolutions = new ArrayList<Resolution>();

        @SuppressWarnings("unchecked")
        Iterator<ParseTree> it = (Iterator<ParseTree>) tree.getChildren().iterator();
        ParseTree child = it.next();

        modifiers.parseModifiers(child);

        child = it.next();

        // generics stuff
        if ("typeParameters".equals(child.toString())) {
            typeParameters = buildTypeVariables(child);
            child = it.next();
        }

        // handle returnType if we're not in a constructor
        if ("type".equals(child.toString())) {
            returnType = buildType(child);
            child = it.next();
        } else if ("void".equals(child.toString())) {
            returnType = new TypeInfo(true, null, "void", "void", null);
            child = it.next();
        }

        // this is the method name
        name = child.toString();

        if (name.equals(containingClass.name())) {
            kind = "constructor";
        }

        // probably don't need this check any longer since I unrolled the loop
//        if (isConstructorOrMethodName(child)) {
//            // this is the method name
//            name = child.toString();
//
//            if (name.equals(containingClass.name())) {
//                kind = "constructor";
//            }
//        }

        child = it.next();

        // method parameters
        if ("formalParameters".equals(child.toString())) {
            isVarArg = buildMethodParameters(child, parameters, flatSignature);
        } else {
            child = it.next();
        }

        child = it.next();
        flatSignature.append(')');

        // handle exception throwing
        if ("throws".equals(child.toString())) {
            child = it.next();

            for (Object o : child.getChildren()) {
                if (",".equals(o.toString())) {
                    continue;
                }

                // get the name of the exception, resolve it and add it to the list
                // unless we can't, in which case, add a resolution
                String exceptionName = buildQualifiedName(((ParseTree) o));
                StringBuilder exceptionQualifiedName = new StringBuilder();
                boolean isGeneric = resolveQualifiedName(exceptionName,
                        exceptionQualifiedName, this);

                if ("".equals(exceptionQualifiedName.toString())) {
                    pendingResolutions.add(new Resolution("thrownException", exceptionName, null));
                } else if (!isGeneric) {
                    thrownExceptions.add(Caches.obtainClass(exceptionQualifiedName.toString()));
                }
            }
        // handle default values for annotation elements
        } else if ("default".equals(child.toString())) {
            child = it.next();

            elementValue = buildElementValue(child, this);
            child = it.next();
        }

        if (isAnnotation) {
            kind = "annotationElement";
        }

        // Here we set signature, overridden method to null because
        // MethodInfo figures these values out later on
        MethodInfo method =  new MethodInfo(commentAndPosition.getCommentText(), typeParameters,
                name, null, containingClass, containingClass, modifiers.isPublic(),
                modifiers.isProtected(), modifiers.isPackagePrivate(),
                modifiers.isPrivate(), modifiers.isFinal(),
                modifiers.isStatic(), modifiers.isSynthetic(),
                modifiers.isAbstract(), modifiers.isSynchronized(),
                false, isAnnotationElement, kind, flatSignature.toString(),
                null, returnType, parameters, thrownExceptions,
                commentAndPosition.getPosition(), modifiers.getAnnotations());

        method.setVarargs(isVarArg);
        method.init(elementValue);

        for (Resolution r : pendingResolutions) {
            addFutureResolution(method, r.getVariable(), r.getValue(), this);
        }

        return method;
    }

    /**
     * Build the method parameters.
     * @param tree The tree to parse. formalParamaters should be the root value.
     * @param parameters List to put the method ParamaterInfos into.
     * @param flatSignature Pass in a StringBuilder with "(" in it to build the
     * flatSignature of the MethodInfo
     * @return true if the Method has a VarArgs parameter. false otherwise.
     */
    private boolean buildMethodParameters(ParseTree tree,
                                    ArrayList<ParameterInfo> parameters,
                                    StringBuilder flatSignature) {
        boolean isVarArg = false;
        for (Object obj : tree.getChildren()) {
            ParseTree child = (ParseTree) obj;

            if ("formalParameterDecls".equals(child.toString())) {
                for (Object formalParam : child.getChildren()) {
                    ParseTree param = (ParseTree) formalParam;
                    TypeInfo type = null;

                    if (param.getChildCount() == 0) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    Iterator<ParseTree> it = (Iterator<ParseTree>) param.getChildren().iterator();

                    ParseTree paramPart = it.next();

                    if ("variableModifiers".equals(paramPart.toString())) {
                        // TODO - handle variable modifiers - final, etc
                    }

                    paramPart = it.next();

                    type = buildType(paramPart);

                    buildSignatureForType(flatSignature, type);

                    if (param != child.getChildren().get(child.getChildCount()-1)) {
                        flatSignature.append(", ");
                    }

                    paramPart = it.next();

                    if ("...".equals(paramPart.toString())) {
                        isVarArg = true;
                        // thank you varargs for only being the last parameter
                        // you make life so much nicer
                        flatSignature.append("...");
                        paramPart = it.next();
                    }

                    String name = paramPart.toString();

                    CommentAndPosition commentAndPosition = new CommentAndPosition();
                    commentAndPosition.setPosition(paramPart);

                    parameters.add(new ParameterInfo(name, type.qualifiedTypeName(), type,
                            isVarArg, commentAndPosition.getPosition()));
                }
            }
        }

        return isVarArg;
    }

    /**
     * Builds a StringBuilder representing the Type, including type arguments.
     * @param builder StringBuilder in which the Type will be placed.
     * @param type the TypeInfo to turn into a String.
     */
    private void buildSignatureForType(StringBuilder builder, TypeInfo type) {
        // simple name
        builder.append(type.simpleTypeName());

        // generics
        if (type.typeArguments() != null && !type.typeArguments().isEmpty()) {
            builder.append('<');
            for (TypeInfo inner : type.typeArguments()) {
                if (inner != type.typeArguments().get(0)) {
                    builder.append(", ");
                }

                // recurse
                buildSignatureForType(builder, inner);
            }
            builder.append('>');
        }
    }

    /**
     * Builds a ClassInfo for an enum.
     * @param tree The tree to parse. enumDeclaration should be the root value.
     * @param containingClass ClassInfo that contains the enum declaration.
     * null if the enum is a root class.
     * @return the enum as a ClassInfo
     */
    private ClassInfo buildEnum(ParseTree tree, ClassInfo containingClass) {
        CommentAndPosition commentAndPosition = parseCommentAndPosition(tree);
        Modifiers modifiers = new Modifiers(this);
        ClassInfo cls = null;

        @SuppressWarnings("unchecked")
        Iterator<ParseTree> it = (Iterator<ParseTree>) tree.getChildren().iterator();

        ParseTree child = it.next();

        modifiers.parseModifiers(child);

        child = it.next();
        child = it.next();

        cls = buildClassName(child, containingClass, modifiers,
                commentAndPosition.getCommentText(),
                commentAndPosition.getPosition(), ClassType.ENUM);

        child = it.next();

        // handle implements
        if ("implements".equals(child.toString())) {
            child = it.next();

            parseInterfaces(child, cls);

            child = it.next();
        }

        buildEnumBody(child, cls);

        return cls;
    }

    /**
     * Parses the body of an enum.
     * @param tree The tree to parse. enumBody should be the root value.
     * @param containingClass ClassInfo to which this enum body pertains.
     */
    private void buildEnumBody(ParseTree tree, ClassInfo containingClass) {
        for (Object o : tree.getChildren()) {
            ParseTree child = (ParseTree) o;

            if ("enumConstants".equals(child.toString())) {
                for (Object o2 : child.getChildren()) {
                    ParseTree tmp = (ParseTree) o2;

                    if ("enumConstant".equals(tmp.toString())) {
                        containingClass.addEnumConstant(buildEnumConstant(tmp, containingClass));
                    }
                }
            } else if ("enumBodyDeclarations".equals(child.toString())) {
                buildClassBody(child, containingClass);
            }
        }
        return;
    }

    /**
     * Builds an enum constant.
     * @param tree The tree to parse. enumConstant should be the root value.
     * @param containingClass ClassInfo to which this enum constant pertains.
     * @return
     */
    private FieldInfo buildEnumConstant(ParseTree tree, ClassInfo containingClass) {
        @SuppressWarnings("unchecked")
        Iterator<ParseTree> it = (Iterator<ParseTree>) tree.getChildren().iterator();
        ParseTree child = it.next();

        Modifiers modifiers = new Modifiers(this);
        if ("annotations".equals(child.toString())) {
            modifiers.parseModifiers(child);
            child = it.next();
        }

        String name = child.toString();
        CommentAndPosition commentAndPosition = new CommentAndPosition();
        commentAndPosition.setCommentText(child);
        commentAndPosition.setPosition(child);
        Object constantValue = null;

        // get constantValue if it exists
        if (it.hasNext()) {
            child = it.next();

            // if we have an expressionList
            if (child.getChildCount() == 3) {
                StringBuilder builder = new StringBuilder();
                child = (ParseTree) child.getChild(1); // get the middle child

                for (Object o : child.getChildren()) {
                    if ("expression".equals(o.toString())) {
                        builder.append(parseExpression((ParseTree) o));

                        if (o != child.getChild(child.getChildCount()-1)) {
                            builder.append(", ");
                        }
                    }
                }

                constantValue = builder.toString();
            }
        }

        return new FieldInfo(name, containingClass, containingClass, containingClass.isPublic(),
        containingClass.isProtected(), containingClass.isPackagePrivate(),
        containingClass.isPrivate(), containingClass.isFinal(),
        containingClass.isStatic(), false, false, false,
        containingClass.type(), commentAndPosition.getCommentText(),
        constantValue, commentAndPosition.getPosition(),
        modifiers.getAnnotations());
    }

    /**
     * Builds a ClassInfo for an interface.
     * @param tree The tree to parse. normalInterfaceDeclaration should be the root value.
     * @param containingClass ClassInfo that contains the interface declaration.
     * null if the interface is a root class.
     * @return a ClassInfo representing the interface.
     */
    private ClassInfo buildInterface(ParseTree tree, ClassInfo containingClass) {
        @SuppressWarnings("unchecked")
        Iterator<ParseTree> it = (Iterator<ParseTree>) tree.getChildren().iterator();
        ParseTree child = it.next();

        // parse modifiers and get comment and position
        Modifiers modifiers = new Modifiers(this);
        modifiers.parseModifiers(child);
        CommentAndPosition commentAndPosition = parseCommentAndPosition(tree);

        it.next();
        child = it.next();

        // get class name
        ClassInfo iface = buildClassName(child, containingClass, modifiers,
                commentAndPosition.getCommentText(),
                commentAndPosition.getPosition(), ClassType.INTERFACE);

        child = it.next();

        // parse generics if they exist
        if ("typeParameters".equals(child.toString())) {
            iface.type().setTypeArguments(buildTypeVariables(child));
            child = it.next();
        }

        // parse interfaces implemented by this interface
        if ("extends".equals(child.toString())) {
            child = it.next();

            parseInterfaces(child, iface);

            child = it.next();
        }

        // finally, build the body of the interface
        buildInterfaceBody(child, iface);

        return iface;
    }

    /**
     * Parses the body of the interface, adding it to iface.
     * @param tree The tree to parse. interfaceBody should be the root value.
     * @param iface ClassInfo that will contain all of the interface body.
     */
    private void buildInterfaceBody(ParseTree tree, ClassInfo iface) {
        for (Object o : tree.getChildren()) {
            if (!o.toString().equals("interfaceBodyDeclaration")) {
                continue;
            }

            ParseTree child = (ParseTree) ((ParseTree) o).getChild(0);

            if (";".equals(child.toString())) {
                continue;
            }

            // field
            if ("interfaceFieldDeclaration".equals(child.toString())) {
                for (FieldInfo f : buildFields(child, iface)) {
                    iface.addField(f);
                }
            // method
            } else if ("interfaceMethodDeclaration".equals(child.toString())) {
                iface.addMethod(buildMethod(child, iface, false));
            // inner class
            } else if ("normalClassDeclaration".equals(child.getChild(0).toString())) {
                iface.addInnerClass(buildClass((ParseTree) child.getChild(0), iface));
            // inner enum
            } else if ("enumDeclaration".equals(child.getChild(0).toString())) {
                iface.addInnerClass(buildEnum((ParseTree) child.getChild(0), iface));
            // inner interface
            } else if ("normalInterfaceDeclaration".equals(child.getChild(0).toString())) {
                iface.addInnerClass(buildInterface((ParseTree) child.getChild(0), iface));
            // inner annotation
            } else if ("annotationTypeDeclaration".equals(child.getChild(0).toString())) {
                iface.addInnerClass(buildAnnotationDeclaration(
                        (ParseTree) child.getChild(0), iface));
            }
        }
    }

    /**
     * Builds a ClassInfo of an annotation declaration.
     * @param tree The tree to parse. annotationTypeDeclaration should be the root value.
     * @param containingClass The class that contains this annotation.
     * null if this is a root annotation.
     * @return the ClassInfo of the annotation declaration.
     */
    private ClassInfo buildAnnotationDeclaration(ParseTree tree, ClassInfo containingClass) {
        @SuppressWarnings("unchecked")
        Iterator<ParseTree> it = (Iterator<ParseTree>) tree.getChildren().iterator();
        ParseTree child = it.next();

        // get comment and position
        CommentAndPosition commentAndPosition = parseCommentAndPosition(tree);

        // modifiers
        Modifiers modifiers = new Modifiers(this);
        modifiers.parseModifiers(child);

        // three calls to next to skip over @, interface and then
        // make child = the name of this annotation
        it.next();
        it.next();
        child = it.next();

        // build class name and initialize the class
        ClassInfo annotation = buildClassName(child, containingClass, modifiers,
                commentAndPosition.getCommentText(),
                commentAndPosition.getPosition(), ClassType.INTERFACE);

        child = it.next();

        // build annotation body
        buildAnnotationBody(child, annotation);

        return annotation;
    }

    /**
     * Parses the body of the annotation declaration.
     * @param tree The tree to parse. annotationTypeBody should be the root value.
     * @param annotation the Classinfo in which the annotation elements should be added.
     */
    private void buildAnnotationBody(ParseTree tree, ClassInfo annotation) {
        for (Object o : tree.getChildren()) {
            if (!"annotationTypeElementDeclaration".equals(o.toString())) {
                continue;
            }

            ParseTree child = (ParseTree) ((ParseTree) o).getChild(0);

            // annotation fields
            if ("interfaceFieldDeclaration".equals(child.toString())) {
                for (FieldInfo f : buildFields(child, annotation)) {
                    annotation.addField(f);
                }
            // annotation methods
            } else if ("annotationMethodDeclaration".equals(child.toString())) {
                annotation.addAnnotationElement(buildMethod(child, annotation, true));
            // inner class
            } else if ("normalClassDeclaration".equals(child.toString())) {
                annotation.addInnerClass(buildClass((ParseTree) child, annotation));
            // enum
            } else if ("enumDeclaration".equals(child.toString())) {
                annotation.addInnerClass(buildEnum((ParseTree) child, annotation));
            // inner interface
            } else if ("normalInterfaceDeclaration".equals(child.toString())) {
                annotation.addInnerClass(buildInterface((ParseTree) child, annotation));
            // inner annotation
            } else if ("annotationTypeDeclaration".equals(child.toString())) {
                annotation.addInnerClass(buildAnnotationDeclaration(
                        (ParseTree) child, annotation));
            }
        }
    }

    /**
     * Build an annotation instance.
     * @param tree The tree to parse. annotation should be the root value.
     * @param builder InfoBuilder of this file.
     * @return The AnnotationInstanceInfo being parsed.
     */
    private static AnnotationInstanceInfo buildAnnotationInstance(ParseTree tree,
            InfoBuilder builder) {
        @SuppressWarnings("unchecked")
        Iterator<ParseTree> it = (Iterator<ParseTree>) tree.getChildren().iterator();

        AnnotationInstanceInfo annotationInstance = new AnnotationInstanceInfo();

        it.next();

        // parse the name, get its full version, and then get the ClassInfo of it, if possible.
        String name = InfoBuilder.buildQualifiedName(it.next());
        StringBuilder qualifiedNameBuilder = new StringBuilder();
        resolveQualifiedName(name, qualifiedNameBuilder, builder);

        if ("".equals(qualifiedNameBuilder.toString())) {
            addFutureResolution(annotationInstance, "annotationTypeName", name, builder);
            annotationInstance.setSimpleAnnotationName(name); // TODO - remove once we've completed the parser
        } else { // can't have generics here so we won't do a test
            annotationInstance.setClass(Caches.obtainClass(qualifiedNameBuilder.toString()));
        }

        // at this point, the annotation is either finished or we have more work to do
        if (!it.hasNext()) {
            return annotationInstance;
        }

        it.next();
        ParseTree child = it.next();

        // parse elementValue pairs
        if ("elementValuePairs".equals(child.toString())) {
            for (Object o : child.getChildren()) {
                if (!"elementValuePair".equals(o.toString())) {
                    continue;
                }

                ParseTree inner = (ParseTree) o;
                MethodInfo element = null;
                String methodName = inner.getChild(0).toString();

                // try and look up the MethodInfo for this annotation, if possible
                if (annotationInstance.type() != null) {
                    for (MethodInfo m : annotationInstance.type().annotationElements()) {
                        if (methodName.equals(m.name()) ||
                                annotationInstance.type().annotationElements().size() == 1) {
                            element = m;
                            break;
                        }
                    }
                }

                // go to elementValue
                AnnotationValueInfo info = buildElementValue(
                        (ParseTree) inner.getChild(2), builder);

                if (element == null) {
                    addFutureResolution(info, "element", methodName, builder);
                    info.setAnnotationInstanceName(name);
                } else {
                    info.setElement(element);
                }

                annotationInstance.addElementValue(info);
            }
        // parse element value
        } else if ("elementValue".equals(child.toString())) {
            annotationInstance.addElementValue(buildElementValue(child, builder));
        }

        return annotationInstance;
    }

    /**
     * Builds the value of the annotation element.
     * @param tree The tree to parse. elementValue should be the root value.
     * @param builder InfoBuilder of this file.
     * @return AnnotationValueInfo representing the elementValue.
     */
    private static AnnotationValueInfo buildElementValue(ParseTree tree, InfoBuilder builder) {
        AnnotationValueInfo elementValue = new AnnotationValueInfo();
        Object value = null;

        // parse some stuff
        String str = tree.getChild(0).toString();
        if ("conditionalExpression".equals(str)) {
            value = parseExpression((ParseTree) tree.getChild(0));
        } else if ("annotation".equals(str)) {
            value = InfoBuilder.buildAnnotationInstance((ParseTree) tree.getChild(0), builder);
        } else if ("elementValueArrayInitializer".equals(str)) {
            ParseTree child = (ParseTree) tree.getChild(0);
            ArrayList<AnnotationValueInfo> values = new ArrayList<AnnotationValueInfo>();
            for (Object o : child.getChildren()) {
                if ("elementValue".equals(o.toString())) {
                    values.add(buildElementValue((ParseTree) o, builder));
                }
            }

            value = values;
        }

        elementValue.init(value);

        return elementValue;
    }

    /**
     * Get the dimensions of the type, as a String.
     * @param tree The tree to parse. type should be the root value.
     * @return A String of the dimensions of the type.
     */
    private String getDimensions(ParseTree tree) {
        // we only have dimensions if the count is not 1
        if (tree.getChildCount() == 1) {
            return null;
        }

        StringBuilder builder = new StringBuilder();

        for (int i = 1; i < tree.getChildCount(); i++) {
            builder.append(((ParseTree) tree.getChild(i)).toString());
        }

        return builder.toString();
    }

    /**
     * When we have data that we can't yet parse, save it for later.
     * @param resolvable Resolvable to which the data refers.
     * @param variable Variable in the document to which the data refers;
     * @param value Value for the variable
     * @param builder The InfoBuilder of this file
     */
    private static void addFutureResolution(Resolvable resolvable, String variable,
            String value, InfoBuilder builder) {
        resolvable.addResolution(new Resolution(variable, value, builder));

        Caches.addResolvableToCache(resolvable);
    }

    /**
     * Turns a short name of a class into the qualified name of a class.
     * StringBuilder will contain an empty string if not found.
     * @param name the abbreviated name of the class
     * @param qualifiedClassName the qualified name that will be set if found.
     * Unchanged if not found.
     * @param builder InfoBuilder with all of the file specific information necessary
     * to properly resolve the name.
     * @return a boolean is returned that will be true if the type is a generic. false otherwise.
     */
    public static boolean resolveQualifiedName(String name,
                                                StringBuilder qualifiedClassName,
                                                InfoBuilder builder) {
        // steps to figure out a class's real name
        // check class(es) in this file

        // trying something out. let's see how this works
        if (name.indexOf('.') != -1) {
            qualifiedClassName.append(name);
            return false;
        }

        // TODO - search since we're now a HashSet
        for (String className : builder.getClassNames()) {
            int beginIndex = className.lastIndexOf(".") + 1;

            if (className.substring(beginIndex).equals(name)) {
                qualifiedClassName.append(className);
                return qualifiedClassName.toString().equals(name);
            }
        }

        // check package
        ClassInfo potentialClass = builder.getPackage().getClass(name);

        if (potentialClass != null) {
            qualifiedClassName.append(potentialClass.qualifiedName());
            return qualifiedClassName.toString().equals(name);
        }

        potentialClass = null;

        String potentialName = null;
        // check superclass and interfaces for type
        if (builder.getRootClass() != null) {
            potentialName = resolveQualifiedNameInInheritedClass(name, builder.getRootClass(),
                    builder.getRootClass().containingPackage().name());
        }

        if (potentialName != null) {
            qualifiedClassName.append(potentialName);
            return false;
        }


        // check class imports - ie, java.lang.String;
        ArrayList<String> packagesToCheck = new ArrayList<String>();
        for (String imp : builder.getImports()) {
            // +1 to get rid of off by 1 error
            String endOfName = imp.substring(imp.lastIndexOf('.') + 1);
            if (endOfName.equals(name) || (name.indexOf('.') != -1 &&
                                           endOfName.equals(
                                                   name.substring(0, name.lastIndexOf('.'))))) {
                qualifiedClassName.append(imp);
                return qualifiedClassName.toString().equals(name);
            } else if (endOfName.equals("*")) {
                // add package to check
                packagesToCheck.add(imp.substring(0, imp.lastIndexOf('.')));
            } else {
                // check inner classes
                ClassInfo cl = Caches.obtainClass(imp);
                String possibleName = resolveQualifiedInnerName(cl.qualifiedName() + "." + name,
                        cl);
                if (possibleName != null) {
                    qualifiedClassName.append(possibleName);
                    return false;
                }
            }
        }

        // check package imports - ie, java.lang.*;
        for (String packageName : packagesToCheck) {
            PackageInfo pkg = Caches.obtainPackage(packageName);

            ClassInfo cls = pkg.getClass(name);

            if (cls != null && name.equals(cls.name())) {
                qualifiedClassName.append(cls.qualifiedName());
                return qualifiedClassName.toString().equals(name);
            }
        }
        //     including import's inner classes...
        // check package of imports...

        // TODO - remove
        // FROM THE JAVADOC VERSION OF THIS FUNCTION
        // Find the specified class or interface within the context of this class doc.
        // Search order: 1) qualified name, 2) nested in this class or interface,
        // 3) in this package, 4) in the class imports, 5) in the package imports.
        // Return the ClassDoc if found, null if not found.

        return false;
    }

    private static String resolveQualifiedNameInInheritedClass(String name, ClassInfo cl,
            String originalPackage) {
        ArrayList<ClassInfo> classesToCheck = new ArrayList<ClassInfo>();
        if (cl != null) {
            // if we're in a new package only, check it
            if (cl.containingPackage() != null &&
                    !originalPackage.equals(cl.containingPackage().name())) {
                // check for new class
                ClassInfo cls = cl.containingPackage().getClass(name);

                if (cls != null && name.equals(cls.name())) {
                    return cls.name();
                }
            }

            if (cl.realSuperclass() != null) {
                classesToCheck.add(cl.realSuperclass());
            }

            if (cl.realInterfaces() != null) {
                for (ClassInfo iface : cl.realInterfaces()) {
                    classesToCheck.add(iface);
                }
            }

            for (ClassInfo cls : classesToCheck) {
                String potential = resolveQualifiedNameInInheritedClass(name, cls, originalPackage);

                if (potential != null) {
                    return potential;
                }
            }
        }
        return null;
    }

    private static String resolveQualifiedInnerName(String possibleQualifiedName, ClassInfo cl) {
        if (cl.innerClasses() == null) {
            return null;
        }

        for (ClassInfo inner : cl.innerClasses()) {
            if (possibleQualifiedName.equals(inner.qualifiedName())) {
                return possibleQualifiedName;
            }

            String name = resolveQualifiedInnerName(possibleQualifiedName + "." + inner.name(),
                    inner);

            if (name != null) {
                return name;
            }
        }

        return null;
    }

    /**
     * Parses the tree, looking for the comment and position.
     * @param tree The tree to parse.
     * @return a CommentAndPosition object containing the comment and position of the element.
     */
    private CommentAndPosition parseCommentAndPosition(ParseTree tree) {
        Tree child = tree.getChild(0).getChild(0);

        // three options (modifiers with annotations, modifiers w/o annotations, no modifiers)
        // if there are no modifiers, use tree.getChild(1)
        // otherwise, dive as deep as possible into modifiers to get to the comment and position.
        child = ("<epsilon>".equals(child.toString())) ? tree.getChild(1) : child;

        while (child.getChildCount() > 0) {
            child = child.getChild(0);
        }

        CommentAndPosition cAndP = new CommentAndPosition();
        cAndP.setCommentText((ParseTree) child);
        cAndP.setPosition((ParseTree) child);
        return cAndP;
    }

    /**
     * Private class to facilitate passing the comment and position out of a function.
     */
    private class CommentAndPosition {
        public String getCommentText() {
            return mCommentText;
        }

        /**
         * Parses the tree to get the commentText and set that value.
         * @param tree The tree to parse. Should be pointing to the node containing the comment.
         */
        public void setCommentText(ParseTree tree) {
            if (tree.hiddenTokens != null && !tree.hiddenTokens.isEmpty()) {
                mCommentText = ((CommonToken) tree.hiddenTokens.get(0)).getText();

                if (mCommentText != null) {
                    return;
                }
            }

            mCommentText = "";
        }

        public SourcePositionInfo getPosition() {
            return mPosition;
        }

        /**
         * Parses the tree to get the SourcePositionInfo of the node.
         * @param tree The tree to parse. Should be pointing to the node containing the position.
         */
        public void setPosition(ParseTree tree) {
          CommonToken token = (CommonToken) tree.payload;

          int line = token.getLine();
          int column = token.getCharPositionInLine();
          String fileName = ((ANTLRFileStream) token.getInputStream()).getSourceName();

          mPosition = new SourcePositionInfo(fileName, line, column);
        }

        private String mCommentText;
        private SourcePositionInfo mPosition;
    }

    /**
     * Private class to handle all the possible modifiers to a class/interface/field/anything else.
     */
    private class Modifiers {
        private boolean mIsPublic = false;
        private boolean mIsProtected = false;
        private boolean mIsPackagePrivate = true;
        private boolean mIsPrivate = false;
        private boolean mIsStatic = false;
        private boolean mIsAbstract = false;
        private boolean mIsFinal = false;
        private boolean mIsTransient = false;
        private boolean mIsVolatile = false;
        private boolean mIsSynthetic = false;
        private boolean mIsSynchronized = false;
        private boolean mIsStrictfp = false;
        private InfoBuilder mBuilder;
        private ArrayList<AnnotationInstanceInfo> mAnnotations;

        public Modifiers(InfoBuilder builder) {
            mAnnotations = new ArrayList<AnnotationInstanceInfo>();
            mBuilder = builder;
        }

        /**
         * Parses all of the modifiers of any declaration, including annotations.
         * @param tree
         */
        public void parseModifiers(ParseTree tree) {
            for (Object child : tree.getChildren()) {
                String modifier = child.toString();

                if ("public".equals(modifier)) {
                    mIsPublic = true;
                    mIsPackagePrivate = false;
                } else if ("protected".equals(modifier)) {
                    mIsProtected = true;
                    mIsPackagePrivate = false;
                } else if ("private".equals(modifier)) {
                    mIsPrivate = true;
                    mIsPackagePrivate = false;
                } else if ("static".equals(modifier)) {
                    mIsStatic = true;
                } else if ("abstract".equals(modifier)) {
                    mIsAbstract = true;
                } else if ("final".equals(modifier)) {
                    mIsFinal = true;
                } else if ("transient".equals(modifier)) {
                    mIsTransient = true;
                } else if ("volatile".equals(modifier)) {
                    mIsVolatile = true;
                } else if ("synthetic".equals(modifier)) {
                    mIsSynthetic = true;
                } else if ("synchronized".equals(modifier)) {
                    mIsSynchronized = true;
                }  else if ("strictfp".equals(modifier)) {
                    mIsStrictfp = true;
                } else if ("annotation".equals(modifier)) {
                    mAnnotations.add(buildAnnotationInstance((ParseTree) child, mBuilder));
                }
            }
        }

        public boolean isPublic() {
            return mIsPublic;
        }

        public boolean isProtected() {
            return mIsProtected;
        }

        public boolean isPackagePrivate() {
            return mIsPackagePrivate;
        }

        public boolean isPrivate() {
            return mIsPrivate;
        }

        public boolean isStatic() {
            return mIsStatic;
        }

        public boolean isAbstract() {
            return mIsAbstract;
        }

        public boolean isFinal() {
            return mIsFinal;
        }

        public boolean isTransient() {
            return mIsTransient;
        }

        public boolean isVolatile() {
            return mIsVolatile;
        }

        public boolean isSynthetic() {
            return mIsSynthetic;
        }

        public boolean isSynchronized() {
            return mIsSynchronized;
        }

        @SuppressWarnings("unused")
        public boolean isStrictfp() {
            return mIsStrictfp;
        }

        public ArrayList<AnnotationInstanceInfo> getAnnotations() {
            return mAnnotations;
        }
    };


    /**
     * Singleton class to store all of the global data amongst every InfoBuilder.
     */
    public static class Caches {
        private static HashMap<String, PackageInfo> mPackages
                                        = new HashMap<String, PackageInfo>();
        private static HashMap<String, ClassInfo> mClasses
                                        = new HashMap<String, ClassInfo>();
        private static HashSet<Resolvable> mInfosToResolve
                                        = new HashSet<Resolvable>();

        public static PackageInfo obtainPackage(String packageName) {
            PackageInfo pkg = mPackages.get(packageName);

            if (pkg == null) {
                pkg = new PackageInfo(packageName);
                mPackages.put(packageName, pkg);
            }

            return pkg;
        }

        /**
         * Gets the ClassInfo from the master list or creates a new one if it does not exist.
         * @param qualifiedClassName Qualified name of the ClassInfo to obtain.
         * @return the ClassInfo
         */
        public static ClassInfo obtainClass(String qualifiedClassName) {
            ClassInfo cls = mClasses.get(qualifiedClassName);

            if (cls == null) {
                cls = new ClassInfo(qualifiedClassName);
                mClasses.put(cls.qualifiedName(), cls);
            }

            return cls;
        }

        /**
         * Gets the ClassInfo from the master list or returns null if it does not exist.
         * @param qualifiedClassName Qualified name of the ClassInfo to obtain.
         * @return the ClassInfo or null, if the ClassInfo does not exist.
         */
        public static ClassInfo getClass(String qualifiedClassName) {
            return mClasses.get(qualifiedClassName);
        }

        public static void addResolvableToCache(Resolvable resolvable) {
            mInfosToResolve.add(resolvable);
        }

        public static void printResolutions() {
            if (mInfosToResolve.isEmpty()) {
                System.out.println("We've resolved everything.");
                return;
            }

            for (Resolvable r : mInfosToResolve) {
                r.printResolutions();
                System.out.println();
            }
        }

        public static void resolve() {
            HashSet<Resolvable> resolveList = mInfosToResolve;
            mInfosToResolve = new HashSet<Resolvable>();

            for (Resolvable r : resolveList) {
                // if we could not resolve everything in this class
                if (!r.resolveResolutions()) {
                    mInfosToResolve.add(r);
                }

                System.out.println();
            }
        }
    }

    public PackageInfo getPackage() {
        return mPackage;
    }

    public ArrayList<String> getImports() {
        return mImports;
    }

    public HashSet<String> getClassNames() {
        return mClassNames;
    }

    public ClassInfo getRootClass() {
        return mRootClass;
    }
}
