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

import static signature.converter.dex.DexUtil.getClassName;
import static signature.converter.dex.DexUtil.getPackageName;
import signature.model.IClassDefinition;
import signature.model.IClassReference;
import signature.model.IConstructor;
import signature.model.IGenericDeclaration;
import signature.model.IMethod;
import signature.model.ITypeReference;
import signature.model.ITypeVariableDefinition;
import signature.model.ITypeVariableReference;
import signature.model.impl.SigArrayType;
import signature.model.impl.SigParameterizedType;
import signature.model.impl.SigPrimitiveType;
import signature.model.impl.SigTypeVariableDefinition;
import signature.model.impl.SigWildcardType;
import signature.model.impl.Uninitialized;
import signature.model.util.ITypeFactory;

import java.lang.reflect.GenericSignatureFormatError;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements a parser for the generics signature attribute. Uses a top-down,
 * recursive descent parsing approach for the following grammar:
 * 
 * <pre>
 * ClassSignature ::=
 *     OptFormalTypeParams SuperclassSignature {SuperinterfaceSignature}.
 * SuperclassSignature ::= ClassTypeSignature.
 * SuperinterfaceSignature ::= ClassTypeSignature.
 *
 * OptFormalTypeParams ::=
 *     ["<" FormalTypeParameter {FormalTypeParameter} ">"].
 *
 * FormalTypeParameter ::= Ident ClassBound {InterfaceBound}.
 * ClassBound ::= ":" [FieldTypeSignature].
 * InterfaceBound ::= ":" FieldTypeSignature.
 *
 * FieldTypeSignature ::=
 *     ClassTypeSignature | ArrayTypeSignature | TypeVariableSignature.
 * ArrayTypeSignature ::= "[" TypSignature.
 *
 * ClassTypeSignature ::=
 *     "L" {Ident "/"} Ident OptTypeArguments {"." Ident OptTypeArguments} ";".
 *
 * OptTypeArguments ::= "<" TypeArgument {TypeArgument} ">".
 *
 * TypeArgument ::= ([WildcardIndicator] FieldTypeSignature) | "*".
 * WildcardIndicator ::= "+" | "-".
 *
 * TypeVariableSignature ::= "T" Ident ";".
 *
 * TypSignature ::= FieldTypeSignature | BaseType.
 * BaseType ::= "B" | "C" | "D" | "F" | "I" | "J" | "S" | "Z".
 *
 * MethodTypeSignature ::=
 *     OptFormalTypeParams "(" {TypeSignature} ")" ReturnType {ThrowsSignature}.
 * ThrowsSignature ::= ("^" ClassTypeSignature) | ("^" TypeVariableSignature).
 *
 * ReturnType ::= TypSignature | VoidDescriptor.
 * VoidDescriptor ::= "V".
 * </pre>
 */
public class GenericSignatureParser {

    public List<ITypeReference> exceptionTypes;
    public List<ITypeReference> parameterTypes;
    public List<ITypeVariableDefinition> formalTypeParameters;
    public ITypeReference returnType;
    public ITypeReference fieldType;
    public List<ITypeReference> interfaceTypes;
    public ITypeReference superclassType;

    private IGenericDeclaration genericDecl;

    /*
     * Parser:
     */
    private char symbol; // 0: eof; else valid term symbol or first char of
    // identifier.
    private String identifier;


    /*
     * Scanner: eof is private to the scan methods and it's set only when a scan
     * is issued at the end of the buffer.
     */
    private boolean eof;

    private char[] buffer;
    private int pos;

    private final ITypeFactory factory;
    private final IClassInitializer classFinder;
    private boolean parseForField;


    public GenericSignatureParser(ITypeFactory factory,
            IClassInitializer classFinder) {
        this.factory = factory;
        this.classFinder = classFinder;
    }

    private void setInput(IGenericDeclaration genericDecl, String input) {
        if (input != null) {
            this.genericDecl = genericDecl;
            this.buffer = input.toCharArray();
            this.eof = false;
            scanSymbol();
        } else {
            this.eof = true;
        }
    }

    public ITypeReference parseNonGenericType(String typeSignature) {
        setInput(null, typeSignature);
        ITypeReference type = parsePrimitiveType();
        if (type == null) {
            type = parseFieldTypeSignature();
        }
        return type;
    }

    public ITypeReference parseNonGenericReturnType(String typeSignature) {
        setInput(null, typeSignature);
        ITypeReference returnType = parsePrimitiveType();
        if (returnType == null) {
            returnType = parseReturnType();
        }
        return returnType;
    }

