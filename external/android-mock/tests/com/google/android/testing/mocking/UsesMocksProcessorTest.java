/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.android.testing.mocking;

import javassist.CannotCompileException;

import junit.framework.TestCase;

import org.easymock.EasyMock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.tools.JavaFileObject;

/**
 * @author swoodward@google.com (Stephen Woodward)
 */
public class UsesMocksProcessorTest extends TestCase {

  private Set<? extends Element> getAnnotatedElementsSet(Class<?>... classes) {
    Set<Element> set = new HashSet<Element>();
    for (Class<?> clazz : classes) {
      set.add(getMockElement(clazz));
    }
    return set;
  }

  @SuppressWarnings("unchecked")
  private Element getMockElement(Class<?> clazz) {
    Element mockElement = EasyMock.createNiceMock(Element.class);
    EasyMock.expect(mockElement.getAnnotationMirrors()).andReturn(getMockAnnotationMirrors(clazz))
        .anyTimes();
    EasyMock.replay(mockElement);
    return mockElement;
  }

  @SuppressWarnings("unchecked")
  private List getMockAnnotationMirrors(Class<?> clazz) {
    List<AnnotationMirror> mockMirrorList = new ArrayList<AnnotationMirror>();
    AnnotationMirror mockMirror = EasyMock.createNiceMock(AnnotationMirror.class);
    EasyMock.expect(mockMirror.getAnnotationType()).andReturn(getMockAnnotationType()).anyTimes();
    EasyMock.expect(mockMirror.getElementValues()).andReturn(getMockElementValuesMap(clazz))
        .anyTimes();
    EasyMock.replay(mockMirror);
    mockMirrorList.add(mockMirror);
    return mockMirrorList;
  }

  @SuppressWarnings("unchecked")
  private Map getMockElementValuesMap(Class<?> clazz) {
    Map mockValuesMap = new HashMap();
    mockValuesMap.put(getMockExecutableElement(), getMockAnnotationValue(clazz));
    return mockValuesMap;
  }

  private AnnotationValue getMockAnnotationValue(Class<?> clazz) {
    AnnotationValue mockValue = EasyMock.createMock(AnnotationValue.class);
    EasyMock.expect(mockValue.getValue()).andReturn(
        Arrays.asList(new String[] {clazz.getName() + ".class"})).anyTimes();
    EasyMock.replay(mockValue);
    return mockValue;
  }

  private ExecutableElement getMockExecutableElement() {
    ExecutableElement mockElement = EasyMock.createNiceMock(ExecutableElement.class);
    EasyMock.replay(mockElement);
    return mockElement;
  }

  private DeclaredType getMockAnnotationType() {
    return new DeclaredType() {
      @Override
      public String toString() {
        return UsesMocks.class.getName();
      }

      @Override
      public Element asElement() {
        return null;
      }

      @Override
      public TypeMirror getEnclosingType() {
        return null;
      }

      @Override
      public List<? extends TypeMirror> getTypeArguments() {
        return null;
      }

      @Override
      public <R, P> R accept(TypeVisitor<R, P> v, P p) {
        return null;
      }

      @Override
      public TypeKind getKind() {
        return null;
      }
    };
  }

  private UsesMocksProcessor getProcessor() {
    return getProcessor(getMockProcessingEnvironment());
  }

  private UsesMocksProcessor getProcessor(ProcessingEnvironment processingEnv) {
    UsesMocksProcessor processor = new UsesMocksProcessor();
    processor.init(processingEnv);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    processor.logger = new ProcessorLogger(outputStream, processingEnv);
    return processor;
  }

  private ProcessingEnvironment getMockProcessingEnvironment(Filer mockFiler) {
    ProcessingEnvironment mockEnvironment = EasyMock.createNiceMock(ProcessingEnvironment.class);
    EasyMock.expect(mockEnvironment.getMessager()).andReturn(getMockMessager()).anyTimes();
    EasyMock.expect(mockEnvironment.getFiler()).andReturn(mockFiler).anyTimes();
    EasyMock.expect(mockEnvironment.getOptions()).andReturn(getMockOptions()).anyTimes();
    EasyMock.replay(mockEnvironment);
    return mockEnvironment;
  }

  private Map<String, String> getMockOptions() {
    Map<String, String> map = new HashMap<String, String>();
    map.put("bin_dir", ".");
    map.put("logfile", "logfile");
    return map;
  }

  private ProcessingEnvironment getMockProcessingEnvironment() {
    return getMockProcessingEnvironment(getMockFiler());
  }

