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

package signature.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import signature.converter.util.AbstractConvertTest;
import signature.converter.util.CompilationUnit;
import signature.model.IApi;
import signature.model.IClassDefinition;
import signature.model.IClassReference;
import signature.model.IField;
import signature.model.IPackage;
import signature.model.IParameterizedType;
import signature.model.ITypeReference;
import signature.model.IWildcardType;
import signature.model.util.ModelUtil;

import java.io.IOException;

public abstract class ConvertWildcardTest extends AbstractConvertTest {
    
    @Test
    public void convertWildcardUpperBound() throws IOException {
        String source = 
            "package a; " +
            "public class A{" +
            "  public java.util.Set<? extends Number> f; "+
            "}";
            IApi api = convert(new CompilationUnit("a.A", source));
            IPackage sigPackage = ModelUtil.getPackage(api, "a");
            IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
            IField field = ModelUtil.getField(sigClass, "f");
            
            ITypeReference type = field.getType();
            assertTrue(type instanceof IParameterizedType);
            
            IParameterizedType parametrizedType = (IParameterizedType)type;
            
            IClassDefinition rawType = parametrizedType.getRawType().getClassDefinition();
            assertEquals("Set", rawType.getName());
            
            assertEquals(1, parametrizedType.getTypeArguments().size());
            IWildcardType wildcardType = (IWildcardType) parametrizedType.getTypeArguments().get(0);
            assertEquals(1, wildcardType.getUpperBounds().size());
            ITypeReference upperBound = wildcardType.getUpperBounds().get(0);
            assertTrue(upperBound instanceof IClassReference);
            
            assertEquals("Number", ((IClassReference)upperBound).getClassDefinition().getName());
    }    
    
    @Test
    public void convertWildcardLowerBound() throws IOException {
        String source = 
        "package a; " +
        "public class A{" +
        "  public java.util.Set<? super Number> f; "+
        "}";
        IApi api = convert(new CompilationUnit("a.A", source));
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        IField field = ModelUtil.getField(sigClass, "f");
        
        ITypeReference type = field.getType();
        assertTrue(type instanceof IParameterizedType);
        
        IParameterizedType parametrizedType = (IParameterizedType)type;
        
        IClassDefinition rawType = parametrizedType.getRawType().getClassDefinition();
        assertEquals("Set", rawType.getName());
        
        assertEquals(1, parametrizedType.getTypeArguments().size());
        IWildcardType wildcardType = (IWildcardType) parametrizedType.getTypeArguments().get(0);
        assertEquals(1, wildcardType.getUpperBounds().size());
        ITypeReference upperBound = wildcardType.getUpperBounds().get(0);
        assertTrue(upperBound instanceof IClassReference);
        assertEquals("Object", ((IClassReference)upperBound).getClassDefinition().getName());
        
        ITypeReference lowerBound = wildcardType.getLowerBound();
        assertTrue(lowerBound instanceof IClassReference);
        assertEquals("Number", ((IClassReference)lowerBound).getClassDefinition().getName());
    }
}