    private ITypeReference parsePrimitiveType() {
        switch (symbol) {
        case 'B':
            scanSymbol();
            return SigPrimitiveType.BYTE_TYPE;
        case 'C':
            scanSymbol();
            return SigPrimitiveType.CHAR_TYPE;
        case 'D':
            scanSymbol();
            return SigPrimitiveType.DOUBLE_TYPE;
        case 'F':
            scanSymbol();
            return SigPrimitiveType.FLOAT_TYPE;
        case 'I':
            scanSymbol();
            return SigPrimitiveType.INT_TYPE;
        case 'J':
            scanSymbol();
            return SigPrimitiveType.LONG_TYPE;
        case 'S':
            scanSymbol();
            return SigPrimitiveType.SHORT_TYPE;
        case 'Z':
            scanSymbol();
            return SigPrimitiveType.BOOLEAN_TYPE;
        default:
            return null;
        }
    }

    /**
     * Parses the generic signature of a class and creates the data structure
     * representing the signature.
     * 
     * @param classToProcess
     *            the GenericDeclaration calling this method
     * @param signature
     *            the generic signature of the class
     */
    public void parseForClass(IClassDefinition classToProcess,
            String signature) {
        setInput(classToProcess, signature);
        if (!eof) {
            parseClassSignature();
        } else {
            throw new IllegalStateException("Generic signature is invalid!");
        }
    }

    /**
     * Parses the generic signature of a method and creates the data structure
     * representing the signature.
     * 
     * @param genericDecl
     *            the GenericDeclaration calling this method
     * @param signature
     *            the generic signature of the class
     */
    public void parseForMethod(IMethod genericDecl, String signature) {
        setInput(genericDecl, signature);
        if (!eof) {
            parseMethodTypeSignature();
        } else {
            throw new IllegalStateException("Generic signature is invalid!");
        }
    }

    /**
     * Parses the generic signature of a constructor and creates the data
     * structure representing the signature.
     * 
     * @param genericDecl
     *            the GenericDeclaration calling this method
     * @param signature
     *            the generic signature of the class
     */
    public void parseForConstructor(IConstructor genericDecl,
            String signature) {
        setInput(genericDecl, signature);
        if (!eof) {
            parseMethodTypeSignature();
        } else {
            throw new IllegalStateException("Generic signature is invalid!");
        }
    }

    /**
     * Parses the generic signature of a field and creates the data structure
     * representing the signature.
     * 
     * @param genericDecl
     *            the GenericDeclaration calling this method
     * @param signature
     *            the generic signature of the class
     */
    public void parseForField(IClassDefinition genericDecl, String signature) {
        parseForField = true;
        setInput(genericDecl, signature);
        try {
            if (!eof) {
                this.fieldType = parseFieldTypeSignature();
            } else {
                throw new IllegalStateException(
                        "Generic signature is invalid!");
            }
        } finally {
            parseForField = false;
        }
    }

    private void parseClassSignature() {
        // ClassSignature ::=
        // OptFormalTypeParameters SuperclassSignature
        // {SuperinterfaceSignature}.

        parseOptFormalTypeParameters();

        // SuperclassSignature ::= ClassTypeSignature.
        this.superclassType = parseClassTypeSignature();

        interfaceTypes = new ArrayList<ITypeReference>(16);
        while (symbol > 0) {
            // SuperinterfaceSignature ::= ClassTypeSignature.
            interfaceTypes.add(parseClassTypeSignature());
        }
    }

    private void parseOptFormalTypeParameters() {
        // OptFormalTypeParameters ::=
        // ["<" FormalTypeParameter {FormalTypeParameter} ">"].

        List<ITypeVariableDefinition> typeParameters =
                new ArrayList<ITypeVariableDefinition>();

        if (symbol == '<') {
            scanSymbol();
            typeParameters.add(parseFormalTypeParameter());
            while ((symbol != '>') && (symbol > 0)) {
                typeParameters.add(parseFormalTypeParameter());
            }
            expect('>');
        }

        formalTypeParameters = typeParameters;
    }

    private SigTypeVariableDefinition parseFormalTypeParameter() {
        // FormalTypeParameter ::= Ident ClassBound {InterfaceBound}.

        scanIdentifier();
        String name = identifier.intern();
        SigTypeVariableDefinition typeVariable = factory.getTypeVariable(name,
                genericDecl);

        List<ITypeReference> bounds = new ArrayList<ITypeReference>();

        // ClassBound ::= ":" [FieldTypeSignature].
        expect(':');
        if (symbol == 'L' || symbol == '[' || symbol == 'T') {
            bounds.add(parseFieldTypeSignature());
        }

        while (symbol == ':') {
            // InterfaceBound ::= ":" FieldTypeSignature.
            scanSymbol();
            bounds.add(parseFieldTypeSignature());
        }
        typeVariable.setUpperBounds(bounds);
        return typeVariable;
    }

