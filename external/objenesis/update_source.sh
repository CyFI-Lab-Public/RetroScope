#!/bin/bash
#
# Copyright 2013 The Android Open Source Project.
#
# Retrieves the current Objenesis source code into the current directory.
# Does not create a GIT commit.

SOURCE="http://objenesis.googlecode.com/svn/trunk/"
INCLUDE="
    LICENSE.txt
    main
    tck
    tck-android
    "

working_dir="$(mktemp -d)"
trap "echo \"Removing temporary directory\"; rm -rf $working_dir" EXIT

echo "Fetching Objenesis source into $working_dir"
svn export -q $SOURCE $working_dir/source

for include in ${INCLUDE}; do
  echo "Updating $include"
  rm -rf $include
  cp -R $working_dir/source/$include .
done;

echo "Done"

