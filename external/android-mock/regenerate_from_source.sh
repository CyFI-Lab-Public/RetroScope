#!/bin/bash
#
# Copyright (C) 2011 The Android Open Source Project.
# This script imports the src, test, etc. from android-mock It DOESN'T commit
# to git giving you a chance to review the changes. Remember that changes in
# bin are normally ignored by git, but we need to force them this case.
# This script doesn't take any parameter.

svn export --force http://android-mock.googlecode.com/svn/trunk/ .
rm lib/easymock.jar
rm lib/junit.jar
rm lib/javassist.jar
rm lib/android/*
# I don't check if there is something on lib on purpose; that way, if
# anything new is added, we will get a visible error.
rmdir lib/android
rmdir lib
