#!/bin/bash
#
# Copyright 2013 The Android Open Source Project.
#
# Retrieves the current Mockito source code into the current direcory, exlcuding portions related
# to constructing Mock objects in the JVM.

SOURCE="git://github.com/mockito/mockito.git"
INCLUDE="
    LICENSE
    src
    "

EXCLUDE="
    src/org/mockito/internal/creation/cglib
    src/org/mockito/internal/creation/jmock
    src/org/mockito/internal/creation/AbstractMockitoMethodProxy.java
    src/org/mockito/internal/creation/AcrossJVMSerializationFeature.java
    src/org/mockito/internal/creation/CglibMockMaker.java
    src/org/mockito/internal/creation/DelegatingMockitoMethodProxy.java
    src/org/mockito/internal/creation/MethodInterceptorFilter.java
    src/org/mockito/internal/creation/MockitoMethodProxy.java
    src/org/mockito/internal/creation/SerializableMockitoMethodProxy.java
    src/org/mockito/internal/invocation/realmethod/FilteredCGLIBProxyRealMethod.java
    src/org/mockito/internal/invocation/realmethod/CGLIBProxyRealMethod.java
    src/org/mockito/internal/invocation/realmethod/HasCGLIBMethodProxy.java
    "

working_dir="$(mktemp -d)"
trap "echo \"Removing temporary directory\"; rm -rf $working_dir" EXIT

echo "Fetching Mockito source into $working_dir"
git clone $SOURCE $working_dir/source

for include in ${INCLUDE}; do
  echo "Updating $include"
  rm -rf $include
  cp -R $working_dir/source/$include .
done;

for exclude in ${EXCLUDE}; do
  echo "Excluding $exclude"
  rm -r $exclude
done;

echo "Done"