    /**
     * Returns the generic declaration for the type variable with the specified
     * name.
     * 
     * @param variableName
     *            the name of the type variable
     * @param declaration
     *            the declaration to start searching
     * @return the declaration which defines the specified type variable
     */
    private IGenericDeclaration getDeclarationOfTypeVariable(
            String variableName, IClassDefinition declaration) {
        assert variableName != null;
        assert declaration != null;

        if (!Uninitialized.isInitialized(declaration.getTypeParameters())) {
            declaration = classFinder.initializeClass(declaration
                    .getPackageName(), declaration.getName());
        }

        for (ITypeVariableDefinition typeVariable : declaration
                .getTypeParameters()) {
            if (variableName.equals(typeVariable.getName())) {
                return declaration;
            }
        }
        return getDeclarationOfTypeVariable(variableName, declaration
                .getDeclaringClass());
    }

    private ITypeReference parseFieldTypeSignature() {
        // FieldTypeSignature ::= ClassTypeSignature | ArrayTypeSignature
        // | TypeVariableSignature.

        switch (symbol) {
        case 'L':
            return parseClassTypeSignature();
        case '[':
            // ArrayTypeSignature ::= "[" TypSignature.
            scanSymbol();
            SigArrayType arrayType = factory.getArrayType(parseTypeSignature());
            return arrayType;
        case 'T':
            return parseTypeVariableSignature();
        default:
            throw new GenericSignatureFormatError();
        }
    }

    private ITypeReference parseClassTypeSignature() {
        // ClassTypeSignature ::= "L" {Ident "/"} Ident
        // OptTypeArguments {"." Ident OptTypeArguments} ";".

        expect('L');

        StringBuilder qualIdent = new StringBuilder("L");
        scanIdentifier();
        while (symbol == '/') {
            scanSymbol();
            qualIdent.append(identifier).append("/");
            scanIdentifier();
        }

        qualIdent.append(this.identifier);


        List<ITypeReference> typeArgs = parseOptTypeArguments();

        ITypeReference parentType = null;

        String packageName = getPackageName(qualIdent.toString() + ";");
        String className = getClassName(qualIdent.toString() + ";");

        if (typeArgs.isEmpty()) {
            parentType = factory.getClassReference(packageName, className);
        } else {
            IClassReference rawType = factory.getClassReference(packageName,
                    className);
            SigParameterizedType parameterizedType = factory
                    .getParameterizedType(null, rawType, typeArgs);
            parentType = parameterizedType;
        }

        ITypeReference typeToReturn = parentType;


        // if owner type is a parameterized type, the types are separated by '.'
        while (symbol == '.') {
            // Deal with Member Classes:
            scanSymbol();
            scanIdentifier();
            qualIdent.append("$").append(identifier);
            typeArgs = parseOptTypeArguments();
            ITypeReference memberType = null;

            packageName = getPackageName(qualIdent.toString() + ";");
            className = getClassName(qualIdent.toString() + ";");

            if (typeArgs.isEmpty()) {
                memberType = factory.getClassReference(packageName, className);
            } else {
                IClassReference rawType = factory.getClassReference(
                        packageName, className);
                SigParameterizedType parameterizedType = factory
                        .getParameterizedType(parentType, rawType, typeArgs);
                memberType = parameterizedType;
            }
            typeToReturn = memberType;
        }

        expect(';');

        return typeToReturn;
    }

    private List<ITypeReference> parseOptTypeArguments() {
        // OptTypeArguments ::= "<" TypeArgument {TypeArgument} ">".

        List<ITypeReference> typeArgs = new ArrayList<ITypeReference>(8);
        if (symbol == '<') {
            scanSymbol();

            typeArgs.add(parseTypeArgument());
            while ((symbol != '>') && (symbol > 0)) {
                typeArgs.add(parseTypeArgument());
            }
            expect('>');
        }
        return typeArgs;
    }

    private ITypeReference parseTypeArgument() {
        // TypeArgument ::= (["+" | "-"] FieldTypeSignature) | "*".
        List<ITypeReference> extendsBound = new ArrayList<ITypeReference>(1);
        ITypeReference superBound = null;
        if (symbol == '*') {
            scanSymbol();
            extendsBound.add(factory.getClassReference("java.lang", "Object"));
            SigWildcardType wildcardType = factory.getWildcardType(superBound,
                    extendsBound);
            return wildcardType;
        } else if (symbol == '+') {
            scanSymbol();
            extendsBound.add(parseFieldTypeSignature());
            SigWildcardType wildcardType = factory.getWildcardType(superBound,
                    extendsBound);
            return wildcardType;
        } else if (symbol == '-') {
            scanSymbol();
            superBound = parseFieldTypeSignature();
            extendsBound.add(factory.getClassReference("java.lang", "Object"));
            SigWildcardType wildcardType = factory.getWildcardType(superBound,
                    extendsBound);
            return wildcardType;
        } else {
            return parseFieldTypeSignature();
        }
    }