  private Messager getMockMessager() {
    Messager mockMessager = EasyMock.createNiceMock(Messager.class);
    EasyMock.replay(mockMessager);
    return mockMessager;
  }

  private Filer getMockFiler() {
    try {
      return getMockFiler(getMockFileObject());
    } catch (IOException e) {
      // Can't happen
      throw new RuntimeException(e);
    }
  }

  private Filer getMockFiler(JavaFileObject mockFileObject) {
    Filer mockFiler = EasyMock.createNiceMock(Filer.class);
    try {
      EasyMock.expect(mockFiler.createClassFile((CharSequence) EasyMock.anyObject())).andReturn(
          mockFileObject).anyTimes();
    } catch (IOException e) {
      // Can't happen
      throw new RuntimeException(e);
    }
    EasyMock.replay(mockFiler);
    return mockFiler;
  }

  private JavaFileObject getMockFileObject() throws IOException {
    return getMockFileObject(new ByteArrayOutputStream());
  }

  private JavaFileObject getMockFileObject(OutputStream outStream) throws IOException {
    JavaFileObject mockFileObject = EasyMock.createNiceMock(JavaFileObject.class);
    EasyMock.expect(mockFileObject.openOutputStream()).andReturn(outStream).anyTimes();
    EasyMock.replay(mockFileObject);
    return mockFileObject;
  }

  private RoundEnvironment getMockRoundEnvironment(Set<? extends Element> elementsWithAnnotation) {
    return getMockRoundEnvironment(elementsWithAnnotation, false);
  }

  @SuppressWarnings("unchecked")
  private RoundEnvironment getMockRoundEnvironment(Set<? extends Element> elementsWithAnnotation,
      boolean finishedProcessing) {
    RoundEnvironment mockEnv = EasyMock.createNiceMock(RoundEnvironment.class);
    EasyMock.expect(mockEnv.getElementsAnnotatedWith(UsesMocks.class)).andReturn(
        (Set) elementsWithAnnotation).anyTimes();
    EasyMock.expect(mockEnv.processingOver()).andReturn(finishedProcessing).anyTimes();
    EasyMock.replay(mockEnv);
    return mockEnv;
  }

  public void testGetClassMocks() throws IOException, CannotCompileException {
    List<Class<?>> classesToMock = new ArrayList<Class<?>>();
    classesToMock.add(TestCase.class);
    List<String> expectedMocks =
        new ArrayList<String>(Arrays.asList(new String[] {
            "genmocks." + TestCase.class.getName() + "DelegateInterface",
            "genmocks." + TestCase.class.getName() + "DelegateSubclass"}));
    Set<GeneratedClassFile> mockedClasses =
        getProcessor().getClassMocks(classesToMock, true);

    assertEquals(2, mockedClasses.size());
    for (GeneratedClassFile clazz : mockedClasses) {
      assertTrue(expectedMocks.contains(clazz.getClassName()));
      expectedMocks.remove(clazz.getClassName());
    }
  }

  public void testWriteMocks() throws IOException, CannotCompileException {
    List<Class<?>> classesToMock = new ArrayList<Class<?>>();
    classesToMock.add(TestCase.class);
    Set<GeneratedClassFile> mockedClassesSet =
        getProcessor().getClassMocks(classesToMock, true);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    getProcessor(getMockProcessingEnvironment(getMockFiler(getMockFileObject(outputStream))))
        .writeMocks(mockedClassesSet);

    String output = new String(outputStream.toByteArray());
    for (GeneratedClassFile mockClass : mockedClassesSet) {
      String expected = new String(mockClass.getContents());
      assertTrue(output.contains(expected));
      output = output.replace(expected, "");
    }
    assertEquals(0, output.length());
  }

  public void testProcess() {
    assertFalse(getProcessor().process(null,
        getMockRoundEnvironment(getAnnotatedElementsSet(TestCase.class))));
    assertFalse(getProcessor().process(null,
        getMockRoundEnvironment(getAnnotatedElementsSet(TestCase.class), true)));
  }

  public void testFindClassesToMock() {
    Set<? extends Element> annotatedElements = getAnnotatedElementsSet(Set.class, TestCase.class);
    List<Class<?>> classesList = getProcessor().findClassesToMock(annotatedElements);

    assertEquals(annotatedElements.size(), classesList.size());
    assertTrue(classesList.contains(Set.class));
    assertTrue(classesList.contains(TestCase.class));
  }
}