    private ITypeVariableReference parseTypeVariableSignature() {
        // TypeVariableSignature ::= "T" Ident ";".
        expect('T');
        scanIdentifier();
        expect(';');

        IGenericDeclaration declaration = genericDecl;

        if (!factory.containsTypeVariableDefinition(identifier, declaration)) {
            // since a field is not a generic declaration, i need to treat it
            // differently.
            // the generic declaration
            if (parseForField) {
                declaration = getDeclarationOfTypeVariable(identifier,
                        (IClassDefinition) genericDecl);
            } else {
                declaration = getDeclarationOfTypeVariable(identifier,
                        genericDecl.getDeclaringClass());
            }
            // just create type variable
            factory.getTypeVariable(identifier, declaration);
        }

        return factory.getTypeVariableReference(identifier, declaration);
    }

    private ITypeReference parseTypeSignature() {
        switch (symbol) {
        case 'B':
            scanSymbol();
            return SigPrimitiveType.BYTE_TYPE;
        case 'C':
            scanSymbol();
            return SigPrimitiveType.CHAR_TYPE;
        case 'D':
            scanSymbol();
            return SigPrimitiveType.DOUBLE_TYPE;
        case 'F':
            scanSymbol();
            return SigPrimitiveType.FLOAT_TYPE;
        case 'I':
            scanSymbol();
            return SigPrimitiveType.INT_TYPE;
        case 'J':
            scanSymbol();
            return SigPrimitiveType.LONG_TYPE;
        case 'S':
            scanSymbol();
            return SigPrimitiveType.SHORT_TYPE;
        case 'Z':
            scanSymbol();
            return SigPrimitiveType.BOOLEAN_TYPE;
        default:
            // Not an elementary type, but a FieldTypeSignature.
            return parseFieldTypeSignature();
        }
    }

    private void parseMethodTypeSignature() {
        // MethodTypeSignature ::= [FormalTypeParameters]
        // "(" {TypeSignature} ")" ReturnType {ThrowsSignature}.

        parseOptFormalTypeParameters();

        parameterTypes = new ArrayList<ITypeReference>(16);
        expect('(');
        while (symbol != ')' && (symbol > 0)) {
            parameterTypes.add(parseTypeSignature());
        }
        expect(')');

        returnType = parseReturnType();

        exceptionTypes = new ArrayList<ITypeReference>(8);
        while (symbol == '^') {
            scanSymbol();

            // ThrowsSignature ::= ("^" ClassTypeSignature) |
            // ("^" TypeVariableSignature).
            if (symbol == 'T') {
                exceptionTypes.add(parseTypeVariableSignature());
            } else {
                exceptionTypes.add(parseClassTypeSignature());
            }
        }
    }

    private ITypeReference parseReturnType() {
        // ReturnType ::= TypeSignature | "V".
        if (symbol != 'V') {
            return parseTypeSignature();
        } else {
            scanSymbol();
            return SigPrimitiveType.VOID_TYPE;
        }
    }


    //
    // Scanner:
    //

    private void scanSymbol() {
        if (!eof) {
            if (pos < buffer.length) {
                symbol = buffer[pos];
                pos++;
            } else {
                symbol = 0;
                eof = true;
            }
        } else {
            throw new GenericSignatureFormatError();
        }
    }

    private void expect(char c) {
        if (symbol == c) {
            scanSymbol();
        } else {
            throw new GenericSignatureFormatError();
        }
    }

    private boolean isStopSymbol(char ch) {
        switch (ch) {
        case ':':
        case '/':
        case ';':
        case '<':
        case '.':
            return true;
        }
        return false;
    }

    // PRE: symbol is the first char of the identifier.
    // POST: symbol = the next symbol AFTER the identifier.
    private void scanIdentifier() {
        if (!eof) {
            StringBuilder identBuf = new StringBuilder(32);
            if (!isStopSymbol(symbol)) {
                identBuf.append(symbol);
                do {
                    char ch = buffer[pos];
                    if ((ch >= 'a') && (ch <= 'z') || (ch >= 'A')
                            && (ch <= 'Z') || !isStopSymbol(ch)) {
                        identBuf.append(buffer[pos]);
                        pos++;
                    } else {
                        identifier = identBuf.toString();
                        scanSymbol();
                        return;
                    }
                } while (pos != buffer.length);
                identifier = identBuf.toString();
                symbol = 0;
                eof = true;
            } else {
                // Ident starts with incorrect char.
                symbol = 0;
                eof = true;
                throw new GenericSignatureFormatError();
            }
        } else {
            throw new GenericSignatureFormatError();
        }
    }
}
